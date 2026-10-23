package com.marcopt74.damelume.system.sound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.PI
import kotlin.math.cos

@RunWith(RobolectricTestRunner::class)
class ChimeAssetTest {
    @Test
    fun rawExists_durationAndDominantFrequencies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ogg = context.resources.openRawResource(R.raw.lume_chime).use { it.readBytes() }
        assertThat(ogg.size).isGreaterThan(1000)

        val decoded = decodeVorbis(ogg)
        val duration = decoded.samples.size.toDouble() / decoded.sampleRate.toDouble()
        assertThat(duration).isGreaterThan(1.0)
        assertThat(duration).isLessThan(1.1)

        val targets = listOf(523.25, 659.25, 783.99)
        val targetPower = targets.sumOf { goertzel(decoded.samples, decoded.sampleRate, it) }
        val offPower = goertzel(decoded.samples, decoded.sampleRate, 400.0) + goertzel(decoded.samples, decoded.sampleRate, 1000.0)
        assertThat(targetPower).isGreaterThan(offPower * 5)
    }

    private fun goertzel(
        samples: DoubleArray,
        sampleRate: Int,
        frequency: Double,
    ): Double {
        val normalized = frequency / sampleRate
        val coefficient = 2.0 * cos(2.0 * PI * normalized)
        var s0 = 0.0
        var s1 = 0.0
        var s2 = 0.0
        for (sample in samples) {
            s0 = sample + coefficient * s1 - s2
            s2 = s1
            s1 = s0
        }
        return s1 * s1 + s2 * s2 - coefficient * s1 * s2
    }
}
