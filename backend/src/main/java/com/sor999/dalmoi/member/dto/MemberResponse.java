package com.sor999.dalmoi.member.dto;

import com.sor999.dalmoi.member.domain.Member;

public record MemberResponse(Long memberId, String email, String nickname, String profileImageUrl) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname(), null);
    }
}
