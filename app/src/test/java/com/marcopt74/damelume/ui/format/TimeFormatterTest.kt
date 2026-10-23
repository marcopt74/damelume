package com.marcopt74.damelume.ui.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeFormatterTest {
    @Test
    fun zero_isZero() {
        assertThat(TimeFormatter.format(0)).isEqualTo("00:00")
    }

    @Test
    fun underOneSecond_ceilsToOne() {
        assertThat(TimeFormatter.format(999)).isEqualTo("00:01")
    }

    @Test
    fun justUnderOneMinute_ceilsToNextMinute() {
        assertThat(TimeFormatter.format(59_999)).isEqualTo("01:00")
    }

    @Test
    fun ninetyMinutes() {
        assertThat(TimeFormatter.format(5_400_000)).isEqualTo("90:00")
    }
}
