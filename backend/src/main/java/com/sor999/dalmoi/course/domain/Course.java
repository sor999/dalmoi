package com.sor999.dalmoi.course.domain;

import com.sor999.dalmoi.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false, updatable = false)
    private Member creator;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "route", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString route;

    @Column(name = "estimated_distance_m", nullable = false)
    private double estimatedDistanceMeters;

    @Column(name = "is_loop", nullable = false)
    private boolean loop;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Course(Member creator, String title, LineString route, double estimatedDistanceMeters, boolean loop) {
        this.creator = creator;
        this.title = title;
        this.route = (LineString) route.copy();
        this.estimatedDistanceMeters = estimatedDistanceMeters;
        this.loop = loop;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Course create(Member creator, String title, LineString route, double estimatedDistanceMeters, boolean loop) {
        return builder()
                .creator(creator)
                .title(title)
                .route(route)
                .estimatedDistanceMeters(estimatedDistanceMeters)
                .loop(loop)
                .build();
    }

    public LineString getRoute() {
        return (LineString) route.copy();
    }
}
