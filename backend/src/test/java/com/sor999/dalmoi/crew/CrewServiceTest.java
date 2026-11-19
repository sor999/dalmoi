package com.sor999.dalmoi.crew;

import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.crew.repository.CrewRepository;
import com.sor999.dalmoi.crew.service.CrewService;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@PostgresIntegrationTest
class CrewServiceTest extends MvpDatabaseTest {

    @Autowired
    private CrewService crewService;

    @Autowired
    private CrewRepository crewRepository;

    @Test
    @DisplayName("크루 생성자는 자동 가입하며 초대코드는 크루장만 조회한다")
    void create_joinAndQuery_enforcesMembershipAndOwner() {
        Long owner = member();
        CrewResponse.Created crew = crewService.createResponse(owner, "러닝 크루");
        Long joined = member();
        crewService.joinResponse(joined, " " + crew.inviteCode().toLowerCase() + " ");
        assertThat(crew.inviteCode()).matches("[A-Z0-9]{16}");
        assertThat(crewService.detailResponse(owner, crew.crewId()).members()).hasSize(2);
        assertThat(crewService.mineResponse(joined)).hasSize(1);
        assertThat(crewService.inviteResponse(owner, crew.crewId()).inviteCode()).isEqualTo(crew.inviteCode());
        assertThatThrownBy(() -> crewService.inviteResponse(joined, crew.crewId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_CREW_OWNER));
        assertThatThrownBy(() -> crewService.joinResponse(joined, crew.inviteCode()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CREW_MEMBER));
        assertThatThrownBy(() -> crewService.joinResponse(member(), "INVALID"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INVITE_CODE));
    }

    @Test
    @DisplayName("마지막 한 자리 동시 가입은 실제 잠금 경합 이후 한 명만 성공한다")
    void join_lastSeatWithContention_keepsTenMembers() throws Exception {
        CrewResponse.Created crew = crewService.createResponse(member(), "정원 경합");
        for (int i = 0; i < 8; i++) {
            crewService.joinResponse(member(), crew.inviteCode());
        }
        Long first = member();
        Long second = member();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            Future<?> blocker = executor.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
                crewRepository.findLockedById(crew.crewId()).orElseThrow();
                held.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("잠금 해제 시간 초과");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
            }));
            assertThat(held.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> a = executor.submit(() -> joinResult(first, crew.inviteCode()));
            Future<String> b = executor.submit(() -> joinResult(second, crew.inviteCode()));
            try {
                await().atMost(5, TimeUnit.SECONDS)
                        .until(() -> jdbcTemplate.queryForObject(
                                "select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock' and query like '%crews%'",
                                Long.class) >= 2);
            } finally {
                release.countDown();
            }
            blocker.get(5, TimeUnit.SECONDS);
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "CREW_CAPACITY_EXCEEDED");
        }
        assertThat(crewService.count(crew.crewId())).isEqualTo(10);
    }

    private String joinResult(Long memberId, String invite) {
        try {
            crewService.joinResponse(memberId, invite);
            return "OK";
        } catch (BusinessException error) {
            return error.getErrorCode().name();
        }
    }
}
