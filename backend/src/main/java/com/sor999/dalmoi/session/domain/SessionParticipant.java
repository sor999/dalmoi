package com.sor999.dalmoi.session.domain;

import com.sor999.dalmoi.member.domain.Member;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "session_participants", uniqueConstraints = @UniqueConstraint(name = "uq_session_participants_session_member", columnNames = {
        "session_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private RunningSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ParticipantStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private SessionParticipant(RunningSession session, Member member) {
        this.session = session;
        this.member = member;
        this.status = ParticipantStatus.JOINED;
        this.joinedAt = Instant.now();
    }

    public void start(Instant now) {
        if (status == ParticipantStatus.RUNNING) {
            return;
        }

        if (status != ParticipantStatus.JOINED) {
            throw new BusinessException(ErrorCode.PARTICIPANT_ALREADY_FINISHED);
        }

        status = ParticipantStatus.RUNNING;
        startedAt = now;
    }

    public void finish(Instant now) {
        if (status == ParticipantStatus.FINISHED) {
            return;
        }

        if (status != ParticipantStatus.RUNNING) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_RUNNING);
        }

        status = ParticipantStatus.FINISHED;
        endedAt = now;
    }

    public void markAbsent() {
        if (status == ParticipantStatus.JOINED) {
            status = ParticipantStatus.ABSENT;
        }
    }

    public static SessionParticipant create(RunningSession session, Member member) {
        return builder()
                .session(session)
                .member(member)
                .build();
    }
}
