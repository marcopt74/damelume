package com.marcopt74.damelume.system.notify

import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState

interface TimerNotifier {
    fun showOngoing(state: TimerState)

    fun cancelOngoing()

    fun showCompletion(
        finished: Mode,
        next: Mode,
        nextMinutes: Int,
        soundEnabled: Boolean,
    )
}
