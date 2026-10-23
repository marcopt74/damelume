package com.marcopt74.damelume.system.sound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos

@RunWith(RobolectricTestRunner::class)
class ChimeAssetTest {
    @Test
    fun rawExists_durationAndDominantFrequencies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ogg = context.resources.openRawResource(R.raw.lume_chime).use { it.readBytes() }
        assertThat(ogg.size).isGreaterThan(1000)

        val wav = javaClass.classLoader!!.getResourceAsStream("lume_chime.wav")!!.use { it.readBytes() }
        val pcm = pcmSamples(wav)
        val rate = sampleRate(wav)
        val duration = pcm.size.toDouble() / rate.toDouble()
        assertThat(duration).isGreaterThan(1.0)
        assertThat(duration).isLessThan(1.1)

        val targets = listOf(523.25, 659.25, 783.99)
        val targetPower = targets.sumOf { goertzel(pcm, rate, it) }
        val offPower = goertzel(pcm, rate, 400.0) + goertzel(pcm, rate, 1000.0)
        assertThat(targetPower).isGreaterThan(offPower * 5)
    }

    private fun sampleRate(wav: ByteArray): Int = ByteBuffer.wrap(wav, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun pcmSamples(wav: ByteArray): DoubleArray {
        var offset = 12
        while (offset + 8 < wav.size) {
            val id = String(wav, offset, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(wav, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val data = offset + 8
            if (id == "data") {
                val samples = DoubleArray(size / 2)
                val buffer = ByteBuffer.wrap(wav, data, size).order(ByteOrder.LITTLE_ENDIAN)
                for (index in samples.indices) {
                    samples[index] = buffer.short.toDouble() / 32768.0
                }
                return samples
            }
            offset = data + size
        }
        error("WAV data chunk missing")
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
