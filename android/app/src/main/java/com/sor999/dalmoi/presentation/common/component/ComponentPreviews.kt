package com.sor999.dalmoi.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreview
import com.sor999.dalmoi.presentation.common.preview.DalmoiPreviewContainer

@DalmoiPreview
@Composable
private fun FormComponentsPreview() {
    DalmoiPreviewContainer {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledInput(title = label(R.string.course_name), value = "반포 한강 루프", onValueChange = {})
            ActionButton(title = label(R.string.course_save), onClick = {})
            ActionButton(title = label(R.string.course_save), onClick = {}, enabled = false)
        }
    }
}
