package com.sor999.dalmoi.presentation.course

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.R
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.DIALOG_SCRIM_ALPHA
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.common.theme.Spacing
import com.sor999.dalmoi.presentation.common.theme.TextSizes
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun LiveCourseEditor(
    points: List<GeoPoint>,
    title: String,
    busy: Boolean,
    canSave: Boolean,
    limitReached: Boolean,
    error: String?,
    onPoint: (GeoPoint) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onTitle: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var recenterRequest by rememberSaveable { mutableIntStateOf(0) }
    val distance = remember(points) { String.format(Locale.getDefault(), "%.2f", routeLengthKm(points)) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelLimit = maxHeight * .62f
        val panelScale = (maxWidth / 344.dp).coerceIn(1f, 1.18f)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CourseMap(points, Modifier.fillMaxSize(), onPickPoint = onPoint, recenterRequest = recenterRequest)
                Column(
                    Modifier.fillMaxWidth().windowInsetsPadding(
                        WindowInsets.statusBars.union(WindowInsets(top = 32.dp)),
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onBack,
                            enabled = !busy,
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(Corners.Input),
                            color = Palette.White,
                            shadowElevation = 3.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painterResource(R.drawable.ic_course_back),
                                    label(R.string.course_back),
                                    Modifier.size(8.dp, 17.dp),
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            shape = RoundedCornerShape(Corners.Input),
                            color = Palette.White,
                            shadowElevation = 3.dp,
                        ) {
                            Box(
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Body(label(R.string.course_title), size = TextSizes.Label, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (error != null && !naming) {
                        Surface(
                            Modifier.fillMaxWidth().padding(start = 14.dp, top = Spacing.Tiny, end = 14.dp),
                            shape = RoundedCornerShape(Corners.Input),
                            color = Palette.Error,
                        ) {
                            Body(
                                error,
                                Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                size = 13.sp,
                                color = Palette.White,
                                weight = FontWeight.SemiBold,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
                if (BuildConfig.NAVER_MAP_CONFIGURED) {
                    Surface(
                        onClick = { recenterRequest++ },
                        enabled = !busy,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 12.dp).size(44.dp),
                        shape = CircleShape,
                        color = Palette.White,
                        shadowElevation = 3.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painterResource(R.drawable.ic_course_recenter),
                                label(R.string.course_recenter),
                                Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
            Surface(
                color = Palette.White,
                shape = RoundedCornerShape(topStart = Corners.Panel, topEnd = Corners.Panel),
                shadowElevation = 6.dp,
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = panelLimit).navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 18.dp * panelScale,
                            top = 18.dp * panelScale,
                            end = 18.dp * panelScale,
                            bottom = 14.dp * panelScale,
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp * panelScale),
                ) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("api-loading"))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Body(
                            label(R.string.course_distance),
                            size = TextSizes.Caption * panelScale,
                            color = Palette.Secondary,
                            weight = FontWeight.Medium,
                        )
                        Metric(
                            distance,
                            size = 44.sp * panelScale,
                            unit = label(R.string.course_kilometers),
                            unitSize = 20.sp * panelScale,
                        )
                    }
                    Body(
                        label(R.string.course_distance_note),
                        Modifier.heightIn(min = 33.dp * panelScale),
                        size = 11.5.sp * panelScale,
                        lineHeight = 16.1.sp * panelScale,
                        color = Palette.Muted,
                    )
                    if (limitReached) {
                        Body(label(R.string.course_limit), color = Palette.Error, size = TextSizes.Caption)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small * panelScale)) {
                        val canEdit = !busy && points.isNotEmpty()
                        ActionButton(
                            label(R.string.course_undo), onUndo, Modifier.weight(1f), height = 46.dp * panelScale,
                            color = Palette.White, foreground = Palette.Ink, outline = Palette.InputBorder,
                            enabled = canEdit, fontSize = TextSizes.Label * panelScale, radius = Corners.Input,
                            fontWeight = FontWeight.SemiBold, outlineWidth = 1.dp,
                        )
                        ActionButton(
                            label(R.string.course_reset), {
                                confirmingReset = true
                            }, Modifier.weight(1f), height = 46.dp * panelScale,
                            color = Palette.White, foreground = Palette.Ink, outline = Palette.InputBorder,
                            enabled = canEdit, fontSize = TextSizes.Label * panelScale, radius = Corners.Input,
                            fontWeight = FontWeight.SemiBold, outlineWidth = 1.dp,
                        )
                    }
                    ActionButton(
                        label(R.string.course_save),
                        { naming = true },
                        height = 54.dp * panelScale,
                        fontSize = 16.sp * panelScale,
                        enabled = !busy && canSave,
                    )
                }
            }
        }
    }

    if (confirmingReset) {
        ConfirmationDialog(
            label(R.string.course_reset_title),
            label(R.string.course_reset_message),
            label(R.string.course_reset),
            onDismiss = { confirmingReset = false },
            onConfirm = {
                confirmingReset = false
                onReset()
            },
        )
    }
    if (naming) {
        CourseNameDialog(title, busy, error, onTitle, onSave, onDismiss = { naming = false })
    }
}

@Composable
private fun CourseNameDialog(
    title: String,
    busy: Boolean,
    error: String?,
    onTitle: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(DIALOG_SCRIM_ALPHA) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Corners.Dialog),
            color = Palette.White,
            contentColor = Palette.Ink,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState())
                    .padding(start = Spacing.Large, top = Spacing.Large, end = Spacing.Large, bottom = Spacing.Card),
                verticalArrangement = Arrangement.spacedBy(Spacing.Card),
            ) {
                Body(label(R.string.course_save), size = TextSizes.DialogTitle, weight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    LabeledInput(label(R.string.course_name), title, { if (!busy) onTitle(it) })
                    error?.let { Body(it, color = Palette.Error, size = TextSizes.Caption) }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small, Alignment.End),
                ) {
                    ActionButton(
                        label(R.string.course_cancel),
                        onDismiss,
                        height = Spacing.Touch,
                        color = Palette.White,
                        foreground = Palette.Ink,
                        outline = Palette.InputBorder,
                        enabled = !busy,
                        fontSize = TextSizes.Label,
                        fillWidth = false,
                        horizontalPadding = Spacing.Card,
                    )
                    ActionButton(
                        label(R.string.course_save),
                        onSave,
                        modifier = Modifier.testTag("course-name-confirm"),
                        height = Spacing.Touch,
                        enabled = title.isNotBlank() && !busy,
                        fontSize = TextSizes.Label,
                        fillWidth = false,
                        horizontalPadding = Spacing.Page,
                    )
                }
            }
        }
    }
}

private fun routeLengthKm(points: List<GeoPoint>): Double = points.zipWithNext().sumOf { (start, end) ->
    val lat = Math.toRadians(end.latitude - start.latitude)
    val lon = Math.toRadians(end.longitude - start.longitude)
    val halfLat = sin(lat / 2)
    val halfLon = sin(lon / 2)
    val h = halfLat * halfLat + cos(Math.toRadians(start.latitude)) *
        cos(Math.toRadians(end.latitude)) * halfLon * halfLon
    6371.0088 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}
