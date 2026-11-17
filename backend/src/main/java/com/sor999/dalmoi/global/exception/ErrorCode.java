package com.sor999.dalmoi.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "올바르지 않은 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "지원하지 않는 응답 형식입니다."),
    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 형식입니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "DATA_CONFLICT", "요청이 현재 데이터와 충돌합니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 인증
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_ACCESS_TOKEN(
            HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "인증 토큰이 유효하지 않거나 만료되었습니다."),
    INVALID_AUTH_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_AUTH_REQUEST", "인증 요청을 확인해 주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호를 확인해 주세요."),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "Refresh Token이 만료되었습니다."),
    REVOKED_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED, "REVOKED_REFRESH_TOKEN", "폐기된 Refresh Token입니다."),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),

    // 코스
    COURSE_TOO_FEW_POINTS(
            HttpStatus.BAD_REQUEST, "COURSE_TOO_FEW_POINTS", "경로를 그려 주세요. 출발점과 도착점이 필요해요."),
    COURSE_TOO_MANY_POINTS(HttpStatus.BAD_REQUEST, "COURSE_TOO_MANY_POINTS", "코스 좌표는 최대 50개입니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "코스를 찾을 수 없습니다."),
    COURSE_NOT_ACCESSIBLE(HttpStatus.FORBIDDEN, "COURSE_NOT_ACCESSIBLE", "코스에 접근할 수 없습니다."),

    // 크루
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "INVALID_INVITE_CODE", "유효하지 않은 초대코드입니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "ALREADY_CREW_MEMBER", "이미 가입한 크루입니다."),
    CREW_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "CREW_CAPACITY_EXCEEDED", "크루 정원이 가득 찼습니다."),
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "CREW_NOT_FOUND", "크루를 찾을 수 없습니다."),
    NOT_CREW_MEMBER(HttpStatus.FORBIDDEN, "NOT_CREW_MEMBER", "해당 크루의 멤버가 아닙니다."),
    NOT_CREW_OWNER(HttpStatus.FORBIDDEN, "NOT_CREW_OWNER", "크루장만 실행할 수 있습니다."),

    // 러닝 세션 및 참가자
    SESSION_NOT_WAITING(HttpStatus.CONFLICT, "SESSION_NOT_WAITING", "대기 중인 러닝이 아닙니다."),
    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "ALREADY_PARTICIPATING", "이미 참여한 러닝입니다."),
    NO_PARTICIPANTS(HttpStatus.CONFLICT, "NO_PARTICIPANTS", "참가자가 필요합니다."),
    SESSION_NOT_RUNNING(HttpStatus.CONFLICT, "SESSION_NOT_RUNNING", "진행 중인 러닝이 아닙니다."),
    ALREADY_RUNNING_OTHER_SESSION(
            HttpStatus.CONFLICT, "ALREADY_RUNNING_OTHER_SESSION", "이미 다른 러닝에서 주행 중입니다."),
    ACTIVE_PARTICIPANTS_EXIST(HttpStatus.CONFLICT, "ACTIVE_PARTICIPANTS_EXIST", "주행 중인 참가자가 있습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "러닝을 찾을 수 없습니다."),
    NOT_SESSION_PARTICIPANT(HttpStatus.FORBIDDEN, "NOT_SESSION_PARTICIPANT", "해당 러닝의 참가자가 아닙니다."),
    INVALID_SESSION_STATE(HttpStatus.CONFLICT, "INVALID_SESSION_STATE", "변경할 수 없는 러닝 상태입니다."),
    PARTICIPANT_ALREADY_FINISHED(HttpStatus.CONFLICT, "PARTICIPANT_ALREADY_FINISHED", "종료된 러닝입니다."),
    PARTICIPANT_NOT_RUNNING(HttpStatus.CONFLICT, "PARTICIPANT_NOT_RUNNING", "주행 중이 아닙니다."),
    ACTIVE_SESSION_EXISTS(HttpStatus.CONFLICT, "ACTIVE_SESSION_EXISTS", "이미 진행 중인 러닝이 있습니다."),

    // GPS
    GPS_EVENT_CONFLICT(HttpStatus.CONFLICT, "GPS_EVENT_CONFLICT", "GPS 이벤트 내용이 기존 이벤트와 다릅니다."),
    GPS_SEQUENCE_CONFLICT(HttpStatus.CONFLICT, "GPS_SEQUENCE_CONFLICT", "GPS 순번이 중복되었습니다."),
    INVALID_GPS_PAYLOAD(HttpStatus.BAD_REQUEST, "INVALID_GPS_PAYLOAD", "GPS 데이터를 확인해 주세요."),

    // 기록
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD_NOT_FOUND", "기록을 찾을 수 없습니다."),
    RECORD_NOT_REQUESTED(HttpStatus.CONFLICT, "RECORD_NOT_REQUESTED", "정산이 요청되지 않은 참가자입니다.");

    private final HttpStatus status;

    private final String code;

    private final String message;
}
