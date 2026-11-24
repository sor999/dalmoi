package com.sor999.dalmoi.auth.security;

import com.sor999.dalmoi.member.domain.MemberCredential;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.userdetails.User;

public class MemberUserDetails extends User {

    private static final long serialVersionUID = 1L;

    @Getter
    private final Long memberId;

    private MemberUserDetails(Long memberId, String email, String passwordHash) {
        super(email, passwordHash, List.of());
        this.memberId = memberId;
    }

    public static MemberUserDetails from(MemberCredential credential) {
        return new MemberUserDetails(credential.getMember().getId(), credential.getMember().getEmail(),
                credential.getPasswordHash());
    }
}
