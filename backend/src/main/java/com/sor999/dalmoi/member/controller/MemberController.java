package com.sor999.dalmoi.member.controller;

import com.sor999.dalmoi.auth.service.AuthService;
import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.member.dto.MemberResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import com.sor999.dalmoi.auth.security.CurrentMember;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final AuthService authService;

    @GetMapping("/api/v1/members/me")
    @Operation(summary = "내 정보 조회", description = "Access JWT로 식별한 본인 회원 정보를 반환합니다.")
    public ApiResponse<MemberResponse> me(@CurrentMember Long memberId) {
        return ApiResponse.of(authService.me(memberId));
    }
}
