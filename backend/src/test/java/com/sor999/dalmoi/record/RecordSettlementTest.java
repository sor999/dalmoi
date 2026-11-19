package com.sor999.dalmoi.record;

import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.record.domain.RecordJobStatus;
import com.sor999.dalmoi.record.repository.RecordJobRepository;
import com.sor999.dalmoi.record.repository.RunningRecordRepository;
import com.sor999.dalmoi.record.service.RecordJobFailureService;
import com.sor999.dalmoi.record.service.RecordJobWorker;
import com.sor999.dalmoi.record.service.RecordProcessingService;
import com.sor999.dalmoi.record.service.RecordService;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresIntegrationTest
class RecordSettlementTest extends MvpDatabaseTest {

    @Autowired
    private RecordJobRepository recordJobRepository;

    @Autowired
    private RunningRecordRepository runningRecordRepository;

    @Autowired
    private RecordProcessingService recordProcessingService;

    @Autowired
    private RecordJobFailureService recordJobFailureService;

    @Autowired
    private RecordService recordService;

    @Test
    @DisplayName("새 워커가 DB 작업을 재개하고 중복 기록을 만들지 않으며 타인 조회를 차단한다")
    void run_persistedJob_completesOnceAndProtectsRecord() {
        Scenario s = running();
        storedGps(s, 0);
        enqueueRecord(s);
        worker().run();
        worker().run();
        RecordJob job = recordJobRepository.findById(s.participantId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(RecordJobStatus.COMPLETED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        Long id = recordService.status(s.memberId(), s.sessionId()).recordId();
        assertThat(recordService.detail(s.memberId(), id).actualRoute()).hasSize(1);
        assertThat(recordService.mine(s.memberId(), 0, 20).items()).hasSize(1);
        Long outsider = member();
        assertThatThrownBy(() -> recordService.detail(outsider, id)).isInstanceOf(BusinessException.class);
        assertThat(jdbcTemplate.queryForObject("select count(*) from running_records where participant_id=?",
                Long.class, s.participantId())).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 GPS 경로도 거리 0으로 정산한다")
    void run_emptyGps_completesWithZeroDistance() {
        Scenario s = running();
        enqueueRecord(s);
        worker().run();
        assertThat(runningRecordRepository.findByParticipantId(s.participantId()).orElseThrow().getDistanceMeters())
                .isZero();
    }

    @Test
    @DisplayName("정산 실패 후 간격을 지켜 재시도하고 세 번 실패하면 FAILED를 유지한다")
    void run_repeatedFailure_retriesAtIntervalsAndStopsAfterThree() {
        Scenario s = running();
        storedGps(s, 0);
        enqueueRecord(s);
        jdbcTemplate.update(
                "update gps_events set validation_status='PENDING',processed_at=null where participant_id=?",
                s.participantId());
        worker().run();
        worker().run();
        assertThat(recordJobRepository.findById(s.participantId()).orElseThrow().getAttemptCount()).isEqualTo(1);
        for (int i = 0; i < 2; i++) {
            jdbcTemplate.update("update record_jobs set updated_at=now()-interval '6 seconds' where participant_id=?",
                    s.participantId());
            worker().run();
        }
        worker().run();
        RecordJob job = recordJobRepository.findById(s.participantId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(RecordJobStatus.FAILED);
        assertThat(job.getAttemptCount()).isEqualTo(3);
        assertThat(job.getErrorCode()).isEqualTo("RECORD_PROCESSING_FAILED");
        assertThat(runningRecordRepository.findByParticipantId(s.participantId())).isEmpty();
    }

    @Test
    @DisplayName("실패한 작업의 원인을 복구하면 다음 시도에서 정상 완료한다")
    void run_transientFailure_recoversWithoutUndoingFinish() {
        Scenario s = running();
        storedGps(s, 0);
        enqueueRecord(s);
        jdbcTemplate.update(
                "update gps_events set validation_status='PENDING',processed_at=null where participant_id=?",
                s.participantId());
        worker().run();
        jdbcTemplate.update("update gps_events set validation_status='VALID',processed_at=now() where participant_id=?",
                s.participantId());
        jdbcTemplate.update("update record_jobs set updated_at=now()-interval '6 seconds' where participant_id=?",
                s.participantId());
        worker().run();
        assertThat(recordJobRepository.findById(s.participantId()).orElseThrow().getStatus())
                .isEqualTo(RecordJobStatus.COMPLETED);
        assertThat(jdbcTemplate.queryForObject("select status from session_participants where id=?", String.class,
                s.participantId())).isEqualTo("FINISHED");
    }

    private RecordJobWorker worker() {
        return new RecordJobWorker(recordJobRepository, recordProcessingService, recordJobFailureService, clock);
    }
}
