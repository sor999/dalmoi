package com.sor999.dalmoi.integration;

import com.sor999.dalmoi.auth.service.TokenService;
import com.sor999.dalmoi.course.dto.CoordinateRequest;
import com.sor999.dalmoi.course.dto.CreateCourseRequest;
import com.sor999.dalmoi.course.service.CourseService;
import com.sor999.dalmoi.fixture.MemberFixture;
import com.sor999.dalmoi.member.repository.MemberRepository;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
@AutoConfigureMockMvc
class CourseApiTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "코스 좌표 수: {0}")
    @ValueSource(ints = {2, 50})
    @DisplayName("좌표 경계값을 저장·복원하고 클라이언트의 거리 대신 서버 계산을 사용한다")
    void create_validBoundary_preservesCoordinatesAndCalculatesDistance(int count) {
        Long member = memberRepository.saveAndFlush(MemberFixture.createMember()).getId();
        List<CoordinateRequest> points = points(count);
        Long course = courseService.create(member, new CreateCourseRequest("강변", points, 999.0)).courseId();
        assertThat(courseService.detail(member, course).points()).isEqualTo(points);
        assertThat(courseService.detail(member, course).estimatedDistanceKm()).isBetween(0.01, 10.0);
    }

    @ParameterizedTest(name = "허용하지 않는 좌표 수: {0}")
    @ValueSource(ints = {0, 1, 51})
    @DisplayName("좌표 개수가 범위를 벗어나면 API가 400을 반환한다")
    void create_invalidCount_returnsBadRequest(int count) throws Exception {
        Long member = memberRepository.saveAndFlush(MemberFixture.createMember()).getId();
        mockMvc.perform(post("/api/v1/courses").header("Authorization", "Bearer " + tokenService.accessToken(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateCourseRequest("코스", points(count), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("잘못된 페이지 번호와 크기를 요청 경계에서 거절한다")
    void course_invalidPage_rejectsRequest() throws Exception {
        Long member = memberRepository.saveAndFlush(MemberFixture.createMember()).getId();
        for (String query : List.of("page=-1", "size=0", "size=101")) {
            mockMvc.perform(get("/api/v1/courses/me?" + query).header("Authorization",
                    "Bearer " + tokenService.accessToken(member)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    private List<CoordinateRequest> points(int count) {
        return IntStream.range(0, count).mapToObj(i -> new CoordinateRequest(37.52 + i * 0.0001, 126.93)).toList();
    }
}
