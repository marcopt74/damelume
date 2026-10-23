package com.marcopt74.damelume.system.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcopt74.damelume.system.alarm.AndroidAlarmScheduler.Companion.EXTRA_ENDS_AT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: TimerAlarmHandler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, Long.MIN_VALUE)
        if (endsAt == Long.MIN_VALUE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.onAlarm(endsAt)
            } finally {
                pending.finish()
            }
        }
    }
}
