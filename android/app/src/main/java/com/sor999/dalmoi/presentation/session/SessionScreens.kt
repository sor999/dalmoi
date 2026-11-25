package com.sor999.dalmoi.presentation.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.CourseCardData
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.kilometers
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Avatar
import com.sor999.dalmoi.presentation.common.component.Badge
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.Caption
import com.sor999.dalmoi.presentation.common.component.ContentCard
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.SectionTitle
import com.sor999.dalmoi.presentation.common.component.TopBar
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.map.FigmaMap
import com.sor999.dalmoi.presentation.common.map.MapArtwork
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.course.CourseMap
import com.sor999.dalmoi.presentation.course.RunnerMarker
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CourseOption(course: CourseCardData, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CourseOption(course.name, kilometers(course.distanceMeters), selected, onClick, modifier) { mapModifier ->
        FigmaMap(course.thumbnail, mapModifier)
    }
}

@Composable
internal fun CourseOption(
    name: String,
    distance: String,
    selected: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbnail: (@Composable (Modifier) -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        color = Palette.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selected == true) 2.dp else 1.dp,
            if (selected == true) Palette.Ink else Palette.Border,
        ),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            thumbnail?.invoke(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Body(
                    name,
                    size = 15.sp,
                    weight = if (selected == true) FontWeight.Bold else FontWeight.SemiBold,
                )
                Caption("$distance km")
            }
            if (selected != null) {
                RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun CreateSessionScreen(
    state: DalmoiUiState,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CreateSessionLayout(state.crewName, state.runName, onBack, onCreate, modifier) {
        state.courses.forEach { course ->
            CourseOption(course, course.id == state.selectedCourseId, { onSelect(course.id) })
        }
    }
}

@Composable
internal fun CreateSessionLayout(
    crewName: String,
    initialName: String,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ready: Boolean = true,
    inputLabel: Int = R.string.run_name,
    submitLabel: Int = R.string.run_create_room,
    courseOptions: @Composable ColumnScope.() -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    Column(modifier.fillMaxSize().imePadding()) {
        TopBar(label(R.string.run_create_title), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(
                horizontal = 20.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LabeledInput(label(inputLabel), name, { name = it }, strong = true, enabled = enabled)
                Caption(crewName, color = Palette.Muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(label(R.string.run_common_course), detail = label(R.string.run_course_fixed))
                courseOptions()
            }
        }
        ActionButton(
            label(submitLabel),
            { onCreate(name) },
            Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp,
            ),
            enabled = enabled && ready && name.isNotBlank(),
        )
    }
}

internal data class WaitingRunner(val name: String, val status: String, val joined: Boolean, val highlight: Boolean)

@Composable
fun WaitingRoomScreen(state: DalmoiUiState, onBack: () -> Unit, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    val runners = listOf("민준 · 크루장", "서연", "지호", "하은").mapIndexed { index, name ->
        val joined = index != 3
        WaitingRunner(name, label(if (joined) R.string.run_joined else R.string.run_not_joined), joined, index == 0)
    }
    WaitingRoomLayout(
        title = state.runName,
        crewName = state.crewName,
        status = "WAITING",
        course = state.course.name,
        distance = kilometers(state.course.distanceMeters),
        attendance = label(R.string.run_participant_numbers),
        runners = runners,
        onBack = onBack,
        modifier = modifier,
        map = { FigmaMap(MapArtwork.Waiting, Modifier.fillMaxSize()) },
    ) {
        ActionButton(
            label(R.string.run_launch),
            onLaunch,
            color = Palette.Lime,
            foreground = Palette.Ink,
            outline = Palette.Ink,
        )
        Body(label(R.string.run_launch_note), size = 12.sp, color = Palette.Muted)
    }
}

@Composable
internal fun WaitingRoomLayout(
    title: String,
    crewName: String,
    status: String,
    course: String,
    distance: String,
    attendance: String,
    runners: List<WaitingRunner>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    header: Int = R.string.run_waiting_room,
    map: @Composable () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        TopBar(label(header), onBack) {
            Badge(status, color = Palette.Ink, foreground = Palette.Lime)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Body(title, size = 26.sp, weight = FontWeight.Bold)
                Body(crewName, size = 13.sp, color = Palette.Secondary)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(304f / 150f).clip(RoundedCornerShape(18.dp))) {
                map()
                Surface(Modifier.align(Alignment.BottomEnd).padding(10.dp), shape = RoundedCornerShape(10.dp)) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Body(course, size = 12.sp, weight = FontWeight.SemiBold)
                        Metric(distance, size = 17.sp, unit = label(R.string.course_kilometers), unitSize = 17.sp)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(label(R.string.run_participants), detail = attendance)
                ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                    if (runners.isEmpty()) Caption(label(R.string.api_empty), Modifier.padding(14.dp))
                    runners.forEachIndexed { index, runner ->
                        if (index > 0) HorizontalDivider(color = Palette.Divider)
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(
                                runner.name.take(1),
                                size = 32.dp,
                                circle = true,
                                outline = runner.highlight,
                                color = if (runner.highlight) Palette.Lime else Palette.Soft,
                            )
                            Body(
                                runner.name,
                                Modifier.weight(1f),
                                weight = if (runner.joined) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (runner.joined) Palette.Ink else Palette.Muted,
                            )
                            Body(
                                runner.status,
                                size = 12.sp,
                                color = if (runner.joined) Palette.Positive else Palette.Faint,
                            )
                        }
                    }
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = actions,
        )
    }
}

