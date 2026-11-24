package com.sor999.dalmoi.auth.service;

import com.sor999.dalmoi.auth.security.MemberUserDetails;
import com.sor999.dalmoi.member.domain.MemberCredential;
import com.sor999.dalmoi.member.repository.MemberCredentialRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberCredentialRepository memberCredentialRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username.strip().toLowerCase(Locale.ROOT);
        MemberCredential credential = memberCredentialRepository.findByMemberEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원 인증 정보를 찾을 수 없습니다."));

        return MemberUserDetails.from(credential);
    }
}
