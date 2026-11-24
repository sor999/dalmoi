package com.sor999.dalmoi.course.service;

import com.sor999.dalmoi.course.domain.Course;
import com.sor999.dalmoi.course.dto.CreateCourseRequest;
import com.sor999.dalmoi.course.dto.CourseResponse;
import com.sor999.dalmoi.course.repository.CourseRepository;
import com.sor999.dalmoi.crew.repository.CrewRepository;
import com.sor999.dalmoi.crew.repository.CrewMemberRepository;
import com.sor999.dalmoi.global.exception.BusinessException;
import com.sor999.dalmoi.global.exception.ErrorCode;
import com.sor999.dalmoi.global.response.PageResponse;
import com.sor999.dalmoi.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    private final MemberRepository memberRepository;

    private final CrewRepository crewRepository;

    private final CrewMemberRepository crewMemberRepository;

    @Value("${dalmoi.course.loop-threshold-meters:20}")
    private double loopThresholdMeters;

    @Transactional
    public CourseResponse create(Long memberId, CreateCourseRequest request) {
        LineString route = GeoSupport.route(request.points());
        Course course = Course.create(memberRepository.getReferenceById(memberId), request.title().strip(), route,
                GeoSupport.length(route), GeoSupport.distance(route.getCoordinateN(0),
                        route.getCoordinateN(route.getNumPoints() - 1)) <= loopThresholdMeters);

        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse detail(Long memberId, Long courseId) {
        Course course = requireCourse(courseId);

        if (!course.getCreator().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.COURSE_NOT_ACCESSIBLE);
        }

        return CourseResponse.from(course);
    }

    public Course requireCrewCourse(Long courseId, Long crewId) {
        Course course = requireCourse(courseId);

        if (!crewMemberRepository.existsByCrewIdAndMemberIdAndLeftAtIsNull(crewId, course.getCreator().getId())) {
            throw new BusinessException(ErrorCode.COURSE_NOT_ACCESSIBLE);
        }

        return course;
    }

    private Course requireCourse(Long id) {
        return courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
    }

    public PageResponse<CourseResponse.Summary> mine(Long memberId, int page, int size) {
        return PageResponse.from(courseRepository
                .findByCreatorIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(memberId, PageRequest.of(page, size))
                .map(CourseResponse.Summary::from));
    }

    public PageResponse<CourseResponse> shared(Long memberId, Long crewId, int page, int size) {
        if (!crewRepository.existsById(crewId)) {
            throw new BusinessException(ErrorCode.CREW_NOT_FOUND);
        }
        if (!crewMemberRepository.existsByCrewIdAndMemberIdAndLeftAtIsNull(crewId, memberId)) {
            throw new BusinessException(ErrorCode.NOT_CREW_MEMBER);
        }

        return PageResponse
                .from(courseRepository.findCrewCourses(crewId, PageRequest.of(page, size)).map(CourseResponse::from));
    }
}
