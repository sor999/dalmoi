package com.sor999.dalmoi.presentation.crew

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun CrewListPreview() {
    CrewListPreviewContent(DalmoiUiState())
}

@DalmoiPreview
@Composable
private fun CrewInviteErrorPreview() {
    CrewListPreviewContent(DalmoiUiState(inviteCode = "INVALID", inviteError = true))
}

@DalmoiPreview
@Composable
private fun CrewDetailPreview() {
    DalmoiPreviewContainer {
        CrewDetailScreen(
            state = DalmoiUiState(),
            onBack = {},
            onCreateRun = {},
            onWaiting = {},
            onRecord = {},
        )
    }
}

@Composable
private fun CrewListPreviewContent(state: DalmoiUiState) {
    DalmoiPreviewContainer {
        CrewListScreen(state = state, onCode = {}, onJoin = {}, onCrew = {}, onCreate = {})
    }
}
