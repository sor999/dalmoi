package com.sor999.dalmoi.presentation.auth

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun LoginPreview() {
    AuthPreviewContent(DalmoiUiState())
}

@DalmoiPreview
@Composable
private fun LoginErrorPreview() {
    AuthPreviewContent(DalmoiUiState(authError = true))
}

@DalmoiPreview
@Composable
private fun SignupPreview() {
    AuthPreviewContent(DalmoiUiState(signingUp = true, nickname = "민준"))
}

@Composable
private fun AuthPreviewContent(state: DalmoiUiState) {
    DalmoiPreviewContainer {
        AuthScreen(
            state = state,
            onSignup = {},
            onEmail = {},
            onPassword = {},
            onNickname = {},
            onContinue = {},
        )
    }
}
