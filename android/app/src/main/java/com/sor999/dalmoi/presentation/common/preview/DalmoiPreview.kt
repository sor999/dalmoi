package com.sor999.dalmoi.presentation.common.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sor999.dalmoi.presentation.common.theme.DalmoiTheme
import com.sor999.dalmoi.presentation.common.theme.Palette

@Preview(name = "기본", widthDp = 360, heightDp = 800, locale = "ko", showBackground = true)
@Preview(name = "큰 글씨", widthDp = 360, heightDp = 800, locale = "ko", fontScale = 1.3f, showBackground = true)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class DalmoiPreview

@Composable
fun DalmoiPreviewContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Palette.Background,
    content: @Composable () -> Unit,
) {
    DalmoiTheme {
        Surface(modifier = modifier, color = backgroundColor, content = content)
    }
}
