package com.sor999.dalmoi.gps.service;

import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocketAuthorizationService {

    private final SessionParticipantRepository sessionParticipantRepository;

    private static final Pattern DESTINATION = Pattern.compile("^/topic/sessions/([0-9]+)/(locations|status)$");

    @Transactional(readOnly = true)
    public void subscribe(Long memberId, String destination) {
        Matcher match = DESTINATION.matcher(destination == null ? "" : destination);

        if (!match.matches()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        SessionParticipant participant = sessionParticipantRepository
                .findBySessionIdAndMemberId(Long.valueOf(match.group(1)), memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_SESSION_PARTICIPANT));

        if (match.group(2).equals("locations") && (participant.getStatus() != ParticipantStatus.RUNNING
                || participant.getSession().getStatus() != SessionStatus.RUNNING)) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_RUNNING);
        }

        if (participant.getStatus() == ParticipantStatus.ABSENT
                || participant.getSession().getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
