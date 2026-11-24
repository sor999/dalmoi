package com.sor999.dalmoi.record.repository;

import com.sor999.dalmoi.record.domain.RunningRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {

    Optional<RunningRecord> findByParticipantId(Long participantId);

    Slice<RunningRecord> findByParticipantMemberIdOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);
}
