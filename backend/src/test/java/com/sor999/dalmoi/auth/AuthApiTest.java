package com.sor999.dalmoi.auth;

import com.sor999.dalmoi.auth.domain.AuthSession;
import com.sor999.dalmoi.auth.dto.LoginRequest;
import com.sor999.dalmoi.auth.dto.RefreshRequest;
import com.sor999.dalmoi.auth.dto.SignupRequest;
import com.sor999.dalmoi.auth.repository.AuthSessionRepository;
import com.sor999.dalmoi.auth.service.AuthService;
import com.sor999.dalmoi.auth.service.TokenService;
import com.sor999.dalmoi.auth.security.MemberUserDetails;
import com.sor999.dalmoi.member.dto.MemberResponse;
import com.sor999.dalmoi.member.repository.MemberCredentialRepository;
import com.sor999.dalmoi.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@PostgresIntegrationTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("가입부터 재발급·로그아웃까지 Redis 없이 처리하며 Refresh 만료 초를 노출하지 않는다")
    void auth_completeFlow_preservesRefreshAndRevokesOnlyOneSession() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SignupRequest("  " + email.toUpperCase() + "  ", "password123", " 러너 "))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value("러너"));
        JsonNode login = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshTokenExpiresInSeconds").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("data");
        String access = login.path("accessToken").asText();
        String refresh = login.path("refreshToken").asText();
        JsonNode me = objectMapper
                .readTree(mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + access))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .path("data");
        String hash = memberCredentialRepository.findById(me.path("memberId").asLong()).orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", hash)).isTrue();
        AuthSession session = authSessionRepository.findById(jdbcTemplate.queryForObject(
                "select id from auth_sessions where refresh_token_hash=?", Long.class, tokenService.hash(refresh)))
                .orElseThrow();
        Instant expiry = session.getExpiresAt();
        String otherRefresh = authService.login(new LoginRequest(email, "password123")).refreshToken();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
        }
        assertThat(authSessionRepository.findById(session.getId()).orElseThrow().getExpiresAt()).isEqualTo(expiry);
        assertThat(session.getRefreshTokenHash()).isNotEqualTo(refresh);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REVOKED_REFRESH_TOKEN"));
        assertThat(authService.refresh(new RefreshRequest(otherRefresh)).accessToken()).isNotBlank();
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"unknown\"}")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 계정·잘못된 비밀번호·만료 Refresh·위조 Access를 거절한다")
    void auth_invalidTokensAndCredentials_returnsUnauthorized() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        authService.signup(new SignupRequest(email, "password123", "러너"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        String refresh = authService.login(new LoginRequest(email, "password123")).refreshToken();
        jdbcTemplate.update(
                "update auth_sessions set issued_at = now() - interval '2 days', expires_at = now() - interval '1 day' where refresh_token_hash = ?",
                tokenService.hash(refresh));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EXPIRED_REFRESH_TOKEN"));
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SignupRequest(email, "가".repeat(25), "러너"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UserDetails로 회원을 조회하고 표준 인증 성공 후 비밀번호를 지운다")
    void authenticate_validMember_returnsMemberPrincipalAndErasesPassword() {
        String email = UUID.randomUUID() + "@example.com";
        MemberResponse member = authService.signup(new SignupRequest(email, "password123", "러너"));
        UserDetails loaded = userDetailsService.loadUserByUsername("  " + email.toUpperCase() + "  ");
        assertThat(loaded).isInstanceOf(MemberUserDetails.class);
        assertThat(loaded.getUsername()).isEqualTo(email);
        assertThat(passwordEncoder.matches("password123", loaded.getPassword())).isTrue();

        Authentication authenticated = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, "password123"));

        assertThat(authenticated.isAuthenticated()).isTrue();
        assertThat(authenticated.getCredentials()).isNull();
        assertThat(authenticated.getPrincipal()).isInstanceOfSatisfying(MemberUserDetails.class, principal -> {
            assertThat(principal.getMemberId()).isEqualTo(member.memberId());
            assertThat(principal.getPassword()).isNull();
        });
    }

    @Test
    @DisplayName("없는 회원·인증정보 누락·잘못된 비밀번호는 동일한 401이며 세션을 생성하지 않는다")
    void login_invalidCredentials_returnsSameErrorWithoutCreatingSessions() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        MemberResponse member = authService.signup(new SignupRequest(email, "password123", "러너"));
        for (LoginRequest request : List.of(new LoginRequest(UUID.randomUUID() + "@example.com", "password123"),
                new LoginRequest(email, "wrongpassword"))) {
            mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        jdbcTemplate.update("delete from member_credentials where member_id=?", member.memberId());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        assertThat(jdbcTemplate.queryForObject("select count(*) from auth_sessions where member_id=?",
                Long.class, member.memberId())).isZero();
    }

    @Test
    @DisplayName("UTF-8 72바이트를 넘는 로그인 비밀번호는 입력 오류로 거절하고 세션을 생성하지 않는다")
    void login_oversizedUtf8Password_returnsBadRequestWithoutSession() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        MemberResponse member = authService.signup(new SignupRequest(email, "password123", "러너"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "가".repeat(25)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(jdbcTemplate.queryForObject("select count(*) from auth_sessions where member_id=?",
                Long.class, member.memberId())).isZero();
    }

    @Test
    @DisplayName("같은 이메일의 동시 가입은 하나만 생성하고 다른 요청은 409를 반환한다")
    void signup_concurrentEmail_createsOneMember() throws Exception {
        String body = objectMapper
                .writeValueAsString(new SignupRequest(UUID.randomUUID() + "@example.com", "password123", "러너"));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> signup = () -> {
                start.await(5, TimeUnit.SECONDS);
                return mockMvc
                        .perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };
            Future<Integer> first = executor.submit(signup);
            Future<Integer> second = executor.submit(signup);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from members where email = ?", Long.class,
                objectMapper.readTree(body).path("email").asText())).isEqualTo(1);
    }
}
