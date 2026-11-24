package com.sor999.dalmoi.record.domain;

import com.sor999.dalmoi.session.domain.SessionParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "record_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecordJob {

    @Id
    @Column(name = "participant_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private SessionParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RecordJobStatus status;

    @Column(name = "cutoff_ingest_order", nullable = false, updatable = false)
    private long cutoffIngestOrder;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RecordJob(SessionParticipant participant, long cutoffIngestOrder, Instant now) {
        this.participant = participant;
        this.status = RecordJobStatus.PROCESSING;
        this.cutoffIngestOrder = cutoffIngestOrder;
        this.requestedAt = now;
        this.updatedAt = this.requestedAt;
    }

    public void complete(Instant now) {
        status = RecordJobStatus.COMPLETED;
        completedAt = now;
        updatedAt = now;
        attemptCount++;
        errorCode = null;
    }

    public void fail(Instant now) {
        if (status != RecordJobStatus.PROCESSING) {
            return;
        }
        attemptCount++;
        updatedAt = now;
        errorCode = "RECORD_PROCESSING_FAILED";
        if (attemptCount >= 3) {
            status = RecordJobStatus.FAILED;
        }
    }

    public boolean ready(Instant now) {
        return status == RecordJobStatus.PROCESSING
                && (attemptCount == 0 || !updatedAt.plusSeconds(5).isAfter(now));
    }

    public static RecordJob create(SessionParticipant participant, long cutoffIngestOrder) {
        return create(participant, cutoffIngestOrder, Instant.now());
    }

    public static RecordJob create(SessionParticipant participant, long cutoffIngestOrder, Instant now) {
        return builder()
                .participant(participant)
                .cutoffIngestOrder(cutoffIngestOrder)
                .now(now)
                .build();
    }
}
