package com.marcopt74.damelume.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlarmScheduler {
        override fun schedule(endsAtEpochMs: Long) {
            val alarmManager = alarmManager()
            val pending = pendingIntent(endsAtEpochMs)
            val capability =
                ExactAlarmCapability(
                    sdkInt = Build.VERSION.SDK_INT,
                    canScheduleExact = canScheduleExact(alarmManager),
                )
            if (capability.useExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtEpochMs, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAtEpochMs, pending)
            }
        }

        override fun cancel(endsAtEpochMs: Long?) {
            if (endsAtEpochMs == null) return
            val pending = pendingIntent(endsAtEpochMs)
            alarmManager().cancel(pending)
            pending.cancel()
        }

        private fun alarmManager(): AlarmManager = context.getSystemService(AlarmManager::class.java)

        private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

        private fun pendingIntent(endsAtEpochMs: Long): PendingIntent {
            val intent =
                Intent(context, TimerAlarmReceiver::class.java).apply {
                    action = ACTION_ALARM
                    data = Uri.parse("lume://timer/$endsAtEpochMs")
                    putExtra(EXTRA_ENDS_AT, endsAtEpochMs)
                }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        companion object {
            const val ACTION_ALARM = "com.marcopt74.damelume.action.TIMER_ALARM"
            const val EXTRA_ENDS_AT = "ends_at"
        }
    }
