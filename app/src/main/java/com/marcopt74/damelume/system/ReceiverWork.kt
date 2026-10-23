package com.marcopt74.damelume.system

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Runs receiver work on a supervised scope and always finishes the pending result.
 * A failure is logged. It does not crash the process or skip [finish].
 */
internal fun launchReceiverWork(
    dispatcher: CoroutineDispatcher,
    finish: () -> Unit,
    block: suspend () -> Unit,
) {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    scope.launch {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "Receiver work failed", error)
        } finally {
            finish()
            scope.cancel()
        }
    }
}

private const val TAG = "LumeReceiver"
