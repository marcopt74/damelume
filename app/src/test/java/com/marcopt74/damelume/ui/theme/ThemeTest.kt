package com.marcopt74.damelume.ui.theme

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Test

class ThemeTest {
    @Test
    fun breakModeUsesOrangeAccent() {
        assertThat(LumePalette.graphic(Mode.BREAK)).isEqualTo(0xFFE5631A.toInt())
        assertThat(LumePalette.button(Mode.BREAK)).isEqualTo(0xFFC2410C.toInt())
        assertThat(LumePalette.soft(Mode.BREAK)).isEqualTo(LumePalette.BREAK_SOFT)
    }

    @Test
    fun modeAccents_work_break_longBreak() {
        assertThat(LumePalette.graphic(Mode.WORK)).isEqualTo(0xFF2F6DF6.toInt())
        assertThat(LumePalette.button(Mode.WORK)).isEqualTo(0xFF2F6DF6.toInt())
        assertThat(LumePalette.text(Mode.WORK)).isEqualTo(0xFF2558CC.toInt())
        assertThat(LumePalette.graphic(Mode.BREAK)).isEqualTo(0xFFE5631A.toInt())
        assertThat(LumePalette.graphic(Mode.LONG_BREAK)).isEqualTo(0xFF3E584A.toInt())
        assertThat(LumePalette.button(Mode.LONG_BREAK)).isEqualTo(0xFF3E584A.toInt())
        assertThat(LumePalette.text(Mode.LONG_BREAK)).isEqualTo(0xFF3E584A.toInt())
    }

    @Test
    fun runningBackgroundBlend() {
        Mode.entries.forEach { mode ->
            val expected = LumePalette.mixSrgb(LumePalette.soft(mode), LumePalette.WHITE, 0.70f)
            assertThat(LumePalette.runningBackground(mode)).isEqualTo(expected)
        }
        val work = LumePalette.runningBackground(Mode.WORK)
        assertThat((work shr 16) and 0xFF).isGreaterThan(0xE0)
    }
}
