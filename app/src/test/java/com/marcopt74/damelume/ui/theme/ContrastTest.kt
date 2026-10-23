package com.marcopt74.damelume.ui.theme

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Test

class ContrastTest {
    @Test
    fun textPairsMeetAaAndGraphicsMeetThreeToOne() {
        assertText(LumePalette.WHITE, LumePalette.button(Mode.WORK))
        assertText(LumePalette.text(Mode.WORK), LumePalette.WHITE)
        assertText(LumePalette.text(Mode.WORK), LumePalette.runningBackground(Mode.WORK))
        assertGraphic(LumePalette.graphic(Mode.WORK), LumePalette.runningBackground(Mode.WORK))
        assertGraphic(LumePalette.graphic(Mode.WORK), LumePalette.WHITE)

        assertText(LumePalette.WHITE, LumePalette.button(Mode.BREAK))
        assertText(LumePalette.text(Mode.BREAK), LumePalette.runningBackground(Mode.BREAK))
        assertText(LumePalette.text(Mode.BREAK), LumePalette.WHITE)
        assertGraphic(LumePalette.graphic(Mode.BREAK), LumePalette.runningBackground(Mode.BREAK))
        assertGraphic(LumePalette.graphic(Mode.BREAK), LumePalette.WHITE)

        assertText(LumePalette.WHITE, LumePalette.button(Mode.LONG_BREAK))
        assertText(LumePalette.text(Mode.LONG_BREAK), LumePalette.WHITE)
        assertText(LumePalette.text(Mode.LONG_BREAK), LumePalette.runningBackground(Mode.LONG_BREAK))
        assertGraphic(LumePalette.graphic(Mode.LONG_BREAK), LumePalette.runningBackground(Mode.LONG_BREAK))

        assertText(LumePalette.MUTED, LumePalette.WHITE)
        Mode.entries.forEach { mode ->
            assertText(LumePalette.MUTED, LumePalette.runningBackground(mode))
        }
    }

    private fun assertText(
        foreground: Int,
        background: Int,
    ) {
        assertThat(LumePalette.contrastRatio(foreground, background)).isAtLeast(4.5)
    }

    private fun assertGraphic(
        foreground: Int,
        background: Int,
    ) {
        assertThat(LumePalette.contrastRatio(foreground, background)).isAtLeast(3.0)
    }
}
