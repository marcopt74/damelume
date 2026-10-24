package com.marcopt74.damelume.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
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
                TimerState(workMinutes = 40, breakMinutes = 8),
            )
            source.setSoundEnabled(false)

            val read = source.current()
            assertThat(read.timer.workMinutes).isEqualTo(40)
            assertThat(read.timer.breakMinutes).isEqualTo(8)
            assertThat(read.settings.soundEnabled).isFalse()
        }

    @Test
    fun storedLongBreakReloadsAsWork() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val file = File(context.filesDir, "legacy-long.preferences_pb")
            val store = PreferenceDataStoreFactory.create { file }
            store.edit { prefs ->
                prefs[LumePreferencesDataSource.KEY_MODE] = "long_break"
                prefs[LumePreferencesDataSource.KEY_RUNNING] = true
                prefs[LumePreferencesDataSource.KEY_REMAINING] = 15 * 60_000L
            }
            val source = LumePreferencesDataSource(store)
            source.setLocale(AppLocale.EN)

            val read = source.current()
            assertThat(read.timer.mode).isEqualTo(Mode.WORK)
            assertThat(read.timer.isRunning).isTrue()
            assertThat(read.settings.locale).isEqualTo(AppLocale.EN)
        }

    private fun newSource(name: String): LumePreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "$name.preferences_pb")
        val store = PreferenceDataStoreFactory.create { file }
        return LumePreferencesDataSource(store)
    }
}
