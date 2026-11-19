package com.sor999.dalmoi.course.controller;

import com.sor999.dalmoi.course.dto.CreateCourseRequest;
import com.sor999.dalmoi.course.dto.CourseResponse;
import com.sor999.dalmoi.course.service.CourseService;
import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.global.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import com.sor999.dalmoi.auth.security.CurrentMember;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/api/v1/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "코스 생성", description = "2~50개 좌표를 순서대로 저장하고 거리를 서버에서 계산합니다.")
    public ApiResponse<CourseResponse> create(@CurrentMember Long memberId,
            @Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.of(courseService.create(memberId, request));
    }

    @GetMapping("/api/v1/courses/me")
    @Operation(summary = "내 코스 목록", description = "0부터 시작하는 페이지, 기본 크기 20, 최대 100입니다.")
    public ApiResponse<PageResponse<CourseResponse.Summary>> mine(@CurrentMember Long memberId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(courseService.mine(memberId, page, size));
    }

    @GetMapping("/api/v1/courses/{courseId}")
    @Operation(summary = "내 코스 상세", description = "생성자만 조회할 수 있습니다.")
    public ApiResponse<CourseResponse> detail(@CurrentMember Long memberId, @PathVariable Long courseId) {
        return ApiResponse.of(courseService.detail(memberId, courseId));
    }

    @GetMapping("/api/v1/crews/{crewId}/courses")
    @Operation(summary = "크루 공유 코스 목록", description = "해당 크루원이 생성한 코스를 크루원에게 제공합니다.")
    public ApiResponse<PageResponse<CourseResponse>> shared(@CurrentMember Long memberId, @PathVariable Long crewId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(courseService.shared(memberId, crewId, page, size));
    }
}
