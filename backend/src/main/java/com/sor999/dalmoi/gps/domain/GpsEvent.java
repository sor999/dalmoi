package com.sor999.dalmoi.gps.domain;

import com.sor999.dalmoi.session.domain.SessionParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "gps_events", uniqueConstraints = {@UniqueConstraint(name = "uq_gps_events_participant_sequence", columnNames = {"participant_id", "sequence"}),
        @UniqueConstraint(name = "uq_gps_events_participant_ingest", columnNames = {"participant_id", "ingest_order"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GpsEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private SessionParticipant participant;

    @Column(name = "sequence", nullable = false, updatable = false)
    private long sequence;

    @Column(name = "ingest_order", nullable = false, updatable = false)
    private long ingestOrder;

    @Column(name = "location", nullable = false, updatable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "accuracy_m", nullable = false, updatable = false)
    private double accuracyMeters;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 24)
    private GpsValidationStatus validationStatus;

    @Column(name = "rejection_reason", length = 64)
    private String rejectionReason;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private GpsEvent(UUID eventId, SessionParticipant participant, long sequence, long ingestOrder, Point location, double accuracyMeters, Instant recordedAt, Instant receivedAt) {
        this.eventId = eventId;
        this.participant = participant;
        this.sequence = sequence;
        this.ingestOrder = ingestOrder;
        this.location = (Point) location.copy();
        this.accuracyMeters = accuracyMeters;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
        this.validationStatus = GpsValidationStatus.PENDING;
    }

    public static GpsEvent create(UUID eventId, SessionParticipant participant, long sequence, long ingestOrder, Point location, double accuracyMeters, Instant recordedAt, Instant receivedAt) {
        return builder()
                .eventId(eventId)
                .participant(participant)
                .sequence(sequence)
                .ingestOrder(ingestOrder)
                .location(location)
                .accuracyMeters(accuracyMeters)
                .recordedAt(recordedAt)
                .receivedAt(receivedAt)
                .build();
    }

    public Point getLocation() {
        return (Point) location.copy();
    }
}
