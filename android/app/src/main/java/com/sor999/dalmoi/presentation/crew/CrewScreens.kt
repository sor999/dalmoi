package com.sor999.dalmoi.presentation.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.graphics.Color
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
    CrewListLayout(
        code = state.inviteCode,
        onCode = onCode,
        onJoin = onJoin,
        crews = listOf(
            CrewListItem(label(R.string.sample_crew), label(R.string.crew_member_count), true, onCrew, state.runName),
            CrewListItem(label(R.string.sample_company_crew), label(R.string.crew_company_count), false, onCrew),
        ),
        count = label(R.string.crew_my_count),
        onCreate = onCreate,
        modifier = modifier,
        joinError = if (state.inviteError) label(R.string.crew_invalid) else null,
    )
}

internal data class CrewListItem(
    val name: String,
    val count: String,
    val owner: Boolean,
    val onClick: () -> Unit,
    val runName: String? = null,
)

@Composable
internal fun CrewListLayout(
    code: String,
    onCode: (String) -> Unit,
    onJoin: () -> Unit,
    crews: List<CrewListItem>,
    count: String,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onBack: (() -> Unit)? = null,
    joinError: String? = null,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(
            horizontal = 20.dp,
            vertical = 12.dp,
        ),

        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (onBack != null) {
            TopBar(label(R.string.crew_join), onBack)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(label(R.string.nav_crews), Modifier.weight(1f), size = 24.sp, weight = FontWeight.Bold)
                ActionButton(
                    label(R.string.crew_create),
                    { showCreate = true },
                    enabled = enabled,
                    height = 36.dp,
                    fontSize = 13.sp,
                    radius = 10.dp,
                    fillWidth = false,
                )
            }
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
                        code,
                        onCode,
                        Modifier.weight(1f),
                        strong = true,
                        numeric = true,
                        showLabel = false,
                        enabled = enabled,
                    )
                    ActionButton(
                        label(R.string.crew_join_action),
                        onJoin,
                        enabled = enabled && code.isNotBlank(),
                        height = 48.dp,
                        color = Palette.Lime,

                        foreground = Palette.Ink,
                        fontSize = 14.sp,
                        radius = 12.dp,
                        fillWidth = false,
                    )
                }
                joinError?.let { Body(it, size = 13.sp, color = Palette.Error) }
            }
        }
        if (onBack == null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Body(count, size = 13.sp, weight = FontWeight.SemiBold, color = Palette.Secondary)
                if (crews.isEmpty()) Caption(label(R.string.api_empty))
                crews.forEach { crew -> CrewCard(crew, enabled) }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Body(label(R.string.crew_create), size = 20.sp, weight = FontWeight.Bold) },
            text = { LabeledInput(label(R.string.crew_name), name, { name = it }, enabled = enabled) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name)
                            showCreate = false
                        }
                    },
                    enabled = enabled && name.isNotBlank(),
                ) { Body(label(R.string.crew_create_action)) }
            },

            dismissButton = { TextButton(onClick = { showCreate = false }) { Body(label(R.string.course_cancel)) } },

        )
    }
}

