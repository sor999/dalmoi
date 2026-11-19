package com.sor999.dalmoi.global.validation;

import com.sor999.dalmoi.auth.dto.LoginRequest;
import com.sor999.dalmoi.auth.dto.SignupRequest;
import com.sor999.dalmoi.course.dto.CoordinateRequest;
import com.sor999.dalmoi.course.dto.CreateCourseRequest;
import com.sor999.dalmoi.gps.dto.GpsRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private final Validator validator = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    @DisplayName("ASCII와 한글 및 이모지 비밀번호는 UTF-8 72바이트 경계에서 검증한다")
    void password_utf8Boundary_validatesActualByteLength() {
        for (String password : List.of("a".repeat(72), "가".repeat(24), "😀".repeat(18))) {
            assertThat(validator.validate(new SignupRequest("runner@example.com", password, "러너"))).isEmpty();
            assertThat(validator.validate(new LoginRequest("runner@example.com", password))).isEmpty();
        }
        for (String password : List.of("a".repeat(73), "가".repeat(25), "😀".repeat(19))) {
            assertThat(validator.validate(new SignupRequest("runner@example.com", password, "러너")))
                    .extracting(error -> error.getPropertyPath().toString())
                    .containsExactly("password");
            assertThat(validator.validate(new LoginRequest("runner@example.com", password)))
                    .extracting(error -> error.getPropertyPath().toString())
                    .containsExactly("password");
        }
    }

    @Test
    @DisplayName("비밀번호의 필수 입력과 가입 최소 길이는 DTO에서 검증한다")
    void password_nullBlankAndShort_rejectsInvalidSignup() {
        assertThat(validator.validate(new SignupRequest("runner@example.com", null, "러너"))).isNotEmpty();
        assertThat(validator.validate(new SignupRequest("runner@example.com", " ", "러너"))).isNotEmpty();
        assertThat(validator.validate(new SignupRequest("runner@example.com", "1234567", "러너"))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("runner@example.com", null))).isNotEmpty();
    }

    @ParameterizedTest(name = "좌표 수: {0}")
    @ValueSource(ints = {0, 1, 2, 50, 51})
    @DisplayName("코스 좌표 개수의 2개와 50개 경계를 DTO에서 검증한다")
    void points_countBoundary_validatesSize(int count) {
        List<CoordinateRequest> points = IntStream.range(0, count)
                .mapToObj(i -> new CoordinateRequest(37.52, 126.93))
                .toList();
        assertThat(validator.validate(new CreateCourseRequest("코스", points, null)).isEmpty())
                .isEqualTo(count >= 2 && count <= 50);
    }

    @ParameterizedTest(name = "유효하지 않은 위도: {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -91, 91})
    @DisplayName("NaN과 무한대 및 범위 밖 좌표를 중첩 DTO 검증으로 거절한다")
    void coordinates_nonFiniteOrOutOfRange_rejectsNestedPoint(double latitude) {
        CreateCourseRequest request = new CreateCourseRequest("코스", List.of(new CoordinateRequest(latitude, 126.93),
                new CoordinateRequest(37.52, 126.93)), null);
        assertThat(validator.validate(request)).extracting(error -> error.getPropertyPath().toString())
                .contains("points[0].latitude");
        assertThat(validator.validate(new CoordinateRequest(37.52, latitude * 2))).isNotEmpty();
    }

    @ParameterizedTest(name = "유효하지 않은 GPS 정확도: {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1})
    @DisplayName("GPS 정확도는 유한한 0 이상의 숫자만 허용한다")
    void accuracy_nonFiniteOrNegative_rejectsInput(double accuracy) {
        GpsRequest request = new GpsRequest(UUID.randomUUID(), 0L, 37.52, 126.93, accuracy, Instant.now());
        assertThat(validator.validate(request)).extracting(error -> error.getPropertyPath().toString())
                .contains("accuracyMeters");
    }

    @Test
    @DisplayName("정확도 품질과 이벤트 시각 정책은 DTO 형식 검증과 구분한다")
    void gps_qualityAndReplayTime_leavesBusinessPolicyToService() {
        GpsRequest request = new GpsRequest(UUID.randomUUID(), 0L, 37.52, 126.93, 51.0,
                Instant.now().minusSeconds(3600));
        assertThat(validator.validate(request)).isEmpty();
    }
}
