package com.marcopt74.damelume.system.alarm

import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.domain.model.TimerAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerAlarmHandler
    @Inject
    constructor(
        private val repository: TimerRepository,
    ) {
        suspend fun onAlarm(expectedEndsAt: Long) {
            repository.dispatch(TimerAction.Complete(expectedEndsAt = expectedEndsAt))
        }
    }
