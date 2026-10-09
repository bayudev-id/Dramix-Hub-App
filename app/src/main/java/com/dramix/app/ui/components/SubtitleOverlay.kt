package com.dramix.app.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.dramix.app.R
import com.dramix.app.ui.screens.player_vod.SubtitleStyleConfig

@Composable
fun SubtitleOverlay(
    text: String,
    style: SubtitleStyleConfig,
    modifier: Modifier = Modifier
) {
    val cleanedText = remember(text) {
        text.replace(Regex("<[^>]*>"), "").trim()
    }

    if (cleanedText.isBlank()) return

    val rawLines = remember(cleanedText) {
        cleanedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    if (rawLines.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val fontSizePx = with(density) { style.fontSizePx.sp.toPx() }
    val typeface = remember(context, style.fontFamily) {
        resolveSubtitleTypeface(context, style.fontFamily)
    }

    val paint = remember(typeface, fontSizePx) {
        Paint().apply {
            isAntiAlias = true
            textSize = fontSizePx
            this.typeface = typeface
        }
    }

    val refBounds = remember(paint) {
        val r = Rect()
        paint.getTextBounds("Hg", 0, 2, r)
        r
    }

    val outlineStrokeWidthPx = when (style.outlineStyle.lowercase().trim()) {
        "none" -> 0f
        "thin" -> (fontSizePx * 0.08f).coerceIn(2.0f, 4.0f)
        "medium" -> (fontSizePx * 0.16f).coerceIn(3.5f, 7.0f)
        "thick" -> (fontSizePx * 0.25f).coerceIn(6.0f, 10.0f)
        else -> (fontSizePx * 0.08f).coerceIn(2.0f, 4.0f)
    }

    val hPadPx = with(density) { (style.backgroundPaddingPx * 0.5f).dp.toPx() }
    val vPadPx = with(density) { (style.backgroundPaddingPx * 0.22f).dp.toPx() }
    val cornerRadiusPx = with(density) { 4.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxAvailableWidthPx = with(density) { (maxWidth - 32.dp).toPx().coerceAtLeast(100f) }

        val subtitleLines = remember(rawLines, paint, maxAvailableWidthPx) {
            rawLines.flatMap { line ->
                wrapLineIfNeeded(line, paint, maxAvailableWidthPx)
            }
        }

        Column(
            modifier = Modifier
                .padding(
                    bottom = (style.positionPercent * 2.2f).dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(style.lineSpacingPx.dp)
        ) {
            subtitleLines.forEach { lineText ->
                SubtitleLinePill(
                    lineText = lineText,
                    paint = paint,
                    refBounds = refBounds,
                    style = style,
                    outlineStrokeWidthPx = outlineStrokeWidthPx,
                    hPadPx = hPadPx,
                    vPadPx = vPadPx,
                    cornerRadiusPx = cornerRadiusPx
                )
            }
        }
    }
}

@Composable
private fun SubtitleLinePill(
    lineText: String,
    paint: Paint,
    refBounds: Rect,
    style: SubtitleStyleConfig,
    outlineStrokeWidthPx: Float,
    hPadPx: Float,
    vPadPx: Float,
    cornerRadiusPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeHalf = outlineStrokeWidthPx / 2f

    val lineBounds = remember(lineText, paint) {
        val r = Rect()
        paint.getTextBounds(lineText, 0, lineText.length, r)
        r
    }

    val textWidth = remember(lineText, paint) { paint.measureText(lineText) }

    val inkLeftOffset = remember(lineBounds) {
        if (lineBounds.left < 0) -lineBounds.left.toFloat() else 0f
    }
    val inkRightExtra = remember(lineBounds, textWidth) {
        if (lineBounds.right > textWidth) (lineBounds.right - textWidth) else 0f
    }

    val effectiveTop = remember(refBounds, lineBounds) {
        minOf(refBounds.top.toFloat(), lineBounds.top.toFloat())
    }
    val effectiveBottom = remember(refBounds, lineBounds) {
        maxOf(refBounds.bottom.toFloat(), lineBounds.bottom.toFloat())
    }
    val refHeight = remember(effectiveTop, effectiveBottom) {
        effectiveBottom - effectiveTop
    }
    val opticalCenterY = remember(effectiveTop, effectiveBottom) {
        (effectiveTop + effectiveBottom) / 2f
    }

    val pillWidthPx = textWidth + (2 * hPadPx) + (2 * strokeHalf) + inkLeftOffset + inkRightExtra
    val pillHeightPx = refHeight + (2 * vPadPx) + (2 * strokeHalf)
    val baselineY = (pillHeightPx / 2f) - opticalCenterY
    val startX = hPadPx + strokeHalf + inkLeftOffset

    val pillWidthDp = with(density) { pillWidthPx.toDp() }
    val pillHeightDp = with(density) { pillHeightPx.toDp() }

    Canvas(
        modifier = modifier.size(width = pillWidthDp, height = pillHeightDp)
    ) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Draw rounded rectangle background pill
            if (style.backgroundColor != 0L) {
                val bgPaint = Paint().apply {
                    isAntiAlias = true
                    this.style = Paint.Style.FILL
                    color = style.backgroundColor.toInt()
                }
                val rect = RectF(0f, 0f, pillWidthPx, pillHeightPx)
                nativeCanvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)
            }

            // 2. Stroke outline
            if (outlineStrokeWidthPx > 0f) {
                val strokePaint = Paint(paint).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = outlineStrokeWidthPx
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    color = android.graphics.Color.BLACK
                }
                nativeCanvas.drawText(lineText, startX, baselineY, strokePaint)
            }

            // 3. Fill text
            val fillPaint = Paint(paint).apply {
                this.style = Paint.Style.FILL
                color = style.textColor.toInt()
            }
            nativeCanvas.drawText(lineText, startX, baselineY, fillPaint)
        }
    }
}

private fun wrapLineIfNeeded(
    text: String,
    paint: Paint,
    maxWidthPx: Float
): List<String> {
    if (paint.measureText(text) <= maxWidthPx) {
        return listOf(text)
    }
    val words = text.split(" ")
    if (words.size <= 1) return listOf(text)

    val lines = mutableListOf<String>()
    var current = StringBuilder()

    for (word in words) {
        val test = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(test) <= maxWidthPx) {
            current = StringBuilder(test)
        } else {
            if (current.isNotEmpty()) {
                lines.add(current.toString())
            }
            current = StringBuilder(word)
        }
    }
    if (current.isNotEmpty()) {
        lines.add(current.toString())
    }
    return lines
}

fun resolveSubtitleTypeface(context: Context, fontFamily: String): Typeface {
    val lower = fontFamily.lowercase().trim()
    return when {
        lower.contains("comic") -> try {
            ResourcesCompat.getFont(context, R.font.comic_bold)
                ?: Typeface.create("cursive", Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create("cursive", Typeface.BOLD)
        }
        lower.contains("georgia") || lower.contains("serif") -> {
            Typeface.SERIF
        }
        lower.contains("courier") || lower.contains("monospace") -> {
            Typeface.MONOSPACE
        }
        lower.contains("verdana") -> try {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.SANS_SERIF
        }
        lower.contains("gemuk") || lower.contains("impact") -> try {
            Typeface.create("sans-serif-black", Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        else -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
}
