package com.sor999.dalmoi.home.controller;

import com.sor999.dalmoi.home.service.HomeService;
import com.sor999.dalmoi.home.dto.HomeResponse;
import com.sor999.dalmoi.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import com.sor999.dalmoi.auth.security.CurrentMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/v1/home/summary")
    @Operation(summary = "홈 요약", description = "본인 정보, 최근 코스 3개, 크루 요약 3개와 참여한 최신 대기 러닝을 반환합니다.")
    public ApiResponse<HomeResponse> summary(@CurrentMember Long memberId) {
        return ApiResponse.of(homeService.summary(memberId));
    }
}
