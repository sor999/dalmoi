package com.sor999.dalmoi.crew.service;

import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.crew.domain.CrewMember;
import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.crew.repository.CrewMemberRepository;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.repository.RunningSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewSummaryService {

    private final CrewMemberRepository crewMemberRepository;

    private final RunningSessionRepository runningSessionRepository;

    public List<CrewResponse> mine(Long memberId) {
        List<CrewMember> memberships = crewMemberRepository
                .findByMemberIdAndLeftAtIsNullOrderByJoinedAtDescIdDesc(memberId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> ids = memberships.stream().map(cm -> cm.getCrew().getId()).toList();
        Map<Long, Long> counts = crewMemberRepository.countMembers(ids)
                .stream()
                .collect(Collectors.toMap(CrewMemberRepository.CrewCount::getCrewId,
                        CrewMemberRepository.CrewCount::getTotal));
        Map<Long, RunningSession> waiting = runningSessionRepository
                .findByCrewIdInAndStatus(ids, SessionStatus.WAITING)
                .stream()
                .collect(Collectors.toMap(s -> s.getCrew().getId(), Function.identity(),
                        (first, second) -> first.getCreatedAt().isAfter(second.getCreatedAt())
                                || (first.getCreatedAt().equals(second.getCreatedAt())
                                        && first.getId() > second.getId()) ? first : second));
        return memberships.stream().map(cm -> {
            Crew crew = cm.getCrew();
            RunningSession session = waiting.get(crew.getId());
            CrewResponse.WaitingSession room = session == null
                    ? null
                    : new CrewResponse.WaitingSession(
                            session.getId(), session.getTitle());
            return new CrewResponse(crew.getId(), crew.getName(), crew.getOwner().getId(),
                    counts.getOrDefault(crew.getId(), 0L), crew.getMaxMembers(),
                    crew.getOwner().getId().equals(memberId) ? "OWNER" : "MEMBER", List.of(), room);
        }).toList();

    }
}
