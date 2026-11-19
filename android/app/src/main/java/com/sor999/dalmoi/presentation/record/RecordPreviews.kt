package com.sor999.dalmoi.presentation.record

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun RecordPreview() {
    RecordPreviewContent(DalmoiUiState())
}

@DalmoiPreview
@Composable
private fun RecordProcessingPreview() {
    RecordPreviewContent(DalmoiUiState(processingRecord = true))
}

@Composable
private fun RecordPreviewContent(state: DalmoiUiState) {
    DalmoiPreviewContainer {
        RecordScreen(state = state, onBack = {})
    }
}
