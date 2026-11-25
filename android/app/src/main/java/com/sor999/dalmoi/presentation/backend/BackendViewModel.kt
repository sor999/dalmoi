package com.sor999.dalmoi.presentation.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.domain.model.Course
import com.sor999.dalmoi.domain.model.Crew
import com.sor999.dalmoi.domain.model.Dashboard
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.domain.model.RecordStatus
import com.sor999.dalmoi.domain.model.RunRecord
import com.sor999.dalmoi.domain.model.RunningSession
import com.sor999.dalmoi.domain.model.SessionSummary
import com.sor999.dalmoi.domain.repository.BackendRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BackendScreen {
    Home,
    Courses,
    CreateCourse,
    Course,
    Crews,
    JoinCrew,
    Crew,
    CreateSession,
    Session,
    Records,
    Record,
}

data class BackendUiState(
    val signedIn: Boolean = false,
    val screen: BackendScreen = BackendScreen.Home,
    val busy: Boolean = false,
    val error: ApiFailure? = null,
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val signingUp: Boolean = false,
    val dashboard: Dashboard? = null,
    val courses: List<Course> = emptyList(),
    val coursePage: Int = -1,
    val hasMoreCourses: Boolean = false,
    val course: Course? = null,
    val courseDraftTitle: String = "",
    val courseDraftPoints: List<GeoPoint> = emptyList(),
    val crews: List<Crew> = emptyList(),
    val crew: Crew? = null,
    val inviteCode: String? = null,
    val sessions: List<SessionSummary> = emptyList(),
    val sharedCourses: List<Course> = emptyList(),
    val sharedCoursePage: Int = -1,
    val hasMoreSharedCourses: Boolean = false,
    val session: RunningSession? = null,
    val recordStatus: RecordStatus? = null,
    val records: List<RunRecord> = emptyList(),
    val recordPage: Int = -1,
    val hasMoreRecords: Boolean = false,
    val record: RunRecord? = null,
)

