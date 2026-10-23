package com.marcopt74.damelume.system.notify

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.marcopt74.damelume.system.alarm.ExactAlarmCapability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface DevicePermissions {
    fun sdkInt(): Int

    fun notificationsGranted(): Boolean

    fun exactAlarmCapability(): ExactAlarmCapability
}

fun notificationRationale(
    activity: Activity?,
    sdkInt: Int,
): Boolean {
    if (sdkInt < 33 || activity == null) return false
    return activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
}

@Singleton
class AndroidDevicePermissions
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DevicePermissions {
        override fun sdkInt(): Int = Build.VERSION.SDK_INT

        override fun notificationsGranted(): Boolean {
            if (Build.VERSION.SDK_INT < 33) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

        override fun exactAlarmCapability(): ExactAlarmCapability {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val can =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            return ExactAlarmCapability(sdkInt = Build.VERSION.SDK_INT, canScheduleExact = can)
        }
    }
