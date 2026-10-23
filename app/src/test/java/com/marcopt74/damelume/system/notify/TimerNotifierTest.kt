package com.marcopt74.damelume.system.notify

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TimerNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notifier = AndroidTimerNotifier(context)

    @Before
    fun createChannels() {
        NotificationChannels.create(context)
    }

    @Test
    fun completion_usesChannelBySound_andTexts() {
        notifier.showCompletion(Mode.WORK, Mode.BREAK, nextMinutes = 5, soundEnabled = true)
        val withSound = notification(AndroidTimerNotifier.COMPLETE_ID)
        assertThat(withSound.channelId).isEqualTo(NotificationChannels.SESSION_COMPLETE)
        assertThat(withSound.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Work session over")
        assertThat(withSound.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Time for a break · 5 min")
        assertThat(withSound.flags and Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
        assertContentIntent(withSound)

        notifier.showCompletion(Mode.BREAK, Mode.WORK, nextMinutes = 25, soundEnabled = false)
        val silent = notification(AndroidTimerNotifier.COMPLETE_ID)
        assertThat(silent.channelId).isEqualTo(NotificationChannels.SESSION_COMPLETE_SILENT)
        assertThat(silent.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Break over")
        assertThat(silent.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Ready to work · 25 min")
    }

    @Test
    fun completion_longBreakTexts() {
        notifier.showCompletion(Mode.WORK, Mode.LONG_BREAK, nextMinutes = 15, soundEnabled = true)
        val intoLong = notification(AndroidTimerNotifier.COMPLETE_ID)
        assertThat(intoLong.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Work session over")
        assertThat(intoLong.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Time for a long break · 15 min")
        assertThat(intoLong.channelId).isEqualTo(NotificationChannels.SESSION_COMPLETE)

        notifier.showCompletion(Mode.LONG_BREAK, Mode.WORK, nextMinutes = 25, soundEnabled = false)
        val outOfLong = notification(AndroidTimerNotifier.COMPLETE_ID)
        assertThat(outOfLong.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Long break over")
        assertThat(outOfLong.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Ready to work · 25 min")
        assertThat(outOfLong.channelId).isEqualTo(NotificationChannels.SESSION_COMPLETE_SILENT)
    }

    @Test
    fun ongoing_runningHasChronometerCountdownAndActions() {
        val endsAt = 1_700_000_000_000L
        notifier.showOngoing(
            TimerState(mode = Mode.WORK, isRunning = true, endsAtEpochMs = endsAt, remainingMs = 60_000),
        )
        val notification = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(notification.channelId).isEqualTo(NotificationChannels.TIMER_RUNNING)
        assertThat(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)).isTrue()
        assertThat(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)).isTrue()
        assertThat(notification.`when`).isEqualTo(endsAt)
        assertContentIntent(notification)
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).isEqualTo(context.getString(R.string.working))
        val titles = notification.actions.map { it.title.toString() }
        assertThat(titles).containsExactly(context.getString(R.string.pause), context.getString(R.string.reset)).inOrder()

        notifier.showOngoing(
            TimerState(mode = Mode.LONG_BREAK, isRunning = true, endsAtEpochMs = endsAt, cycleWorkCount = 4),
        )
        val longBreak = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(longBreak.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo(context.getString(R.string.long_resting))
        assertThat(longBreak.actions.map { it.title.toString() }).contains(context.getString(R.string.skip_break))
    }

    @Test
    fun ongoing_pausedStatic() {
        notifier.showOngoing(
            TimerState(mode = Mode.WORK, isRunning = false, remainingMs = 60_000, workMinutes = 25),
        )
        val paused = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(paused.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)).isFalse()
        assertThat(paused.extras.getString(Notification.EXTRA_TITLE)).isEqualTo(context.getString(R.string.paused))
        val titles = paused.actions.map { it.title.toString() }
        assertThat(titles).containsExactly(context.getString(R.string.resume), context.getString(R.string.reset)).inOrder()

        notifier.showOngoing(
            TimerState(
                mode = Mode.LONG_BREAK,
                isRunning = false,
                remainingMs = 90_000,
                longBreakMinutes = 15,
                cycleWorkCount = 4,
            ),
        )
        val longPaused = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(longPaused.extras.getString(Notification.EXTRA_TITLE)).contains("01:30")
        assertThat(longPaused.actions.map { it.title.toString() }).doesNotContain(context.getString(R.string.skip_break))
    }

    @Test
    fun noOngoingWhenIdle() {
        notifier.showOngoing(TimerState())
        assertThat(shadowOf(manager()).getNotification(AndroidTimerNotifier.ONGOING_ID)).isNull()
    }

    private fun assertContentIntent(notification: Notification) {
        val pending = notification.contentIntent ?: error("Missing content intent")
        val intent = shadowOf(pending).savedIntent
        assertThat(intent.component?.className).isEqualTo("com.marcopt74.damelume.MainActivity")
        assertThat(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
        assertThat(intent.flags and android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
        assertThat(intent.flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP).isNotEqualTo(0)
        assertThat(pending.isImmutable).isTrue()
    }

    private fun notification(id: Int): Notification = shadowOf(manager()).getNotification(id) ?: error("Missing notification $id")

    private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)
}
