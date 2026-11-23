package com.sor999.dalmoi.presentation.course

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.CourseCardData
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.DraftError
import com.sor999.dalmoi.app.demo.kilometers
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.Caption
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.TopBar
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.map.FigmaMap
import com.sor999.dalmoi.presentation.common.map.MapArtwork
import com.sor999.dalmoi.presentation.common.map.MapPoint
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.session.CourseOption

@Composable
fun CourseEditorScreen(
    state: DalmoiUiState,
    onPoint: (MapPoint) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onName: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var resetCamera by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelLimit = maxHeight * .62f
        val artwork = if (state.mvp) {
            MapArtwork.MvpEditor
        } else if (state.draftError ==
            DraftError.SaveFailed
        ) {
            MapArtwork.EditorError
        } else {
            MapArtwork.Editor
        }
        FigmaMap(
            artwork,
            Modifier.fillMaxSize(),
            fullScreen = true,
            editedRoute = if (state.draftEdited) state.draftPoints else null,
            onMapTap = onPoint,
            cameraResetKey = resetCamera,
        )
        Column(Modifier.fillMaxSize()) {
            if (state.mvp) {
                Surface {
                    Body(
                        label(R.string.course_title),
                        Modifier.fillMaxWidth().padding(20.dp),
                        size = 18.sp,
                        weight = FontWeight.Bold,
                    )
                }
            } else {
                Row(
                    Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 6.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val backDescription = label(R.string.course_back)
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(40.dp).semantics { contentDescription = backDescription },
                    ) {
                        Box(contentAlignment = Alignment.Center) { Body("‹", size = 20.sp) }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Body(
                            label(R.string.course_title),
                            Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),
                            weight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (!state.mvp) {
                DraftErrorBanner(
                    state.draftError,
                    Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 4.dp,
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.mvp) {
                DraftErrorBanner(
                    state.draftError,
                    Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                )
            }
            if (!state.mvp) {
                val recenterDescription = label(R.string.course_recenter)
                Surface(
                    onClick = { resetCamera++ },
                    shape = CircleShape,
                    shadowElevation = 3.dp,
                    modifier = Modifier.align(Alignment.End).padding(end = 14.dp, bottom = 12.dp).size(44.dp)
                        .semantics { contentDescription = recenterDescription },
                ) {
                    Box(contentAlignment = Alignment.Center) { Body("⊙", size = 25.sp) }
                }
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = Corners.Panel,
                    topEnd = Corners.Panel,
                ),
                shadowElevation = 6.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = panelLimit)
                        .pointerInput(Unit) { detectTapGestures {} }
                        .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Caption(label(if (state.mvp) R.string.mvp_distance else R.string.course_distance))
                        Metric(
                            kilometers(state.draftDistanceMeters),
                            size = if (state.mvp) 56.sp else 44.sp,
                            unit = label(R.string.course_kilometers),
                            unitSize = if (state.mvp) 22.sp else 20.sp,
                        )
                    }
                    if (state.mvp) {
                        LabeledInput(label(R.string.course_name), state.draftName, onName, showLabel = false)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton(
                                label(R.string.course_undo),
                                onUndo,
                                Modifier.weight(1f),
                                height = 56.dp,

                                color = Palette.White,
                                foreground = Palette.Ink,
                                outline = Palette.InputBorder,
                                fontSize = 15.sp,
                            )
                            ActionButton(
                                label(R.string.course_save),
                                onSave,
                                Modifier.weight(1f),
                                height = 56.dp,
                            )
                        }
                    } else {
                        Body(
                            label(R.string.course_distance_note),
                            size = 11.5.sp,
                            lineHeight = 16.1.sp,
                            color = Palette.Muted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton(
                                label(R.string.course_undo),
                                onUndo,
                                Modifier.weight(1f),
                                height = 48.dp,

                                color = Palette.White,
                                foreground = Palette.Ink,
                                outline = Palette.InputBorder,
                                fontSize = 14.sp,
                                radius = 12.dp,
                            )
                            ActionButton(
                                label(R.string.course_reset),
                                { confirmReset = true },
                                Modifier.weight(1f),
                                height = 48.dp,

                                color = Palette.White,
                                foreground = Palette.Ink,
                                outline = Palette.InputBorder,
                                fontSize = 14.sp,
                                radius = 12.dp,
                            )
                        }
                        ActionButton(label(R.string.course_save), onSave)
                    }
                }
            }
        }
    }
    if (confirmReset) {
        ConfirmationDialog(
            label(R.string.course_reset_title),
            label(R.string.course_reset_message),
            label(R.string.course_reset),
            { confirmReset = false },
            {
                confirmReset = false
                onReset()
            },
        )
    }
}

