package com.sor999.dalmoi.record.controller;

import com.sor999.dalmoi.record.service.RecordService;
import com.sor999.dalmoi.record.dto.RecordResponse;
import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.global.response.PageResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import com.sor999.dalmoi.auth.security.CurrentMember;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/api/v1/records/me")
    @Operation(summary = "내 러닝 기록 목록", description = "0부터 시작하는 페이지, 기본 크기 20, 최대 100입니다.")
    public ApiResponse<PageResponse<RecordResponse.Summary>> mine(@CurrentMember Long memberId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.of(recordService.mine(memberId, page, size));
    }

    @GetMapping("/api/v1/records/{recordId}")
    @Operation(summary = "내 러닝 기록·실제 경로", description = "유효 GPS 경로와 정산 거리를 반환합니다. 타인 기록은 404입니다.")
    public ApiResponse<RecordResponse> detail(@CurrentMember Long memberId, @PathVariable Long recordId) {
        return ApiResponse.of(recordService.detail(memberId, recordId));
    }

    @GetMapping("/api/v1/sessions/{sessionId}/me/record-status")
    @Operation(summary = "본인 기록 정산 상태", description = "PROCESSING·COMPLETED·FAILED와 완료 시 recordId를 반환합니다. 실패 작업은 총 3회 시도합니다.")
    public ApiResponse<RecordResponse.Status> status(@CurrentMember Long memberId, @PathVariable Long sessionId) {
        return ApiResponse.of(recordService.status(memberId, sessionId));
    }
}
