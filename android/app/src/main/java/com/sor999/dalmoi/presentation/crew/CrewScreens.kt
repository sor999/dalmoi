package com.sor999.dalmoi.presentation.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
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
import com.sor999.dalmoi.presentation.common.theme.Palette

@Composable
fun CrewListScreen(
    state: DalmoiUiState,
    onCode: (String) -> Unit,
    onJoin: () -> Unit,
    onCrew: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            horizontal = 20.dp,
            vertical = 12.dp,
        ),

        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Body(label(R.string.nav_crews), Modifier.weight(1f), size = 24.sp, weight = FontWeight.Bold)
            ActionButton(
                label(R.string.crew_create),
                { showCreate = true },
                height = 36.dp,
                fontSize = 13.sp,
                radius = 10.dp,
                fillWidth = false,
            )
        }
        ContentCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body(label(R.string.crew_join), weight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabeledInput(
                        label(R.string.crew_code),
                        state.inviteCode,
                        onCode,
                        Modifier.weight(1f),
                        strong = true,
                        numeric = true,
                        showLabel = false,
                    )
                    ActionButton(
                        label(R.string.crew_join_action),
                        onJoin,
                        height = 48.dp,
                        color = Palette.Lime,

                        foreground = Palette.Ink,
                        fontSize = 14.sp,
                        radius = 12.dp,
                        fillWidth = false,
                    )
                }
                if (state.inviteError) Body(label(R.string.crew_invalid), size = 13.sp, color = Palette.Error)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Body(
                label(R.string.crew_my_count),
                size = 13.sp,
                weight = FontWeight.SemiBold,
                color = Palette.Secondary,
            )
            CrewCard(
                label(R.string.sample_crew),
                label(R.string.crew_member_count),
                true,
                onCrew,
                state.runName,
            )
            CrewCard(label(R.string.sample_company_crew), label(R.string.crew_company_count), false, onCrew)
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Body(label(R.string.crew_create), size = 20.sp, weight = FontWeight.Bold) },
            text = { LabeledInput(label(R.string.crew_name), name, { name = it }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name)
                            showCreate = false
                        }
                    },
                    enabled = name.isNotBlank(),
                ) { Body(label(R.string.crew_create_action)) }
            },

            dismissButton = { TextButton(onClick = { showCreate = false }) { Body(label(R.string.course_cancel)) } },

        )
    }
}

@Composable
private fun CrewCard(name: String, count: String, owner: Boolean, onClick: () -> Unit, runName: String? = null) {
    ContentCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    name.take(1),
                    size = 44.dp,
                    color = if (owner) Palette.Ink else Palette.Soft,
                    foreground = if (owner) Palette.Lime else Palette.Ink,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Body(name, size = 15.sp, weight = FontWeight.Bold)
                    Caption(count)
                }
                Badge(
                    label(if (owner) R.string.crew_owner else R.string.crew_member),
                    color = if (owner) Palette.Lime else Palette.Soft,
                )
            }
            if (runName != null) {
                HorizontalDivider(color = Palette.Divider)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Body(label(R.string.crew_pending), size = 13.sp, color = Palette.Secondary)
                    Body(runName, size = 13.sp, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun CrewDetailScreen(
    state: DalmoiUiState,
    onBack: () -> Unit,
    onCreateRun: () -> Unit,
    onWaiting: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Surface(color = Palette.Ink, contentColor = Palette.White) {
            Column {
                TopBar("", onBack)
                Column(
                    Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Body(state.crewName, size = 24.sp, weight = FontWeight.Bold)
                        Caption(label(R.string.crew_leader_description), color = Palette.Faint)
                    }
                    Row(
                        Modifier.fillMaxWidth().background(
                            androidx.compose.ui.graphics.Color(0xFF262624),
                            RoundedCornerShape(14.dp),
                        )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Body(label(R.string.crew_code), size = 11.sp, color = Palette.Faint)
                            Metric("RS-7K2Q9", size = 24.sp, color = Palette.Lime)
                        }
                        ActionButton(
                            label(if (copied) R.string.crew_copied else R.string.crew_copy), {
                                clipboard.setText(AnnotatedString("RS-7K2Q9"))
                                copied = true
                            },
                            height = 36.dp,
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            foreground = Palette.White,

                            outline = androidx.compose.ui.graphics.Color(0xFF4A4A47),
                            fontSize = 13.sp,
                            radius = 10.dp,
                            fillWidth = false,
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Body(
                        label(R.string.crew_running),
                        Modifier.weight(1f),
                        size = 15.sp,
                        weight = FontWeight.Bold,
                    )
                    Body(
                        label(R.string.crew_add_run),
                        Modifier.clickable(onClick = onCreateRun).padding(vertical = 8.dp),
                        size = 13.sp,
                        weight = FontWeight.Bold,
                    )
                }
                ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                    CrewRunRow(state.runName, label(R.string.crew_running_members), true, onWaiting)
                    HorizontalDivider(color = Palette.Divider)
                    CrewRunRow(
                        label(R.string.sample_old_run),
                        label(R.string.crew_old_run_detail),
                        false,
                        onRecord,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body(label(R.string.crew_members), size = 15.sp, weight = FontWeight.Bold)
                listOf("민준 (나)", "서연", "지호", "하은").forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(
                            name.take(1),
                            size = 34.dp,
                            circle = true,
                            outline = index == 0,
                            color = if (index == 0) Palette.Lime else Palette.Soft,
                        )
                        Body(name, Modifier.weight(1f), weight = FontWeight.SemiBold)
                        if (index == 0) Caption(label(R.string.crew_owner))
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewRunRow(name: String, subtitle: String, waiting: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(
            horizontal = 16.dp,
            vertical = 14.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Body(name, weight = FontWeight.SemiBold, color = if (waiting) Palette.Ink else Palette.Secondary)
            Caption(subtitle)
        }
        Badge(
            label(if (waiting) R.string.run_waiting else R.string.run_ended),
            color = if (waiting) Palette.Lime else Palette.Soft,
        )
    }
}
