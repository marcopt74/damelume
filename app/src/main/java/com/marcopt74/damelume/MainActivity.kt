package com.marcopt74.damelume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.system.alarm.ExactAlarmSignals
import com.marcopt74.damelume.ui.timer.TimerScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var repository: TimerRepository

    private val exactAlarmReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                ExactAlarmSignals.notifyChanged()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runBlocking { repository.ensureInitialLocale() }
        setContent {
            TimerScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        resumed = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.registerReceiver(
                this,
                exactAlarmReceiver,
                IntentFilter(android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        ExactAlarmSignals.notifyChanged()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { unregisterReceiver(exactAlarmReceiver) }
        }
        if (resumed === this) resumed = null
        super.onStop()
    }

    companion object {
        @Volatile
        var resumed: MainActivity? = null
    }
}
