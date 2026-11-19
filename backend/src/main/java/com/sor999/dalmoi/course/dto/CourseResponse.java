package com.sor999.dalmoi.course.dto;

import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.course.service.GeoSupport;
import java.time.Instant;
import java.util.List;

public record CourseResponse(Long courseId, Long creatorMemberId, String title, double estimatedDistanceKm,
        boolean isLoop, List<CoordinateRequest> points, Instant createdAt) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getCreator().getId(), course.getTitle(),
                GeoSupport.km(course.getEstimatedDistanceMeters()), course.isLoop(),
                GeoSupport.points(course.getRoute()), course.getCreatedAt());
    }

    public record Summary(Long courseId, String title, double estimatedDistanceKm, boolean isLoop, Instant createdAt) {

        public static Summary from(Course course) {
            return new Summary(course.getId(), course.getTitle(), GeoSupport.km(course.getEstimatedDistanceMeters()),
                    course.isLoop(), course.getCreatedAt());
        }
    }
}
