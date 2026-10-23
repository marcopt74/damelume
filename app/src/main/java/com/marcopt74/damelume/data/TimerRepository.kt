package com.marcopt74.damelume.data

import com.marcopt74.damelume.data.db.CompletedSessionDao
import com.marcopt74.damelume.data.db.CompletedSessionEntity
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.data.prefs.PersistedState
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    ) {
        private val mutex = Mutex()

        val persisted: Flow<PersistedState> = preferences.state

        val todayCount: Flow<Int> =
            preferences.state.flatMapLatest { persisted ->
                dao.todayCount(clock.today().toString(), persisted.sessionsResetAtEpochMs)
            }

        val snapshot: Flow<RepositorySnapshot> =
            combine(preferences.state, todayCount) { persisted, count ->
                RepositorySnapshot(persisted = persisted, todayCount = count)
            }

        suspend fun dispatch(action: TimerAction) {
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
        }

        suspend fun reconcileOnLaunch() {
            dispatch(TimerAction.Reconcile)
        }

        suspend fun ensureInitialLocale() {
            mutex.withLock {
                if (localeApplicator.current() != null) return
                val stored = preferences.current().settings.locale
                val chosen = stored ?: localeApplicator.initialFromSystem()
                preferences.setLocale(chosen)
                localeApplicator.apply(chosen)
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
            preferences.writeTimer(result.state)
            result.sessionsResetAtEpochMs?.let { preferences.setSessionsResetAt(it) }
            applyEffects(previous, result)
        }

        private suspend fun applyEffects(
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
            dao.insert(session.toEntity())
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

        companion object {
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
