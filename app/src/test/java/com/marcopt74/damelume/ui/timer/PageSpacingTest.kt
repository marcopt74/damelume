package com.marcopt74.damelume.ui.timer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PageSpacingTest {
    @Test
    fun tallPhoneGrowsTheDialToTheContentWidthAndSpreadsTheRemainder() {
        val spacing =
            pageSpacing(
                viewportHeight = 800,
                contentWidth = 360,
                headerHeight = 140,
                bannerHeight = 0,
                controlsHeight = 56,
                cardHeight = 130,
                bannerGap = 0,
                verticalPadding = 12,
                baseGap = 16,
                minDial = 200,
                maxDial = 448,
            )
        assertThat(spacing.dialSide).isEqualTo(360)
        assertThat(spacing.height).isEqualTo(800)
        assertThat(spacing.gapBeforeDial).isGreaterThan(16)
        assertThat(spacing.gapBeforeCard).isGreaterThan(16)
    }

    @Test
    fun shortScreenKeepsAMinimumDialAndGrowsPastTheViewport() {
        val spacing =
            pageSpacing(
                viewportHeight = 420,
                contentWidth = 320,
                headerHeight = 140,
                bannerHeight = 80,
                controlsHeight = 56,
                cardHeight = 130,
                bannerGap = 16,
                verticalPadding = 12,
                baseGap = 16,
                minDial = 200,
                maxDial = 448,
            )
        assertThat(spacing.dialSide).isEqualTo(200)
        assertThat(spacing.height).isGreaterThan(420)
        assertThat(spacing.gapBeforeDial).isEqualTo(16)
    }
}
