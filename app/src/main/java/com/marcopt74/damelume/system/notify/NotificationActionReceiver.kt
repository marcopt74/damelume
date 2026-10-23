package com.marcopt74.damelume.system.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: NotificationActionHandler

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handler.handle(intent.action)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.marcopt74.damelume.action.PAUSE"
        const val ACTION_RESUME = "com.marcopt74.damelume.action.RESUME"
        const val ACTION_RESET = "com.marcopt74.damelume.action.RESET"
        const val ACTION_SKIP = "com.marcopt74.damelume.action.SKIP"
    }
}
