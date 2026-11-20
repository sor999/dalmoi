package com.sor999.dalmoi.data.repository

import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.local.Tokens
import com.sor999.dalmoi.data.remote.ApiClient
import com.sor999.dalmoi.data.remote.CourseDto
import com.sor999.dalmoi.data.remote.CreateCourseRequest
import com.sor999.dalmoi.data.remote.CreateCrewRequest
import com.sor999.dalmoi.data.remote.CreateSessionRequest
import com.sor999.dalmoi.data.remote.CrewDto
import com.sor999.dalmoi.data.remote.CrewIdDto
import com.sor999.dalmoi.data.remote.HomeDto
import com.sor999.dalmoi.data.remote.InviteCodeDto
import com.sor999.dalmoi.data.remote.JoinCrewRequest
import com.sor999.dalmoi.data.remote.LoginRequest
import com.sor999.dalmoi.data.remote.LoginResponse
import com.sor999.dalmoi.data.remote.PageDto
import com.sor999.dalmoi.data.remote.PointDto
import com.sor999.dalmoi.data.remote.RecordDto
import com.sor999.dalmoi.data.remote.RecordStatusDto
import com.sor999.dalmoi.data.remote.RefreshRequest
import com.sor999.dalmoi.data.remote.SessionDto
import com.sor999.dalmoi.data.remote.SignupRequest
import com.sor999.dalmoi.data.remote.WaitingDto
import com.sor999.dalmoi.data.remote.apiJson
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.domain.model.Page
import com.sor999.dalmoi.domain.model.RecordStatus
import com.sor999.dalmoi.domain.repository.BackendRepository
import io.ktor.http.HttpMethod
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class HttpBackendRepository(private val api: ApiClient, private val auth: AuthSession) : BackendRepository {
    override val signedIn = auth.signedIn

    override suspend fun authenticate(email: String, password: String, nickname: String?) = result {
        if (nickname != null) {
            api.call(
                "auth/signup",
                HttpMethod.Post,
                apiJson.encodeToJsonElement(SignupRequest(email, password, nickname)),
                false,
            )
        }
        val login: LoginResponse = post("auth/login", LoginRequest(email, password), authenticated = false)
        auth.save(Tokens(login.accessToken, login.refreshToken))
    }

    override suspend fun logout() = result {
        val previous = auth.current
        auth.clearIfCurrent(previous)
        if (previous != null) {
            api.call(
                "auth/logout",
                HttpMethod.Post,
                apiJson.encodeToJsonElement(RefreshRequest(previous.refreshToken)),
                false,
            )
        }
        Unit
    }

    override suspend fun home() = result { get<HomeDto>("home/summary").toDomain() }
    override suspend fun courses(page: Int) = result {
        val response = get<PageDto<CourseDto>>("courses/me?page=$page&size=20")
        Page(response.items.map { it.toDomain() }, response.page, response.hasNext)
    }
    override suspend fun course(id: Long) = result { get<CourseDto>("courses/$id").toDomain() }
    override suspend fun createCourse(title: String, points: List<GeoPoint>) = result {
        post<CourseDto, CreateCourseRequest>(
            "courses",
            CreateCourseRequest(
                title,
                points.map { PointDto(it.latitude, it.longitude) },
            ),
        ).toDomain()
    }
    override suspend fun crews() = result { get<List<CrewDto>>("crews/me").map { it.toDomain() } }
    override suspend fun crew(id: Long) = result { get<CrewDto>("crews/$id").toDomain() }
    override suspend fun createCrew(name: String) = result {
        val response = post<CrewIdDto, CreateCrewRequest>("crews", CreateCrewRequest(name))
        get<CrewDto>("crews/${response.crewId}").toDomain()
    }
    override suspend fun joinCrew(code: String) = result {
        val response = post<CrewIdDto, JoinCrewRequest>("crews/join", JoinCrewRequest(code.trim().uppercase()))
        get<CrewDto>("crews/${response.crewId}").toDomain()
    }
    override suspend fun inviteCode(crewId: Long) =
        result { get<InviteCodeDto>("crews/$crewId/invite-code").inviteCode }
    override suspend fun sharedCourses(crewId: Long, page: Int) = result {
        val response = get<PageDto<CourseDto>>("crews/$crewId/courses?page=$page&size=20")
        Page(response.items.map { it.toDomain() }, response.page, response.hasNext)
    }
    override suspend fun sessions(crewId: Long) = result {
        get<List<WaitingDto>>("crews/$crewId/sessions").map { it.toDomain() }
    }
    override suspend fun session(id: Long) = result { get<SessionDto>("sessions/$id").toDomain() }
    override suspend fun createSession(crewId: Long, title: String, courseId: Long) = result {
        post<SessionDto, CreateSessionRequest>(
            "crews/$crewId/sessions",
            CreateSessionRequest(title, courseId),
        ).toDomain()
    }
    override suspend fun sessionCommand(id: Long, command: String) = result {
        require(command in setOf("participants", "start", "me/start", "me/finish", "complete"))
        api.call("sessions/$id/$command", HttpMethod.Post)
        Unit
    }
    override suspend fun recordStatus(sessionId: Long) = result {
        val response = get<RecordStatusDto>("sessions/$sessionId/me/record-status")
        RecordStatus(response.recordStatus, response.recordId)
    }
    override suspend fun records(page: Int) = result {
        val response = get<PageDto<RecordDto>>("records/me?page=$page&size=20")
        Page(response.items.map { it.toDomain() }, response.page, response.hasNext)
    }
    override suspend fun record(id: Long) = result { get<RecordDto>("records/$id").toDomain() }

    private suspend inline fun <reified T> get(path: String): T = decode(api.call(path))
    private suspend inline fun <reified T, reified B> post(path: String, body: B, authenticated: Boolean = true): T =
        decode(api.call(path, HttpMethod.Post, apiJson.encodeToJsonElement(body), authenticated))
    private inline fun <reified T> decode(value: JsonElement?): T =
        apiJson.decodeFromJsonElement(value ?: throw ApiFailure(ApiFailureKind.InvalidResponse))

    private suspend fun <T> result(action: suspend () -> T): Result<T> = try {
        Result.success(action())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ApiFailure) {
        Result.failure(failure)
    } catch (_: SerializationException) {
        Result.failure(ApiFailure(ApiFailureKind.InvalidResponse))
    } catch (_: IOException) {
        Result.failure(ApiFailure(ApiFailureKind.Network))
    } catch (_: Exception) {
        Result.failure(ApiFailure(ApiFailureKind.Server))
    }
}
