package com.marcopt74.damelume.ui.theme

import com.marcopt74.damelume.domain.model.Mode
import kotlin.math.pow

object LumePalette {
    const val BG = 0xFFFFFFFF.toInt()
    const val SURFACE = 0xFFF5F4F1.toInt()
    const val FG = 0xFF1C1B19.toInt()
    const val MUTED = 0xFF6D6962.toInt()
    const val BORDER = 0xFFE7E4DE.toInt()
    const val DANGER = 0xFF9A4036.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    const val WORK_ACCENT = 0xFF2F6DF6.toInt()
    const val WORK_TEXT = 0xFF2558CC.toInt()
    const val WORK_SOFT = 0xFFE8F0FF.toInt()

    const val BREAK_ACCENT = 0xFFEF6C1C.toInt()
    const val BREAK_GRAPHIC = 0xFFE5631A.toInt()
    const val BREAK_TEXT = 0xFFC2410C.toInt()
    const val BREAK_SOFT = 0xFFFFF1E6.toInt()

    const val LONG_ACCENT = 0xFF3E584A.toInt()
    const val LONG_SOFT = 0xFFE9F0EC.toInt()

    fun graphic(mode: Mode): Int =
        when (mode) {
            Mode.WORK -> WORK_ACCENT
            Mode.BREAK -> BREAK_GRAPHIC
            Mode.LONG_BREAK -> LONG_ACCENT
        }

    fun text(mode: Mode): Int =
        when (mode) {
            Mode.WORK -> WORK_TEXT
            Mode.BREAK -> BREAK_TEXT
            Mode.LONG_BREAK -> LONG_ACCENT
        }

    fun button(mode: Mode): Int =
        when (mode) {
            Mode.WORK -> WORK_ACCENT
            Mode.BREAK -> BREAK_TEXT
            Mode.LONG_BREAK -> LONG_ACCENT
        }

    fun soft(mode: Mode): Int =
        when (mode) {
            Mode.WORK -> WORK_SOFT
            Mode.BREAK -> BREAK_SOFT
            Mode.LONG_BREAK -> LONG_SOFT
        }

    fun runningBackground(mode: Mode): Int = mixSrgb(soft(mode), WHITE, 0.70f)

    fun mixSrgb(
        foreground: Int,
        background: Int,
        foregroundWeight: Float,
    ): Int {
        val weight = foregroundWeight.coerceIn(0f, 1f)
        val inverse = 1f - weight
        val red = ((red(foreground) * weight) + (red(background) * inverse)).toInt()
        val green = ((green(foreground) * weight) + (green(background) * inverse)).toInt()
        val blue = ((blue(foreground) * weight) + (blue(background) * inverse)).toInt()
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    fun contrastRatio(
        first: Int,
        second: Int,
    ): Double {
        val lighter = maxOf(relativeLuminance(first), relativeLuminance(second))
        val darker = minOf(relativeLuminance(first), relativeLuminance(second))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(component: Int): Double {
            val s = component / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(color)) + 0.7152 * channel(green(color)) + 0.0722 * channel(blue(color))
    }

    private fun red(color: Int): Int = (color shr 16) and 0xFF

    private fun green(color: Int): Int = (color shr 8) and 0xFF

    private fun blue(color: Int): Int = color and 0xFF
}
