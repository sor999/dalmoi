package com.sor999.dalmoi.presentation.home

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun HomePreview() {
    DalmoiPreviewContainer {
        HomeScreen(
            state = DalmoiUiState(),
            onEditor = {},
            onCrews = {},
            onCrew = {},
            onWaiting = {},
            onCourse = {},
        )
    }
}
