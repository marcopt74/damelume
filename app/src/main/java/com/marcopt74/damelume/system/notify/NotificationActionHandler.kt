package com.marcopt74.damelume.system.notify

import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.domain.model.TimerAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationActionHandler
    @Inject
    constructor(
        private val repository: TimerRepository,
    ) {
        suspend fun handle(action: String?) {
            val timerAction =
                when (action) {
                    NotificationActionReceiver.ACTION_PAUSE -> TimerAction.Pause
                    NotificationActionReceiver.ACTION_RESUME -> TimerAction.Start
                    NotificationActionReceiver.ACTION_RESET -> TimerAction.Reset
                    NotificationActionReceiver.ACTION_SKIP -> TimerAction.SkipBreak
                    else -> return
                }
            repository.dispatch(timerAction)
        }
    }
