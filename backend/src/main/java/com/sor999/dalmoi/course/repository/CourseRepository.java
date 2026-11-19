package com.sor999.dalmoi.course.repository;

import com.sor999.dalmoi.course.domain.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Slice<Course> findByCreatorIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

    long countByCreatorIdAndDeletedAtIsNull(Long memberId);

    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    @Query("select c from Course c where c.deletedAt is null and c.creator.id in (select cm.member.id from CrewMember cm where cm.crew.id = :crewId and cm.leftAt is null) order by c.createdAt desc, c.id desc")
    Slice<Course> findCrewCourses(Long crewId, Pageable pageable);
}
