package com.sor999.dalmoi.session.controller;

import com.sor999.dalmoi.global.response.ApiResponse;
import com.sor999.dalmoi.session.service.SessionService;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.dto.SessionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    public record CreateSessionRequest(@NotBlank @Size(max = 100) String title, @NotNull Long courseId) {
    }

    @PostMapping("/api/v1/crews/{crewId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "그룹 러닝 생성", description = "크루장만 생성하며 자동 참가합니다. 크루당 대기·진행 중 러닝은 합쳐 1개입니다.")
    public ApiResponse<SessionResponse> create(@CurrentMember Long memberId, @PathVariable Long crewId,
            @Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse
                .of(sessionService.create(memberId, crewId, request.title(), request.courseId()));
    }

    @GetMapping("/api/v1/crews/{crewId}/sessions")
    @Operation(summary = "크루 러닝 목록", description = "크루원만 조회할 수 있으며 status로 필터링합니다.")
    public ApiResponse<List<SessionResponse.Summary>> list(@CurrentMember Long memberId, @PathVariable Long crewId,
            @RequestParam(required = false) SessionStatus status) {
        return ApiResponse.of(sessionService.list(memberId, crewId, status));
    }

    @GetMapping("/api/v1/sessions/{sessionId}")
    @Operation(summary = "러닝·대기방 상세", description = "크루원에게 코스 스냅샷과 참가 상태를 반환합니다.")
    public ApiResponse<SessionResponse> detail(@CurrentMember Long memberId, @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.detail(memberId, sessionId));
    }

    @GetMapping("/api/v1/sessions/{sessionId}/participants")
    @Operation(summary = "러닝 참가자 조회", description = "크루원만 조회할 수 있습니다.")
    public ApiResponse<List<SessionResponse.Participant>> participants(@CurrentMember Long memberId,
            @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.participants(memberId, sessionId));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "대기 러닝 참가", description = "대기 중에만 참가할 수 있으며 중복 참가는 409입니다.")
    public ApiResponse<SessionResponse.Participant> join(@CurrentMember Long memberId,
            @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.join(memberId, sessionId));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/start")
    @Operation(summary = "그룹 러닝 시작", description = "크루장 전용입니다. 반복 시작은 최초 시작 시각을 유지합니다.")
    public ApiResponse<SessionResponse.Command> start(@CurrentMember Long memberId, @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.start(memberId, sessionId));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/me/start")
    @Operation(summary = "본인 주행 시작", description = "그룹 진행 중에만 가능합니다. 다른 러닝과 동시 주행은 409입니다.")
    public ApiResponse<SessionResponse.Personal> startMe(@CurrentMember Long memberId,
            @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.startMe(memberId, sessionId));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/me/finish")
    @Operation(summary = "본인 주행 종료·정산 요청", description = "종료를 확정하고 recordStatus PROCESSING을 반환합니다. 반복 요청은 현재 상태를 반환합니다.")
    public ApiResponse<SessionResponse.Personal> finishMe(@CurrentMember Long memberId,
            @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.finishMe(memberId, sessionId));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/complete")
    @Operation(summary = "그룹 러닝 마감", description = "크루장 전용이며 주행 중 참가자가 있으면 409입니다. 미시작 참가자는 ABSENT로 처리합니다.")
    public ApiResponse<SessionResponse.Command> complete(@CurrentMember Long memberId,
            @PathVariable Long sessionId) {
        return ApiResponse.of(sessionService.complete(memberId, sessionId));
    }
}
