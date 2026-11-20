package com.sor999.dalmoi.presentation.backend

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.domain.model.ApiFailure
import com.sor999.dalmoi.domain.model.ApiFailureKind
import com.sor999.dalmoi.domain.model.Course
import com.sor999.dalmoi.presentation.auth.AuthScreen
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.course.CourseMap
import com.sor999.dalmoi.presentation.course.LiveCourseEditor
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BackendApp(factory: ViewModelProvider.Factory, modifier: Modifier = Modifier) {
    val model: BackendViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.signedIn && state.screen != BackendScreen.Home) { model.back() }
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
    Surface(modifier.fillMaxSize(), color = Palette.Background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("api-loading"))
            state.error?.let { error ->
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(stringResource(errorResource(error)), Modifier.weight(1f), color = Palette.Error)
                    TextButton(onClick = model::dismissError) { Text(stringResource(R.string.api_dismiss)) }
                }
            }
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
                )
            } else {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = model::back, enabled = !state.busy) { Text(stringResource(R.string.api_back)) }
                    TextButton(onClick = model::refresh, enabled = !state.busy) {
                        Text(stringResource(R.string.api_refresh))
                    }
                    TextButton(onClick = model::logout, enabled = !state.busy) {
                        Text(stringResource(R.string.api_logout))
                    }
                }
                val screenScroll = key(state.screen) { rememberScrollState() }
                Column(
                    Modifier.weight(1f).imePadding().verticalScroll(screenScroll).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (state.screen) {
                        BackendScreen.Home -> HomeContent(state, model)

                        BackendScreen.Courses -> {
                            Heading(R.string.nav_courses)
                            ApiButton(R.string.course_title, state.busy) { model.open(BackendScreen.CreateCourse) }
                            if (state.courses.isEmpty() && !state.busy) EmptyContent()
                            state.courses.forEach { course ->
                                CourseRow(course, state.busy) { model.selectCourse(course) }
                            }
                            if (state.hasMoreCourses) ApiButton(R.string.api_more, state.busy, model::moreCourses)
                        }

                        BackendScreen.CreateCourse -> Unit

                        BackendScreen.Course -> state.course?.let { course ->
                            Text(course.title, style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.api_distance_km, course.distanceKm))
                            Text(formatDate(course.createdAt))
                            if (course.points.isNotEmpty()) {
                                key(course.id) {
                                    CourseMap(course.points, Modifier.fillMaxWidth().height(320.dp))
                                }
                            }
                            Text(stringResource(R.string.api_choose_crew_for_run))
                            ApiButton(R.string.nav_crews, state.busy) { model.open(BackendScreen.Crews) }
                        }

                        BackendScreen.Crews -> CrewsContent(state, model)

                        BackendScreen.Crew -> CrewContent(state, model)

                        BackendScreen.CreateSession -> CreateSessionContent(state, model)

                        BackendScreen.Session -> SessionContent(state, model)

                        BackendScreen.Records -> {
                            Heading(R.string.nav_records)
                            if (state.records.isEmpty() && !state.busy) EmptyContent()
                            state.records.forEach { record ->
                                Card(onClick = { model.selectRecord(record) }, enabled = !state.busy) {
                                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(record.title)
                                        Text(stringResource(R.string.api_distance_km, record.distanceKm))
                                        Text(formatDate(record.endedAt))
                                    }
                                }
                            }
                            if (state.hasMoreRecords) ApiButton(R.string.api_more, state.busy, model::moreRecords)
                        }

                        BackendScreen.Record -> state.record?.let { record ->
                            Text(record.title, style = MaterialTheme.typography.titleLarge)
                            Text(formatDate(record.endedAt))
                            Text(stringResource(R.string.api_distance_km, record.distanceKm))
                            Text(stringResource(R.string.api_duration_seconds, record.durationSeconds))
                            Text(stringResource(R.string.api_actual_points, record.actualRoute.size))
                            record.actualRoute.forEach { Text("${it.latitude}, ${it.longitude}") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf(
                        BackendScreen.Home to R.string.nav_home,
                        BackendScreen.Courses to R.string.nav_courses,
                        BackendScreen.Crews to R.string.nav_crews,
                        BackendScreen.Records to R.string.nav_records,
                    )
                        .forEach { (screen, label) ->
                            TextButton(
                                onClick = { model.open(screen) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f).testTag("api-tab-${screen.name}"),
                            ) { Text(stringResource(label)) }
                        }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(state: BackendUiState, model: BackendViewModel) {
    val home = state.dashboard ?: return
    Text(stringResource(R.string.api_greeting, home.member.nickname), style = MaterialTheme.typography.titleLarge)
    home.waiting?.let { waiting ->
        Card(onClick = { model.selectSession(waiting.id) }, enabled = !state.busy) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.home_waiting))
                Text(waiting.title)
            }
        }
    }
    Heading(R.string.home_my_crews)
    if (home.crews.isEmpty()) EmptyContent()
    home.crews.forEach { crew ->
        ApiTextButton(crew.name, state.busy) { model.selectCrew(crew) }
    }
    Heading(R.string.home_my_courses)
    if (home.courses.isEmpty()) EmptyContent()
    home.courses.forEach { course -> CourseRow(course, state.busy) { model.selectCourse(course) } }
}

