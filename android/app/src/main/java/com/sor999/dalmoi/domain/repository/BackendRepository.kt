package com.sor999.dalmoi.domain.repository

import com.sor999.dalmoi.domain.model.Course
import com.sor999.dalmoi.domain.model.Crew
import com.sor999.dalmoi.domain.model.Dashboard
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.domain.model.Page
import com.sor999.dalmoi.domain.model.RecordStatus
import com.sor999.dalmoi.domain.model.RunRecord
import com.sor999.dalmoi.domain.model.RunningSession
import com.sor999.dalmoi.domain.model.SessionSummary
import kotlinx.coroutines.flow.StateFlow

interface BackendRepository {
    val signedIn: StateFlow<Boolean>
    suspend fun authenticate(email: String, password: String, nickname: String?): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun home(): Result<Dashboard>
    suspend fun courses(page: Int): Result<Page<Course>>
    suspend fun course(id: Long): Result<Course>
    suspend fun createCourse(title: String, points: List<GeoPoint>): Result<Course>
    suspend fun crews(): Result<List<Crew>>
    suspend fun crew(id: Long): Result<Crew>
    suspend fun createCrew(name: String): Result<Crew>
    suspend fun joinCrew(code: String): Result<Crew>
    suspend fun inviteCode(crewId: Long): Result<String>
    suspend fun sharedCourses(crewId: Long, page: Int): Result<Page<Course>>
    suspend fun sessions(crewId: Long): Result<List<SessionSummary>>
    suspend fun session(id: Long): Result<RunningSession>
    suspend fun createSession(crewId: Long, title: String, courseId: Long): Result<RunningSession>
    suspend fun sessionCommand(id: Long, command: String): Result<Unit>
    suspend fun recordStatus(sessionId: Long): Result<RecordStatus>
    suspend fun records(page: Int): Result<Page<RunRecord>>
    suspend fun record(id: Long): Result<RunRecord>
}
