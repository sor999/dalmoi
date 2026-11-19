package com.sor999.dalmoi.record.service;

import com.sor999.dalmoi.course.service.GeoSupport;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.record.domain.RecordJobStatus;
import com.sor999.dalmoi.record.domain.RunningRecord;
import com.sor999.dalmoi.record.repository.RecordJobRepository;
import com.sor999.dalmoi.record.repository.RunningRecordRepository;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordProcessingService {

    private final RecordJobRepository recordJobRepository;

    private final RunningRecordRepository runningRecordRepository;

    private final GpsEventRepository gpsEventRepository;

    private final SessionParticipantRepository sessionParticipantRepository;

    private final Clock clock;

    @Transactional
    public void process(Long participantId) {
        SessionParticipant participant = sessionParticipantRepository.findLockedById(participantId).orElseThrow();
        RecordJob job = recordJobRepository.findLockedById(participantId).orElseThrow();

        if (!job.ready(clock.instant())) {
            return;
        }

        List<GpsEvent> events = gpsEventRepository.findByParticipantIdAndIngestOrderLessThanEqualOrderBySequenceAsc(
                participantId, job.getCutoffIngestOrder());

        if (events.stream().anyMatch(e -> e.getValidationStatus() == GpsValidationStatus.PENDING)) {
            throw new IllegalStateException("검증되지 않은 GPS 이벤트가 있습니다.");
        }

        List<GpsEvent> valid = events.stream()
                .filter(e -> e.getValidationStatus() == GpsValidationStatus.VALID)
                .toList();
        double distance = 0;

        for (int i = 1; i < valid.size(); i++) {
            distance += GeoSupport.distance(valid.get(i - 1).getLocation().getCoordinate(),
                    valid.get(i).getLocation().getCoordinate());
        }

        if (runningRecordRepository.findByParticipantId(participantId).isEmpty()) {
            runningRecordRepository.save(RunningRecord.create(participant, distance,
                    Duration.between(participant.getStartedAt(), participant.getEndedAt()).toSeconds(), "gps-v1"));
        }

        job.complete(clock.instant());
    }
}
