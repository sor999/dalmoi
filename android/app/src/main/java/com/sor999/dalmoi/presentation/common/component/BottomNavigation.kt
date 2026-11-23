package com.sor999.dalmoi.presentation.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.common.theme.Spacing

internal data class NavigationTab<T>(val screen: T, @param:StringRes val title: Int, val testTag: String? = null)

@Composable
internal fun <T> BottomNavigation(
    tabs: List<NavigationTab<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(color = Palette.White) {
        Column(modifier) {
            HorizontalDivider(color = Palette.Border)
            Row(
                Modifier.fillMaxWidth().selectableGroup()
                    .padding(horizontal = Spacing.Small, vertical = Spacing.Tiny),
            ) {
                tabs.forEach { tab ->
                    val active = tab.screen == selected
                    Column(
                        Modifier.weight(1f).heightIn(min = Spacing.Touch)
                            .then(tab.testTag?.let { Modifier.testTag(it) } ?: Modifier)
                            .selectable(active, enabled, Role.Tab) { onSelect(tab.screen) }
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.Tiny),
                    ) {
                        Box(
                            Modifier.width(22.dp).height(Spacing.Tiny)
                                .background(if (active) Palette.Ink else Color.Transparent, RoundedCornerShape(2.dp)),
                        )
                        Body(
                            label(tab.title),
                            size = 11.sp,
                            weight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Palette.Ink else Palette.Muted,
                        )
                    }
                }
            }
        }
    }
}
