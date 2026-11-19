package com.sor999.dalmoi.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.kilometers
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Avatar
import com.sor999.dalmoi.presentation.common.component.Badge
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.Caption
import com.sor999.dalmoi.presentation.common.component.ContentCard
import com.sor999.dalmoi.presentation.common.component.Metric
import com.sor999.dalmoi.presentation.common.component.SectionTitle
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.common.theme.Spacing

@Composable
fun HomeScreen(
    state: DalmoiUiState,
    onEditor: () -> Unit,
    onCrews: () -> Unit,
    onCrew: () -> Unit,
    onWaiting: () -> Unit,
    onCourse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Spacing.Page, end = Spacing.Page, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Body(label(R.string.home_greeting), size = 22.sp, weight = FontWeight.Bold)
            Avatar("민", color = Palette.Ink, foreground = Palette.Lime, circle = true)
        }
        Surface(shape = RoundedCornerShape(20.dp), color = Palette.Ink, contentColor = Palette.White) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Body(
                        label(R.string.home_waiting),
                        size = 12.sp,
                        weight = FontWeight.SemiBold,
                        color = Palette.Lime,
                    )
                    Badge("WAITING", color = Palette.RunningBorder, foreground = Palette.Faint)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Body(state.runName, size = 19.sp, weight = FontWeight.Bold)
                    Body(label(R.string.home_run_description), size = 11.5.sp, color = Palette.Faint)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Body(
                        label(R.string.run_participant_count),
                        Modifier.weight(1f),
                        size = 13.sp,
                        color = Palette.Faint,
                    )
                    ActionButton(
                        label(R.string.home_enter),
                        onWaiting,
                        height = 34.dp,
                        fillWidth = false,
                        horizontalPadding = 16.dp,
                        color = Palette.Lime,
                        foreground = Palette.Ink,
                        fontSize = 13.sp,
                        radius = 10.dp,
                    )
                }
            }
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeShortcut(
                label(R.string.course_title),
                "+",
                Palette.Lime,
                onEditor,
                Modifier.weight(1f).fillMaxHeight(),
            )
            HomeShortcut(
                label(R.string.crew_join),
                "CODE",
                Palette.Soft,
                onCrews,
                Modifier.weight(1f).fillMaxHeight(),
                numeric = true,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(label(R.string.home_my_crews), detail = label(R.string.home_total_crews))
            ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                HomeCrewRow(
                    "새",
                    label(R.string.sample_crew),
                    label(R.string.crew_owner_members),
                    true,
                    onCrew,
                )
                HorizontalDivider(color = Palette.Divider)
                HomeCrewRow(
                    "회",
                    label(R.string.sample_company_crew),
                    label(R.string.crew_company_members),
                    false,
                    onCrew,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(label(R.string.home_my_courses), detail = label(R.string.home_total_courses))
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.courses.take(2).forEach { course ->
                    ContentCard(
                        Modifier.weight(1f).fillMaxHeight(),
                        radius = 14.dp,
                        onClick = { onCourse(course.id) },
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Metric(
                                kilometers(course.distanceMeters),
                                size = 26.sp,
                                weight = FontWeight.SemiBold,
                                unit = label(R.string.course_kilometers),
                                unitSize = 14.sp,
                            )
                            Body(course.name, size = 13.sp, weight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    title: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
) {
    ContentCard(modifier, radius = 16.dp, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Box(
                Modifier.size(28.dp).background(
                    color,
                    RoundedCornerShape(8.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (numeric) {
                    Metric(
                        icon,
                        size = 12.sp,
                    )
                } else {
                    Body(
                        icon,
                        size = 18.sp,
                        weight = FontWeight.Bold,
                    )
                }
            }
            Body(title, size = 15.sp, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeCrewRow(initial: String, name: String, description: String, owner: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Palette.White) {
        Row(
            Modifier.fillMaxWidth().padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                initial,
                color = if (owner) Palette.Ink else Palette.Soft,
                foreground = if (owner) Palette.Lime else Palette.Ink,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Body(name, weight = FontWeight.SemiBold)
                Caption(description)
            }
            Body("›", size = 18.sp, color = Palette.Faint)
        }
    }
}
