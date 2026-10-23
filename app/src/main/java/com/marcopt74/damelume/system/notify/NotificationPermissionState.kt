package com.marcopt74.damelume.system.notify

import com.marcopt74.damelume.data.TimerRepository

data class NotificationPermissionState(
    val sdkInt: Int,
    val granted: Boolean,
    val asked: Boolean,
    val rationale: Boolean,
    val dismissedMarker: Long?,
    val endsAtEpochMs: Long?,
) {
    val shouldRequestOnFirstStart: Boolean = sdkInt >= 33 && !granted && !asked

    val showRationaleBeforeRequest: Boolean =
        sdkInt >= 33 && !granted && asked && rationale

    val showBanner: Boolean =
        sdkInt >= 33 && !granted && asked && dismissedMarker != TimerRepository.bannerMarker(endsAtEpochMs)
}