class BackendViewModel(private val repository: BackendRepository, private val onRunFinished: () -> Unit = {}) :
    ViewModel() {
    private val mutableState = MutableStateFlow(BackendUiState(signedIn = repository.signedIn.value))
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var courseBackScreen = BackendScreen.Courses
    private var sessionBackScreen = BackendScreen.Crew

    init {
        viewModelScope.launch {
            repository.signedIn.collect { signedIn ->
                if (!signedIn && mutableState.value.signedIn) {
                    request?.cancel()
                    mutableState.value = BackendUiState(error = ApiFailure(ApiFailureKind.Unauthorized))
                }
            }
        }
        if (repository.signedIn.value) refresh()
    }

    fun changeEmail(value: String) {
        mutableState.update { it.copy(email = value, error = null) }
    }
    fun changePassword(value: String) {
        mutableState.update { it.copy(password = value, error = null) }
    }
    fun changeNickname(value: String) {
        mutableState.update { it.copy(nickname = value, error = null) }
    }
    fun selectSignup(value: Boolean) {
        mutableState.update { it.copy(signingUp = value, error = null) }
    }
    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    fun authenticate() = execute {
        val state = mutableState.value
        repository.authenticate(
            state.email.trim(),
            state.password,
            state.nickname.trim().takeIf { state.signingUp },
        ).getOrThrow()
        mutableState.update { it.copy(signedIn = true, password = "", screen = BackendScreen.Home) }
        loadHome()
    }

    fun logout() = execute {
        // Local credentials are erased even when the server cannot be reached.
        mutableState.value = BackendUiState(busy = true)
        repository.logout().getOrThrow()
        mutableState.value = BackendUiState()
    }

    fun open(screen: BackendScreen) {
        if (mutableState.value.busy) return
        if (screen == BackendScreen.CreateCourse) courseBackScreen = mutableState.value.screen
        mutableState.update { it.copy(screen = screen, error = null) }
        refresh()
    }

    fun back() {
        if (mutableState.value.busy) return
        val target = when (mutableState.value.screen) {
            BackendScreen.Course -> BackendScreen.Courses
            BackendScreen.CreateCourse -> courseBackScreen
            BackendScreen.Crew -> BackendScreen.Crews
            BackendScreen.CreateSession -> BackendScreen.Crew
            BackendScreen.Session -> sessionBackScreen
            BackendScreen.Record -> BackendScreen.Records
            else -> BackendScreen.Home
        }
        open(target)
    }

    fun refresh() = execute {
        val state = mutableState.value
        when (state.screen) {
            BackendScreen.Home -> loadHome()

            BackendScreen.Courses -> loadCourses(0)

            BackendScreen.Course -> state.course?.let { selected ->
                val detail = repository.course(selected.id).getOrThrow()
                mutableState.update { it.copy(course = detail) }
            }

            BackendScreen.Crews -> {
                val crews = repository.crews().getOrThrow()
                mutableState.update { it.copy(crews = crews) }
            }

            BackendScreen.Crew -> state.crew?.let { loadCrew(it.id) }

            BackendScreen.CreateSession -> state.crew?.let { loadSharedCourses(it.id, 0) }

            BackendScreen.Session -> state.session?.let { loadSession(it.id) }

            BackendScreen.Records -> loadRecords(0)

            BackendScreen.Record -> state.record?.let { selected ->
                val detail = repository.record(selected.id).getOrThrow()
                mutableState.update { it.copy(record = detail) }
            }

            BackendScreen.CreateCourse, BackendScreen.JoinCrew -> Unit
        }
    }

    fun moreCourses() = execute { loadCourses(mutableState.value.coursePage + 1) }
    fun moreRecords() = execute { loadRecords(mutableState.value.recordPage + 1) }
    fun moreSharedCourses() = execute {
        val state = mutableState.value
        state.crew?.let { loadSharedCourses(it.id, state.sharedCoursePage + 1) }
    }

    fun selectCourse(course: Course) {
        mutableState.update { it.copy(course = course) }
        open(BackendScreen.Course)
    }

    fun changeCourseDraftTitle(title: String) {
        if (title.length > 100) return
        mutableState.update { it.copy(courseDraftTitle = title, error = null) }
    }

    fun addCoursePoint(point: GeoPoint) {
        if (point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) return
        mutableState.update {
            if (it.busy || it.courseDraftPoints.size >= MAX_COURSE_POINTS) {
                it
            } else {
                it.copy(courseDraftPoints = it.courseDraftPoints + point, error = null)
            }
        }
    }

    fun undoCoursePoint() {
        mutableState.update { it.copy(courseDraftPoints = it.courseDraftPoints.dropLast(1), error = null) }
    }

    fun resetCoursePoints() {
        mutableState.update { it.copy(courseDraftPoints = emptyList(), error = null) }
    }

    fun createCourse() = execute {
        val title = mutableState.value.courseDraftTitle.trim()
        val points = mutableState.value.courseDraftPoints
        if (title.isBlank() || points.size !in MIN_COURSE_POINTS..MAX_COURSE_POINTS) {
            throw ApiFailure(ApiFailureKind.InvalidInput)
        }

        val course = repository.createCourse(title, points).getOrThrow()
        mutableState.update {
            it.copy(
                course = course,
                screen = BackendScreen.Course,
                courseDraftTitle = "",
                courseDraftPoints = emptyList(),
            )
        }
    }

    fun selectCrew(crew: Crew) {
        mutableState.update {
            it.copy(
                crew = crew,
                inviteCode = null,
                sessions = emptyList(),
                sharedCourses = emptyList(),
                sharedCoursePage = -1,
                hasMoreSharedCourses = false,
            )
        }
        open(BackendScreen.Crew)
    }
    fun createCrew(name: String) = execute {
        val crew = repository.createCrew(name.trim()).getOrThrow()
        mutableState.update {
            it.copy(crew = crew, inviteCode = null, sessions = emptyList(), screen = BackendScreen.Crew)
        }
        loadCrew(crew.id)
    }
    fun joinCrew(code: String) = execute {
        val crew = repository.joinCrew(code).getOrThrow()
        mutableState.update {
            it.copy(crew = crew, inviteCode = null, sessions = emptyList(), screen = BackendScreen.Crew)
        }
        loadCrew(crew.id)
    }
    fun showInvite() = execute {
        val id = mutableState.value.crew?.id ?: return@execute
        val code = repository.inviteCode(id).getOrThrow()
        mutableState.update { it.copy(inviteCode = code) }
    }
    fun selectSession(id: Long) = execute {
        sessionBackScreen = mutableState.value.screen
        mutableState.update { it.copy(session = null, recordStatus = null) }
        loadSession(id)
        mutableState.update { it.copy(screen = BackendScreen.Session) }
    }
    fun createSession(title: String, courseId: Long) = execute {
        val crewId = mutableState.value.crew?.id ?: return@execute
        val session = repository.createSession(crewId, title.trim(), courseId).getOrThrow()
        sessionBackScreen = BackendScreen.Crew
        mutableState.update { it.copy(session = session, recordStatus = null, screen = BackendScreen.Session) }
    }
    fun commandSession(command: String) = execute {
        val session = mutableState.value.session ?: return@execute
        repository.sessionCommand(session.id, command).getOrThrow()
        if (command == "me/finish") {
            onRunFinished()
            mutableState.update { it.copy(session = session.copy(myStatus = "FINISHED")) }
        }
        loadSession(session.id)
    }
    fun selectRecord(record: RunRecord) {
        mutableState.update { it.copy(record = record) }
        open(BackendScreen.Record)
    }

    private suspend fun loadHome() {
        val home = repository.home().getOrThrow()
        mutableState.update { it.copy(dashboard = home) }
    }
    private suspend fun loadCourses(page: Int) {
        val result = repository.courses(page).getOrThrow()
        mutableState.update {
            it.copy(
                courses = if (page ==
                    0
                ) {
                    result.items
                } else {
                    (it.courses + result.items).distinctBy { course -> course.id }
                },
                coursePage = result.page,
                hasMoreCourses = result.hasNext,
            )
        }
    }
    private suspend fun loadRecords(page: Int) {
        val result = repository.records(page).getOrThrow()
        mutableState.update {
            it.copy(
                records = if (page ==
                    0
                ) {
                    result.items
                } else {
                    (it.records + result.items).distinctBy { record -> record.id }
                },
                recordPage = result.page,
                hasMoreRecords = result.hasNext,
            )
        }
    }
    private suspend fun loadCrew(id: Long) {
        val crew = repository.crew(id).getOrThrow()
        mutableState.update { it.copy(crew = crew) }
        val sessions = repository.sessions(id).getOrThrow()
        mutableState.update { it.copy(sessions = sessions) }
    }
    private suspend fun loadSharedCourses(crewId: Long, page: Int) {
        val result = repository.sharedCourses(crewId, page).getOrThrow()
        mutableState.update {
            it.copy(
                sharedCourses = if (page == 0) {
                    result.items
                } else {
                    (it.sharedCourses + result.items).distinctBy { course -> course.id }
                },
                sharedCoursePage = result.page,
                hasMoreSharedCourses = result.hasNext,
            )
        }
    }
    private suspend fun loadSession(id: Long) {
        val session = repository.session(id).getOrThrow()
        mutableState.update { it.copy(session = session) }
        if (mutableState.value.crew?.id != session.crewId) {
            val crew = repository.crew(session.crewId).getOrThrow()
            mutableState.update { it.copy(crew = crew, inviteCode = null) }
        }
        if (session.myStatus == "FINISHED") {
            val status = repository.recordStatus(id).getOrThrow()
            mutableState.update { it.copy(recordStatus = status) }
        }
    }

    private fun execute(action: suspend () -> Unit) {
        if (request?.isActive == true) return
        mutableState.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: ApiFailure) {
                mutableState.update { it.copy(error = failure) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}

internal const val MIN_COURSE_POINTS = 2
internal const val MAX_COURSE_POINTS = 50