@Composable
fun CourseDetailScreen(
    course: CourseCardData,
    onBack: () -> Unit,
    onCreateRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CourseDetailLayout(
        course.name,
        kilometers(course.distanceMeters),
        course.createdOn,
        course.points.isEmpty() || course.points.first() == course.points.last(),
        onBack,
        onCreateRun,
        modifier,
    ) {
        FigmaMap(
            MapArtwork.Course,
            Modifier.fillMaxSize(),
            editedRoute = course.points.takeIf { it.isNotEmpty() }
                ?.map { MapPoint(it.x / 344f * 312f, it.y / 744f * 300f) },
        )
    }
}

@Composable
internal fun CourseDetailLayout(
    title: String,
    distance: String,
    createdOn: String,
    loop: Boolean,
    onBack: () -> Unit,
    onCreateRun: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    map: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        TopBar(label(R.string.course_detail), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier.padding(
                    horizontal = 16.dp,
                ).fillMaxWidth().aspectRatio(312f / 300f).clip(RoundedCornerShape(20.dp)),
            ) {
                map()
                Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(99.dp), color = Palette.Ink) {
                    Body(
                        label(if (loop) R.string.course_loop_type else R.string.course_open_type),
                        Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp,
                        ),
                        size = 11.sp,
                        weight = FontWeight.SemiBold,
                        color = Palette.White,
                    )
                }
            }
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Body(title, size = 24.sp, weight = FontWeight.Bold)
                    Body(
                        androidx.compose.ui.res.stringResource(
                            R.string.course_created_format,
                            createdOn,
                        ),
                        size = 13.sp,
                        color = Palette.Secondary,
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Palette.Border)
                    Column(
                        Modifier.padding(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Caption(label(R.string.course_distance))
                        Metric(
                            distance,
                            size = 32.sp,
                            unit = label(R.string.course_kilometers),
                        )
                    }
                    HorizontalDivider(color = Palette.Border)
                }
                Body(label(R.string.course_detail_note), size = 12.sp, lineHeight = 17.sp, color = Palette.Muted)
            }
        }
        ActionButton(
            label(R.string.course_create_run),
            onCreateRun,
            Modifier.padding(
                horizontal = 20.dp,
                vertical = 12.dp,
            ),
            enabled = enabled,
        )
    }
}

@Composable
fun CourseListScreen(
    state: DalmoiUiState,
    onEditor: () -> Unit,
    onCourse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CourseListLayout(onEditor, modifier) {
        state.courses.forEach { course -> CourseOption(course, false, { onCourse(course.id) }) }
    }
}

@Composable
internal fun CourseListLayout(
    onEditor: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Body(label(R.string.nav_courses), size = 24.sp, weight = FontWeight.Bold)
        ActionButton(label(R.string.course_title), onEditor, enabled = enabled)
        content()
    }
}

@Composable
fun ConfirmationDialog(title: String, message: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Corners.Dialog),
        title = { Body(title, size = 20.sp, weight = FontWeight.Bold) },
        text = { Body(message, size = 15.sp, lineHeight = 22.5.sp, color = Palette.Secondary) },
        confirmButton = {
            ActionButton(
                action,
                onConfirm,
                Modifier.padding(start = 8.dp),
                height = 48.dp,
                color = Palette.Error,
                foreground = Palette.White,
                fontSize = 15.sp,
                fillWidth = false,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Body(
                    label(R.string.course_cancel),
                    weight = FontWeight.SemiBold,
                )
            }
        },
    )
}

@Composable
private fun DraftErrorBanner(error: DraftError?, modifier: Modifier = Modifier) {
    if (error == null) return
    val message = when (error) {
        DraftError.TooFewPoints -> R.string.mvp_points_error
        DraftError.NameRequired -> R.string.course_name_required
        DraftError.PointLimit -> R.string.course_limit
        DraftError.SaveFailed -> R.string.course_save_unavailable
    }
    Surface(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = Palette.Error,
    ) {
        Body(
            label(message),
            Modifier.padding(12.dp),
            size = 13.sp,
            weight = FontWeight.SemiBold,
            color = Palette.White,
        )
    }
}
