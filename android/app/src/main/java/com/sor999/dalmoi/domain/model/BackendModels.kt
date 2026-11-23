package com.sor999.dalmoi.domain.model

import java.time.Instant

data class Member(val id: Long, val email: String, val nickname: String)
data class GeoPoint(val latitude: Double, val longitude: Double)
data class Course(
    val id: Long,
    val title: String,
    val distanceKm: Double,
    val points: List<GeoPoint>,
    val createdAt: Instant,
    val isLoop: Boolean = points.size > 1 && points.first() == points.last(),
)
data class CrewMember(val id: Long, val nickname: String, val role: String)
data class Crew(
    val id: Long,
    val name: String,
    val memberCount: Long,
    val myRole: String,
    val members: List<CrewMember>,
    val waitingSessionId: Long?,
)
data class RunningSession(
    val id: Long,
    val crewId: Long,
    val title: String,
    val crewName: String,
    val status: String,
    val myStatus: String,
    val canStart: Boolean,
    val canJoin: Boolean,
    val course: Course,
    val participants: List<Runner>,
    val crewMemberCount: Long = participants.size.toLong(),
    val memberStatuses: List<Runner> = participants,
    val myStartedAt: Instant? = null,
)
data class Runner(val nickname: String, val status: String, val memberId: Long? = null, val role: String? = null)
data class SessionSummary(val id: Long, val title: String, val status: String)
data class FeaturedWaitingSession(
    val id: Long,
    val title: String,
    val status: String,
    val crewName: String,
    val courseTitle: String,
    val distanceKm: Double,
    val participantCount: Long,
    val crewMemberCount: Long,
)
data class RunRecord(
    val id: Long,
    val title: String,
    val distanceKm: Double,
    val durationSeconds: Long,
    val endedAt: Instant,
    val actualRoute: List<GeoPoint> = emptyList(),
    val plannedDistanceKm: Double? = null,
    val plannedRoute: List<GeoPoint> = emptyList(),
    val crewName: String? = null,
)
data class RecordStatus(val status: String, val recordId: Long?)
data class Dashboard(
    val member: Member,
    val crews: List<Crew>,
    val courses: List<Course>,
    val waiting: FeaturedWaitingSession?,
    val crewCount: Long = crews.size.toLong(),
    val courseCount: Long = courses.size.toLong(),
)
data class Page<T>(val items: List<T>, val page: Int, val hasNext: Boolean)

enum class ApiFailureKind {
    Network,
    InvalidResponse,
    Unauthorized,
    InvalidInput,
    Conflict,
    Forbidden,
    NotFound,
    Server,
}
class ApiFailure(val kind: ApiFailureKind, val code: String? = null) : Exception(kind.name)
