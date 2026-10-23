package com.marcopt74.damelume.system.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidAlarmSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scheduler = AndroidAlarmScheduler(context)

    @Test
    fun schedulesExactAtEndsAt_cancelsOnPause() {
        val endsAt = 1_800_000_000_000L
        scheduler.schedule(endsAt)

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val shadow = shadowOf(alarmManager)
        val scheduled = shadow.scheduledAlarms
        assertThat(scheduled).hasSize(1)
        assertThat(scheduled[0].type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(scheduled[0].triggerAtMs).isEqualTo(endsAt)

        scheduler.cancel(endsAt)
        assertThat(shadow.scheduledAlarms).isEmpty()
    }
}
