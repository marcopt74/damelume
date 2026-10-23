package com.marcopt74.damelume.ui.format

import kotlin.math.ceil

object TimeFormatter {
    fun format(remainingMs: Long): String {
        val safe = remainingMs.coerceAtLeast(0L)
        val totalSeconds = ceil(safe / 1000.0).toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
