package com.marcopt74.damelume

import android.app.Application
import com.marcopt74.damelume.system.notify.NotificationChannels
import com.marcopt74.damelume.system.sound.AppForegroundTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LumeApp : Application() {
    @Inject lateinit var foregroundTracker: AppForegroundTracker

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        foregroundTracker.start()
    }
}
