package com.sor999.dalmoi.data

import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.local.TokenStore
import com.sor999.dalmoi.data.local.Tokens
import com.sor999.dalmoi.data.remote.ApiClient
import com.sor999.dalmoi.data.remote.createHttpClient
import com.sor999.dalmoi.data.repository.HttpBackendRepository
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpBackendRepositoryTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun loginPersistsTokensAndHomeUsesBearerAndMapsEnvelope() = runTest {
        val store = MemoryTokenStore()
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/login" -> {
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond(LOGIN_JSON, headers = jsonHeaders)
                }

                "/api/v1/home/summary" -> {
                    assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
                    respond(HOME_JSON, headers = jsonHeaders)
                }

                else -> error("Unexpected path")
            }
        }
        createHttpClient(engine).use { client ->
            val auth = AuthSession(store)
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            assertTrue(repository.authenticate("runner@example.com", "password123", null).isSuccess)
            val home = repository.home().getOrThrow()
            assertEquals("러너", home.member.nickname)
            assertTrue(home.crews.isEmpty())
            assertEquals("refresh", store.current?.refreshToken)
            assertTrue(repository.signedIn.value)
        }
    }

    @Test
    fun concurrentExpiredRequestsRefreshOnceAndKeepExistingRefreshToken() = runTest {
        val store = MemoryTokenStore(Tokens("expired", "same-refresh"))
        val auth = AuthSession(store)
        var refreshCalls = 0
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("auth/refresh") -> {
                    refreshCalls++
                    respond(
                        """
{
  "data": {
    "accessToken": "new-access",
    "accessTokenExpiresInSeconds": 1800
  }
}
""",
                        headers = jsonHeaders,
                    )
                }

                request.headers[HttpHeaders.Authorization] == "Bearer expired" ->
                    respondError(
                        HttpStatusCode.Unauthorized,
                        """
{
  "code": "INVALID_ACCESS_TOKEN"
}
""",
                        jsonHeaders,
                    )

                else -> {
                    assertEquals("Bearer new-access", request.headers[HttpHeaders.Authorization])
                    respond(HOME_JSON, headers = jsonHeaders)
                }
            }
        }
        createHttpClient(engine).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            val results = List(2) { async { repository.home().getOrThrow() } }.awaitAll()
            assertEquals(2, results.size)
            assertEquals(1, refreshCalls)
            assertEquals(Tokens("new-access", "same-refresh"), store.current)
        }
    }

    @Test
    fun revokedRefreshClearsSessionAndDoesNotLoop() = runTest {
        val store = MemoryTokenStore(Tokens("expired", "revoked"))
        val auth = AuthSession(store)
        var requests = 0
        val engine = MockEngine {
            requests++
            respondError(
                HttpStatusCode.Unauthorized,
                """
{
  "code": "REVOKED_REFRESH_TOKEN"
}
""",
                jsonHeaders,
            )
        }
        createHttpClient(engine).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            val error = repository.home().exceptionOrNull() as ApiFailure
            assertEquals(ApiFailureKind.Unauthorized, error.kind)
            assertEquals(2, requests)
            assertNull(store.current)
            assertFalse(repository.signedIn.value)
        }
    }

    @Test
    fun serverConflictIsNotRetriedAndRetainsErrorCode() = runTest {
        var requests = 0
        val auth = AuthSession(MemoryTokenStore(Tokens("access", "refresh")))
        createHttpClient(
            MockEngine {
                requests++
                respondError(
                    HttpStatusCode.Conflict,
                    """
{
  "code": "CREW_CAPACITY_EXCEEDED",
  "message": "internal"
}
""",
                    jsonHeaders,
                )
            },
        ).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            val error = repository.joinCrew(" ABC123 ").exceptionOrNull() as ApiFailure
            assertEquals("CREW_CAPACITY_EXCEEDED", error.code)
            assertEquals(ApiFailureKind.Conflict, error.kind)
            assertEquals(1, requests)
        }
    }

    @Test
    fun logoutAcceptsEmpty204AndClearsLocalTokens() = runTest {
        val store = MemoryTokenStore(Tokens("access", "refresh"))
        val auth = AuthSession(store)
        createHttpClient(
            MockEngine { request ->
                assertTrue(request.url.encodedPath.endsWith("auth/logout"))
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("", HttpStatusCode.NoContent)
            },
        ).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            assertTrue(repository.logout().isSuccess)
            assertNull(store.current)
            assertFalse(repository.signedIn.value)
        }
    }

    @Test
    fun networkFailureDoesNotEraseSessionAndOfflineLogoutStillClearsIt() = runTest {
        val store = MemoryTokenStore(Tokens("access", "refresh"))
        val auth = AuthSession(store)
        createHttpClient(MockEngine { throw IOException("offline") }).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            assertEquals(ApiFailureKind.Network, (repository.home().exceptionOrNull() as ApiFailure).kind)
            assertTrue(auth.signedIn.value)
            assertTrue(repository.logout().isFailure)
            assertNull(store.current)
        }
    }

    @Test
    fun cancellationIsPropagatedInsteadOfBecomingFailure() = runTest {
        val auth = AuthSession(MemoryTokenStore(Tokens("access", "refresh")))
        createHttpClient(MockEngine { throw CancellationException("cancelled") }).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            var cancelled = false
            try {
                repository.home()
            } catch (_: CancellationException) {
                cancelled = true
            }
            assertTrue(cancelled)
        }
    }

    @Test
    fun logoutWhileRefreshIsInFlightCannotRestoreOldSession() = runTest {
        val refreshing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val store = MemoryTokenStore(Tokens("expired", "refresh"))
        val auth = AuthSession(store)
        createHttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("auth/refresh")) {
                    refreshing.complete(Unit)
                    release.await()
                    respond(
                        """
{
  "data": {
    "accessToken": "new",
    "accessTokenExpiresInSeconds": 1800
  }
}
""",
                        headers = jsonHeaders,
                    )
                } else {
                    respondError(
                        HttpStatusCode.Unauthorized,
                        """
{
  "code": "INVALID_ACCESS_TOKEN"
}
""",
                        jsonHeaders,
                    )
                }
            },
        ).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            val home = async { repository.home() }
            refreshing.await()
            auth.clearIfCurrent(store.current)
            release.complete(Unit)
            assertTrue(home.await().isFailure)
            assertNull(store.current)
            assertFalse(auth.signedIn.value)
        }
    }

    @Test
    fun paginatedCoursesMapKilometersAndKeepPageMetadata() = runTest {
        val auth = AuthSession(MemoryTokenStore(Tokens("access", "refresh")))
        createHttpClient(
            MockEngine { request ->
                assertEquals("2", request.url.parameters["page"])
                assertEquals("20", request.url.parameters["size"])
                respond(
                    """
{
  "data": {
    "items": [
      {
        "courseId": 7,
        "title": "한강",
        "estimatedDistanceKm": 1.25,
        "isLoop": false,
        "createdAt": "2026-11-20T00:00:00Z"
      }
    ],
    "page": 2,
    "size": 20,
    "hasNext": true
  }
}
""",
                    headers = jsonHeaders,
                )
            },
        ).use { client ->
            val page = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth).courses(2).getOrThrow()
            assertEquals(2, page.page)
            assertTrue(page.hasNext)
            assertEquals(1.25, page.items.single().distanceKm, 0.0)
            assertEquals(7L, page.items.single().id)
        }
    }

    @Test
    fun failedSignupDoesNotAttemptLoginOrCreateSession() = runTest {
        val auth = AuthSession(MemoryTokenStore())
        var requests = 0
        createHttpClient(
            MockEngine { request ->
                requests++
                assertTrue(request.url.encodedPath.endsWith("auth/signup"))
                respondError(
                    HttpStatusCode.Conflict,
                    """
{
  "code": "EMAIL_ALREADY_EXISTS"
}
""",
                    jsonHeaders,
                )
            },
        ).use { client ->
            val repository = HttpBackendRepository(ApiClient(client, BASE_URL, auth), auth)
            assertTrue(repository.authenticate("runner@example.com", "password123", "러너").isFailure)
            assertEquals(1, requests)
            assertFalse(repository.signedIn.value)
        }
    }

    private class MemoryTokenStore(override var current: Tokens? = null) : TokenStore {
        override fun save(tokens: Tokens) {
            current = tokens
        }
        override fun clear() {
            current = null
        }
    }

    companion object {
        private const val BASE_URL = "http://localhost/api/v1/"
        private const val LOGIN_JSON = """
{
  "data": {
    "accessToken": "access",
    "refreshToken": "refresh",
    "accessTokenExpiresInSeconds": 1800
  }
}
"""
        private const val HOME_JSON = """
{
  "data": {
    "member": {
      "memberId": 1,
      "email": "runner@example.com",
      "nickname": "러너"
    },
    "featuredWaitingSession": null,
    "crewSummary": {
      "totalCount": 0,
      "items": []
    },
    "courseSummary": {
      "totalCount": 0,
      "items": []
    }
  }
}
"""
    }
}
