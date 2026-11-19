package com.sor999.dalmoi.gps.repository;

import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.domain.GpsValidationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GpsEventRepository extends JpaRepository<GpsEvent, UUID> {

    @Query("select coalesce(max(g.ingestOrder),0) from GpsEvent g where g.participant.id = :participantId")
    long maxIngestOrder(Long participantId);

    Optional<GpsEvent> findFirstByParticipantIdOrderBySequenceDesc(Long participantId);

    Optional<GpsEvent> findByEventId(UUID eventId);

    Optional<GpsEvent> findByParticipantIdAndSequence(Long participantId, long sequence);

    List<GpsEvent> findByParticipantIdAndIngestOrderLessThanEqualOrderBySequenceAsc(Long participantId, long cutoff);

    List<GpsEvent> findByParticipantIdOrderBySequenceAsc(Long participantId);

    List<GpsEvent> findByParticipantIdAndValidationStatusOrderBySequenceAsc(Long participantId,
            GpsValidationStatus status);

    Optional<GpsEvent> findFirstByParticipantIdAndValidationStatusOrderBySequenceDesc(Long participantId,
            GpsValidationStatus status);
}
