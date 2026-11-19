package com.sor999.dalmoi.crew.repository;

import com.sor999.dalmoi.crew.domain.CrewMember;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    interface CrewCount {
        Long getCrewId();

        long getTotal();
    }

    @Query("select cm.crew.id as crewId, count(cm) as total from CrewMember cm where cm.crew.id in :crewIds and cm.leftAt is null group by cm.crew.id")
    List<CrewCount> countMembers(Collection<Long> crewIds);

    boolean existsByCrewIdAndMemberIdAndLeftAtIsNull(Long crewId, Long memberId);

    long countByCrewIdAndLeftAtIsNull(Long crewId);

    List<CrewMember> findByCrewIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(Long crewId);

    @EntityGraph(attributePaths = {"crew", "crew.owner"})
    List<CrewMember> findByMemberIdAndLeftAtIsNullOrderByJoinedAtDescIdDesc(Long memberId);
}
