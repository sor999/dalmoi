package com.sor999.dalmoi.integration;

import com.sor999.dalmoi.course.service.CourseService;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresIntegrationTest
class CourseSharingTest extends MvpDatabaseTest {

    @Autowired
    private CourseService courseService;

    @Test
    @DisplayName("크루원에게 코스를 공유하되 개인 상세와 비크루원 조회는 차단한다")
    void shared_memberAndOutsider_enforcesVisibility() {
        Scenario s = waiting();
        Long joined = member();
        participant(s, joined, false);
        assertThat(courseService.shared(joined, s.crewId(), 0, 20).items()).hasSize(1);
        assertThatThrownBy(() -> courseService.detail(joined, s.courseId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_ACCESSIBLE));
        Long outsider = member();
        assertThatThrownBy(() -> courseService.shared(outsider, s.crewId(), 0, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_CREW_MEMBER));
    }
}