@Composable
private fun CrewsContent(state: BackendUiState, model: BackendViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    Heading(R.string.nav_crews)
    Input(R.string.api_crew_name, name, { name = it }, state.busy)
    ApiButton(R.string.crew_create, state.busy, enabled = name.isNotBlank()) { model.createCrew(name) }
    Input(R.string.crew_code, code, { code = it }, state.busy)
    ApiButton(R.string.crew_join, state.busy, enabled = code.isNotBlank()) { model.joinCrew(code) }
    if (state.crews.isEmpty() && !state.busy) EmptyContent()
    state.crews.forEach { crew ->
        ApiTextButton(stringResource(R.string.api_crew_count, crew.name, crew.memberCount), state.busy) {
            model.selectCrew(crew)
        }
    }
}

@Composable
private fun CrewContent(state: BackendUiState, model: BackendViewModel) {
    val crew = state.crew ?: return
    Text(crew.name, style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.api_crew_count, crew.name, crew.memberCount))
    crew.members.forEach { Text(stringResource(R.string.api_member_role, it.nickname, it.role)) }
    if (crew.myRole == "OWNER") {
        ApiButton(R.string.api_show_invite, state.busy, model::showInvite)
        state.inviteCode?.let { Text(it) }
        ApiButton(R.string.course_create_run, state.busy) { model.open(BackendScreen.CreateSession) }
    }
    Heading(R.string.api_sessions)
    if (state.sessions.isEmpty() && !state.busy) EmptyContent()
    state.sessions.forEach { session ->
        ApiTextButton("${session.title} · ${statusLabel(session.status)}", state.busy) {
            model.selectSession(session.id)
        }
    }
}

@Composable
private fun CreateSessionContent(state: BackendUiState, model: BackendViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable(state.crew?.id) { mutableStateOf<Long?>(null) }
    Heading(R.string.course_create_run)
    Input(R.string.api_session_name, title, { title = it }, state.busy)
    if (state.sharedCourses.isEmpty() && !state.busy) Text(stringResource(R.string.api_create_course_first))
    state.sharedCourses.forEach { course ->
        androidx.compose.material3.FilterChip(
            selected = course.id == selectedId,
            onClick = { selectedId = course.id },
            enabled = !state.busy,
            label = { Text(stringResource(R.string.api_course_distance, course.title, course.distanceKm)) },
        )
    }
    if (state.hasMoreSharedCourses) ApiButton(R.string.api_more, state.busy, model::moreSharedCourses)
    ApiButton(R.string.api_create_session, state.busy, enabled = selectedId != null && title.isNotBlank()) {
        selectedId?.let { model.createSession(title, it) }
    }
}

@Composable
private fun SessionContent(state: BackendUiState, model: BackendViewModel) {
    val session = state.session ?: return
    Text(session.title, style = MaterialTheme.typography.titleLarge)
    Text(session.crewName)
    Text(statusLabel(session.status))
    Text(stringResource(R.string.api_course_distance, session.course.title, session.course.distanceKm))
    Text(stringResource(R.string.api_my_status, statusLabel(session.myStatus)))
    session.participants.forEach { Text(stringResource(R.string.api_member_role, it.nickname, statusLabel(it.status))) }
    if (session.canJoin) ApiButton(R.string.api_join_session, state.busy) { model.commandSession("participants") }
    if (session.canStart) ApiButton(R.string.api_start_group, state.busy) { model.commandSession("start") }
    if (session.status == "RUNNING" && session.myStatus == "JOINED") {
        Text(stringResource(R.string.api_gps_pending))
        ApiButton(R.string.api_start_me, state.busy) { model.commandSession("me/start") }
    }
    if (session.status == "RUNNING" && session.myStatus == "RUNNING") {
        Text(stringResource(R.string.api_gps_pending))
        ApiButton(R.string.api_finish_me, state.busy) { model.commandSession("me/finish") }
    }
    val isOwner = state.crew?.takeIf { it.id == session.crewId }?.myRole == "OWNER"
    if (session.status == "RUNNING" && isOwner) {
        ApiButton(
            R.string.api_complete_group,
            state.busy,
            enabled = session.participants.none { it.status == "RUNNING" },
        ) { model.commandSession("complete") }
    }
    state.recordStatus?.let { status ->
        Text(stringResource(R.string.api_record_status, statusLabel(status.status)))
        if (status.status == "PROCESSING") Text(stringResource(R.string.api_record_refresh))
        if (status.status ==
            "COMPLETED"
        ) {
            ApiButton(R.string.nav_records, state.busy) { model.open(BackendScreen.Records) }
        }
    }
}

@Composable
private fun CourseRow(course: Course, busy: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = !busy) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(course.title)
            Text(stringResource(R.string.api_distance_km, course.distanceKm))
        }
    }
}

@Composable
private fun ApiButton(label: Int, busy: Boolean, onClick: () -> Unit) = ApiButton(label, busy, true, onClick)

@Composable
private fun ApiButton(label: Int, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    ActionButton(stringResource(label), onClick, enabled = enabled && !busy)
}

@Composable
private fun ApiTextButton(text: String, busy: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = !busy) { Text(text) }
}

@Composable
private fun Input(label: Int, value: String, onChange: (String) -> Unit, busy: Boolean) {
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth(),
        enabled = !busy,
        singleLine = true,
        label = { Text(stringResource(label)) },
    )
}

@Composable
private fun Heading(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun EmptyContent() {
    Text(stringResource(R.string.api_empty), color = Palette.Muted)
}

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
