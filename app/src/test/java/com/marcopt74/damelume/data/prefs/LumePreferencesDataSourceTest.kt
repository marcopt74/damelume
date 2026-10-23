package com.marcopt74.damelume.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LumePreferencesDataSourceTest {
    @Test
    fun persistsDurationsAndSound() =
        runTest {
            val source = newSource("durations")
            source.writeTimer(
                TimerState(workMinutes = 40, breakMinutes = 8, longBreakMinutes = 20),
            )
            source.setSoundEnabled(false)

            val read = source.current()
            assertThat(read.timer.workMinutes).isEqualTo(40)
            assertThat(read.timer.breakMinutes).isEqualTo(8)
            assertThat(read.timer.longBreakMinutes).isEqualTo(20)
            assertThat(read.settings.soundEnabled).isFalse()
        }

    @Test
    fun persistsLongBreakAndCycle() =
        runTest {
            val source = newSource("cycle")
            source.writeTimer(
                TimerState(
                    mode = Mode.LONG_BREAK,
                    longBreakMinutes = 22,
                    cycleWorkCount = 4,
                    remainingMs = 22 * 60_000L,
                ),
            )
            source.setLocale(AppLocale.EN)

            val read = source.current()
            assertThat(read.timer.longBreakMinutes).isEqualTo(22)
            assertThat(read.timer.cycleWorkCount).isEqualTo(4)
            assertThat(read.timer.mode).isEqualTo(Mode.LONG_BREAK)
            assertThat(read.settings.locale).isEqualTo(AppLocale.EN)
        }

    private fun newSource(name: String): LumePreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "$name.preferences_pb")
        val store = PreferenceDataStoreFactory.create { file }
        return LumePreferencesDataSource(store)
    }
}
