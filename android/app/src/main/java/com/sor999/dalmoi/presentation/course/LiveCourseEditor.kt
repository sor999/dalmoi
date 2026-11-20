package com.sor999.dalmoi.presentation.course

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.BuildConfig
import com.sor999.dalmoi.R
import com.sor999.dalmoi.domain.model.GeoPoint
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.Palette
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
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onBack,
                        enabled = !busy,
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
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
                        shape = RoundedCornerShape(12.dp),
                        color = Palette.White,
                        shadowElevation = 3.dp,
                    ) {
                        Box(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Body(label(R.string.course_title), size = 14.sp, weight = FontWeight.Bold)
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
                        Body(label(R.string.course_distance), size = 12.sp * panelScale, color = Palette.Secondary)
                        Metric(
                            distance,
                            size = 44.sp * panelScale,
                            unit = label(R.string.course_kilometers),
                            unitSize = 20.sp * panelScale,
                        )
                    }
                    Body(
                        label(R.string.course_distance_note),
                        size = 11.5.sp * panelScale,
                        lineHeight = 16.1.sp * panelScale,
                        color = Palette.Muted,
                    )
                    if (limitReached || error != null) {
                        Body(error ?: label(R.string.course_limit), color = Palette.Error, size = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp * panelScale)) {
                        val canEdit = !busy && points.isNotEmpty()
                        ActionButton(
                            label(R.string.course_undo), onUndo, Modifier.weight(1f), height = 46.dp * panelScale,
                            color = Palette.White, foreground = Palette.Ink, outline = Palette.InputBorder,
                            enabled = canEdit, fontSize = 14.sp * panelScale, radius = 12.dp,
                        )
                        ActionButton(
                            label(R.string.course_reset), {
                                confirmingReset = true
                            }, Modifier.weight(1f), height = 46.dp * panelScale,
                            color = Palette.White, foreground = Palette.Ink, outline = Palette.InputBorder,
                            enabled = canEdit, fontSize = 14.sp * panelScale, radius = 12.dp,
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
        AlertDialog(
            onDismissRequest = { if (!busy) naming = false },
            title = { Body(label(R.string.course_save), size = 20.sp, weight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledInput(label(R.string.course_name), title, { if (!busy) onTitle(it) })
                    error?.let { Body(it, color = Palette.Error, size = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSave,
                    enabled = title.isNotBlank() && !busy,
                    modifier = Modifier.testTag("course-name-confirm"),
                ) { Body(label(R.string.course_save)) }
            },
            dismissButton = {
                TextButton(onClick = { naming = false }, enabled = !busy) { Body(label(R.string.course_cancel)) }
            },
        )
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