internal data class LiveRunningContent(
    val course: List<GeoPoint>,
    val startedAt: Instant?,
    val runners: List<RunnerMarker>,
    val ownMarker: RunnerMarker?,
)

@Composable
fun RunningScreen(state: DalmoiUiState, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    RunningLayout(state.mvp, state.disconnected, kilometers(state.course.distanceMeters), onFinish, modifier)
}

@Composable
internal fun RunningLayout(
    mvp: Boolean,
    disconnected: Boolean,
    plannedDistance: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    live: LiveRunningContent? = null,
    enabled: Boolean = true,
) {
    val elapsed by produceState(0L, live?.startedAt) {
        val startedAt = live?.startedAt ?: return@produceState
        while (true) {
            value = Duration.between(startedAt, Instant.now()).seconds.coerceAtLeast(0)
            delay(1_000)
        }
    }
    var endDialog by rememberSaveable { mutableStateOf(false) }
    var centerOnRunner by rememberSaveable { mutableIntStateOf(0) }
    BackHandler { if (enabled) endDialog = true }
    val dark = !mvp
    val background = if (dark) Palette.Running else Palette.White
    val foreground = if (dark) Palette.White else Palette.Ink
    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        val panelLimit = maxHeight * .7f
        if (live != null) {
            CourseMap(
                live.course,
                Modifier.fillMaxSize(),
                nightMode = true,
                runnerMarkers = live.runners + listOfNotNull(live.ownMarker),
                centerOnRunner = centerOnRunner,
            )
        } else {
            FigmaMap(
                if (mvp) {
                    MapArtwork.MvpRunning
                } else if (disconnected) {
                    MapArtwork.RunningError
                } else {
                    MapArtwork.Running
                },

                Modifier.fillMaxSize(),
                fullScreen = true,
            )
        }
        Column(Modifier.fillMaxSize()) {
            if (mvp) {
                Surface {
                    Column(
                        Modifier.fillMaxWidth().padding(
                            horizontal = 20.dp,
                            vertical = 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Body(label(R.string.mvp_running_name), size = 18.sp, weight = FontWeight.Bold)
                        Body(label(R.string.run_mvp_people), size = 13.sp, color = Palette.Positive)
                    }
                }
            } else if (live != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val status = when {
                        live.ownMarker == null -> R.string.run_tracking_waiting
                        disconnected -> R.string.run_tracking_local_only
                        else -> R.string.run_tracking_sharing
                    }
                    StatusChip(label(status), dot = if (disconnected) Palette.Muted else Palette.Lime)
                    if (BuildConfig.NAVER_MAP_CONFIGURED) {
                        Surface(
                            onClick = { centerOnRunner++ },
                            enabled = live.ownMarker != null,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Palette.White,
                            shadowElevation = 3.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painterResource(R.drawable.ic_course_recenter),
                                    label(R.string.run_my_location),
                                    Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(
                        horizontal = 14.dp,
                        vertical = 6.dp,
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusChip(
                        label(if (disconnected) R.string.run_stopped else R.string.run_sharing),
                        dot = if (disconnected) Palette.Muted else Palette.Lime,
                    )
                    StatusChip(label(if (disconnected) R.string.run_last_received else R.string.run_received))
                }
                if (disconnected) {
                    Surface(
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 6.dp,
                        ).fillMaxWidth(),
                        color = Palette.Error,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Column(
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Body(
                                label(R.string.run_network_error),
                                size = 13.sp,
                                weight = FontWeight.Bold,
                                color = Palette.White,
                            )
                            Body(label(R.string.run_stale_location), size = 12.sp, color = Palette.White)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(
                color = background,
                contentColor = foreground,
                shape = RoundedCornerShape(topStart = Corners.Panel, topEnd = Corners.Panel),
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = panelLimit).verticalScroll(rememberScrollState())
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (mvp) {
                        Row {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Caption(label(R.string.run_distance))
                                Metric(
                                    "3.76",
                                    size = 60.sp,
                                    unit = label(R.string.course_kilometers),
                                    unitSize = 22.sp,
                                )
                            }
                            Column(
                                Modifier.weight(1f).padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Caption(label(R.string.run_elapsed_label))
                                Metric("24:18", size = 60.sp)
                            }
                        }
                        ActionButton(
                            label(R.string.run_end),
                            { endDialog = true },
                            height = 64.dp,
                            foreground = Palette.White,
                            fontSize = 17.sp,
                            radius = 16.dp,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Caption(label(R.string.run_duration), color = Palette.Muted)
                            Metric(
                                if (live == null) "14:34" else formatElapsed(elapsed),
                                size = 76.sp,
                                color = Palette.Lime,
                            )
                        }
                        Row(
                            Modifier.background(
                                Palette.RunningBorder,
                                RoundedCornerShape(14.dp),
                            ).clip(RoundedCornerShape(14.dp)),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            RunningMetric(
                                label(R.string.run_distance),
                                if (live == null) "2.31" else "0.00",
                                Palette.White,
                                Modifier.weight(1f),
                            )
                            RunningMetric(
                                label(R.string.run_course_distance),
                                plannedDistance,
                                Palette.Muted,
                                Modifier.weight(1f),
                            )
                        }
                        if (live == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PeerChip(
                                    label(R.string.sample_peer_seoyeon),
                                    label(R.string.run_peer_fresh_time),
                                    Palette.White,
                                    Modifier.weight(1f),
                                )
                                PeerChip(
                                    label(R.string.sample_peer_jiho),
                                    label(R.string.run_peer_delay),
                                    Palette.Delay,
                                    Modifier.weight(1f),
                                )
                            }
                        }
                        val endLabel = label(R.string.run_end)
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .background(background, RoundedCornerShape(16.dp))
                                .then(Modifier.border(2.dp, Palette.White, RoundedCornerShape(16.dp)))
                                .combinedClickable(enabled = enabled, onClick = {}, onLongClick = { endDialog = true })
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction(endLabel) {
                                            if (enabled) endDialog = true
                                            enabled
                                        },
                                    )
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Body(
                                label(R.string.run_hold_end),
                                weight = FontWeight.Bold,
                                color = Palette.White,
                                size = 16.sp,
                            )
                        }
                    }
                }
            }
        }
    }
    if (endDialog) {
        EndRunningDialog({ endDialog = false }, {
            endDialog = false
            onFinish()
        })
    }
}

