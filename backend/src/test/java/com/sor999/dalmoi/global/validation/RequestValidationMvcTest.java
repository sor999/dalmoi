package com.sor999.dalmoi.global.validation;

import com.sor999.dalmoi.auth.controller.AuthController;
import com.sor999.dalmoi.auth.service.AuthService;
import com.sor999.dalmoi.course.controller.CourseController;
import com.sor999.dalmoi.course.service.CourseService;
import com.sor999.dalmoi.gps.controller.GpsController;
import com.sor999.dalmoi.gps.service.GpsService;
import com.sor999.dalmoi.session.controller.SessionController;
import com.sor999.dalmoi.session.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, CourseController.class, GpsController.class, SessionController.class})
@AutoConfigureMockMvc(addFilters = false)
class RequestValidationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private GpsService gpsService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    @DisplayName("입력 검증 실패는 서비스 실행 전에 공통 400으로 반환한다")
    void request_invalidBody_rejectsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"runner@example.com\",\"password\":\"" + "가".repeat(25)
                        + "\",\"nickname\":\"러너\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(post("/api/v1/courses").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"코스\",\"points\":[{\"latitude\":37.52,\"longitude\":126.93}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(post("/api/v1/sessions/1/locations").contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"eventId\":\"00000000-0000-0000-0000-000000000001\",\"sequence\":0,\"latitude\":91,\"longitude\":126.93,\"accuracyMeters\":5,\"recordedAt\":\"2026-11-19T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(authService, courseService, gpsService, sessionService);
    }

    @Test
    @DisplayName("지원하지 않는 러닝 상태는 요청 바인딩에서 거절한다")
    void list_invalidSessionStatus_rejectsBeforeServiceInvocation() throws Exception {
        mockMvc.perform(get("/api/v1/crews/1/sessions").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(sessionService);
    }
}
