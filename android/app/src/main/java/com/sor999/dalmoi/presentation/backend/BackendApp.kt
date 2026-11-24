package com.sor999.dalmoi.presentation.backend

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.presentation.auth.AuthScreen
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.BottomNavigation
import com.sor999.dalmoi.presentation.common.component.Caption
import com.sor999.dalmoi.presentation.common.component.ContentCard
import com.sor999.dalmoi.presentation.common.component.NavigationTab
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.course.CourseDetailLayout
import com.sor999.dalmoi.presentation.course.CourseListLayout
import com.sor999.dalmoi.presentation.course.CourseMap
import com.sor999.dalmoi.presentation.course.LiveCourseEditor
import com.sor999.dalmoi.presentation.crew.CrewDetailLayout
import com.sor999.dalmoi.presentation.crew.CrewListItem
import com.sor999.dalmoi.presentation.crew.CrewListLayout
import com.sor999.dalmoi.presentation.crew.CrewMemberItem
import com.sor999.dalmoi.presentation.crew.CrewRunItem
import com.sor999.dalmoi.presentation.home.HomeContent
import com.sor999.dalmoi.presentation.home.HomeCourse
import com.sor999.dalmoi.presentation.home.HomeCrew
import com.sor999.dalmoi.presentation.home.HomeRun
import com.sor999.dalmoi.presentation.home.HomeScreen
import com.sor999.dalmoi.presentation.record.RecordLayout
import com.sor999.dalmoi.presentation.record.RecordListLayout
import com.sor999.dalmoi.presentation.session.CourseOption
import com.sor999.dalmoi.presentation.session.CreateSessionLayout
import com.sor999.dalmoi.presentation.session.LiveRunningContent
import com.sor999.dalmoi.presentation.session.RunLocationStartButton
import com.sor999.dalmoi.presentation.session.RunningLayout
import com.sor999.dalmoi.presentation.session.WaitingRoomLayout
import com.sor999.dalmoi.presentation.session.WaitingRunner
import com.sor999.dalmoi.presentation.session.formatElapsed
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BackendApp(factory: ViewModelProvider.Factory, modifier: Modifier = Modifier) {
    val model: BackendViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    val running = state.screen == BackendScreen.Session && state.session?.myStatus == "RUNNING" &&
        state.session?.status == "RUNNING"
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !running
                isAppearanceLightNavigationBars = !running
            }
        }
    }
    BackHandler(enabled = state.signedIn && state.screen != BackendScreen.Home && !running) { model.back() }
    if (state.signedIn && state.screen == BackendScreen.CreateCourse) {
        LiveCourseEditor(
            points = state.courseDraftPoints,
            title = state.courseDraftTitle,
            busy = state.busy,
            canSave = state.courseDraftPoints.size >= MIN_COURSE_POINTS,
            limitReached = state.courseDraftPoints.size >= MAX_COURSE_POINTS,
            error = state.error?.let { stringResource(errorResource(it)) },
            onPoint = model::addCoursePoint,
            onUndo = model::undoCoursePoint,
            onReset = model::resetCoursePoints,
            onTitle = model::changeCourseDraftTitle,
            onSave = model::createCourse,
            onBack = model::back,
            modifier = modifier,
        )
        return
    }
    Surface(modifier.fillMaxSize(), color = if (running) Palette.Running else Palette.Background) {
        val chromeInsets = if (state.signedIn) {
            Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        } else {
            Modifier.safeDrawingPadding()
        }
        Column(Modifier.fillMaxSize().then(chromeInsets)) {
            if (!state.signedIn) {
                AuthScreen(
                    DalmoiUiState(
                        email = state.email,
                        password = state.password,
                        nickname = state.nickname,
                        signingUp = state.signingUp,
                        authError = false,
                    ),
                    model::selectSignup,
                    model::changeEmail,
                    model::changePassword,
                    model::changeNickname,
                    model::authenticate,
                    busy = state.busy,
                    errorMessage = state.error?.let { stringResource(errorResource(it)) },
                )
            } else {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("api-loading"))
                state.error?.let { error ->
                    Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(stringResource(errorResource(error)), Modifier.weight(1f), color = Palette.Error)
                        TextButton(onClick = model::dismissError) { Text(stringResource(R.string.api_dismiss)) }
                    }
                }
                val hasTabs = backendTabs.any { it.screen == state.screen }
                val contentModifier = if (hasTabs) Modifier.weight(1f) else Modifier.weight(1f).navigationBarsPadding()
                key(state.screen) { BackendScreenContent(state, model, contentModifier) }
                if (hasTabs) {
                    BottomNavigation(
                        tabs = backendTabs,
                        selected = state.screen,
                        onSelect = model::open,
                        modifier = Modifier.navigationBarsPadding(),
                        enabled = !state.busy,
                    )
                }
            }
        }
    }
}

