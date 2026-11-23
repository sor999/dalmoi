package com.sor999.dalmoi.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.kilometers
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.Caption
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.TopBar
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.map.FigmaMap
import com.sor999.dalmoi.presentation.common.map.MapArtwork
import com.sor999.dalmoi.presentation.common.theme.Palette

@Composable
fun RecordScreen(state: DalmoiUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    RecordLayout(
        title = state.runName,
        date = label(if (state.mvp) R.string.mvp_date else R.string.record_date),
        distance = if (state.mvp) "5.12" else "5.31",
        duration = if (state.mvp) "32:47" else "31:48",
        plannedDistance = if (state.mvp) "5.00" else kilometers(state.course.distanceMeters),
        onBack = onBack,
        modifier = modifier,
        mvp = state.mvp,
        processing = state.processingRecord,
        showLegend = !state.mvp,
        map = { FigmaMap(if (state.mvp) MapArtwork.MvpRecord else MapArtwork.Record, Modifier.fillMaxSize()) },
    )
}

@Composable
internal fun RecordLayout(
    title: String,
    date: String,
    distance: String,
    duration: String,
    plannedDistance: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    mvp: Boolean = false,
    processing: Boolean = false,
    showLegend: Boolean = false,
    map: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        if (!mvp) {
            TopBar(label(R.string.record_title), onBack) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(7.dp).background(Palette.Positive, CircleShape))
                    Body(
                        label(if (processing) R.string.record_processing else R.string.record_saved),

                        size = 12.sp,
                        color = Palette.Positive,
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(
                horizontal = 20.dp,
                vertical = if (mvp) 16.dp else 4.dp,
            ),

            verticalArrangement = Arrangement.spacedBy(if (mvp) 20.dp else 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (mvp) Body(label(R.string.record_complete), size = 26.sp, weight = FontWeight.Bold)
                Body(date, size = 13.sp, color = Palette.Secondary)
                if (!mvp) Body(title, size = 24.sp, weight = FontWeight.Bold)
            }
            if (mvp) RecordMap(mvp, showLegend, map)
            Column {
                Caption(label(if (mvp) R.string.run_distance else R.string.record_actual))
                Metric(
                    distance,
                    size = if (mvp) 80.sp else 88.sp,
                    unit = label(R.string.course_kilometers),
                    unitSize = 28.sp,
                )
            }
            Column {
                HorizontalDivider(color = Palette.Border)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(
                        Modifier.weight(1f).padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Caption(label(R.string.run_duration))
                        Metric(duration, size = if (mvp) 34.sp else 30.sp)
                    }
                    if (plannedDistance != null) {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Palette.Border))
                        Column(
                            Modifier.weight(1f).padding(
                                start = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Caption(label(R.string.record_planned))
                            Metric(
                                plannedDistance,
                                size = if (mvp) 34.sp else 30.sp,
                                weight = if (mvp) FontWeight.Bold else FontWeight.SemiBold,

                                unit = label(R.string.course_kilometers),
                                unitSize = if (mvp) 30.sp else 28.sp,
                                color = Palette.Muted,
                                unitColor = Palette.Muted,
                            )
                        }
                    }
                }
                HorizontalDivider(color = Palette.Border)
            }
            if (!mvp) {
                RecordMap(mvp, showLegend, map)
                Caption(label(R.string.record_private), color = Palette.Muted)
            }
        }
        if (mvp) {
            ActionButton(
                label(R.string.record_confirm),
                onBack,
                Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
                height = 56.dp,
            )
        }
    }
}

@Composable
private fun RecordMap(mvp: Boolean, showLegend: Boolean, map: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(if (mvp) 320f / 260f else 304f / 220f).clip(RoundedCornerShape(18.dp))) {
        map()
        if (showLegend) {
            Surface(Modifier.padding(10.dp), shape = RoundedCornerShape(10.dp)) {
                Body(
                    label(R.string.record_legend),
                    Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp,
                    ),
                    size = 11.sp,
                    weight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun RecordListLayout(modifier: Modifier = Modifier, records: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Body(label(R.string.nav_records), size = 24.sp, weight = FontWeight.Bold)
        records()
    }
}
