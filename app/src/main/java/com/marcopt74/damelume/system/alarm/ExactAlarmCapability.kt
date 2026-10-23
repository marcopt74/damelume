package com.marcopt74.damelume.system.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Signals that the exact-alarm permission may have changed while the app is alive. */
object ExactAlarmSignals {
    private val ticks = MutableStateFlow(0)
    val updates: StateFlow<Int> = ticks.asStateFlow()

    fun notifyChanged() {
        ticks.value = ticks.value + 1
    }
}

/**
 * Exact alarms are granted on install from API 33 (`USE_EXACT_ALARM`) and are
 * unrestricted through API 30. API 31 and 32 can revoke `SCHEDULE_EXACT_ALARM`.
 */
data class ExactAlarmCapability(
    val sdkInt: Int,
    val canScheduleExact: Boolean,
) {
    val useExact: Boolean = sdkInt >= 33 || sdkInt <= 30 || canScheduleExact

    val showBanner: Boolean = sdkInt in 31..32 && !canScheduleExact
}