@Composable
private fun BackendScreenContent(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    when (state.screen) {
        BackendScreen.Home -> BackendHome(state, model, modifier)

        BackendScreen.Courses -> CourseListLayout(
            onEditor = { model.open(BackendScreen.CreateCourse) },
            modifier = modifier,
            enabled = !state.busy,
        ) {
            if (state.courses.isEmpty() && !state.busy) EmptyContent()
            state.courses.forEach { course ->
                CourseOption(
                    name = course.title,
                    distance = formatDistance(course.distanceKm),
                    selected = null,
                    onClick = { model.selectCourse(course) },
                    enabled = !state.busy,
                )
            }
            if (state.hasMoreCourses) ApiButton(R.string.api_more, state.busy, model::moreCourses)
        }

        BackendScreen.Course -> state.course?.let { course ->
            CourseDetailLayout(
                title = course.title,
                distance = formatDistance(course.distanceKm),
                createdOn = course.createdAt.atZone(ZoneId.systemDefault()).format(courseDateFormat),
                loop = course.isLoop,
                onBack = model::back,
                onCreateRun = { model.open(BackendScreen.Crews) },
                modifier = modifier,
                enabled = !state.busy,
            ) {
                key(course.id) { CourseMap(course.points, Modifier.fillMaxSize(), detailLoop = course.isLoop) }
            }
        } ?: Box(modifier)

        BackendScreen.Crews, BackendScreen.JoinCrew -> BackendCrewList(state, model, modifier)

        BackendScreen.Crew -> BackendCrew(state, model, modifier)

        BackendScreen.CreateSession -> BackendCreateSession(state, model, modifier)

        BackendScreen.Session -> BackendSession(state, model, modifier)

        BackendScreen.Records -> BackendRecords(state, model, modifier)

        BackendScreen.Record -> state.record?.let { record ->
            RecordLayout(
                title = record.title,
                date = listOfNotNull(formatDate(record.endedAt), record.crewName).joinToString(" · "),
                distance = formatDistance(record.distanceKm),
                duration = formatElapsed(record.durationSeconds),
                plannedDistance = record.plannedDistanceKm?.let(::formatDistance),
                onBack = model::back,
                modifier = modifier,
                showLegend = record.plannedRoute.isNotEmpty() && record.actualRoute.isNotEmpty(),
            ) {
                if (record.actualRoute.isEmpty() && record.plannedRoute.isEmpty()) {
                    EmptyContent()
                } else {
                    CourseMap(record.actualRoute, Modifier.fillMaxSize(), referencePoints = record.plannedRoute)
                }
            }
        } ?: Box(modifier)

        BackendScreen.CreateCourse -> Box(modifier)
    }
}

private val backendTabs = listOf(
    NavigationTab(BackendScreen.Home, R.string.nav_home, "api-tab-Home"),
    NavigationTab(BackendScreen.Courses, R.string.nav_courses, "api-tab-Courses"),
    NavigationTab(BackendScreen.Crews, R.string.nav_crews, "api-tab-Crews"),
    NavigationTab(BackendScreen.Records, R.string.nav_records, "api-tab-Records"),
)

