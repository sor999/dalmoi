package com.sor999.dalmoi.record.service;

import com.sor999.dalmoi.record.repository.RecordJobRepository;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordJobFailureService {

    private final SessionParticipantRepository sessionParticipantRepository;

    private final RecordJobRepository recordJobRepository;

    private final Clock clock;

    @Transactional
    public void fail(Long participantId) {
        sessionParticipantRepository.findLockedById(participantId).orElseThrow();
        recordJobRepository.findLockedById(participantId).orElseThrow().fail(clock.instant());
    }
}
