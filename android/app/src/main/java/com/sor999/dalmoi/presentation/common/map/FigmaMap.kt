package com.sor999.dalmoi.presentation.common.map

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.caverock.androidsvg.SVG
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.theme.Palette

data class MapPoint(val x: Float, val y: Float)

@Composable
fun FigmaMap(
    artwork: MapArtwork,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    editedRoute: List<MapPoint>? = null,
    onMapTap: ((MapPoint) -> Unit)? = null,
    cameraResetKey: Int = 0,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(cameraResetKey) { zoom = 1f }
    val context = LocalContext.current
    val spec = remember(artwork) { artworkSpec(artwork) }
    val pictures = remember(artwork) {
        spec.slots.map { slot -> SVG.getFromAsset(context.assets, "figma/${slot.file}").renderToPicture() }
    }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = context.resources.getFont(R.font.pretendard_bold)
        }
    }
    val description = stringResource(R.string.map_description)
    val addPoint = stringResource(R.string.map_add_point)
    val inputModifier = if (onMapTap == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(artwork, onMapTap) {
                detectTapGestures(onDoubleTap = { zoom = if (zoom == 1f) 1.35f else 1f }, onTap = { point ->
                    val factor = size.width / spec.width * zoom
                    onMapTap(MapPoint(point.x / factor, point.y / factor + if (fullScreen) 32f else 0f))
                })
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(addPoint) {
                        onMapTap(MapPoint(spec.width / 2, spec.height / 3))
                        true
                    },
                )
            }
    }
    Canvas(modifier.clipToBounds().then(inputModifier).semantics { contentDescription = description }) {
        val factor = size.width / spec.width * zoom
        scale(factor, factor, pivot = Offset.Zero) {
            translate(top = if (fullScreen) -32f else 0f) {
                drawMapBackground(artwork, spec.width, spec.height)
                if (editedRoute == null) {
                    spec.slots.forEachIndexed { index, slot ->
                        drawContext.canvas.nativeCanvas.drawPicture(
                            pictures[index],
                            RectF(slot.x, slot.y, slot.x + slot.width, slot.y + slot.height),
                        )
                    }
                    spec.labels.forEach { label ->
                        paint.textSize = label.size
                        paint.color = label.color.toInt()
                        drawContext.canvas.nativeCanvas.drawText(
                            label.text,
                            label.x + label.width / 2,
                            label.y + label.height / 2 - (paint.ascent() + paint.descent()) / 2,
                            paint,
                        )
                    }
                } else {
                    drawEditedRoute(editedRoute, paint)
                }
                if (artwork == MapArtwork.MvpRunning) drawMvpPeers(paint)
            }
        }
    }
}