@Composable
private fun BackendHome(state: BackendUiState, model: BackendViewModel, modifier: Modifier = Modifier) {
    val home = state.dashboard
    if (home == null) {
        Column(modifier.padding(20.dp)) {
            if (!state.busy) ApiButton(R.string.api_refresh, false, model::refresh)
        }
        return
    }
    HomeScreen(
        content = HomeContent(
            greeting = stringResource(R.string.home_greeting_format, home.member.nickname),
            initial = home.member.nickname.take(1),
            waiting = home.waiting?.let { run ->
                HomeRun(
                    title = run.title,
                    status = run.status,
                    description = stringResource(
                        R.string.home_run_description_format,
                        run.crewName,
                        run.courseTitle,
                        run.distanceKm,
                    ),
                    participants = stringResource(
                        R.string.home_run_participants_format,
                        run.participantCount,
                        run.crewMemberCount,
                    ),
                    onEnter = { model.selectSession(run.id) },
                )
            },
            crews = home.crews.map { crew ->
                val owner = crew.myRole == "OWNER"
                HomeCrew(
                    name = crew.name,
                    description = stringResource(
                        if (owner) R.string.home_crew_owner_format else R.string.home_crew_members_format,
                        crew.memberCount,
                    ),
                    owner = owner,
                    onClick = { model.selectCrew(crew) },
                )
            },
            crewTotal = stringResource(R.string.home_total_format, home.crewCount),
            courses = home.courses.map { course ->
                HomeCourse(course.title, formatDistance(course.distanceKm)) {
                    model.selectCourse(course)
                }
            },
            courseTotal = stringResource(R.string.home_total_format, home.courseCount),
        ),
        onEditor = { model.open(BackendScreen.CreateCourse) },
        onJoin = { model.open(BackendScreen.JoinCrew) },
        modifier = modifier,
        enabled = !state.busy,
        onRefresh = model::refresh,
        onLogout = model::logout,
    )
}

@Composable
private fun BackendCrewList(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    var code by rememberSaveable { mutableStateOf("") }
    val joinOnly = state.screen == BackendScreen.JoinCrew
    CrewListLayout(
        code = code,
        onCode = { code = it },
        onJoin = { model.joinCrew(code) },
        crews = state.crews.map { crew ->
            CrewListItem(
                name = crew.name,
                count = stringResource(R.string.crew_count_format, crew.memberCount),
                owner = crew.myRole == "OWNER",
                onClick = { model.selectCrew(crew) },
                runName = state.dashboard?.waiting?.takeIf { it.id == crew.waitingSessionId }?.title,
            )
        },
        count = stringResource(R.string.crew_my_count_format, state.crews.size),
        onCreate = model::createCrew,
        modifier = modifier,
        enabled = !state.busy,
        onBack = if (joinOnly) model::back else null,
    )
}

@Composable
private fun BackendCrew(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    val crew = state.crew
    if (crew == null) {
        Box(modifier)
        return
    }
    val owner = crew.myRole == "OWNER"
    val leader = crew.members.firstOrNull { it.role == "OWNER" }
    val description = if (leader != null) {
        stringResource(R.string.crew_leader_description_format, leader.nickname, crew.memberCount)
    } else {
        stringResource(R.string.crew_count_format, crew.memberCount)
    }
    CrewDetailLayout(
        name = crew.name,
        description = description,
        onBack = model::back,
        inviteCode = state.inviteCode,
        onCreateRun = if (owner) ({ model.open(BackendScreen.CreateSession) }) else null,
        onShowInvite = if (owner) model::showInvite else null,
        runs = state.sessions.map { session ->
            CrewRunItem(
                name = session.title,
                subtitle = "",
                status = statusLabel(session.status),
                waiting = session.status == "WAITING",
                onClick = { model.selectSession(session.id) },
            )
        },
        members = crew.members.map { member ->
            val isMe = member.id == state.dashboard?.member?.id
            CrewMemberItem(
                if (isMe) stringResource(R.string.crew_self_format, member.nickname) else member.nickname,
                isMe,
                member.role == "OWNER",
            )
        },
        modifier = modifier,
        enabled = !state.busy,
    )
}

