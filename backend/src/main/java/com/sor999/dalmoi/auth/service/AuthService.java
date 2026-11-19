package com.sor999.dalmoi.auth.service;

import com.sor999.dalmoi.auth.config.AuthProperties;
import com.sor999.dalmoi.auth.domain.AuthSession;
import com.sor999.dalmoi.auth.dto.SignupRequest;
import com.sor999.dalmoi.auth.dto.LoginRequest;
import com.sor999.dalmoi.auth.dto.LoginResponse;
import com.sor999.dalmoi.auth.dto.RefreshRequest;
import com.sor999.dalmoi.auth.dto.RefreshResponse;
import com.sor999.dalmoi.auth.repository.AuthSessionRepository;
import com.sor999.dalmoi.auth.security.MemberUserDetails;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.member.domain.Member;
import com.sor999.dalmoi.member.domain.MemberCredential;
import com.sor999.dalmoi.member.dto.MemberResponse;
import com.sor999.dalmoi.member.repository.MemberCredentialRepository;
import com.sor999.dalmoi.member.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;

    private final MemberCredentialRepository memberCredentialRepository;

    private final AuthSessionRepository authSessionRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final TokenService tokenService;

    private final AuthProperties authProperties;

    private final Clock clock;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_AUTH_REQUEST);
        }

        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = memberRepository.saveAndFlush(Member.create(email, request.nickname().strip()));
        memberCredentialRepository
                .saveAndFlush(MemberCredential.create(member, passwordEncoder.encode(request.password())));

        return MemberResponse.from(member);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (BadCredentialsException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        MemberUserDetails principal = (MemberUserDetails) authentication.getPrincipal();
        Member member = memberRepository.getReferenceById(principal.getMemberId());

        String refreshToken = tokenService.refreshToken();
        Instant now = clock.instant();
        authSessionRepository.save(AuthSession.create(member, tokenService.hash(refreshToken), now,
                now.plus(authProperties.refreshTtl())));

        return LoginResponse.of(tokenService.accessToken(member.getId()), refreshToken,
                tokenService.accessExpiresInSeconds());
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        AuthSession session = authSessionRepository.findByRefreshTokenHash(tokenService.hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        session.validateRefresh(clock.instant());

        return RefreshResponse.of(tokenService.accessToken(session.getMember().getId()),
                tokenService.accessExpiresInSeconds());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        authSessionRepository.findByRefreshTokenHash(tokenService.hash(request.refreshToken()))
                .ifPresent(s -> s.revoke(clock.instant()));
    }

    public MemberResponse me(Long memberId) {
        return MemberResponse.from(memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)));
    }
}
