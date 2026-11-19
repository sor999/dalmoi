package com.sor999.dalmoi.record.repository;

import java.time.Instant;

import com.sor999.dalmoi.record.domain.RecordJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface RecordJobRepository extends JpaRepository<RecordJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from RecordJob j where j.id = :id")
    Optional<RecordJob> findLockedById(Long id);

    @Query("select j.id from RecordJob j where j.status = com.sor999.dalmoi.record.domain.RecordJobStatus.PROCESSING and (j.attemptCount = 0 or j.updatedAt <= :retryBefore) order by j.requestedAt, j.id")
    List<Long> findReadyIds(Instant retryBefore, Pageable pageable);
}