@Composable
private fun CrewCard(crew: CrewListItem, enabled: Boolean) {
    val owner = crew.owner
    ContentCard(Modifier.fillMaxWidth(), onClick = crew.onClick.takeIf { enabled }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(
                    crew.name.take(1),
                    size = 44.dp,
                    color = if (owner) Palette.Ink else Palette.Soft,
                    foreground = if (owner) Palette.Lime else Palette.Ink,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Body(crew.name, size = 15.sp, weight = FontWeight.Bold)
                    Caption(crew.count)
                }
                Badge(
                    label(if (owner) R.string.crew_owner else R.string.crew_member),
                    color = if (owner) Palette.Lime else Palette.Soft,
                )
            }
            crew.runName?.let { runName ->
                HorizontalDivider(color = Palette.Divider)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Body(label(R.string.crew_pending), size = 13.sp, color = Palette.Secondary)
                    Body(runName, size = 13.sp, weight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CrewDetailScreen(
    state: DalmoiUiState,
    onBack: () -> Unit,
    onCreateRun: () -> Unit,
    onWaiting: () -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CrewDetailLayout(
        name = state.crewName,
        description = label(R.string.crew_leader_description),
        onBack = onBack,
        inviteCode = "RS-7K2Q9",
        onCreateRun = onCreateRun,
        runs = listOf(
            CrewRunItem(
                state.runName,
                label(R.string.crew_running_members),
                label(R.string.run_waiting),
                true,
                onWaiting,
            ),
            CrewRunItem(
                label(R.string.sample_old_run),
                label(R.string.crew_old_run_detail),
                label(R.string.run_ended),
                false,
                onRecord,
            ),
        ),
        members = listOf("민준 (나)", "서연", "지호", "하은").mapIndexed { index, name ->
            CrewMemberItem(name, index == 0, index == 0)
        },
        modifier = modifier,
    )
}

internal data class CrewRunItem(
    val name: String,
    val subtitle: String,
    val status: String,
    val waiting: Boolean,
    val onClick: () -> Unit,
)

internal data class CrewMemberItem(val name: String, val isMe: Boolean, val owner: Boolean)

@Suppress("DEPRECATION")
@Composable
internal fun CrewDetailLayout(
    name: String,
    description: String,
    onBack: () -> Unit,
    inviteCode: String?,
    onCreateRun: (() -> Unit)?,
    runs: List<CrewRunItem>,
    members: List<CrewMemberItem>,
    modifier: Modifier = Modifier,
    onShowInvite: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable(inviteCode) { mutableStateOf(false) }
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
                        Body(name, size = 24.sp, weight = FontWeight.Bold)
                        Caption(description, color = Palette.Faint)
                    }
                    if (inviteCode != null || onShowInvite != null) {
                        Row(
                            Modifier.fillMaxWidth().background(Palette.RunningBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Body(label(R.string.crew_code), size = 11.sp, color = Palette.Faint)
                                if (inviteCode != null) Metric(inviteCode, size = 24.sp, color = Palette.Lime)
                            }
                            ActionButton(
                                label(
                                    when {
                                        copied -> R.string.crew_copied
                                        inviteCode != null -> R.string.crew_copy
                                        else -> R.string.api_show_invite
                                    },
                                ),
                                {
                                    if (inviteCode != null) {
                                        clipboard.setText(AnnotatedString(inviteCode))
                                        copied = true
                                    } else {
                                        onShowInvite?.invoke()
                                    }
                                },
                                height = 36.dp,
                                enabled = enabled,
                                color = Color.Transparent,
                                foreground = Palette.White,
                                outline = Palette.Secondary,
                                fontSize = 13.sp,
                                radius = 10.dp,
                                fillWidth = false,
                            )
                        }
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
                    if (onCreateRun != null) {
                        Body(
                            label(R.string.crew_add_run),
                            Modifier.clickable(enabled = enabled, onClick = onCreateRun).padding(vertical = 8.dp),
                            size = 13.sp,
                            weight = FontWeight.Bold,
                        )
                    }
                }
                ContentCard(Modifier.fillMaxWidth(), radius = 16.dp) {
                    if (runs.isEmpty()) Caption(label(R.string.api_empty), Modifier.padding(16.dp))
                    runs.forEachIndexed { index, run ->
                        if (index > 0) HorizontalDivider(color = Palette.Divider)
                        CrewRunRow(run, enabled)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body(label(R.string.crew_members), size = 15.sp, weight = FontWeight.Bold)
                members.forEach { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(
                            member.name.take(1),
                            size = 34.dp,
                            circle = true,
                            outline = member.isMe,
                            color = if (member.isMe) Palette.Lime else Palette.Soft,
                        )
                        Body(member.name, Modifier.weight(1f), weight = FontWeight.SemiBold)
                        if (member.owner) Caption(label(R.string.crew_owner))
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewRunRow(run: CrewRunItem, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = run.onClick).padding(
            horizontal = 16.dp,
            vertical = 14.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Body(run.name, weight = FontWeight.SemiBold, color = if (run.waiting) Palette.Ink else Palette.Secondary)
            if (run.subtitle.isNotBlank()) Caption(run.subtitle)
        }
        Badge(
            run.status,
            color = if (run.waiting) Palette.Lime else Palette.Soft,
        )
    }
}
