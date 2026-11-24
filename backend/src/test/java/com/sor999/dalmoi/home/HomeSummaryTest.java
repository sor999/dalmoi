package com.sor999.dalmoi.home;

import com.sor999.dalmoi.home.service.HomeService;
import com.sor999.dalmoi.home.dto.HomeResponse;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresIntegrationTest
class HomeSummaryTest extends MvpDatabaseTest {

    @Autowired
    private HomeService homeService;

    @Test
    @DisplayName("처음 가입한 회원의 홈은 빈 요약과 대기 러닝 부재를 반환한다")
    void summary_newMember_returnsEmptyCollections() {
        Long id = member();
        HomeResponse response = homeService.summary(id);
        assertThat(response.member().memberId()).isEqualTo(id);
        assertThat(response.featuredWaitingSession()).isNull();
        assertThat(response.crewSummary().totalCount()).isZero();
        assertThat(response.courseSummary().items()).isEmpty();
    }

    @Test
    @DisplayName("본인의 코스와 크루 및 참여한 대기 러닝만 홈에 표시한다")
    void summary_existingData_returnsOwnWaitingSessionAndCounts() {
        Scenario s = waiting();
        waiting();
        HomeResponse response = homeService.summary(s.memberId());
        assertThat(response.crewSummary().totalCount()).isEqualTo(1);
        assertThat(response.crewSummary().items()).hasSize(1);
        assertThat(response.courseSummary().totalCount()).isEqualTo(1);
        assertThat(response.courseSummary().items().getFirst().courseId()).isEqualTo(s.courseId());
        assertThat(response.featuredWaitingSession().sessionId()).isEqualTo(s.sessionId());
        assertThat(response.featuredWaitingSession().myParticipationStatus()).isEqualTo("JOINED");
    }
}
