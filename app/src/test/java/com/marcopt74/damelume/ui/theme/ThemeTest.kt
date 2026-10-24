package com.marcopt74.damelume.ui.theme

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Test

class ThemeTest {
    @Test
    fun breakModeUsesSiteOrange() {
        assertThat(LumePalette.accent(Mode.BREAK)).isEqualTo(0xFFEF6C1C.toInt())
        assertThat(LumePalette.soft(Mode.BREAK)).isEqualTo(LumePalette.BREAK_SOFT)
    }

    @Test
    fun modeAccents_workAndBreakShareOneAccent() {
        assertThat(LumePalette.accent(Mode.WORK)).isEqualTo(0xFF2F6DF6.toInt())
        assertThat(LumePalette.accent(Mode.BREAK)).isEqualTo(0xFFEF6C1C.toInt())
        assertThat(LumePalette.soft(Mode.WORK)).isEqualTo(LumePalette.WORK_SOFT)
    }

    @Test
    fun runningBackgroundBlend() {
        Mode.entries.forEach { mode ->
            val expected = LumePalette.mixSrgb(LumePalette.soft(mode), LumePalette.WHITE, 0.70f)
            assertThat(LumePalette.runningBackground(mode)).isEqualTo(expected)
        }
        val work = LumePalette.runningBackground(Mode.WORK)
        assertThat((work shr 16) and 0xFF).isEqualTo(0xEE)
        assertThat((work shr 8) and 0xFF).isEqualTo(0xF4)
        assertThat(work and 0xFF).isEqualTo(0xFF)
    }
}
