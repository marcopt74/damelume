package com.marcopt74.damelume.data

import android.util.Log
import com.marcopt74.damelume.data.db.CompletedSessionDao
import com.marcopt74.damelume.data.db.CompletedSessionEntity
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.data.prefs.PersistedState
import com.marcopt74.damelume.di.ApplicationScope
import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.AlarmCommand
import com.marcopt74.damelume.domain.model.EngineResult
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.OngoingCommand
import com.marcopt74.damelume.domain.model.SessionDraft
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.system.alarm.AlarmScheduler
import com.marcopt74.damelume.system.locale.LocaleApplicator
import com.marcopt74.damelume.system.notify.TimerNotifier
import com.marcopt74.damelume.system.sound.ChimePlayer
import com.marcopt74.damelume.system.sound.ForegroundTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TimerRepository
    @Inject
    constructor(
        private val preferences: LumePreferencesDataSource,
        private val dao: CompletedSessionDao,
        private val clock: Clock,
        private val alarmScheduler: AlarmScheduler,
        private val notifier: TimerNotifier,
        private val chimePlayer: ChimePlayer,
        private val foregroundTracker: ForegroundTracker,
        private val localeApplicator: LocaleApplicator,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val mutex = Mutex()
        private val today = MutableStateFlow(clock.today())

        val persisted: Flow<PersistedState> = preferences.state

        val todayCount: Flow<Int> =
            combine(preferences.state, today) { persisted, date -> persisted to date }
                .flatMapLatest { (persisted, date) ->
                    dao.todayCount(date.toString(), persisted.sessionsResetAtEpochMs)
                }

        val snapshot: Flow<RepositorySnapshot> =
            combine(preferences.state, todayCount) { persisted, count ->
                RepositorySnapshot(persisted = persisted, todayCount = count)
            }

        init {
            scope.launch {
                while (isActive) {
                    delay(millisUntilNextMidnight())
                    today.value = clock.today()
                }
            }
        }

        fun refreshToday() {
            today.value = clock.today()
        }

        suspend fun dispatch(action: TimerAction) {
            try {
                mutex.withLock {
                    val current = preferences.current()
                    when (action) {
                        is TimerAction.SetSoundEnabled -> preferences.setSoundEnabled(action.enabled)
                        is TimerAction.SetLocale -> {
                            preferences.setLocale(action.locale)
                            localeApplicator.apply(action.locale)
                        }
                        TimerAction.MarkNotificationAsked -> preferences.setNotificationAsked(true)
                        TimerAction.DismissNotificationBanner -> {
                            val marker = bannerMarker(current.timer.endsAtEpochMs)
                            preferences.setBannerDismissedMarker(marker)
                        }
                        else -> applyEngine(current.timer, action, current.settings.soundEnabled)
                    }
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to apply $action", error)
            }
        }

        suspend fun reconcileOnLaunch() {
            dispatch(TimerAction.Reconcile)
        }

        suspend fun rescheduleRunningAlarm() {
            try {
                mutex.withLock {
                    val timer = preferences.current().timer
                    val endsAt = timer.endsAtEpochMs
                    if (timer.isRunning && endsAt != null) {
                        alarmScheduler.cancel(endsAt)
                        alarmScheduler.schedule(endsAt)
                    }
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to reschedule the running alarm", error)
            }
        }

        suspend fun ensureInitialLocale() {
            try {
                mutex.withLock {
                    if (localeApplicator.current() != null) return
                    val stored = preferences.current().settings.locale
                    val chosen = stored ?: localeApplicator.initialFromSystem()
                    preferences.setLocale(chosen)
                    localeApplicator.apply(chosen)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to apply the initial locale", error)
            }
        }

        private suspend fun applyEngine(
            previous: TimerState,
            action: TimerAction,
            soundEnabled: Boolean,
        ) {
            val result =
                TimerEngine.reduce(
                    state = previous,
                    action = action,
                    nowMs = clock.nowMillis(),
                    soundEnabled = soundEnabled,
                    zone = clock.zone(),
                )
            val session = result.session
            try {
                if (session != null) {
                    dao.insert(session.toEntity())
                }
                preferences.writeTimer(result.state)
                result.sessionsResetAtEpochMs?.let { preferences.setSessionsResetAt(it) }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to persist timer state", error)
                return
            }
            try {
                applyEffects(previous, result)
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to apply timer effects", error)
            }
        }

        private fun applyEffects(
            previous: TimerState,
            result: EngineResult,
        ) {
            when (val alarm = result.alarm) {
                AlarmCommand.Unchanged -> Unit
                AlarmCommand.Cancel -> alarmScheduler.cancel(previous.endsAtEpochMs)
                is AlarmCommand.Schedule -> {
                    val previousEnds = previous.endsAtEpochMs
                    if (previousEnds != null && previousEnds != alarm.endsAtEpochMs) {
                        alarmScheduler.cancel(previousEnds)
                    }
                    alarmScheduler.schedule(alarm.endsAtEpochMs)
                }
            }
            when (result.ongoing) {
                OngoingCommand.Unchanged -> Unit
                OngoingCommand.Cancel -> notifier.cancelOngoing()
                OngoingCommand.Update -> notifier.showOngoing(result.state)
            }
            val session = result.session ?: return
            if (foregroundTracker.isInForeground()) {
                if (result.playChime) chimePlayer.play()
            } else {
                notifier.showCompletion(
                    finished = session.mode,
                    next = result.state.mode,
                    nextMinutes = minutesOf(result.state),
                    soundEnabled = result.playChime,
                )
            }
        }

        private fun minutesOf(state: TimerState): Int =
            when (state.mode) {
                Mode.WORK -> state.workMinutes
                Mode.BREAK -> state.breakMinutes
                Mode.LONG_BREAK -> state.longBreakMinutes
            }

        private fun millisUntilNextMidnight(): Long {
            val zone = clock.zone()
            val now = Instant.ofEpochMilli(clock.nowMillis()).atZone(zone)
            val nextMidnight =
                now
                    .toLocalDate()
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            return (nextMidnight - clock.nowMillis()).coerceAtLeast(1L)
        }

        companion object {
            private const val TAG = "TimerRepository"
            const val IDLE_BANNER_MARKER = -1L

            fun bannerMarker(endsAtEpochMs: Long?): Long = endsAtEpochMs ?: IDLE_BANNER_MARKER
        }
    }

data class RepositorySnapshot(
    val persisted: PersistedState,
    val todayCount: Int,
)

private fun SessionDraft.toEntity(): CompletedSessionEntity =
    CompletedSessionEntity(
        mode = mode.name,
        plannedMs = plannedMs,
        endedAtEpochMs = endedAtEpochMs,
        localDate = localDate.toString(),
        lateCompletion = lateCompletion,
    )
