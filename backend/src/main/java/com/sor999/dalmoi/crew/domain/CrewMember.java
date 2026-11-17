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
@Table(name = "crew_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false, updatable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder(access = AccessLevel.PRIVATE)
    private CrewMember(Crew crew, Member member) {
        this.crew = crew;
        this.member = member;
        this.joinedAt = Instant.now();
    }

    public static CrewMember create(Crew crew, Member member) {
        return builder().crew(crew).member(member).build();
    }
}
