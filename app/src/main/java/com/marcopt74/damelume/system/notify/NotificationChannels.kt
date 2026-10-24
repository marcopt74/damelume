package com.marcopt74.damelume.system.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.marcopt74.damelume.R

object NotificationChannels {
    const val TIMER_RUNNING = "timer_running"
    const val SESSION_COMPLETE = "session_complete"
    const val SESSION_COMPLETE_SILENT = "session_complete_notice"

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.lume_chime}")
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        val running =
            NotificationChannel(
                TIMER_RUNNING,
                context.getString(R.string.channel_timer_running),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_timer_running)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        val complete =
            NotificationChannel(
                SESSION_COMPLETE,
                context.getString(R.string.channel_session_complete),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_session_complete)
                setSound(sound, attributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        val silent =
            NotificationChannel(
                SESSION_COMPLETE_SILENT,
                context.getString(R.string.channel_session_complete_silent),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_session_complete_silent)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
        manager.createNotificationChannel(running)
        manager.createNotificationChannel(complete)
        manager.createNotificationChannel(silent)
    }
}
