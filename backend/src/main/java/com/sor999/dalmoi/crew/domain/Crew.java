package com.sor999.dalmoi.crew.domain;

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
@Table(name = "crews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_member_id", nullable = false, updatable = false)
    private Member owner;

    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    @Column(name = "max_members", nullable = false)
    private short maxMembers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Crew(Member owner, String name, String inviteCode) {
        this.owner = owner;
        this.name = name;
        this.inviteCode = inviteCode;
        this.maxMembers = 10;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static Crew create(Member owner, String name, String inviteCode) {
        return builder()
                .owner(owner)
                .name(name)
                .inviteCode(inviteCode)
                .build();
    }
}
