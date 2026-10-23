package com.marcopt74.damelume.system.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcopt74.damelume.di.IoDispatcher
import com.marcopt74.damelume.system.launchReceiverWork
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: NotificationActionHandler

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        launchReceiverWork(ioDispatcher, pending::finish) {
            handler.handle(intent.action)
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.marcopt74.damelume.action.PAUSE"
        const val ACTION_RESUME = "com.marcopt74.damelume.action.RESUME"
        const val ACTION_RESET = "com.marcopt74.damelume.action.RESET"
        const val ACTION_SKIP = "com.marcopt74.damelume.action.SKIP"
    }
}
