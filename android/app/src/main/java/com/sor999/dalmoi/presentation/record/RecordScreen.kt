package com.sor999.dalmoi.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    Column(modifier.fillMaxSize()) {
        if (!state.mvp) {
            TopBar(label(R.string.record_title), onBack) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(7.dp).background(Palette.Positive, CircleShape))
                    Body(
                        label(if (state.processingRecord) R.string.record_processing else R.string.record_saved),

                        size = 12.sp,
                        color = Palette.Positive,
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(
                horizontal = 20.dp,
                vertical = if (state.mvp) 16.dp else 4.dp,
            ),

            verticalArrangement = Arrangement.spacedBy(if (state.mvp) 20.dp else 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.mvp) Body(label(R.string.record_complete), size = 26.sp, weight = FontWeight.Bold)
                Body(
                    label(if (state.mvp) R.string.mvp_date else R.string.record_date),
                    size = 13.sp,
                    color = Palette.Secondary,
                )
                if (!state.mvp) Body(state.runName, size = 24.sp, weight = FontWeight.Bold)
            }
            if (state.mvp) RecordMap(true)
            Column {
                Caption(label(if (state.mvp) R.string.run_distance else R.string.record_actual))
                Metric(
                    if (state.mvp) "5.12" else "5.31",
                    size = if (state.mvp) 80.sp else 88.sp,
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
                        Metric(if (state.mvp) "32:47" else "31:48", size = if (state.mvp) 34.sp else 30.sp)
                    }
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
                            if (state.mvp) "5.00" else kilometers(state.course.distanceMeters),
                            size = if (state.mvp) 34.sp else 30.sp,
                            weight = if (state.mvp) FontWeight.Bold else FontWeight.SemiBold,

                            unit = label(R.string.course_kilometers),
                            unitSize = if (state.mvp) 30.sp else 28.sp,
                            color = Palette.Muted,
                            unitColor = Palette.Muted,
                        )
                    }
                }
                HorizontalDivider(color = Palette.Border)
            }
            if (!state.mvp) {
                RecordMap(false)
                Caption(label(R.string.record_private), color = Palette.Muted)
            }
        }
        if (state.mvp) {
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
private fun RecordMap(mvp: Boolean) {
    Box(Modifier.fillMaxWidth().aspectRatio(if (mvp) 320f / 260f else 304f / 220f).clip(RoundedCornerShape(18.dp))) {
        FigmaMap(if (mvp) MapArtwork.MvpRecord else MapArtwork.Record, Modifier.fillMaxSize())
        if (!mvp) {
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
