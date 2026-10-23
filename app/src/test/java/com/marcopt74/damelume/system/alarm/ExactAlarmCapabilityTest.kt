package com.marcopt74.damelume.system.alarm

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ExactAlarmCapabilityTest {
    @Test
    @Config(sdk = [31])
    fun api31WithoutPermissionUsesInexactAndShowsBanner() {
        val capability = ExactAlarmCapability(sdkInt = 31, canScheduleExact = false)
        assertThat(capability.useExact).isFalse()
        assertThat(capability.showBanner).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun grantedOrOutside12DoesNotShowBanner() {
        assertThat(ExactAlarmCapability(31, canScheduleExact = true).useExact).isTrue()
        assertThat(ExactAlarmCapability(31, canScheduleExact = true).showBanner).isFalse()
        assertThat(ExactAlarmCapability(33, canScheduleExact = false).useExact).isTrue()
        assertThat(ExactAlarmCapability(33, canScheduleExact = false).showBanner).isFalse()
        assertThat(ExactAlarmCapability(30, canScheduleExact = false).useExact).isTrue()
        assertThat(ExactAlarmCapability(30, canScheduleExact = false).showBanner).isFalse()
    }
}
