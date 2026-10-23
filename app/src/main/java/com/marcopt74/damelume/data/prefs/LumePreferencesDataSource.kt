package com.marcopt74.damelume.data.prefs

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.Settings
import com.marcopt74.damelume.domain.model.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LumePreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val state: Flow<PersistedState> =
            dataStore.data
                .catch { error ->
                    Log.e(TAG, "Failed to read timer preferences", error)
                    emit(
                        androidx.datastore.preferences.core
                            .emptyPreferences(),
                    )
                }.map { prefs -> prefs.toPersisted() }

        suspend fun current(): PersistedState = state.first()

        suspend fun writeTimer(timer: TimerState) {
            dataStore.edit { prefs ->
                prefs[KEY_WORK] = timer.workMinutes
                prefs[KEY_BREAK] = timer.breakMinutes
                prefs[KEY_LONG] = timer.longBreakMinutes
                prefs[KEY_CYCLE] = timer.cycleWorkCount
                prefs[KEY_MODE] = timer.mode.wireValue()
                prefs[KEY_RUNNING] = timer.isRunning
                prefs[KEY_REMAINING] = timer.remainingMs
                if (timer.endsAtEpochMs == null) {
                    prefs.remove(KEY_ENDS_AT)
                } else {
                    prefs[KEY_ENDS_AT] = timer.endsAtEpochMs
                }
            }
        }

        suspend fun setSoundEnabled(enabled: Boolean) {
            dataStore.edit { it[KEY_SOUND] = enabled }
        }

        suspend fun setLocale(locale: AppLocale) {
            dataStore.edit { it[KEY_LOCALE] = if (locale == AppLocale.PT) "pt" else "en" }
        }

        suspend fun setSessionsResetAt(epochMs: Long) {
            dataStore.edit { it[KEY_RESET_AT] = epochMs }
        }

        suspend fun setNotificationAsked(asked: Boolean) {
            dataStore.edit { it[KEY_NOTIF_ASKED] = asked }
        }

        suspend fun setBannerDismissedMarker(marker: Long?) {
            dataStore.edit { prefs ->
                if (marker == null) prefs.remove(KEY_BANNER) else prefs[KEY_BANNER] = marker
            }
        }

        private fun Preferences.toPersisted(): PersistedState {
            val mode = Mode.fromWire(this[KEY_MODE])
            val timer =
                TimerState(
                    mode = mode,
                    isRunning = this[KEY_RUNNING] ?: false,
                    remainingMs = this[KEY_REMAINING] ?: TimerState.DEFAULT_REMAINING_MS,
                    endsAtEpochMs = this[KEY_ENDS_AT],
                    workMinutes = this[KEY_WORK] ?: TimerState.DEFAULT_WORK_MINUTES,
                    breakMinutes = this[KEY_BREAK] ?: TimerState.DEFAULT_BREAK_MINUTES,
                    longBreakMinutes = this[KEY_LONG] ?: TimerState.DEFAULT_LONG_BREAK_MINUTES,
                    cycleWorkCount = this[KEY_CYCLE] ?: 0,
                )
            val settings =
                Settings(
                    soundEnabled = this[KEY_SOUND] ?: true,
                    locale = AppLocale.fromWire(this[KEY_LOCALE]),
                )
            return PersistedState(
                timer = timer,
                settings = settings,
                sessionsResetAtEpochMs = this[KEY_RESET_AT] ?: 0L,
                notifPermissionAsked = this[KEY_NOTIF_ASKED] ?: false,
                notifBannerDismissedMarker = this[KEY_BANNER],
            )
        }

        companion object {
            private const val TAG = "LumePreferences"
            val KEY_WORK = intPreferencesKey("work_minutes")
            val KEY_BREAK = intPreferencesKey("break_minutes")
            val KEY_LONG = intPreferencesKey("long_break_minutes")
            val KEY_CYCLE = intPreferencesKey("cycle_work_count")
            val KEY_SOUND = booleanPreferencesKey("sound_enabled")
            val KEY_LOCALE = stringPreferencesKey("locale")
            val KEY_MODE = stringPreferencesKey("mode")
            val KEY_RUNNING = booleanPreferencesKey("is_running")
            val KEY_REMAINING = longPreferencesKey("remaining_ms")
            val KEY_ENDS_AT = longPreferencesKey("ends_at_epoch_ms")
            val KEY_RESET_AT = longPreferencesKey("sessions_reset_at_epoch_ms")
            val KEY_NOTIF_ASKED = booleanPreferencesKey("notif_permission_asked")
            val KEY_BANNER = longPreferencesKey("notif_banner_dismissed_for_ends_at")
        }
    }
