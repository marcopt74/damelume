package com.marcopt74.damelume.system.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.marcopt74.damelume.di.IoDispatcher
import com.marcopt74.damelume.system.launchReceiverWork
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: BootHandler

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        launchReceiverWork(ioDispatcher, pending::finish) {
            handler.onBroadcast(intent.action)
        }
    }
}