internal fun formatElapsed(seconds: Long): String = if (seconds >= 3600) {
    String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
} else {
    String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}

@Composable
private fun RunningMetric(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Palette.RunningCard).padding(
            horizontal = 14.dp,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Body(title, size = 11.sp, color = Palette.Muted)
        Metric(
            value,
            size = 30.sp,
            weight = FontWeight.SemiBold,
            unit = label(R.string.course_kilometers),
            unitSize = 15.sp,
            color = color,
            unitColor = Palette.Muted,
        )
    }
}

@Composable
private fun StatusChip(text: String, modifier: Modifier = Modifier, dot: Color? = null) {
    Row(
        modifier.background(
            Palette.Running.copy(alpha = .85f),
            RoundedCornerShape(99.dp),
        ).padding(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot != null) Box(Modifier.size(8.dp).background(dot, CircleShape))
        Body(text, size = 12.sp, weight = FontWeight.SemiBold, color = Palette.White)
    }
}

@Composable
private fun PeerChip(name: String, status: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.background(
            Palette.RunningCard,
            RoundedCornerShape(10.dp),
        ).padding(
            horizontal = 10.dp,
            vertical = 8.dp,
        ),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Body(name, Modifier.weight(1f), size = 12.sp, color = Palette.White)
        Body(status, size = 12.sp, color = if (color == Palette.Delay) color else Palette.Muted)
    }
}

@Composable
fun EndRunningDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(Corners.Dialog),
            color = Palette.White,
        ) {
            Column(
                Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Body(label(R.string.run_end_title), size = 20.sp, weight = FontWeight.Bold)
                Body(
                    label(R.string.run_end_note),
                    size = 15.sp,
                    color = Palette.Secondary,
                    lineHeight = 22.5.sp,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Body(
                            label(R.string.run_continue),
                            size = 15.sp,
                            weight = FontWeight.SemiBold,
                        )
                    }
                    ActionButton(
                        label(R.string.run_end_confirm),
                        onConfirm,
                        height = 48.dp,
                        color = Palette.Error,
                        foreground = Palette.White,
                        fontSize = 15.sp,
                        fillWidth = false,
                    )
                }
            }
        }
    }
}
