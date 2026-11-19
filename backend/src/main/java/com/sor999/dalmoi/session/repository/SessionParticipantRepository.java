package com.sor999.dalmoi.session.repository;

import com.sor999.dalmoi.session.domain.ParticipantStatus;
import com.sor999.dalmoi.session.domain.SessionParticipant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, Long> {

    Optional<SessionParticipant> findBySessionIdAndMemberId(Long sessionId, Long memberId);

    List<SessionParticipant> findBySessionIdOrderByJoinedAtAscIdAsc(Long sessionId);

    boolean existsBySessionIdAndMemberId(Long sessionId, Long memberId);

    boolean existsByMemberIdAndStatus(Long memberId, ParticipantStatus status);

    long countBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SessionParticipant p where p.id = :id")
    Optional<SessionParticipant> findLockedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SessionParticipant p where p.session.id = :sessionId and p.member.id = :memberId")
    Optional<SessionParticipant> findLockedBySessionAndMember(Long sessionId, Long memberId);
}
