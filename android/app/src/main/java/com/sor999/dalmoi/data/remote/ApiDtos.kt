package com.sor999.dalmoi.data.remote

import com.sor999.dalmoi.domain.model.Course
import com.sor999.dalmoi.domain.model.Crew
import com.sor999.dalmoi.domain.model.CrewMember
import com.sor999.dalmoi.domain.model.Dashboard
import com.sor999.dalmoi.domain.model.FeaturedWaitingSession
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.domain.model.Member
import com.sor999.dalmoi.domain.model.RunRecord
import com.sor999.dalmoi.domain.model.Runner
import com.sor999.dalmoi.domain.model.RunningSession
import com.sor999.dalmoi.domain.model.SessionSummary
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class Envelope<T>(val data: T)

@Serializable
internal data class ErrorDto(val code: String)

@Serializable
internal data class LoginRequest(val email: String, val password: String)

@Serializable
internal data class SignupRequest(val email: String, val password: String, val nickname: String)

@Serializable
internal data class RefreshRequest(val refreshToken: String)

@Serializable
internal data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
)

@Serializable
internal data class RefreshResponse(val accessToken: String, val accessTokenExpiresInSeconds: Long)

@Serializable
internal data class MemberDto(val memberId: Long, val email: String, val nickname: String) {
    fun toDomain() = Member(memberId, email, nickname)
}

@Serializable
internal data class PointDto(val latitude: Double, val longitude: Double) {
    fun toDomain() = GeoPoint(latitude, longitude)
}

@Serializable
internal data class CourseDto(
    val courseId: Long,
    val title: String,
    val estimatedDistanceKm: Double,
    val createdAt: String,
    val points: List<PointDto> = emptyList(),
    val isLoop: Boolean = false,
) {
    fun toDomain() = Course(
        courseId,
        title,
        estimatedDistanceKm,
        points.map {
            it.toDomain()
        },
        Instant.parse(createdAt),
        isLoop,
    )
}

@Serializable
internal data class CreateCourseRequest(val title: String, val points: List<PointDto>)

@Serializable
internal data class CreateCrewRequest(val name: String)

@Serializable
internal data class JoinCrewRequest(val inviteCode: String)

@Serializable
internal data class CrewIdDto(val crewId: Long)

@Serializable
internal data class InviteCodeDto(val inviteCode: String)

@Serializable
internal data class CrewMemberDto(val memberId: Long, val nickname: String, val role: String)

@Serializable
internal data class WaitingDto(val sessionId: Long, val title: String, val status: String = "WAITING") {
    fun toDomain() = SessionSummary(sessionId, title, status)
}

@Serializable
internal data class CrewDto(
    val crewId: Long,
    val name: String,
    val memberCount: Long,
    val myRole: String,
    val members: List<CrewMemberDto>,
    val waitingSession: WaitingDto? = null,
) {
    fun toDomain() = Crew(
        crewId,
        name,
        memberCount,
        myRole,
        members.map { CrewMember(it.memberId, it.nickname, it.role) },
        waitingSession?.sessionId,
    )
}

@Serializable
internal data class SummaryDto<T>(val totalCount: Long, val items: List<T>)

@Serializable
internal data class FeaturedWaitingDto(
    val sessionId: Long,
    val title: String,
    val status: String,
    val crewName: String,
    val courseTitle: String,
    val estimatedDistanceKm: Double,
    val participantCount: Long,
    val crewMemberCount: Long,
) {
    fun toDomain() = FeaturedWaitingSession(
        sessionId,
        title,
        status,
        crewName,
        courseTitle,
        estimatedDistanceKm,
        participantCount,
        crewMemberCount,
    )
}

@Serializable
internal data class HomeDto(
    val member: MemberDto,
    val featuredWaitingSession: FeaturedWaitingDto? = null,
    val crewSummary: SummaryDto<CrewDto>,
    val courseSummary: SummaryDto<CourseDto>,
) {
    fun toDomain() = Dashboard(
        member.toDomain(),
        crewSummary.items.map { it.toDomain() },
        courseSummary.items.map { it.toDomain() },
        featuredWaitingSession?.toDomain(),
        crewSummary.totalCount,
        courseSummary.totalCount,
    )
}

@Serializable
internal data class PageDto<T>(val items: List<T>, val page: Int, val hasNext: Boolean)

@Serializable
internal data class CreateSessionRequest(val title: String, val courseId: Long)

@Serializable
internal data class CourseSnapshotDto(
    val courseId: Long,
    val title: String,
    val estimatedDistanceKm: Double,
    val points: List<PointDto>,
)

@Serializable
internal data class ParticipantDto(val nickname: String, val status: String, val memberId: Long? = null)

@Serializable
internal data class MemberStatusDto(
    val memberId: Long,
    val nickname: String,
    val role: String,
    val participationStatus: String,
)

@Serializable
internal data class SessionDto(
    val sessionId: Long,
    val crewId: Long,
    val title: String,
    val crewName: String,
    val status: String,
    val myParticipationStatus: String,
    val canStartSession: Boolean,
    val canJoinSession: Boolean,
    val course: CourseSnapshotDto,
    val participants: List<ParticipantDto>,
    val createdAt: String,
    val crewMemberCount: Long = participants.size.toLong(),
    val members: List<MemberStatusDto>? = null,
    val myStartedAt: String? = null,
) {
    fun toDomain() = RunningSession(
        sessionId, crewId, title, crewName, status, myParticipationStatus,
        canStartSession, canJoinSession,
        Course(
            course.courseId,
            course.title,
            course.estimatedDistanceKm,
            course.points.map { it.toDomain() },
            Instant.parse(createdAt),
        ),
        participants.map { Runner(it.nickname, it.status, it.memberId) },
        crewMemberCount,
        members?.map { Runner(it.nickname, it.participationStatus, it.memberId, it.role) }
            ?: participants.map { Runner(it.nickname, it.status, it.memberId) },
        myStartedAt?.let(Instant::parse),
    )
}

@Serializable
internal data class RecordDto(
    val recordId: Long,
    val sessionTitle: String,
    val distanceKm: Double,
    val durationSeconds: Long,
    val endedAt: String,
    val actualRoute: List<PointDto> = emptyList(),
    val plannedDistanceKm: Double? = null,
    val plannedRoute: List<PointDto> = emptyList(),
    val crewName: String? = null,
) {
    fun toDomain() = RunRecord(
        recordId,
        sessionTitle,
        distanceKm,
        durationSeconds,
        Instant.parse(endedAt),
        actualRoute.map { it.toDomain() },
        plannedDistanceKm,
        plannedRoute.map { it.toDomain() },
        crewName,
    )
}

@Serializable
internal data class RecordStatusDto(val recordStatus: String, val recordId: Long? = null)
