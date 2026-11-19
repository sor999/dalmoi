package com.sor999.dalmoi.presentation.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.CourseCardData
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.kilometers
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
import com.sor999.dalmoi.presentation.course.CourseOption

@Composable
fun CreateSessionScreen(
    state: DalmoiUiState,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(state.runName) }
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
                LabeledInput(label(R.string.run_name), name, { name = it }, strong = true)
                Caption(state.crewName, color = Palette.Muted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(label(R.string.run_common_course), detail = label(R.string.run_course_fixed))
                state.courses.forEach { course ->
                    CourseOption(
                        course,
                        course.id == state.selectedCourseId,
                        { onSelect(course.id) },
                    )
                }
            }
        }
        ActionButton(
            label(R.string.run_create_room),
            { onCreate(name) },
            Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp,
            ),
            enabled = name.isNotBlank(),
        )
    }
}

@Composable
fun WaitingRoomScreen(state: DalmoiUiState, onBack: () -> Unit, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TopBar(
            label(R.string.run_waiting_room),
            onBack,
        ) {
            Badge(
                "WAITING",
                color = Palette.Ink,
                foreground = Palette.Lime,
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(
                horizontal = 20.dp,
                vertical = 4.dp,
            ),

            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Body(state.runName, size = 26.sp, weight = FontWeight.Bold)
                Body(state.crewName, size = 13.sp, color = Palette.Secondary)
            }
            Box(Modifier.fillMaxWidth().aspectRatio(304f / 150f).clip(RoundedCornerShape(18.dp))) {
                FigmaMap(MapArtwork.Waiting, Modifier.fillMaxSize())
                Surface(
                    Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Body(state.course.name, size = 12.sp, weight = FontWeight.SemiBold)
                        Metric(
                            kilometers(state.course.distanceMeters),
                            size = 17.sp,
                            unit = label(R.string.course_kilometers),
                            unitSize = 17.sp,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(
                    label(R.string.run_participants),
                    detail = label(R.string.run_participant_numbers),
                )
                ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                    listOf("민준 · 크루장", "서연", "지호", "하은").forEachIndexed { index, name ->
                        if (index > 0) HorizontalDivider(color = Palette.Divider)
                        Row(
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(
                                name.take(1),
                                size = 32.dp,
                                circle = true,
                                outline = index == 0,
                                color = if (index == 0) Palette.Lime else Palette.Soft,
                            )
                            Body(
                                name,
                                Modifier.weight(1f),
                                weight = if (index == 3) FontWeight.Normal else FontWeight.SemiBold,

                                color = if (index == 3) Palette.Muted else Palette.Ink,
                            )
                            Body(
                                label(if (index == 3) R.string.run_not_joined else R.string.run_joined),
                                size = 12.sp,

                                color = if (index == 3) Palette.Faint else Palette.Positive,
                            )
                        }
                    }
                }
            }
        }
        Column(
            Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
}

@Composable
fun RunningScreen(state: DalmoiUiState, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    var endDialog by rememberSaveable { mutableStateOf(false) }
    BackHandler { endDialog = true }
    val dark = !state.mvp
    val background = if (dark) Palette.Running else Palette.White
    val foreground = if (dark) Palette.White else Palette.Ink
    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        val panelLimit = maxHeight * .7f
        FigmaMap(
            if (state.mvp) {
                MapArtwork.MvpRunning
            } else if (state.disconnected) {
                MapArtwork.RunningError
            } else {
                MapArtwork.Running
            },

            Modifier.fillMaxSize(),
            fullScreen = true,
        )
        Column(Modifier.fillMaxSize()) {
            if (state.mvp) {
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
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(
                        horizontal = 14.dp,
                        vertical = 6.dp,
                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusChip(
                        label(if (state.disconnected) R.string.run_stopped else R.string.run_sharing),
                        dot = if (state.disconnected) Palette.Muted else Palette.Lime,
                    )
                    StatusChip(label(if (state.disconnected) R.string.run_last_received else R.string.run_received))
                }
                if (state.disconnected) {
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
                    if (state.mvp) {
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
                            Metric("14:34", size = 76.sp, color = Palette.Lime)
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
                                "2.31",
                                Palette.White,
                                Modifier.weight(1f),
                            )
                            RunningMetric(
                                label(R.string.run_course_distance),
                                kilometers(state.course.distanceMeters),
                                Palette.Muted,
                                Modifier.weight(1f),
                            )
                        }
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
                        val endLabel = label(R.string.run_end)
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .background(background, RoundedCornerShape(16.dp))
                                .then(Modifier.border(2.dp, Palette.White, RoundedCornerShape(16.dp)))
                                .combinedClickable(onClick = {}, onLongClick = { endDialog = true })
                                .semantics {
                                    customActions = listOf(
                                        CustomAccessibilityAction(endLabel) {
                                            endDialog = true
                                            true
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
