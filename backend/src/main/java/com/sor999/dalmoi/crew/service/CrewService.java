package com.sor999.dalmoi.crew.service;

import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.crew.domain.CrewMember;
import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.crew.repository.CrewMemberRepository;
import com.sor999.dalmoi.crew.repository.CrewRepository;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.member.repository.MemberRepository;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.repository.RunningSessionRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrewService {

    private final CrewRepository crewRepository;

    private final CrewSummaryService crewSummaryService;

    private final RunningSessionRepository runningSessionRepository;

    private final CrewMemberRepository crewMemberRepository;

    private final MemberRepository memberRepository;

    @Transactional
    public Crew create(Long memberId, String name) {
        Crew crew = crewRepository.save(Crew.create(memberRepository.getReferenceById(memberId), name.strip(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT)));
        crewMemberRepository.save(CrewMember.create(crew, memberRepository.getReferenceById(memberId)));

        return crew;
    }

    @Transactional
    public CrewMember join(Long memberId, String inviteCode) {
        Crew crew = crewRepository.findByInviteCode(inviteCode.strip().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (isMember(crew.getId(), memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_CREW_MEMBER);
        }

        if (count(crew.getId()) >= crew.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CREW_CAPACITY_EXCEEDED);
        }

        return crewMemberRepository.save(CrewMember.create(crew, memberRepository.getReferenceById(memberId)));
    }

    public Crew requireMember(Long crewId, Long memberId) {
        Crew crew = crewRepository.findById(crewId).orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));

        if (!isMember(crewId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }

        return crew;
    }

    public Crew requireLockedMember(Long crewId, Long memberId) {
        Crew crew = crewRepository.findLockedById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        if (!isMember(crewId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }
        return crew;
    }

    public void requireOwner(Crew crew, Long memberId) {
        if (!crew.getOwner().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_OWNER);
        }
    }

    public boolean isMember(Long crewId, Long memberId) {
        return crewMemberRepository.existsByCrewIdAndMemberIdAndLeftAtIsNull(crewId, memberId);
    }

    public long count(Long crewId) {
        return crewMemberRepository.countByCrewIdAndLeftAtIsNull(crewId);
    }

    public List<CrewMember> members(Long crewId) {
        return crewMemberRepository.findByCrewIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(crewId);
    }

    public List<CrewMember> mine(Long memberId) {
        return crewMemberRepository.findByMemberIdAndLeftAtIsNullOrderByJoinedAtDescIdDesc(memberId);
    }

    @Transactional
    public CrewResponse.Created createResponse(Long memberId, String name) {
        return CrewResponse.Created.from(create(memberId, name));
    }

    @Transactional
    public CrewResponse.Joined joinResponse(Long memberId, String code) {
        CrewMember cm = join(memberId, code);

        return new CrewResponse.Joined(cm.getCrew().getId(), cm.getCrew().getName(), cm.getId(), cm.getMember().getId(),
                cm.getJoinedAt());
    }

    public List<CrewResponse> mineResponse(Long memberId) {
        return crewSummaryService.mine(memberId);
    }

    public CrewResponse detailResponse(Long memberId, Long crewId) {
        return response(requireMember(crewId, memberId), memberId, true);
    }

    public CrewResponse.InviteCode inviteResponse(Long memberId, Long crewId) {
        Crew crew = requireMember(crewId, memberId);
        requireOwner(crew, memberId);

        return new CrewResponse.InviteCode(crewId, crew.getInviteCode());
    }

    private CrewResponse response(Crew crew, Long memberId, boolean includeMembers) {
        List<CrewResponse.MemberSummary> members = includeMembers
                ? members(crew.getId()).stream()
                        .map(cm -> new CrewResponse.MemberSummary(cm.getMember().getId(), cm.getMember().getNickname(),
                                crew.getOwner().getId().equals(cm.getMember().getId()) ? "OWNER" : "MEMBER"))
                        .toList()
                : List.of();
        CrewResponse.WaitingSession waiting = runningSessionRepository
                .findFirstByCrewIdAndStatusOrderByCreatedAtDescIdDesc(crew.getId(), SessionStatus.WAITING)
                .map(s -> new CrewResponse.WaitingSession(s.getId(), s.getTitle()))
                .orElse(null);

        return new CrewResponse(crew.getId(), crew.getName(), crew.getOwner().getId(), count(crew.getId()),
                crew.getMaxMembers(), crew.getOwner().getId().equals(memberId) ? "OWNER" : "MEMBER", members, waiting);
    }
}