@Composable
private fun BackendCreateSession(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    var selectedId by rememberSaveable(state.crew?.id) { mutableStateOf<Long?>(null) }
    CreateSessionLayout(
        crewName = state.crew?.name.orEmpty(),
        initialName = "",
        onBack = model::back,
        onCreate = { name -> selectedId?.let { model.createSession(name, it) } },
        modifier = modifier,
        enabled = !state.busy,
        ready = selectedId != null,
        inputLabel = R.string.api_session_name,
        submitLabel = R.string.api_create_session,
    ) {
        if (state.sharedCourses.isEmpty() && !state.busy) Caption(stringResource(R.string.api_create_course_first))
        state.sharedCourses.forEach { course ->
            CourseOption(
                course.title,
                formatDistance(course.distanceKm),
                selected = course.id == selectedId,
                onClick = { selectedId = course.id },
                enabled = !state.busy,
            )
        }
        if (state.hasMoreSharedCourses) ApiButton(R.string.api_more, state.busy, model::moreSharedCourses)
    }
}

@Composable
private fun BackendSession(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    val session = state.session
    if (session == null) {
        Box(modifier)
        return
    }
    if (session.status == "RUNNING" && session.myStatus == "RUNNING") {
        RunningLayout(
            mvp = false,
            disconnected = false,
            plannedDistance = formatDistance(session.course.distanceKm),
            onFinish = { model.commandSession("me/finish") },
            modifier = modifier,
            live = LiveRunningContent(session.course.points, session.myStartedAt),
            enabled = !state.busy,
        )
        return
    }

    val isOwner = state.crew?.takeIf { it.id == session.crewId }?.myRole == "OWNER"
    WaitingRoomLayout(
        title = session.title,
        crewName = session.crewName,
        status = if (session.status == "WAITING") session.status else statusLabel(session.status),
        course = session.course.title,
        distance = formatDistance(session.course.distanceKm),
        attendance = stringResource(R.string.run_attendance_format, session.participants.size, session.crewMemberCount),
        runners = session.memberStatuses.map { runner ->
            WaitingRunner(
                name = runner.nickname,
                status = statusLabel(runner.status),
                joined = runner.status != "NOT_JOINED" && runner.status != "ABSENT",
                highlight = runner.memberId != null && runner.memberId == state.dashboard?.member?.id,
            )
        },
        onBack = model::back,
        modifier = modifier,
        header = if (session.status == "WAITING") R.string.run_waiting_room else R.string.api_sessions,
        map = { CourseMap(session.course.points, Modifier.fillMaxSize()) },
    ) {
        if (session.canJoin) ApiButton(R.string.api_join_session, state.busy) { model.commandSession("participants") }
        if (session.canStart) {
            ActionButton(
                stringResource(R.string.api_start_group),
                { model.commandSession("start") },
                enabled = !state.busy,
                color = Palette.Lime,
                foreground = Palette.Ink,
                outline = Palette.Ink,
            )
            Caption(stringResource(R.string.run_launch_note))
        }
        if (session.status == "RUNNING" && session.myStatus == "JOINED") {
            RunLocationStartButton(enabled = !state.busy) { model.commandSession("me/start") }
        }
        if (session.status == "RUNNING" && isOwner) {
            ActionButton(
                stringResource(R.string.api_complete_group),
                { model.commandSession("complete") },
                enabled = !state.busy && session.participants.none { it.status == "RUNNING" },
            )
        }
        state.recordStatus?.let { status ->
            Caption(stringResource(R.string.api_record_status, statusLabel(status.status)))
            if (status.status == "PROCESSING") Caption(stringResource(R.string.api_record_refresh))
            if (status.status == "COMPLETED") {
                ApiButton(R.string.nav_records, state.busy) { model.open(BackendScreen.Records) }
            }
        }
        TextButton(onClick = model::refresh, enabled = !state.busy) { Body(stringResource(R.string.api_refresh)) }
    }
}

