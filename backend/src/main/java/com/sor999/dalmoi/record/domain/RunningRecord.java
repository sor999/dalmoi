package com.sor999.dalmoi.record.domain;

import com.sor999.dalmoi.session.domain.SessionParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "running_records", uniqueConstraints = @UniqueConstraint(name = "uq_running_records_participant", columnNames = "participant_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private SessionParticipant participant;

    @Column(name = "distance_m", nullable = false)
    private double distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "calculation_version", nullable = false, length = 32)
    private String calculationVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RunningRecord(SessionParticipant participant, double distanceMeters, long durationSeconds, String calculationVersion) {
        this.participant = participant;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.calculationVersion = calculationVersion;
        this.createdAt = Instant.now();
    }

    public static RunningRecord create(SessionParticipant participant, double distanceMeters, long durationSeconds, String calculationVersion) {
        return builder()
                .participant(participant)
                .distanceMeters(distanceMeters)
                .durationSeconds(durationSeconds)
                .calculationVersion(calculationVersion)
                .build();
    }
}
