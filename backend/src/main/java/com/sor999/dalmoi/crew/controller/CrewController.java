package com.sor999.dalmoi.crew.controller;

import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.crew.service.CrewService;
import com.sor999.dalmoi.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
public class CrewController {

    private final CrewService crewService;

    public record CreateCrewRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record JoinCrewRequest(@NotBlank @Size(max = 32) String inviteCode) {
    }

    @PostMapping("/api/v1/crews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "크루 생성", description = "생성자를 크루장과 멤버로 등록합니다. 최대 정원은 크루장 포함 10명입니다.")
    public ApiResponse<CrewResponse.Created> create(@CurrentMember Long memberId,
            @Valid @RequestBody CreateCrewRequest request) {
        return ApiResponse.of(crewService.createResponse(memberId, request.name()));
    }

    @PostMapping("/api/v1/crews/join")
    @Operation(summary = "초대코드로 크루 가입", description = "동시 가입에도 정원 10명을 보장합니다. 중복·정원 초과는 409입니다.")
    public ApiResponse<CrewResponse.Joined> join(@CurrentMember Long memberId,
            @Valid @RequestBody JoinCrewRequest request) {
        return ApiResponse.of(crewService.joinResponse(memberId, request.inviteCode()));
    }

    @GetMapping("/api/v1/crews/me")
    @Operation(summary = "내 크루 목록", description = "가입 시각 내림차순으로 조회합니다.")
    public ApiResponse<List<CrewResponse>> mine(@CurrentMember Long memberId) {
        return ApiResponse.of(crewService.mineResponse(memberId));
    }

    @GetMapping("/api/v1/crews/{crewId}")
    @Operation(summary = "크루 상세·멤버", description = "크루원만 조회할 수 있습니다.")
    public ApiResponse<CrewResponse> detail(@CurrentMember Long memberId, @PathVariable Long crewId) {
        return ApiResponse.of(crewService.detailResponse(memberId, crewId));
    }

    @GetMapping("/api/v1/crews/{crewId}/invite-code")
    @Operation(summary = "크루 초대코드 조회", description = "크루장만 조회할 수 있습니다. 초대코드 만료·재발급은 지원하지 않습니다.")
    public ApiResponse<CrewResponse.InviteCode> invite(@CurrentMember Long memberId, @PathVariable Long crewId) {
        return ApiResponse.of(crewService.inviteResponse(memberId, crewId));
    }
}