@Composable
private fun BackendRecords(state: BackendUiState, model: BackendViewModel, modifier: Modifier) {
    RecordListLayout(modifier) {
        if (state.records.isEmpty() && !state.busy) EmptyContent()
        state.records.forEach { record ->
            ContentCard(Modifier.fillMaxWidth(), onClick = { model.selectRecord(record) }.takeIf { !state.busy }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Body(record.title, size = 16.sp, weight = FontWeight.SemiBold)
                    Caption(stringResource(R.string.api_distance_km, record.distanceKm))
                    Caption(formatDate(record.endedAt))
                }
            }
        }
        if (state.hasMoreRecords) ApiButton(R.string.api_more, state.busy, model::moreRecords)
        TextButton(onClick = model::refresh, enabled = !state.busy) { Body(stringResource(R.string.api_refresh)) }
    }
}

@Composable
private fun ApiButton(label: Int, busy: Boolean, onClick: () -> Unit) {
    ActionButton(stringResource(label), onClick, enabled = !busy)
}

@Composable
private fun EmptyContent() {
    Caption(stringResource(R.string.api_empty), color = Palette.Muted)
}

private fun formatDistance(distance: Double) = String.format(Locale.getDefault(), "%.2f", distance)

private val courseDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDate(instant: java.time.Instant) = DateTimeFormatter.ofPattern(
    "yyyy.MM.dd HH:mm",
    Locale.getDefault(),
).withZone(ZoneId.systemDefault()).format(instant)

@Composable
private fun statusLabel(status: String): String = stringResource(
    when (status) {
        "WAITING" -> R.string.api_status_waiting
        "RUNNING" -> R.string.api_status_running
        "COMPLETED" -> R.string.api_status_completed
        "JOINED" -> R.string.api_status_joined
        "FINISHED" -> R.string.api_status_finished
        "ABSENT" -> R.string.api_status_absent
        "NOT_JOINED" -> R.string.api_status_not_joined
        "PROCESSING" -> R.string.record_processing
        "FAILED" -> R.string.api_status_failed
        else -> R.string.api_status_unknown
    },
)

internal fun errorResource(error: ApiFailure): Int = when (error.code) {
    "EMAIL_ALREADY_EXISTS" -> R.string.api_error_email_exists

    "INVALID_CREDENTIALS" -> R.string.auth_error

    "INVALID_INVITE_CODE" -> R.string.api_error_invite

    "CREW_CAPACITY_EXCEEDED" -> R.string.api_error_crew_full

    "ALREADY_CREW_MEMBER" -> R.string.api_error_already_joined

    "ACTIVE_SESSION_EXISTS" -> R.string.api_error_active_session

    "ACTIVE_PARTICIPANTS_EXIST" -> R.string.api_error_active_runners

    "ALREADY_RUNNING_OTHER_SESSION" -> R.string.api_error_running_elsewhere

    "API_NOT_CONFIGURED" -> R.string.api_error_config

    else -> when (error.kind) {
        ApiFailureKind.Network -> R.string.api_error_network
        ApiFailureKind.InvalidResponse -> R.string.api_error_response
        ApiFailureKind.Unauthorized -> R.string.api_error_expired
        ApiFailureKind.InvalidInput -> R.string.api_error_input
        ApiFailureKind.Conflict -> R.string.api_error_conflict
        ApiFailureKind.Forbidden -> R.string.api_error_forbidden
        ApiFailureKind.NotFound -> R.string.api_error_missing
        ApiFailureKind.Server -> R.string.api_error_server
    }
}
