package com.marcopt74.damelume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.di.IoDispatcher
import com.marcopt74.damelume.system.alarm.ExactAlarmSignals
import com.marcopt74.damelume.system.launchReceiverWork
import com.marcopt74.damelume.ui.timer.TimerScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var repository: TimerRepository

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val exactAlarmReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                ExactAlarmSignals.notifyChanged()
                val pending = goAsync()
                launchReceiverWork(ioDispatcher, pending::finish) {
                    repository.rescheduleRunningAlarm()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        InitialLocale.launch(lifecycleScope) { repository.ensureInitialLocale() }
        setContent {
            TimerScreen()
        }
    }

    override fun onStart() {
        super.onStart()
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
        super.onStop()
    }
}

internal object InitialLocale {
    fun launch(
        scope: CoroutineScope,
        ensure: suspend () -> Unit,
    ): Job =
        scope.launch {
            try {
                ensure()
            } catch (error: Throwable) {
                Log.e(TAG, "Initial locale failed", error)
            }
        }

    private const val TAG = "LumeLocale"
}
