package com.sor999.dalmoi.session;

import com.sor999.dalmoi.session.service.SessionService;
import com.sor999.dalmoi.session.domain.SessionStatus;
import com.sor999.dalmoi.session.dto.SessionResponse;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.support.MvpDatabaseTest;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.util.List;
import java.util.concurrent.Callable;
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

@PostgresIntegrationTest
class SessionServiceTest extends MvpDatabaseTest {

    @Autowired
    private SessionService sessionService;

    @Test
    @DisplayName("그룹과 개인 상태 전이는 멱등 처리하며 종료 시 DB 정산 작업만 등록한다")
    void commands_validLifecycle_preservesTimesAndQueuesRecord() {
        Scenario s = waiting();
        Long other = member();
        participant(s, other, false);
        assertThat(sessionService.detail(s.memberId(), s.sessionId()).participants()).hasSize(2);
        SessionResponse.Command started = sessionService.start(s.memberId(), s.sessionId());
        assertThat(sessionService.start(s.memberId(), s.sessionId()).startedAt()).isEqualTo(started.startedAt());
        sessionService.startMe(s.memberId(), s.sessionId());
        assertThatThrownBy(() -> sessionService.complete(s.memberId(), s.sessionId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ACTIVE_PARTICIPANTS_EXIST));
        SessionResponse.Personal ended = sessionService.finishMe(s.memberId(), s.sessionId());
        assertThat(ended.recordStatus()).isEqualTo("PROCESSING");
        assertThat(sessionService.finishMe(s.memberId(), s.sessionId()).endedAt()).isEqualTo(ended.endedAt());
        assertThat(jdbcTemplate.queryForObject("select count(*) from record_jobs where participant_id=?", Long.class,
                s.participantId())).isEqualTo(1);
        assertThat(sessionService.complete(s.memberId(), s.sessionId()).status()).isEqualTo("COMPLETED");
        sessionService.complete(s.memberId(), s.sessionId());
        assertThat(jdbcTemplate.queryForObject(
                "select status from session_participants where session_id=? and member_id=?", String.class,
                s.sessionId(), other)).isEqualTo("ABSENT");
        assertThatThrownBy(() -> sessionService.start(s.memberId(), s.sessionId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("같은 크루의 동시 러닝 생성은 하나만 성공하고 생성자를 자동 참가시킨다")
    void create_concurrentRequests_createsOneSessionAndOwner() throws Exception {
        Scenario s = waiting();
        sessionService.start(s.memberId(), s.sessionId());
        sessionService.complete(s.memberId(), s.sessionId());
        assertThat(concurrent(() -> sessionService.create(s.memberId(), s.crewId(), "새 러닝", s.courseId()),
                () -> sessionService.create(s.memberId(), s.crewId(), "새 러닝", s.courseId())))
                .containsExactlyInAnyOrder("OK", "ACTIVE_SESSION_EXISTS");
        Long id = sessionService.list(s.memberId(), s.crewId(), SessionStatus.WAITING).getFirst().sessionId();
        assertThat(sessionService.participants(s.memberId(), id)).hasSize(1);
    }

    @Test
    @DisplayName("회원이 서로 다른 러닝을 동시에 시작해도 하나만 주행 상태가 된다")
    void startMe_differentSessions_keepsOneRunningParticipant() throws Exception {
        Scenario a = waiting();
        Scenario b = waiting();
        participant(b, a.memberId(), false);
        sessionService.start(a.memberId(), a.sessionId());
        sessionService.start(b.memberId(), b.sessionId());
        assertThat(concurrent(() -> sessionService.startMe(a.memberId(), a.sessionId()),
                () -> sessionService.startMe(a.memberId(), b.sessionId())))
                .containsExactlyInAnyOrder("OK", "ALREADY_RUNNING_OTHER_SESSION");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from session_participants where member_id=? and status='RUNNING'", Long.class,
                a.memberId())).isEqualTo(1);
    }

    @Test
    @DisplayName("크루원이 아닌 사용자와 일반 멤버의 크루장 명령을 차단한다")
    void commands_wrongOwnerOrMembership_rejectsAccess() {
        Scenario s = waiting();
        Long joined = member();
        participant(s, joined, false);
        assertThatThrownBy(() -> sessionService.start(joined, s.sessionId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_CREW_OWNER));
        Long outsider = member();
        assertThatThrownBy(() -> sessionService.detail(outsider, s.sessionId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.NOT_CREW_MEMBER));
    }

    private List<String> concurrent(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> a = executor.submit(() -> {
                start.await();
                return result(first);
            });
            Future<String> b = executor.submit(() -> {
                start.await();
                return result(second);
            });
            start.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        }
    }

    private String result(Callable<?> action) throws Exception {
        try {
            action.call();
            return "OK";
        } catch (BusinessException error) {
            return error.getErrorCode().name();
        }
    }
}
