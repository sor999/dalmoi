package com.sor999.dalmoi.presentation.auth

import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.LabeledInput
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.Corners
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
    errorMessage: String? = null,
) {
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(modifier.fillMaxSize().imePadding()) {
        val minimumHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = minimumHeight)
                .padding(start = Spacing.Large, end = Spacing.Large, top = 40.dp, bottom = Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(44.dp).background(
                        Palette.Ink,
                        RoundedCornerShape(Corners.Input),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(18.dp).background(Palette.Lime, CircleShape))
                }
                Body(
                    label(R.string.auth_headline),
                    Modifier.padding(top = Spacing.Medium),
                    size = 28.sp,
                    weight = FontWeight.Bold,
                    lineHeight = 35.sp,
                    letterSpacing = (-0.56).sp,
                )
            }
            Row(
                Modifier.fillMaxWidth()
                    .background(Palette.Soft, RoundedCornerShape(Corners.Input))
                    .padding(Spacing.Tiny),
            ) {
                val tabShape = RoundedCornerShape(Corners.AuthTab)
                listOf(R.string.auth_login, R.string.auth_signup).forEachIndexed { index, id ->
                    val selected = state.signingUp == (index == 1)
                    Box(
                        Modifier.weight(1f)
                            .shadow(if (selected) 2.dp else 0.dp, tabShape)
                            .clip(tabShape)
                            .background(if (selected) Palette.White else Palette.Soft)
                            .semantics { this.selected = selected }
                            .clickable(enabled = !busy, role = Role.Tab) { onSignup(index == 1) }.padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Body(
                            label(id),
                            weight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Palette.Ink else Palette.Secondary,
                            lineHeight = 17.sp,
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
                    labelColor = Palette.InputLabel,
                    inputTextStyle = TextStyle(fontSize = 14.4.sp),
                )
                LabeledInput(
                    label(R.string.auth_password),
                    state.password,
                    onPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    transformation = PasswordVisualTransformation(),
                    labelColor = Palette.InputLabel,
                    inputTextStyle = TextStyle(fontSize = 18.sp, letterSpacing = 3.6.sp),
                )
                if (state.signingUp) {
                    LabeledInput(
                        label(R.string.auth_nickname),
                        state.nickname,
                        onNickname,
                        labelColor = Palette.InputLabel,
                    )
                }
                val error = errorMessage ?: if (state.authError) label(R.string.auth_error) else null
                error?.let {
                    Row(
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painterResource(R.drawable.ic_auth_error),
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.Card),
                        )
                        Body(it, size = 13.sp, weight = FontWeight.Medium, color = Palette.Error, lineHeight = 16.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f).heightIn(min = Spacing.Small))
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    ActionButton(
                        label(if (state.signingUp) R.string.auth_signup else R.string.auth_login),
                        {
                            focusManager.clearFocus()
                            onContinue()
                        },
                        modifier = Modifier.testTag("auth-submit"),
                        enabled = !busy,
                    )
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = Spacing.Card).size(Spacing.Card).testTag("api-loading"),
                            color = Palette.Ink,
                            trackColor = Palette.Border,
                            strokeWidth = 2.dp,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Tiny),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Body(label(R.string.auth_first), size = 13.sp, color = Palette.Secondary, lineHeight = 16.sp)
                    Body(
                        label(R.string.auth_signup),
                        Modifier.clickable(enabled = !busy, role = Role.Button) { onSignup(true) },
                        size = 13.sp,
                        weight = FontWeight.SemiBold,
                        lineHeight = 16.sp,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        }
    }
}
