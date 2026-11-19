package com.sor999.dalmoi.home.service;

import com.sor999.dalmoi.auth.service.AuthService;
import com.sor999.dalmoi.course.service.GeoSupport;
import com.sor999.dalmoi.course.dto.CourseResponse;
import com.sor999.dalmoi.course.repository.CourseRepository;
import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.crew.service.CrewSummaryService;
import com.sor999.dalmoi.crew.repository.CrewMemberRepository;
import com.sor999.dalmoi.home.dto.HomeResponse;
import com.sor999.dalmoi.session.domain.RunningSession;
import com.sor999.dalmoi.session.repository.RunningSessionRepository;
import com.sor999.dalmoi.session.repository.SessionParticipantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final AuthService authService;

    private final CrewSummaryService crewSummaryService;

    private final CrewMemberRepository crewMemberRepository;

    private final CourseRepository courseRepository;

    private final RunningSessionRepository runningSessionRepository;

    private final SessionParticipantRepository sessionParticipantRepository;

    public HomeResponse summary(Long memberId) {
        List<CrewResponse> crews = crewSummaryService.mine(memberId);
        List<CourseResponse.Summary> courses = courseRepository
                .findByCreatorIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(memberId, PageRequest.of(0, 3))
                .map(CourseResponse.Summary::from)
                .getContent();
        List<RunningSession> sessions = runningSessionRepository.findMyWaiting(memberId, PageRequest.of(0, 1));
        HomeResponse.WaitingSession featured = null;

        if (!sessions.isEmpty()) {
            RunningSession s = sessions.getFirst();
            featured = new HomeResponse.WaitingSession(s.getId(), s.getCrew().getId(), s.getCrewNameSnapshot(),
                    s.getTitle(), s.getStatus().name(), s.getCourse().getId(), s.getPlannedCourseTitle(),
                    GeoSupport.km(s.getPlannedDistanceMeters()),
                    sessionParticipantRepository.countBySessionId(s.getId()),
                    crewMemberRepository.countByCrewIdAndLeftAtIsNull(s.getCrew().getId()),
                    "JOINED");
        }

        return new HomeResponse(authService.me(memberId), featured,
                new HomeResponse.Summary<>(crews.size(), crews.stream().limit(3).toList()),
                new HomeResponse.Summary<>(courseRepository.countByCreatorIdAndDeletedAtIsNull(memberId), courses));
    }
}
