package com.sor999.dalmoi.auth.controller;

import com.sor999.dalmoi.auth.dto.SignupRequest;
import com.sor999.dalmoi.auth.dto.LoginRequest;
import com.sor999.dalmoi.auth.dto.LoginResponse;
import com.sor999.dalmoi.auth.dto.RefreshRequest;
import com.sor999.dalmoi.auth.dto.RefreshResponse;
import com.sor999.dalmoi.auth.service.AuthService;
import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.member.dto.MemberResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@SecurityRequirements
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "이메일 회원가입", description = "비밀번호는 8자 이상, UTF-8 72바이트 이하입니다. 중복 이메일은 409입니다.")
    public ApiResponse<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.of(authService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인·토큰 발급", description = "Access·Refresh Token과 Access 만료 초만 반환합니다. Refresh 만료 초는 반환하지 않습니다.")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access Token 재발급", description = "기존 Refresh Token을 재사용하며 토큰과 만료 시각을 교체하지 않습니다. 만료·폐기는 401입니다.")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.of(authService.refresh(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "현재 Refresh 세션 로그아웃", description = "반복 요청도 204입니다. 기존 Access JWT는 만료까지 유효합니다.")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }
}
