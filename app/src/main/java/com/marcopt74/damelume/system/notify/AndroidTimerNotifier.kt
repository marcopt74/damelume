package com.marcopt74.damelume.system.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.marcopt74.damelume.MainActivity
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.TimerEngine
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.ui.format.TimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTimerNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TimerNotifier {
        override fun showOngoing(state: TimerState) {
            val notification =
                if (state.isRunning && state.endsAtEpochMs != null) {
                    running(state)
                } else if (TimerEngine.isMidPause(state)) {
                    paused(state)
                } else {
                    cancelOngoing()
                    return
                }
            manager().notify(ONGOING_ID, notification)
        }

        override fun cancelOngoing() {
            manager().cancel(ONGOING_ID)
        }

        override fun showCompletion(
            finished: Mode,
            next: Mode,
            nextMinutes: Int,
            soundEnabled: Boolean,
        ) {
            val channel =
                if (soundEnabled) {
                    NotificationChannels.SESSION_COMPLETE
                } else {
                    NotificationChannels.SESSION_COMPLETE_SILENT
                }
            val notification =
                NotificationCompat
                    .Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_stat_lume)
                    .setContentTitle(context.getString(titleFor(finished)))
                    .setContentText(context.getString(bodyForNext(next), nextMinutes))
                    .setContentIntent(contentIntent())
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .build()
            manager().notify(COMPLETE_ID, notification)
        }

        private fun running(state: TimerState): android.app.Notification {
            val endsAt = state.endsAtEpochMs ?: 0L
            val builder =
                base(NotificationChannels.TIMER_RUNNING)
                    .setContentTitle(context.getString(runningTitle(state.mode)))
                    .setContentText(context.getString(modeName(state.mode)))
                    .setWhen(endsAt)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .addAction(0, context.getString(R.string.pause), actionIntent(NotificationActionReceiver.ACTION_PAUSE))
                    .addAction(0, context.getString(R.string.reset), actionIntent(NotificationActionReceiver.ACTION_RESET))
            if (state.mode != Mode.WORK) {
                builder.addAction(
                    0,
                    context.getString(R.string.skip_break),
                    actionIntent(NotificationActionReceiver.ACTION_SKIP),
                )
            }
            return builder.build()
        }

        private fun paused(state: TimerState): android.app.Notification {
            val time = TimeFormatter.format(state.remainingMs)
            val title =
                if (state.mode == Mode.LONG_BREAK) {
                    context.getString(R.string.notif_paused_long_break, time)
                } else {
                    context.getString(pausedTitle(state.mode))
                }
            return base(NotificationChannels.TIMER_RUNNING)
                .setContentTitle(title)
                .setContentText(time)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setUsesChronometer(false)
                .addAction(0, context.getString(R.string.resume), actionIntent(NotificationActionReceiver.ACTION_RESUME))
                .addAction(0, context.getString(R.string.reset), actionIntent(NotificationActionReceiver.ACTION_RESET))
                .build()
        }

        private fun base(channel: String): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, channel)
                .setSmallIcon(R.drawable.ic_stat_lume)
                .setContentIntent(contentIntent())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setSilent(true)

        private fun contentIntent(): PendingIntent {
            val intent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun actionIntent(action: String): PendingIntent {
            val intent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = action
                }
            val requestCode =
                when (action) {
                    NotificationActionReceiver.ACTION_PAUSE -> 1
                    NotificationActionReceiver.ACTION_RESUME -> 2
                    NotificationActionReceiver.ACTION_RESET -> 3
                    NotificationActionReceiver.ACTION_SKIP -> 4
                    else -> 5
                }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

        companion object {
            const val ONGOING_ID = 42
            const val COMPLETE_ID = 43

            fun runningTitle(mode: Mode): Int =
                when (mode) {
                    Mode.WORK -> R.string.working
                    Mode.BREAK -> R.string.resting
                    Mode.LONG_BREAK -> R.string.long_resting
                }

            fun pausedTitle(mode: Mode): Int =
                when (mode) {
                    Mode.WORK -> R.string.paused
                    Mode.BREAK -> R.string.break_paused
                    Mode.LONG_BREAK -> R.string.long_break_paused
                }

            fun modeName(mode: Mode): Int =
                when (mode) {
                    Mode.WORK -> R.string.work
                    Mode.BREAK -> R.string.break_label
                    Mode.LONG_BREAK -> R.string.long_break
                }

            fun titleFor(finished: Mode): Int =
                when (finished) {
                    Mode.WORK -> R.string.notif_work_done_title
                    Mode.BREAK -> R.string.notif_break_done_title
                    Mode.LONG_BREAK -> R.string.notif_long_break_done_title
                }

            fun bodyForNext(next: Mode): Int =
                when (next) {
                    Mode.WORK -> R.string.notif_work_next
                    Mode.BREAK -> R.string.notif_break_next
                    Mode.LONG_BREAK -> R.string.notif_long_break_next
                }
        }
    }
