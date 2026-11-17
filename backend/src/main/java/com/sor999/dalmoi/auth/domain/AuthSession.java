package com.sor999.dalmoi.auth.domain;

import com.sor999.dalmoi.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 128)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private AuthSession(Member member, String refreshTokenHash, Instant issuedAt, Instant expiresAt) {
        this.member = member;
        this.refreshTokenHash = refreshTokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static AuthSession create(Member member, String refreshTokenHash, Instant issuedAt, Instant expiresAt) {
        return builder()
                .member(member)
                .refreshTokenHash(refreshTokenHash)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}
