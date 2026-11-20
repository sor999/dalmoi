package com.sor999.dalmoi.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.common.theme.Spacing

@Composable
fun AuthScreen(
    state: DalmoiUiState,
    onSignup: (Boolean) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onNickname: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    BoxWithConstraints(modifier.fillMaxSize().imePadding()) {
        val minimumHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minimumHeight)
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(44.dp).background(
                        Palette.Ink,
                        RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(18.dp).background(Palette.Lime, CircleShape))
                }
                Body(
                    label(R.string.auth_headline),
                    Modifier.padding(top = 12.dp),
                    size = 28.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 35.sp,
                )
            }
            Row(Modifier.fillMaxWidth().background(Palette.Soft, RoundedCornerShape(12.dp)).padding(4.dp)) {
                listOf(R.string.auth_login, R.string.auth_signup).forEachIndexed { index, id ->
                    val selected = state.signingUp == (index == 1)
                    Box(
                        Modifier.weight(1f).heightIn(min = 40.dp)
                            .background(
                                if (selected) Palette.White else Palette.Soft,
                                RoundedCornerShape(9.dp),
                            )
                            .semantics { this.selected = selected }
                            .clickable(enabled = !busy, role = Role.Tab) { onSignup(index == 1) }.padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Body(
                            label(id),
                            weight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Palette.Ink else Palette.Secondary,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledInput(
                    label(R.string.auth_email),
                    state.email,
                    onEmail,
                    strong = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                LabeledInput(
                    label(R.string.auth_password),
                    state.password,
                    onPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    transformation = PasswordVisualTransformation(),
                    placeholder = "••••••••",
                )
                if (state.signingUp) LabeledInput(label(R.string.auth_nickname), state.nickname, onNickname)
                if (state.authError) Body(label(R.string.auth_error), size = 13.sp, color = Palette.Error)
            }
            Spacer(Modifier.weight(1f).heightIn(min = Spacing.Small))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ActionButton(
                    label(if (state.signingUp) R.string.auth_signup else R.string.auth_login),
                    onContinue,
                    modifier = Modifier.testTag("auth-submit"),
                    enabled = !busy,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Body(label(R.string.auth_first), size = 13.sp, color = Palette.Secondary)
                    Body(
                        label(R.string.auth_signup),
                        Modifier.clickable(enabled = !busy) { onSignup(true) }.padding(
                            horizontal = 6.dp,
                            vertical = 8.dp,
                        ),

                        size = 13.sp,
                        weight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
