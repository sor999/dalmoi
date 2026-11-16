package com.sor999.dalmoi.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCredential {

    @Id
    @Column(name = "member_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private MemberCredential(Member member, String passwordHash) {
        this.member = member;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public static MemberCredential create(Member member, String passwordHash) {
        return builder()
                .member(member)
                .passwordHash(passwordHash)
                .build();
    }
}
