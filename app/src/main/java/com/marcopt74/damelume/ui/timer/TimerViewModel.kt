package com.marcopt74.damelume.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.system.alarm.ExactAlarmSignals
import com.marcopt74.damelume.system.notify.DevicePermissions
import com.marcopt74.damelume.system.notify.NotificationPermissionState
import com.marcopt74.damelume.system.sound.ForegroundTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel
    @Inject
    constructor(
        private val repository: TimerRepository,
        private val clock: Clock,
        private val foregroundTracker: ForegroundTracker,
        private val permissions: DevicePermissions,
    ) : ViewModel() {
        private val nowMs = MutableStateFlow(clock.nowMillis())
        private val permissionTick = MutableStateFlow(0)
        private val notificationRationale = MutableStateFlow(false)
        private var ticker: Job? = null

        internal val isTicking: Boolean
            get() = ticker?.isActive == true

        val uiState: StateFlow<TimerUiState> =
            combine(
                repository.snapshot,
                nowMs,
                permissionTick,
                notificationRationale,
            ) { snapshot, now, _, rationale ->
                val persisted = snapshot.persisted
                val notification =
                    NotificationPermissionState(
                        sdkInt = permissions.sdkInt(),
                        granted = permissions.notificationsGranted(),
                        asked = persisted.notifPermissionAsked,
                        rationale = rationale,
                        dismissedMarker = persisted.notifBannerDismissedMarker,
                        endsAtEpochMs = persisted.timer.endsAtEpochMs,
                    )
                val exact = permissions.exactAlarmCapability()
                TimerUiState.from(
                    timer = persisted.timer,
                    nowMs = now,
                    todayCount = snapshot.todayCount,
                    soundEnabled = persisted.settings.soundEnabled,
                    locale = persisted.settings.locale,
                    showNotificationBanner = notification.showBanner,
                    showNotificationRationale = notification.showRationaleBeforeRequest,
                    requestNotificationOnStart = notification.shouldRequestOnFirstStart,
                    showExactAlarmBanner = exact.showBanner,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

        init {
            viewModelScope.launch { repository.reconcileOnLaunch() }
            viewModelScope.launch {
                ExactAlarmSignals.updates.collect { permissionTick.value = permissionTick.value + 1 }
            }
            viewModelScope.launch {
                combine(
                    foregroundTracker.isForeground,
                    repository.persisted.map { it.timer.isRunning },
                ) { foreground, running -> foreground && running }
                    .distinctUntilChanged()
                    .collect { shouldTick ->
                        ticker?.cancel()
                        ticker = null
                        if (shouldTick) {
                            ticker = viewModelScope.launch { runTicker() }
                        }
                    }
            }
        }

        fun onResume(showNotificationRationale: Boolean) {
            notificationRationale.value = showNotificationRationale
            permissionTick.value = permissionTick.value + 1
            nowMs.value = clock.nowMillis()
            viewModelScope.launch {
                repository.refreshToday()
                repository.rescheduleRunningAlarm()
            }
        }

        fun onFrame() {
            val now = clock.nowMillis()
            nowMs.value = now
            maybeComplete(now)
        }

        fun onAction(action: TimerAction) {
            viewModelScope.launch { repository.dispatch(action) }
        }

        private suspend fun runTicker() {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val now = clock.nowMillis()
                nowMs.value = now
                maybeComplete(now)
                delay(200)
            }
        }

        private fun maybeComplete(now: Long) {
            val state = uiState.value
            val endsAt = state.endsAtEpochMs ?: return
            if (state.isRunning && endsAt - now <= TimerEngine.COMPLETE_THRESHOLD_MS) {
                onAction(TimerAction.Complete(expectedEndsAt = endsAt))
            }
        }
    }
