package com.sor999.dalmoi.presentation.common.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R

object Palette {
    val Ink = Color(0xFF141414)
    val Lime = Color(0xFFC8F03A)
    val Background = Color(0xFFF7F6F2)
    val White = Color.White
    val Secondary = Color(0xFF6B6A66)
    val Muted = Color(0xFF8A8984)
    val Faint = Color(0xFFA9A8A2)
    val Border = Color(0xFFE2E0DA)
    val InputBorder = Color(0xFFDAD8D1)
    val InputLabel = Color(0xFF3A3936)
    val Divider = Color(0xFFEFEDE8)
    val Soft = Color(0xFFECEAE4)
    val Error = Color(0xFFD9472B)
    val Positive = Color(0xFF5C7A00)
    val Running = Color(0xFF121211)
    val RunningCard = Color(0xFF1C1C1A)
    val RunningBorder = Color(0xFF2A2A27)
    val Delay = Color(0xFFE8A13A)
}

object Spacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Card = 16.dp
    val Page = 20.dp
    val Large = 24.dp
    val Touch = 48.dp
}

object Corners {
    val Input = 12.dp
    val AuthTab = 9.dp
    val Button = 14.dp
    val Card = 18.dp
    val Panel = 24.dp
    val Dialog = 28.dp
}

const val DIALOG_SCRIM_ALPHA = 0.5f

object TextSizes {
    val DialogTitle = 20.sp
    val Label = 14.sp
    val Caption = 12.sp
}

val TextFont = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)
val NumberFont = FontFamily(
    Font(
        R.font.barlow_condensed_bold,
        FontWeight.Bold,
    ),
    Font(
        R.font.barlow_condensed_semibold,
        FontWeight.SemiBold,
    ),
)

@Composable
fun DalmoiTheme(content: @Composable () -> Unit) {
    val body = TextStyle(fontFamily = TextFont, fontSize = 14.sp, lineHeight = 18.sp)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Ink, onPrimary = Palette.Lime,
            secondary = Palette.Lime, onSecondary = Palette.Ink,
            background = Palette.Background, surface = Palette.White,
            onBackground = Palette.Ink, onSurface = Palette.Ink,
            onSurfaceVariant = Palette.Secondary, outline = Palette.InputBorder,
            error = Palette.Error,
        ),
        typography = Typography(
            bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 20.sp),
            bodyMedium = body,
            bodySmall = body.copy(fontSize = TextSizes.Caption, lineHeight = 16.sp),
            titleLarge = body.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            titleMedium = body.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
            labelLarge = body.copy(fontSize = TextSizes.Label, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
        ),
        content = content,
    )
}
