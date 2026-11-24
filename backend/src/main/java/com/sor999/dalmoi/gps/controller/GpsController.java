package com.sor999.dalmoi.gps.controller;

import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.gps.dto.GpsRequest;
import com.sor999.dalmoi.gps.dto.LocationResponse;
import com.sor999.dalmoi.gps.domain.GpsEvent;
import com.sor999.dalmoi.gps.service.GpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import com.sor999.dalmoi.auth.security.CurrentMember;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequiredArgsConstructor
public class GpsController {

    private final GpsService gpsService;

    @PostMapping("/api/v1/sessions/{sessionId}/locations")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "GPS 검증·저장", description = "DB 저장 완료 후 200입니다. accepted는 저장 수락 여부이며 processingStatus는 VALID 또는 REJECTED입니다. 동일 이벤트 재전송은 멱등 처리합니다.")
    public ApiResponse<LocationResponse.Accepted> receive(@CurrentMember Long memberId,
            @PathVariable Long sessionId, @Valid @RequestBody GpsRequest request) {
        GpsEvent event = gpsService.receive(memberId, sessionId, request);

        return ApiResponse
                .of(new LocationResponse.Accepted(event.getEventId(), true, event.getValidationStatus().name()));
    }

}
