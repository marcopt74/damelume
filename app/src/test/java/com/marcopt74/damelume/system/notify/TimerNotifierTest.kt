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

        notifier.showCompletion(Mode.BREAK, Mode.WORK, nextMinutes = 25, soundEnabled = false)
        val silent = notification(AndroidTimerNotifier.COMPLETE_ID)
        assertThat(silent.channelId).isEqualTo(NotificationChannels.SESSION_COMPLETE_SILENT)
        assertThat(silent.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Break over")
        assertThat(silent.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("Ready to work · 25 min")
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
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).isEqualTo(context.getString(R.string.working))
        val titles = notification.actions.map { it.title.toString() }
        assertThat(titles).containsExactly(context.getString(R.string.pause), context.getString(R.string.reset)).inOrder()

        notifier.showOngoing(
            TimerState(mode = Mode.BREAK, isRunning = true, endsAtEpochMs = endsAt),
        )
        val onBreak = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(onBreak.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo(context.getString(R.string.resting))
        assertThat(onBreak.actions.map { it.title.toString() }).contains(context.getString(R.string.skip_break))
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
                mode = Mode.BREAK,
                isRunning = false,
                remainingMs = 90_000,
                breakMinutes = 15,
            ),
        )
        val breakPaused = notification(AndroidTimerNotifier.ONGOING_ID)
        assertThat(breakPaused.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo(context.getString(R.string.break_paused))
        assertThat(breakPaused.actions.map { it.title.toString() }).doesNotContain(context.getString(R.string.skip_break))
    }

    @Test
    fun noOngoingWhenIdle() {
        notifier.showOngoing(TimerState())
        assertThat(shadowOf(manager()).getNotification(AndroidTimerNotifier.ONGOING_ID)).isNull()
    }

    private fun notification(id: Int): Notification = shadowOf(manager()).getNotification(id) ?: error("Missing notification $id")

    private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)
}
