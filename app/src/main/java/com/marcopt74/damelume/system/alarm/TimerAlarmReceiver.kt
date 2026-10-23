package com.marcopt74.damelume.system.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcopt74.damelume.di.IoDispatcher
import com.marcopt74.damelume.system.alarm.AndroidAlarmScheduler.Companion.EXTRA_ENDS_AT
import com.marcopt74.damelume.system.launchReceiverWork
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: TimerAlarmHandler

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, Long.MIN_VALUE)
        if (endsAt == Long.MIN_VALUE) return
        val pending = goAsync()
        launchReceiverWork(ioDispatcher, pending::finish) {
            handler.onAlarm(endsAt)
        }
    }
}
