package com.sor999.dalmoi.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
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
    HomeScreen(
        content = HomeContent(
            greeting = label(R.string.home_greeting),
            initial = "민",
            waiting = HomeRun(
                state.runName,
                "WAITING",
                label(R.string.home_run_description),
                label(R.string.run_participant_count),
                onWaiting,
            ),
            crews = listOf(
                HomeCrew(label(R.string.sample_crew), label(R.string.crew_owner_members), true, onCrew),
                HomeCrew(label(R.string.sample_company_crew), label(R.string.crew_company_members), false, onCrew),
            ),
            crewTotal = label(R.string.home_total_crews),
            courses = state.courses.map { course ->
                HomeCourse(course.name, kilometers(course.distanceMeters), { onCourse(course.id) })
            },
            courseTotal = label(R.string.home_total_courses),
        ),
        onEditor = onEditor,
        onJoin = onCrews,
        modifier = modifier,
    )
}

internal data class HomeContent(
    val greeting: String,
    val initial: String,
    val waiting: HomeRun?,
    val crews: List<HomeCrew>,
    val crewTotal: String,
    val courses: List<HomeCourse>,
    val courseTotal: String,
)

internal data class HomeRun(
    val title: String,
    val status: String,
    val description: String,
    val participants: String,
    val onEnter: () -> Unit,
)

internal data class HomeCrew(val name: String, val description: String, val owner: Boolean, val onClick: () -> Unit)

internal data class HomeCourse(val title: String, val distance: String, val onClick: () -> Unit)

@Composable
internal fun HomeScreen(
    content: HomeContent,
    onEditor: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRefresh: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val deviceDensity = LocalDensity.current
        val layoutScale = (maxWidth / 344.dp).coerceIn(1f, 1.2f)
        CompositionLocalProvider(
            LocalDensity provides Density(deviceDensity.density * layoutScale, deviceDensity.fontScale),
        ) {
            HomeLayout(content, onEditor, onJoin, enabled, onRefresh, onLogout)
        }
    }
}

@Composable
private fun HomeLayout(
    content: HomeContent,
    onEditor: () -> Unit,
    onJoin: () -> Unit,
    enabled: Boolean,
    onRefresh: (() -> Unit)?,
    onLogout: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = Spacing.Page, end = Spacing.Page, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Body(
                content.greeting,
                Modifier.weight(1f).padding(end = Spacing.Small),
                size = 22.sp,
                weight = FontWeight.Bold,
            )
            if (onRefresh != null && onLogout != null) {
                HomeProfile(content.initial, enabled, onRefresh, onLogout)
            } else {
                Avatar(content.initial, color = Palette.Ink, foreground = Palette.Lime, circle = true)
            }
        }
        content.waiting?.let { waiting ->
            HomeWaitingRun(waiting, enabled)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeShortcut(
                label(R.string.course_title),
                "+",
                Palette.Lime,
                onEditor.takeIf { enabled },
                Modifier.weight(1f).fillMaxHeight(),
            )
            HomeShortcut(
                label(R.string.crew_join),
                "CODE",
                Palette.Soft,
                onJoin.takeIf { enabled },
                Modifier.weight(1f).fillMaxHeight(),
                numeric = true,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(label(R.string.home_my_crews), detail = content.crewTotal)
            ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                if (content.crews.isEmpty()) {
                    HomeEmpty()
                }
                content.crews.take(2).forEachIndexed { index, crew ->
                    if (index > 0) HorizontalDivider(color = Palette.Divider)
                    HomeCrewRow(crew, enabled)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(label(R.string.home_my_courses), detail = content.courseTotal)
            if (content.courses.isEmpty()) {
                ContentCard(Modifier.fillMaxWidth(), radius = 14.dp) { HomeEmpty() }
            } else {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    content.courses.take(2).forEach { course ->
                        ContentCard(
                            Modifier.weight(1f).fillMaxHeight(),
                            radius = 14.dp,
                            onClick = course.onClick.takeIf { enabled },
                        ) {
                            Column(
                                Modifier.padding(Spacing.Medium),
                                verticalArrangement = Arrangement.spacedBy(Spacing.Tiny),
                            ) {
                                Metric(
                                    course.distance,
                                    size = 26.sp,
                                    weight = FontWeight.SemiBold,
                                    unit = label(R.string.course_kilometers),
                                    unitSize = 14.sp,
                                )
                                Body(course.title, size = 13.sp, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (content.courses.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeWaitingRun(waiting: HomeRun, enabled: Boolean) {
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
                Badge(waiting.status, color = Palette.RunningBorder, foreground = Palette.Faint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Body(waiting.title, size = 19.sp, weight = FontWeight.Bold)
                Body(waiting.description, size = 11.5.sp, color = Palette.Faint)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Body(
                    waiting.participants,
                    Modifier.weight(1f),
                    size = 13.sp,
                    color = Palette.Faint,
                )
                ActionButton(
                    label(R.string.home_enter),
                    waiting.onEnter,
                    height = 34.dp,
                    enabled = enabled,
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
}

@Composable
private fun HomeShortcut(
    title: String,
    icon: String,
    color: Color,
    onClick: (() -> Unit)?,
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
private fun HomeCrewRow(crew: HomeCrew, enabled: Boolean) {
    Surface(onClick = crew.onClick, enabled = enabled, color = Palette.White) {
        Row(
            Modifier.fillMaxWidth().padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                crew.name.take(1),
                color = if (crew.owner) Palette.Ink else Palette.Soft,
                foreground = if (crew.owner) Palette.Lime else Palette.Ink,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Body(crew.name, weight = FontWeight.SemiBold)
                Caption(crew.description)
            }
            Body("›", size = 18.sp, color = Palette.Faint)
        }
    }
}

@Composable
private fun HomeEmpty() {
    Caption(label(R.string.api_empty), Modifier.padding(Spacing.Card))
}

@Composable
private fun HomeProfile(initial: String, enabled: Boolean, onRefresh: () -> Unit, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = label(R.string.home_profile)
    Box {
        Avatar(
            initial,
            Modifier.semantics { contentDescription = description }
                .clickable(enabled = enabled, role = Role.Button) { expanded = true },
            color = Palette.Ink,
            foreground = Palette.Lime,
            circle = true,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Palette.White) {
            DropdownMenuItem(
                text = { Body(label(R.string.api_refresh)) },
                onClick = {
                    expanded = false
                    onRefresh()
                },
            )
            DropdownMenuItem(
                text = { Body(label(R.string.api_logout)) },
                onClick = {
                    expanded = false
                    onLogout()
                },
            )
        }
    }
}
