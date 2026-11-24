package com.sor999.dalmoi.presentation.session

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer
import com.sor999.dalmoi.presentation.common.theme.Palette

@DalmoiPreview
@Composable
private fun CreateSessionPreview() {
    DalmoiPreviewContainer {
        CreateSessionScreen(state = DalmoiUiState(), onSelect = {}, onBack = {}, onCreate = {})
    }
}

@DalmoiPreview
@Composable
private fun WaitingRoomPreview() {
    DalmoiPreviewContainer {
        WaitingRoomScreen(state = DalmoiUiState(), onBack = {}, onLaunch = {})
    }
}

@DalmoiPreview
@Composable
private fun RunningPreview() {
    RunningPreviewContent(DalmoiUiState())
}

@DalmoiPreview
@Composable
private fun RunningDisconnectedPreview() {
    RunningPreviewContent(DalmoiUiState(disconnected = true))
}

@Composable
private fun RunningPreviewContent(state: DalmoiUiState) {
    DalmoiPreviewContainer(backgroundColor = Palette.Running) {
        RunningScreen(state = state, onFinish = {})
    }
}
