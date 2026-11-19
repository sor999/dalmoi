package com.sor999.dalmoi.session.service;

import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.course.repository.CourseRepository;
import com.sor999.dalmoi.session.event.SessionChanged;
import com.sor999.dalmoi.course.service.GeoSupport;
import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.crew.repository.CrewRepository;
import com.sor999.dalmoi.crew.repository.CrewMemberRepository;
import com.sor999.dalmoi.gps.repository.GpsEventRepository;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.member.repository.MemberRepository;
import com.sor999.dalmoi.record.domain.RecordJob;
import com.sor999.dalmoi.record.repository.RecordJobRepository;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.dto.SessionResponse;
import com.sor999.dalmoi.session.repository.RunningSessionRepository;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final RunningSessionRepository runningSessionRepository;

    private final SessionParticipantRepository sessionParticipantRepository;

    private final MemberRepository memberRepository;

    private final CrewRepository crewRepository;

    private final CrewMemberRepository crewMemberRepository;

    private final CourseRepository courseRepository;

    private final GpsEventRepository gpsEventRepository;

    private final RecordJobRepository recordJobRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Clock clock;

    @Transactional
    public SessionResponse create(Long memberId, Long crewId, String title, Long courseId) {
        Crew crew = requireLockedMember(crewId, memberId);
        requireOwner(crew, memberId);
        if (runningSessionRepository.existsByCrewIdAndStatusIn(crewId,
                List.of(SessionStatus.WAITING, SessionStatus.RUNNING))) {
            throw new BusinessException(ErrorCode.ACTIVE_SESSION_EXISTS);
        }
        Course course = requireCrewCourse(courseId, crewId);
        RunningSession session = runningSessionRepository
                .save(RunningSession.create(crew, memberRepository.getReferenceById(memberId), course, title.strip()));

        sessionParticipantRepository.saveAndFlush(SessionParticipant.create(session,
                memberRepository.getReferenceById(memberId)));
        return detail(memberId, session.getId());
    }

    public List<SessionResponse.Summary> list(Long memberId, Long crewId, SessionStatus status) {
        requireMember(crewId, memberId);

        return runningSessionRepository.findByCrewIdOrderByCreatedAtDescIdDesc(crewId)
                .stream()
                .filter(s -> status == null || s.getStatus() == status)
                .map(s -> new SessionResponse.Summary(s.getId(), s.getTitle(), s.getStatus().name(),
                        s.getCourse().getId(), sessionParticipantRepository.countBySessionId(s.getId())))
                .toList();
    }

    public SessionResponse detail(Long memberId, Long sessionId) {
        RunningSession session = requireSession(sessionId);
        Crew crew = requireMember(session.getCrew().getId(), memberId);
        List<SessionParticipant> participants = sessionParticipantRepository
                .findBySessionIdOrderByJoinedAtAscIdAsc(sessionId);
        Map<Long, SessionParticipant> byMember = participants.stream()
                .collect(Collectors.toMap(p -> p.getMember().getId(), Function.identity()));
        List<SessionResponse.MemberStatus> members = crewMemberRepository
                .findByCrewIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(crew.getId())
                .stream()
                .map(cm -> {
                    SessionParticipant p = byMember.get(cm.getMember().getId());

                    return new SessionResponse.MemberStatus(cm.getMember().getId(), cm.getMember().getNickname(),
                            crew.getOwner().getId().equals(cm.getMember().getId()) ? "OWNER" : "MEMBER",
                            p == null ? "NOT_JOINED" : p.getStatus().name());
                })
                .toList();
        SessionParticipant me = byMember.get(memberId);

        return new SessionResponse(sessionId, crew.getId(), session.getCrewNameSnapshot(), session.getTitle(),
                session.getStatus().name(),
                new SessionResponse.CourseSnapshot(session.getCourse().getId(), session.getPlannedCourseTitle(),
                        GeoSupport.km(session.getPlannedDistanceMeters()), session.isPlannedLoop(),
                        GeoSupport.points(session.getPlannedRoute())),
                members.size(), participants.size(), members,
                participants.stream().map(this::participantResponse).toList(),
                me == null ? "NOT_JOINED" : me.getStatus().name(), me == null ? null : me.getStartedAt(),
                crew.getOwner().getId().equals(memberId) && session.getStatus() == SessionStatus.WAITING
                        && !participants.isEmpty(),
                session.getStatus() == SessionStatus.WAITING && me == null, session.getCreatedAt(),
                session.getStartedAt(), session.getCompletedAt());
    }

    public List<SessionResponse.Participant> participants(Long memberId, Long sessionId) {
        requireMember(requireSession(sessionId).getCrew().getId(), memberId);

        return sessionParticipantRepository.findBySessionIdOrderByJoinedAtAscIdAsc(sessionId)
                .stream()
                .map(this::participantResponse)
                .toList();
    }

    @Transactional
    public SessionResponse.Participant join(Long memberId, Long sessionId) {
        RunningSession session = locked(sessionId);
        requireMember(session.getCrew().getId(), memberId);

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new BusinessException(ErrorCode.SESSION_NOT_WAITING);
        }

        if (sessionParticipantRepository.existsBySessionIdAndMemberId(sessionId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATING);
        }

        SessionParticipant participant = sessionParticipantRepository
                .save(SessionParticipant.create(session, memberRepository.getReferenceById(memberId)));
        eventPublisher.publishEvent(new SessionChanged(sessionId, "PARTICIPANT_JOINED", memberId));

        return participantResponse(participant);
    }

    @Transactional
    public SessionResponse.Command start(Long memberId, Long sessionId) {
        RunningSession session = locked(sessionId);
        requireOwner(session.getCrew(), memberId);

        if (sessionParticipantRepository.countBySessionId(sessionId) == 0) {
            throw new BusinessException(ErrorCode.NO_PARTICIPANTS);
        }

        session.start(clock.instant());
        eventPublisher.publishEvent(new SessionChanged(sessionId, "SESSION_STARTED", memberId));

        return command(session);
    }

    @Transactional
    public SessionResponse.Personal startMe(Long memberId, Long sessionId) {
        memberRepository.findLockedById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        RunningSession session = locked(sessionId);

        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new BusinessException(ErrorCode.SESSION_NOT_RUNNING);
        }

        SessionParticipant participant = lockedParticipant(sessionId, memberId);

        if (participant.getStatus() != ParticipantStatus.RUNNING
                && sessionParticipantRepository.existsByMemberIdAndStatus(memberId, ParticipantStatus.RUNNING)) {
            throw new BusinessException(ErrorCode.ALREADY_RUNNING_OTHER_SESSION);
        }

        participant.start(clock.instant());
        eventPublisher.publishEvent(new SessionChanged(sessionId, "PARTICIPANT_STARTED", memberId));

        return personal(participant, null);
    }

    @Transactional
    public SessionResponse.Personal finishMe(Long memberId, Long sessionId) {
        locked(sessionId);
        SessionParticipant participant = lockedParticipant(sessionId, memberId);
        participant.finish(clock.instant());

        if (!recordJobRepository.existsById(participant.getId())) {
            recordJobRepository.saveAndFlush(
                    RecordJob.create(participant, gpsEventRepository.maxIngestOrder(participant.getId()),
                            clock.instant()));
        }

        eventPublisher
                .publishEvent(new SessionChanged(sessionId, "PARTICIPANT_FINISHED", memberId, participant.getId()));

        return personal(participant,
                recordJobRepository.findById(participant.getId()).orElseThrow().getStatus().name());
    }

    @Transactional
    public SessionResponse.Command complete(Long memberId, Long sessionId) {
        RunningSession session = locked(sessionId);
        requireOwner(session.getCrew(), memberId);
        List<SessionParticipant> participants = sessionParticipantRepository
                .findBySessionIdOrderByJoinedAtAscIdAsc(sessionId);

        if (participants.stream().anyMatch(p -> p.getStatus() == ParticipantStatus.RUNNING)) {
            throw new BusinessException(ErrorCode.ACTIVE_PARTICIPANTS_EXIST);
        }

        session.complete(clock.instant());
        participants.forEach(SessionParticipant::markAbsent);
        eventPublisher.publishEvent(new SessionChanged(sessionId, "SESSION_COMPLETED", memberId));

        return command(session);
    }

    public RunningSession requireSession(Long id) {
        return runningSessionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private Crew requireMember(Long crewId, Long memberId) {
        Crew crew = crewRepository.findById(crewId).orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        validateMember(crewId, memberId);
        return crew;
    }

    private Crew requireLockedMember(Long crewId, Long memberId) {
        Crew crew = crewRepository.findLockedById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        validateMember(crewId, memberId);
        return crew;
    }

    private void validateMember(Long crewId, Long memberId) {
        if (!crewMemberRepository.existsByCrewIdAndMemberIdAndLeftAtIsNull(crewId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }
    }

    private void requireOwner(Crew crew, Long memberId) {
        if (!crew.getOwner().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_OWNER);
        }
    }

    private Course requireCrewCourse(Long courseId, Long crewId) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (!crewMemberRepository.existsByCrewIdAndMemberIdAndLeftAtIsNull(crewId, course.getCreator().getId())) {
            throw new BusinessException(ErrorCode.COURSE_NOT_ACCESSIBLE);
        }
        return course;
    }

    private RunningSession locked(Long id) {
        return runningSessionRepository.findLockedById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private SessionParticipant lockedParticipant(Long sid, Long mid) {
        return sessionParticipantRepository.findLockedBySessionAndMember(sid, mid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_SESSION_PARTICIPANT));
    }

    private SessionResponse.Participant participantResponse(SessionParticipant p) {
        return new SessionResponse.Participant(p.getId(), p.getMember().getId(), p.getMember().getNickname(),
                p.getStatus().name(), p.getJoinedAt(), p.getStartedAt(), p.getEndedAt());
    }

    private SessionResponse.Command command(RunningSession s) {
        return new SessionResponse.Command(s.getId(), s.getStatus().name(), s.getStartedAt(), s.getCompletedAt());
    }

    private SessionResponse.Personal personal(SessionParticipant p, String status) {
        return new SessionResponse.Personal(p.getId(), p.getSession().getId(), p.getStatus().name(), p.getStartedAt(),
                p.getEndedAt(), status);
    }
}
