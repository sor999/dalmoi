package com.sor999.dalmoi.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCourseRequest(@NotBlank @Size(max = 100) String title,
        @NotNull @Size(min = 2, max = 50) List<@NotNull @Valid CoordinateRequest> points, Double estimatedDistanceKm) {
}
