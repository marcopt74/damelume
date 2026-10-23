package com.marcopt74.damelume.data.prefs

import com.marcopt74.damelume.domain.model.Settings
import com.marcopt74.damelume.domain.model.TimerState

data class PersistedState(
    val timer: TimerState = TimerState(),
    val settings: Settings = Settings(),
    val sessionsResetAtEpochMs: Long = 0L,
    val notifPermissionAsked: Boolean = false,
    val notifBannerDismissedMarker: Long? = null,
)
