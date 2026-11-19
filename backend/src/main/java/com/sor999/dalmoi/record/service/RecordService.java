package com.sor999.dalmoi.record.service;

import com.sor999.dalmoi.record.dto.RecordResponse;
import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.record.domain.RunningRecord;
import com.sor999.dalmoi.record.repository.RecordJobRepository;
import com.sor999.dalmoi.record.repository.RunningRecordRepository;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.global.response.PageResponse;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final RunningRecordRepository runningRecordRepository;

    private final RecordJobRepository recordJobRepository;

    private final GpsEventRepository gpsEventRepository;

    private final SessionParticipantRepository sessionParticipantRepository;

    public PageResponse<RecordResponse.Summary> mine(Long memberId, int page, int size) {
        return PageResponse.from(runningRecordRepository
                .findByParticipantMemberIdOrderByCreatedAtDescIdDesc(memberId, PageRequest.of(page, size))
                .map(RecordResponse.Summary::from));
    }

    public RecordResponse detail(Long memberId, Long recordId) {
        RunningRecord record = runningRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_FOUND));

        if (!record.getParticipant().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND);
        }

        return RecordResponse.of(record, gpsEventRepository.findByParticipantIdAndValidationStatusOrderBySequenceAsc(
                record.getParticipant().getId(), GpsValidationStatus.VALID));
    }

    public RecordResponse.Status status(Long memberId, Long sessionId) {
        SessionParticipant participant = sessionParticipantRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_SESSION_PARTICIPANT));
        RecordJob job = recordJobRepository.findById(participant.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_REQUESTED));
        Long recordId = runningRecordRepository.findByParticipantId(participant.getId())
                .map(RunningRecord::getId)
                .orElse(null);

        return new RecordResponse.Status(participant.getId(), job.getStatus().name(), recordId, job.getErrorCode());
    }
}
