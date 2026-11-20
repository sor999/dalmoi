package com.sor999.dalmoi.presentation.common.component

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.theme.Corners
import com.sor999.dalmoi.presentation.common.theme.NumberFont
import com.sor999.dalmoi.presentation.common.theme.Palette
import com.sor999.dalmoi.presentation.common.theme.Spacing
import com.sor999.dalmoi.presentation.common.theme.TextFont

@Composable
fun label(@StringRes id: Int): String = stringResource(id)

@Composable
fun Body(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = LocalContentColor.current,
    lineHeight: TextUnit = size * 1.25f,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
) {
    Text(
        text,
        modifier,
        color = color,
        style = TextStyle(
            fontFamily = TextFont,
            fontSize = size,
            fontWeight = weight,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Palette.Secondary) {
    Body(text, modifier, size = 12.sp, color = color, lineHeight = 16.sp)
}

@Composable
fun Metric(
    value: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 34.sp,
    unit: String = "",
    unitSize: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Bold,
    color: Color = Palette.Ink,
    unitColor: Color = Palette.Secondary,
) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value,
            Modifier.alignByBaseline(),
            color = color,
            style = TextStyle(
                fontFamily = NumberFont,
                fontWeight = weight,
                fontSize = size,
                lineHeight = size,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
        if (unit.isNotEmpty()) {
            Text(
                unit,
                Modifier.alignByBaseline().padding(start = 2.dp),
                color = unitColor,
                style = TextStyle(
                    fontFamily = NumberFont,
                    fontSize = unitSize,
                    lineHeight = unitSize,
                ),
            )
        }
    }
}

@Composable
fun ActionButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    color: Color = Palette.Ink,
    foreground: Color = Palette.Lime,
    outline: Color? = null,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    radius: Dp = Corners.Button,
    fillWidth: Boolean = true,
    horizontalPadding: Dp = 12.dp,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier).heightIn(min = height)
            .background(if (enabled) color else Palette.Soft, shape)
            .then(if (outline != null) Modifier.border(1.5.dp, outline, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = if (height < 48.dp) 9.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Body(
            title,
            size = fontSize,
            weight = FontWeight.Bold,
            color = if (enabled) foreground else Palette.Muted,
        )
    }
}

@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    radius: Dp = Corners.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
            } else {
                Modifier
            },
        ),

        shape = RoundedCornerShape(radius),
        border = BorderStroke(
            1.dp,
            Palette.Border,
        ),
        color = Palette.White,

    ) {
        Column(content = content)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, detail: String = "") {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Body(title, size = 15.sp, weight = FontWeight.Bold)
        Body(detail, size = 13.sp, color = Palette.Secondary)
    }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier, color: Color = Palette.Lime, foreground: Color = Palette.Ink) {
    Box(modifier.background(color, RoundedCornerShape(99.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Body(text, size = 11.sp, weight = FontWeight.SemiBold, color = foreground)
    }
}

@Composable
fun Avatar(
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = Palette.Soft,
    foreground: Color = Palette.Ink,
    circle: Boolean = false,
    outline: Boolean = false,
) {
    val shape = if (circle) CircleShape else RoundedCornerShape(10.dp)
    Box(
        modifier.size(size).background(color, shape)
            .then(
                if (outline) {
                    Modifier.border(
                        2.dp,
                        Palette.Ink,
                        shape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Body(initial, size = 13.sp, weight = FontWeight.Bold, color = foreground)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val backLabel = label(R.string.course_back)
    Row(modifier.fillMaxWidth().padding(end = Spacing.Card), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Body("‹", size = 26.sp)
        }
        Body(title, Modifier.weight(1f), size = 16.sp, weight = FontWeight.Bold)
        trailing()
    }
}

@Composable
fun LabeledInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    transformation: VisualTransformation = VisualTransformation.None,
    numeric: Boolean = false,
    placeholder: String = "",
    showLabel: Boolean = true,
    labelColor: Color = LocalContentColor.current,
    inputTextStyle: TextStyle = TextStyle.Default,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showLabel && title.isNotEmpty()) Body(title, size = 13.sp, weight = FontWeight.SemiBold, color = labelColor)
        BasicTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                .onFocusChanged { focused = it.isFocused }
                .background(Palette.White, RoundedCornerShape(Corners.Input))
                .border(
                    1.dp,
                    if (focused || strong) Palette.Ink else Palette.InputBorder,
                    RoundedCornerShape(Corners.Input),
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .semantics { contentDescription = title },
            textStyle = TextStyle(
                fontFamily = if (numeric) NumberFont else TextFont,
                fontSize = if (numeric) 22.sp else 15.sp,
                letterSpacing = if (numeric) 2.64.sp else 0.sp,
                color = Palette.Ink,
            ).merge(inputTextStyle),
            singleLine = true, cursorBrush = SolidColor(Palette.Ink),
            keyboardOptions = keyboardOptions, visualTransformation = transformation,
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Body(placeholder, color = Palette.Muted)
                    field()
                }
            },
        )
    }
}
