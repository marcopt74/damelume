package com.marcopt74.damelume.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.marcopt74.damelume.ui.theme.EaseSmoothOut
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val BOX = 240f
internal const val DIAL_CENTER = 120f
private const val RING = 86f
private const val TRAIL_DEGREES = 58f

internal data class DialTick(
    val major: Boolean,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

internal object DialGeometry {
    val ticks: List<DialTick> =
        List(60) { index ->
            val major = index % 5 == 0
            val angle = index / 60.0 * (PI * 2) - PI / 2
            val inner = if (major) 100.0 else 107.5
            val outer = 114.0
            DialTick(
                major = major,
                x1 = (DIAL_CENTER + kotlin.math.cos(angle) * inner).toFloat(),
                y1 = (DIAL_CENTER + kotlin.math.sin(angle) * inner).toFloat(),
                x2 = (DIAL_CENTER + kotlin.math.cos(angle) * outer).toFloat(),
                y2 = (DIAL_CENTER + kotlin.math.sin(angle) * outer).toFloat(),
            )
        }

    val ringStroke = Stroke(width = 5.5f, cap = StrokeCap.Round)
    val trackStroke = Stroke(width = 1f)
    val innerStroke = Stroke(width = 0.6f)
    val trailStroke = Stroke(width = 8f, cap = StrokeCap.Round)
}

@Composable
fun Dial(
    remainingMs: Long,
    totalMs: Long,
    running: Boolean,
    reducedMotion: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fraction =
        if (totalMs <= 0L) {
            0f
        } else {
            (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        }
    val animated by animateFloatAsState(
        targetValue = fraction,
        animationSpec =
            if (running || reducedMotion) {
                snap()
            } else {
                tween(durationMillis = 400, easing = EaseSmoothOut)
            },
        label = "dialRing",
    )
    val shown = if (running) fraction else animated
    Canvas(modifier = modifier) {
        val scale = size.minDimension / BOX
        withTransform({
            translate((size.width - BOX * scale) / 2f, (size.height - BOX * scale) / 2f)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawDialGeometry(
                remainingFraction = shown,
                remainingMs = remainingMs,
                totalMs = totalMs,
                running = running,
                accent = accent,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDialGeometry(
    remainingFraction: Float,
    remainingMs: Long,
    totalMs: Long,
    running: Boolean,
    accent: Color,
) {
    val fg = Color(0xFF1C1B19)
    val bg = Color.White
    val elapsed = (1f - remainingFraction).coerceIn(0f, 1f)
    val total = totalMs.coerceAtLeast(1L)
    val safeRemaining = remainingMs.coerceIn(0L, total)
    val secondTurn = (((total - safeRemaining) % 60_000L).toFloat() / 60_000f)
    val handTurns = secondTurn * 60f
    drawTicks(elapsed, running, handTurns, secondTurn, accent, fg)
    drawRingsAndHand(elapsed, remainingFraction, secondTurn, running, accent, fg, bg)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTicks(
    elapsed: Float,
    running: Boolean,
    handTurns: Float,
    secondTurn: Float,
    accent: Color,
    fg: Color,
) {
    DialGeometry.ticks.forEachIndexed { index, tick ->
        val midX = (tick.x1 + tick.x2) / 2f
        val midY = (tick.y1 + tick.y2) / 2f
        val magnify = if (running) tickMagnify(circularDistance(index.toFloat(), handTurns)) else 0f
        val scale = 1f + magnify * (if (tick.major) 0.42f else 1.15f)
        val behind = running && positiveDelta(secondTurn, index / 60f) in 0f..(TRAIL_DEGREES / 360f)
        val color =
            when {
                magnify > 0.08f || behind -> accent
                index / 60f < elapsed -> fg.copy(alpha = 0.15f)
                tick.major -> fg.copy(alpha = 0.45f)
                else -> fg.copy(alpha = 0.20f)
            }
        drawLine(
            color = color,
            start = Offset(midX + (tick.x1 - midX) * scale, midY + (tick.y1 - midY) * scale),
            end = Offset(midX + (tick.x2 - midX) * scale, midY + (tick.y2 - midY) * scale),
            strokeWidth = (if (tick.major) 1.5f else 0.9f) + magnify * 2.1f,
            cap = StrokeCap.Round,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRingsAndHand(
    elapsed: Float,
    remainingFraction: Float,
    secondTurn: Float,
    running: Boolean,
    accent: Color,
    fg: Color,
    bg: Color,
) {
    drawCircle(
        color = fg.copy(alpha = 0.12f),
        radius = 70f,
        center = Offset(DIAL_CENTER, DIAL_CENTER),
        style = DialGeometry.innerStroke,
    )
    drawCircle(
        color = fg.copy(alpha = 0.12f),
        radius = RING,
        center = Offset(DIAL_CENTER, DIAL_CENTER),
        style = DialGeometry.trackStroke,
    )
    val arcSize =
        androidx.compose.ui.geometry
            .Size(RING * 2, RING * 2)
    val arcTopLeft = Offset(DIAL_CENTER - RING, DIAL_CENTER - RING)
    if (remainingFraction > 0f) {
        drawArc(
            color = accent,
            startAngle = -90f + 360f * elapsed,
            sweepAngle = 360f * remainingFraction,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = DialGeometry.ringStroke,
        )
    }
    val handAngle = -PI / 2 + secondTurn * PI * 2
    if (running) {
        drawArc(
            color = accent.copy(alpha = 0.28f),
            startAngle = -90f + secondTurn * 360f - TRAIL_DEGREES,
            sweepAngle = TRAIL_DEGREES,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = DialGeometry.trailStroke,
        )
    }
    drawLine(
        color = accent.copy(alpha = if (running) 1f else 0.45f),
        start = Offset(DIAL_CENTER + cos(handAngle).toFloat() * 72f, DIAL_CENTER + sin(handAngle).toFloat() * 72f),
        end = Offset(DIAL_CENTER + cos(handAngle).toFloat() * 116f, DIAL_CENTER + sin(handAngle).toFloat() * 116f),
        strokeWidth = if (running) 2.6f else 1.6f,
        cap = StrokeCap.Round,
    )
    val knob = Offset(DIAL_CENTER + cos(handAngle).toFloat() * RING, DIAL_CENTER + sin(handAngle).toFloat() * RING)
    drawCircle(color = accent, radius = if (running) 8f else 4.5f, center = knob)
    drawCircle(color = bg, radius = if (running) 4.2f else 2.4f, center = knob)
    drawCircle(color = accent, radius = if (running) 2.1f else 1.2f, center = knob)
}

private fun tickMagnify(distance: Float): Float {
    if (distance >= 4.6f) return 0f
    val t = distance / 4.6f
    val cosValue = cos(t * PI * 0.5)
    return (cosValue * cosValue).toFloat()
}

private fun circularDistance(
    a: Float,
    b: Float,
): Float {
    val delta = abs(a - b)
    return min(delta, 60f - delta)
}

private fun positiveDelta(
    hand: Float,
    tick: Float,
): Float {
    var delta = hand - tick
    if (delta < 0f) delta += 1f
    return delta
}
