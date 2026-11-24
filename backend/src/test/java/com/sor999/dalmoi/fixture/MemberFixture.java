package com.sor999.dalmoi.fixture;

import com.sor999.dalmoi.member.domain.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberFixture {

    public static Member createMember() {
        return Member.create(UUID.randomUUID() + "@example.com", "러너");
    }
}
