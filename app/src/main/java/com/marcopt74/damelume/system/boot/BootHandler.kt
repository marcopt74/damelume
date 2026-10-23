package com.marcopt74.damelume.system.boot

import android.content.Intent
import com.marcopt74.damelume.data.TimerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootHandler
    @Inject
    constructor(
        private val repository: TimerRepository,
    ) {
        suspend fun onBroadcast(action: String?) {
            if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
                return
            }
            repository.reconcileOnLaunch()
        }
    }
