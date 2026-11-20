package com.sor999.dalmoi.presentation.backend

import androidx.lifecycle.ViewModelStore
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.domain.model.Course
import com.sor999.dalmoi.domain.model.Crew
import com.sor999.dalmoi.domain.model.Dashboard
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.domain.model.Member
import com.sor999.dalmoi.domain.model.Page
import com.sor999.dalmoi.domain.model.RecordStatus
import com.sor999.dalmoi.domain.model.RunRecord
import com.sor999.dalmoi.domain.model.RunningSession
import com.sor999.dalmoi.domain.model.SessionSummary
import com.sor999.dalmoi.domain.repository.BackendRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun teardown() {
        store.clear()
        Dispatchers.resetMain()
    }
    private fun model(repository: BackendRepository) = BackendViewModel(repository).also { store.put("model", it) }

    @Test fun failedLoginKeepsInputAndDoesNotNavigate() = runTest {
        val repository = FakeRepository()
        repository.authResult = Result.failure(ApiFailure(ApiFailureKind.Unauthorized, "INVALID_CREDENTIALS"))
        val model = model(repository)
        model.changeEmail("runner@example.com")
        model.changePassword("wrong-password")
        model.authenticate()
        advanceUntilIdle()
        assertFalse(model.uiState.value.signedIn)
        assertEquals("wrong-password", model.uiState.value.password)
        assertEquals("runner@example.com", model.uiState.value.email)
        assertEquals("INVALID_CREDENTIALS", model.uiState.value.error?.code)
        assertFalse(model.uiState.value.busy)
    }

    @Test fun duplicateSubmitIsBlockedUntilLoginCompletes() = runTest {
        val repository = FakeRepository()
        val release = CompletableDeferred<Unit>()
        repository.release = release
        val model = model(repository)
        model.changePassword("password123")
        model.authenticate()
        model.authenticate()
        runCurrent()
        assertEquals(1, repository.authCalls)
        assertTrue(model.uiState.value.busy)
        release.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.uiState.value.signedIn)
        assertEquals("", model.uiState.value.password)
        assertEquals("러너", model.uiState.value.dashboard?.member?.nickname)
    }

    @Test fun expiredSessionClearsAuthenticatedDataAndLoginState() = runTest {
        val repository = FakeRepository()
        val model = model(repository)
        model.authenticate()
        advanceUntilIdle()
        repository.signedIn.value = false
        advanceUntilIdle()
        assertFalse(model.uiState.value.signedIn)
        assertNull(model.uiState.value.dashboard)
        assertNull(model.uiState.value.crew)
        assertNull(model.uiState.value.session)
        assertEquals(ApiFailureKind.Unauthorized, model.uiState.value.error?.kind)
    }

    private class FakeRepository : StubRepository() {
        override val signedIn = MutableStateFlow(false)
        var authResult: Result<Unit> = Result.success(Unit)
        var authCalls = 0
        var release: CompletableDeferred<Unit>? = null
        override suspend fun authenticate(email: String, password: String, nickname: String?): Result<Unit> {
            authCalls++
            release?.await()
            if (authResult.isSuccess) signedIn.value = true
            return authResult
        }
        override suspend fun home() = Result.success(
            Dashboard(
                Member(1, "runner@example.com", "러너"),
                emptyList(),
                emptyList(),
                null,
            ),
        )
    }
    private abstract class StubRepository : BackendRepository {
        override suspend fun authenticate(email: String, password: String, nickname: String?): Result<Unit> =
            Result.failure(UnsupportedOperationException())
        override suspend fun logout(): Result<Unit> = Result.failure(UnsupportedOperationException())
        override suspend fun home(): Result<Dashboard> = Result.failure(UnsupportedOperationException())
        override suspend fun courses(page: Int): Result<Page<Course>> = Result.failure(UnsupportedOperationException())
        override suspend fun course(id: Long): Result<Course> = Result.failure(UnsupportedOperationException())
        override suspend fun createCourse(title: String, points: List<GeoPoint>): Result<Course> =
            Result.failure(UnsupportedOperationException())
        override suspend fun crews(): Result<List<Crew>> = Result.failure(UnsupportedOperationException())
        override suspend fun crew(id: Long): Result<Crew> = Result.failure(UnsupportedOperationException())
        override suspend fun createCrew(name: String): Result<Crew> = Result.failure(UnsupportedOperationException())
        override suspend fun joinCrew(code: String): Result<Crew> = Result.failure(UnsupportedOperationException())
        override suspend fun inviteCode(crewId: Long): Result<String> = Result.failure(UnsupportedOperationException())
        override suspend fun sharedCourses(crewId: Long, page: Int): Result<Page<Course>> =
            Result.failure(UnsupportedOperationException())
        override suspend fun sessions(crewId: Long): Result<List<SessionSummary>> =
            Result.failure(UnsupportedOperationException())
        override suspend fun session(id: Long): Result<RunningSession> = Result.failure(UnsupportedOperationException())
        override suspend fun createSession(crewId: Long, title: String, courseId: Long): Result<RunningSession> =
            Result.failure(UnsupportedOperationException())
        override suspend fun sessionCommand(id: Long, command: String): Result<Unit> =
            Result.failure(UnsupportedOperationException())
        override suspend fun recordStatus(sessionId: Long): Result<RecordStatus> =
            Result.failure(UnsupportedOperationException())
        override suspend fun records(page: Int): Result<Page<RunRecord>> =
            Result.failure(UnsupportedOperationException())
        override suspend fun record(id: Long): Result<RunRecord> = Result.failure(UnsupportedOperationException())
    }
}
