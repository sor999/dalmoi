package com.sor999.dalmoi.data.remote

import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.local.Tokens
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal val apiJson = Json { ignoreUnknownKeys = true }

internal fun createHttpClient(
    engine: HttpClientEngine = OkHttp.create {
        config { retryOnConnectionFailure(false) }
    },
) = HttpClient(engine) {
    expectSuccess = false
    followRedirects = false
    install(ContentNegotiation) { json(apiJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 20_000
        socketTimeoutMillis = 20_000
    }
}

internal class ApiClient(private val client: HttpClient, private val baseUrl: String, private val auth: AuthSession) {
    private val refreshMutex = Mutex()

    suspend fun call(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        payload: JsonElement? = null,
        authenticated: Boolean = true,
    ): JsonElement? {
        if (baseUrl.isBlank()) throw ApiFailure(ApiFailureKind.Server, "API_NOT_CONFIGURED")
        val original = if (authenticated) auth.current ?: throw ApiFailure(ApiFailureKind.Unauthorized) else null
        var response = send(path, method, payload, original)
        if (authenticated && response.status.value == 401) {
            val replacement = refreshMutex.withLock {
                val current = auth.current ?: throw ApiFailure(ApiFailureKind.Unauthorized)
                if (current.refreshToken != original?.refreshToken) throw ApiFailure(ApiFailureKind.Unauthorized)
                if (current != original) {
                    current
                } else {
                    val refresh = send(
                        "auth/refresh",
                        HttpMethod.Post,
                        apiJson.encodeToJsonElement(RefreshRequest.serializer(), RefreshRequest(current.refreshToken)),
                        null,
                    )
                    if (refresh.status.value !in 200..299) {
                        if (refresh.status.value in listOf(400, 401)) auth.clearIfCurrent(current)
                        throw error(refresh.status.value, refresh.bodyAsText())
                    }
                    val envelope = apiJson.decodeFromString(
                        Envelope.serializer(RefreshResponse.serializer()),
                        refresh.bodyAsText(),
                    )
                    val updated = current.copy(accessToken = envelope.data.accessToken)
                    if (!auth.replaceIfCurrent(current, updated)) throw ApiFailure(ApiFailureKind.Unauthorized)
                    updated
                }
            }
            response = send(path, method, payload, replacement)
            if (response.status.value == 401) auth.clearIfCurrent(replacement)
        }
        if (authenticated && auth.current?.refreshToken != original?.refreshToken) {
            throw ApiFailure(ApiFailureKind.Unauthorized)
        }
        if (response.status.value !in 200..299) throw error(response.status.value, response.bodyAsText())
        if (response.status.value == 204) return null
        return apiJson.parseToJsonElement(response.bodyAsText()).jsonObject["data"]
            ?: throw ApiFailure(ApiFailureKind.InvalidResponse)
    }

    private suspend fun send(path: String, method: HttpMethod, payload: JsonElement?, tokens: Tokens?) =
        client.request(baseUrl.trimEnd('/') + "/" + path) {
            this.method = method
            if (tokens != null) bearerAuth(tokens.accessToken)
            if (payload != null) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }

    private fun error(status: Int, body: String): ApiFailure {
        val code = try {
            apiJson.decodeFromString(ErrorDto.serializer(), body).code
        } catch (_: Exception) {
            null
        }
        val kind = when (status) {
            400 -> ApiFailureKind.InvalidInput
            401 -> ApiFailureKind.Unauthorized
            403 -> ApiFailureKind.Forbidden
            404 -> ApiFailureKind.NotFound
            409 -> ApiFailureKind.Conflict
            else -> ApiFailureKind.Server
        }
        return ApiFailure(kind, code)
    }
}
