package com.sor999.dalmoi.session.repository;

import java.util.Collection;
import com.sor999.dalmoi.session.domain.SessionStatus;

import com.sor999.dalmoi.session.domain.RunningSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface RunningSessionRepository extends JpaRepository<RunningSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RunningSession s where s.id = :id")
    Optional<RunningSession> findLockedById(Long id);

    Optional<RunningSession> findFirstByCrewIdAndStatusOrderByCreatedAtDescIdDesc(Long crewId,
            SessionStatus status);

    List<RunningSession> findByCrewIdInAndStatus(Collection<Long> crewIds,
            SessionStatus status);

    boolean existsByCrewIdAndStatusIn(Long crewId, Collection<SessionStatus> statuses);

    List<RunningSession> findByCrewIdOrderByCreatedAtDescIdDesc(Long crewId);

    @Query("select p.session from SessionParticipant p where p.member.id = :memberId and p.session.status = SessionStatus.WAITING order by p.session.createdAt desc, p.session.id desc")
    List<RunningSession> findMyWaiting(Long memberId, Pageable pageable);
}
