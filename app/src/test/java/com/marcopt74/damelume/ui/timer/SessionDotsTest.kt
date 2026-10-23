package com.marcopt74.damelume.ui.timer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.TimerState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDotsTest {
    @Test
    fun cycleWorkCount_zeroThroughFour_andDescription() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (count in 0..4) {
            val ui =
                TimerUiState.from(
                    timer = TimerState(cycleWorkCount = count),
                    nowMs = 0L,
                    todayCount = 0,
                    soundEnabled = true,
                    locale = null,
                )
            assertThat(ui.cycleWorkCount).isEqualTo(count)
            assertThat(context.getString(R.string.cycle_progress_cd, count)).isEqualTo("$count of 4 sessions in cycle")
        }
        val clamped =
            TimerUiState.from(
                timer = TimerState(cycleWorkCount = 9),
                nowMs = 0L,
                todayCount = 0,
                soundEnabled = true,
                locale = null,
            )
        assertThat(clamped.cycleWorkCount).isEqualTo(TimerEngine.CYCLE_LENGTH)
        assertThat(clamped.resetSessionsEnabled).isTrue()
    }
}
