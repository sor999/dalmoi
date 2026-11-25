package com.sor999.dalmoi.presentation.session

import androidx.compose.runtime.Composable
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.component.ActionButton
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.component.rememberPreciseLocationAction

@Composable
internal fun RunLocationStartButton(enabled: Boolean, onStart: () -> Unit) {
    val start = rememberPreciseLocationAction(onStart)
    ActionButton(label(R.string.api_start_me), start, enabled = enabled)
}