private fun DrawScope.drawMapBackground(kind: MapArtwork, width: Float, height: Float) {
    val dark = kind == MapArtwork.Running || kind == MapArtwork.RunningError
    if (kind in listOf(MapArtwork.ThumbLoop, MapArtwork.ThumbReturn, MapArtwork.ThumbIntervals)) {
        drawRect(Palette.Soft, size = Size(width, height))
        return
    }
    val road = if (dark) Palette.RunningBorder else Color.White
    drawRect(if (dark) Palette.RunningCard else Color(0xFFECEAE3), size = Size(width, height))
    drawRect(road, Offset(width * .47917f, 0f), Size(width * .0625f, height))
    drawRect(road, Offset(0f, height * .41071f), Size(width, height * .05358f))
    fun water(x: Float, y: Float, w: Float, h: Float, angle: Float) {
        rotate(angle, Offset(x + w / 2, y + h / 2)) {
            drawRect(if (dark) Color(0xFF18232A) else Color(0xFFCBDCE5), Offset(x, y), Size(w, h))
        }
    }
    when (kind) {
        MapArtwork.Editor, MapArtwork.EditorError -> {
            drawRoundRect(Color(0xFFD5E3C3), Offset(170f, 190f), Size(150f, 120f), CornerRadius(30f))
            water(-40f, 470f, 460f, 54f, -8f)
            rotate(26f, Offset(190f, 285f)) { drawRect(road, Offset(-60f, 280f), Size(500f, 10f)) }
        }

        MapArtwork.Course -> {
            water(-40f, 200f, 420f, 46f, -8f)
            drawRoundRect(Color(0xFFD5E3C3), Offset(140f, 40f), Size(130f, 90f), CornerRadius(26f))
        }

        MapArtwork.Waiting -> water(-30f, 110f, 400f, 40f, -6f)

        MapArtwork.Running, MapArtwork.RunningError -> {
            water(-40f, 360f, 460f, 54f, -8f)
            drawRoundRect(Color(0xFF1F2A1C), Offset(170f, 110f), Size(140f, 100f), CornerRadius(28f))
        }

        MapArtwork.Record -> water(-30f, 160f, 400f, 44f, -6f)

        MapArtwork.MvpRecord -> water(-30f, 210f, 400f, 60f, -8f)

        MapArtwork.MvpEditor, MapArtwork.MvpRunning -> {
            val running = kind == MapArtwork.MvpRunning
            val parkY = if (running) 440f else 470f
            rotate(-10f, Offset(190f, parkY + 28f)) {
                drawRoundRect(Color(0xFFD5E3C3), Offset(-40f, parkY), Size(460f, 56f), CornerRadius(20f))
            }
            water(-60f, if (running) 510f else 540f, 520f, 130f, -10f)
            rotate(24f, Offset(200f, 255f)) { drawRect(road, Offset(-60f, 250f), Size(520f, 10f)) }
        }

        else -> Unit
    }
}

private fun DrawScope.drawEditedRoute(points: List<MapPoint>, paint: Paint) {
    val path = Path()
    points.forEachIndexed { index, point ->
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(path, Palette.Ink, style = Stroke(4f, cap = StrokeCap.Round))
    points.forEachIndexed { index, point ->
        val center = Offset(point.x, point.y)
        val endpoint = index == 0 || index == points.lastIndex
        if (!endpoint) {
            drawCircle(Palette.Ink, 4f, center)
        } else {
            if (index == points.lastIndex && index > 0) {
                drawCircle(
                    Palette.Ink,
                    16f,
                    center,
                    style = Stroke(
                        2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)),
                    ),
                )
            }
            drawCircle(if (index == 0) Palette.Lime else Palette.Ink, if (index == 0) 14f else 12f, center)
            if (index == 0) drawCircle(Palette.Ink, 14f, center, style = Stroke(2.5f))
            paint.color = if (index == 0) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            paint.textSize = 12f
            drawContext.canvas.nativeCanvas.drawText(
                if (index == 0) "S" else "E",
                point.x,
                point.y - (paint.ascent() + paint.descent()) / 2,
                paint,
            )
        }
    }
}

private fun DrawScope.drawMvpPeers(paint: Paint) {
    listOf(
        Triple(MapPoint(196f, 177f), Color(0xFFE8590C), "민"),
        Triple(MapPoint(262f, 202f), Color(0xFFC2255C), "지"),
        Triple(MapPoint(78f, 330f), Color(0xFF7048E8), "태"),
    ).forEach { (point, color, text) ->
        drawCircle(color, 16f, Offset(point.x, point.y))
        drawCircle(Color.White, 16f, Offset(point.x, point.y), style = Stroke(3f))
        paint.textSize = 13f
        paint.color = android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(
            text,
            point.x,
            point.y - (paint.ascent() + paint.descent()) / 2,
            paint,
        )
    }
    drawCircle(Color(0x2E2F6BFF), 22f, Offset(134f, 206f))
    drawCircle(Color(0xFF2F6BFF), 10f, Offset(134f, 206f))
    drawCircle(Color.White, 10f, Offset(134f, 206f), style = Stroke(3f))
}
