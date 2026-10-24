package com.marcopt74.damelume.ui.theme

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.Mode
import org.junit.Test

class ContrastTest {
    @Test
    fun workAndMutedTextMeetAa() {
        assertText(LumePalette.WHITE, LumePalette.accent(Mode.WORK))
        assertText(LumePalette.MUTED, LumePalette.WHITE)
        assertText(LumePalette.MUTED, LumePalette.runningBackground(Mode.WORK))
        assertText(LumePalette.MUTED, LumePalette.runningBackground(Mode.BREAK))
        assertGraphic(LumePalette.accent(Mode.WORK), LumePalette.WHITE)
        assertGraphic(LumePalette.accent(Mode.WORK), LumePalette.runningBackground(Mode.WORK))
    }

    @Test
    fun breakAccentMatchesTheSiteAndIsBelowAa() {
        assertThat(LumePalette.accent(Mode.BREAK)).isEqualTo(0xFFEF6C1C.toInt())
        val ratio = LumePalette.contrastRatio(LumePalette.WHITE, LumePalette.accent(Mode.BREAK))
        assertThat(ratio).isWithin(0.02).of(3.07)
        assertThat(ratio).isLessThan(4.5)
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
