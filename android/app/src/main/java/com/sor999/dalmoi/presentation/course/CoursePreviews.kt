package com.sor999.dalmoi.presentation.course

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.app.demo.DalmoiUiState
import com.sor999.dalmoi.app.demo.DraftError
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun CourseListPreview() {
    DalmoiPreviewContainer {
        CourseListScreen(state = DalmoiUiState(), onEditor = {}, onCourse = {})
    }
}

@DalmoiPreview
@Composable
private fun CourseDetailPreview() {
    DalmoiPreviewContainer {
        CourseDetailScreen(course = DalmoiUiState().course, onBack = {}, onCreateRun = {})
    }
}

@DalmoiPreview
@Composable
private fun CourseEditorPreview() {
    CourseEditorPreviewContent(DalmoiUiState())
}

@DalmoiPreview
@Composable
private fun CourseEditorErrorPreview() {
    CourseEditorPreviewContent(DalmoiUiState(draftError = DraftError.SaveFailed))
}

@Composable
private fun CourseEditorPreviewContent(state: DalmoiUiState) {
    DalmoiPreviewContainer {
        CourseEditorScreen(
            state = state,
            onPoint = {},
            onUndo = {},
            onReset = {},
            onName = {},
            onSave = {},
            onBack = {},
        )
    }
}
