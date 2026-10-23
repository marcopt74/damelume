package com.marcopt74.damelume.system.notify

import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.TimerRepository
import org.junit.Test

class NotificationPermissionStateTest {
    @Test
    fun api32DoesNotRequest() {
        val state = state(sdk = 32, granted = false, asked = false)
        assertThat(state.shouldRequestOnFirstStart).isFalse()
        assertThat(state.showBanner).isFalse()
    }

    @Test
    fun api33FirstStartRequestsAndDeniedShowsBanner() {
        val first = state(sdk = 33, granted = false, asked = false)
        assertThat(first.shouldRequestOnFirstStart).isTrue()
        assertThat(first.showBanner).isFalse()

        val denied = state(sdk = 33, granted = false, asked = true, endsAt = 20L)
        assertThat(denied.shouldRequestOnFirstStart).isFalse()
        assertThat(denied.showBanner).isTrue()

        val dismissed = denied.copy(dismissedMarker = TimerRepository.bannerMarker(20L))
        assertThat(dismissed.showBanner).isFalse()

        val nextSession = dismissed.copy(endsAtEpochMs = 21L)
        assertThat(nextSession.showBanner).isTrue()
    }

    @Test
    fun rationalePrecedesTheSecondRequest() {
        val rationale = state(sdk = 33, granted = false, asked = true, rationale = true)
        assertThat(rationale.showRationaleBeforeRequest).isTrue()
        val granted = rationale.copy(granted = true)
        assertThat(granted.showRationaleBeforeRequest).isFalse()
        assertThat(granted.showBanner).isFalse()
    }

    private fun state(
        sdk: Int,
        granted: Boolean,
        asked: Boolean,
        rationale: Boolean = false,
        endsAt: Long? = null,
    ) = NotificationPermissionState(
        sdkInt = sdk,
        granted = granted,
        asked = asked,
        rationale = rationale,
        dismissedMarker = null,
        endsAtEpochMs = endsAt,
    )
}
