package com.sor999.dalmoi.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비즈니스 예외는 지정한 HTTP 상태와 코드, 메시지를 반환한다")
    void handleBusinessException_invalidInviteCode_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_INVITE_CODE"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 초대코드입니다."));
    }

    @ParameterizedTest(name = "잘못된 요청 본문: {0}")
    @ValueSource(strings = {"{", "{}", "{\"name\":\"\"}"})
    @DisplayName("JSON 파싱 오류와 요청 DTO 검증 실패는 동일한 오류 형식을 반환한다")
    void handleExceptionInternal_invalidBody_returnsInvalidRequest(String body) throws Exception {
        mockMvc.perform(post("/test/errors/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("올바르지 않은 요청입니다."));
    }

    @ParameterizedTest(name = "잘못된 쿼리 요청: {0}")
    @ValueSource(strings = {"/test/errors/parameter", "/test/errors/parameter?count=abc"})
    @DisplayName("필수 파라미터 누락과 타입 불일치는 400을 반환한다")
    void handleExceptionInternal_invalidParameter_returnsInvalidRequest(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("없는 경로는 공통 오류 형식의 404를 반환한다")
    void handleExceptionInternal_missingPath_returnsNotFound() throws Exception {
        mockMvc.perform(get("/test/errors/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 Allow 헤더와 공통 오류 형식의 405를 반환한다")
    void handleExceptionInternal_unsupportedMethod_preservesAllowHeader() throws Exception {
        mockMvc.perform(post("/test/errors/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("지원하지 않는 요청 형식은 415를 반환한다")
    void handleExceptionInternal_unsupportedMediaType_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/errors/body")
                .contentType(MediaType.TEXT_PLAIN)
                .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 내부 메시지를 노출하지 않고 500을 반환한다")
    void handleUnexpectedException_internalError_returnsGenericMessage() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("동시 가입의 이메일 고유 제약 위반을 이메일 중복 응답으로 반환한다")
    void handleDataConflict_duplicateEmail_returnsConflict() throws Exception {
        mockMvc.perform(get("/test/errors/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("알 수 없는 DB 제약 위반은 이메일 중복으로 위장하지 않고 서버 오류로 반환한다")
    void handleDataConflict_unknownConstraint_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/errors/unknown-constraint"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/errors/business")
        void business() {
            throw new BusinessException(ErrorCode.INVALID_INVITE_CODE);
        }

        @PostMapping("/test/errors/body")
        void body(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/errors/parameter")
        void parameter(@RequestParam int count) {
        }

        @GetMapping("/test/errors/duplicate-email")
        void duplicateEmail() {
            throw new DataIntegrityViolationException("duplicate", new ConstraintViolationException(
                    "duplicate", new SQLException("duplicate", "23505"), "uq_members_email"));
        }

        @GetMapping("/test/errors/unknown-constraint")
        void unknownConstraint() {
            throw new DataIntegrityViolationException("internal database detail", new ConstraintViolationException(
                    "internal database detail", new SQLException("check", "23514"), "unknown_constraint"));
        }

        @GetMapping("/test/errors/unexpected")
        void unexpected() {
            throw new IllegalStateException("외부에 노출하면 안 되는 내부 오류 정보");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
