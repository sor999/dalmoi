package com.sor999.dalmoi.home.dto;

import com.sor999.dalmoi.member.dto.MemberResponse;
import com.sor999.dalmoi.crew.dto.CrewResponse;
import com.sor999.dalmoi.course.dto.CourseResponse;
import java.util.List;

public record HomeResponse(MemberResponse member, WaitingSession featuredWaitingSession,
        Summary<CrewResponse> crewSummary, Summary<CourseResponse.Summary> courseSummary) {

    public record Summary<T>(long totalCount, List<T> items) {
    }

    public record WaitingSession(Long sessionId, Long crewId, String crewName, String title, String status,
            Long courseId, String courseTitle, double estimatedDistanceKm, long participantCount, long crewMemberCount,
            String myParticipationStatus) {
    }
}
