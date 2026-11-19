package com.sor999.dalmoi.member.repository;

import com.sor999.dalmoi.member.domain.MemberCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCredentialRepository extends JpaRepository<MemberCredential, Long> {

    @EntityGraph(attributePaths = "member")
    Optional<MemberCredential> findByMemberEmail(String email);
}
