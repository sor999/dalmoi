package com.sor999.dalmoi.gps.controller;

import com.sor999.dalmoi.auth.security.CurrentMember;
import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/api/v1/sessions/{sessionId}/locations/latest")
    @Operation(summary = "최신 위치 조회", description = "주행 중 참가자 전용이며 Redis 누락 및 장애 시 DB 조회로 복구합니다. 30초 이후 isStale을 표시합니다.")
    public ApiResponse<LocationResponse> latest(@CurrentMember Long memberId, @PathVariable Long sessionId) {
        return ApiResponse.of(locationService.latest(memberId, sessionId));
    }
}
