package com.sor999.dalmoi.session.domain;

import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.crew.domain.Crew;
import com.sor999.dalmoi.member.domain.Member;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "running_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private Member createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private SessionStatus status;

    @Column(name = "planned_course_title", nullable = false, updatable = false, length = 100)
    private String plannedCourseTitle;

    @Column(name = "planned_route", nullable = false, updatable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString plannedRoute;

    @Column(name = "planned_distance_m", nullable = false, updatable = false)
    private double plannedDistanceMeters;

    @Column(name = "planned_is_loop", nullable = false, updatable = false)
    private boolean plannedLoop;

    @Column(name = "crew_name_snapshot", nullable = false, updatable = false, length = 100)
    private String crewNameSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RunningSession(Crew crew, Member createdBy, Course course, String title) {
        this.crew = crew;
        this.createdBy = createdBy;
        this.course = course;
        this.title = title;
        this.status = SessionStatus.WAITING;
        this.plannedCourseTitle = course.getTitle();
        this.plannedRoute = course.getRoute();
        this.plannedDistanceMeters = course.getEstimatedDistanceMeters();
        this.plannedLoop = course.isLoop();
        this.crewNameSnapshot = crew.getName();
        this.createdAt = Instant.now();
    }

    public static RunningSession create(Crew crew, Member createdBy, Course course, String title) {
        return builder().crew(crew).createdBy(createdBy).course(course).title(title).build();
    }

    public LineString getPlannedRoute() {
        return (LineString) plannedRoute.copy();
    }
}
