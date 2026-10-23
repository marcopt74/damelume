package com.marcopt74.damelume

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.system.launchReceiverWork
import com.marcopt74.damelume.system.notify.notificationRationale
import com.marcopt74.damelume.system.sound.ChimeGate
import com.marcopt74.damelume.ui.timer.DialGeometry
import com.marcopt74.damelume.ui.timer.PrimaryMotion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import java.io.File

class QaRoundOneTest {
    @Test
    fun initialLocale_returnsBeforeTheDataStoreReadFinishes() {
        val dispatcher = StandardTestDispatcher()
        val scope = CoroutineScope(dispatcher)
        val gate = CompletableDeferred<Unit>()
        var applied = false
        val job =
            InitialLocale.launch(scope) {
                gate.await()
                applied = true
            }
        dispatcher.scheduler.runCurrent()
        assertThat(job.isCompleted).isFalse()
        assertThat(applied).isFalse()
        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()
        assertThat(applied).isTrue()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun receiverWork_finishesAndDoesNotEscapeWhenTheBlockFails() {
        val dispatcher = UnconfinedTestDispatcher()
        var finished = false
        var escaped = false
        try {
            launchReceiverWork(dispatcher, finish = { finished = true }) {
                throw IllegalStateException("database closed")
            }
        } catch (error: Throwable) {
            escaped = true
        }
        assertThat(escaped).isFalse()
        assertThat(finished).isTrue()
    }

    @Test
    fun chimeGate_queuesAPlayThatArrivesBeforeLoad() {
        val gate = ChimeGate()
        assertThat(gate.shouldPlayNow()).isFalse()
        assertThat(gate.onLoadComplete(success = true)).isTrue()
        assertThat(gate.shouldPlayNow()).isTrue()
        gate.reset()
        assertThat(gate.shouldPlayNow()).isFalse()
        assertThat(gate.onLoadComplete(success = false)).isFalse()
    }

    @Test
    fun rationale_isFalseWithoutAnActivityOrBelowApi33() {
        assertThat(notificationRationale(activity = null, sdkInt = 33)).isFalse()
        val unused: Activity? = null
        assertThat(notificationRationale(unused, sdkInt = 32)).isFalse()
    }

    @Test
    fun dialTicks_matchTheReferenceSvg() {
        assertThat(DialGeometry.ticks).hasSize(60)
        val first = DialGeometry.ticks.first()
        assertThat(first.major).isTrue()
        assertThat(first.x1).isWithin(0.05f).of(120f)
        assertThat(first.y1).isWithin(0.05f).of(20f)
        assertThat(first.x2).isWithin(0.05f).of(120f)
        assertThat(first.y2).isWithin(0.05f).of(6f)
    }

    @Test
    fun primaryMotion_matchesTheReferenceDurations() {
        assertThat(PrimaryMotion.ICON_MS).isEqualTo(200)
        assertThat(PrimaryMotion.PRESS_SCALE).isEqualTo(0.96f)
        assertThat(PrimaryMotion.HIDDEN_SCALE).isEqualTo(0.25f)
    }

    @Test
    fun ciWorkflow_runsKtlintAndDetektWithTheGradleChecks() {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        var file: File? = null
        repeat(6) {
            val candidate = File(dir, ".github/workflows/ci.yml")
            if (candidate.exists()) {
                file = candidate
                return@repeat
            }
            dir = dir.parentFile ?: return@repeat
        }
        val resolved = file ?: error("ci.yml not found from ${System.getProperty("user.dir")}")
        val yaml = resolved.readText()
        assertThat(yaml).contains("ktlintCheck")
        assertThat(yaml).contains("detekt")
        assertThat(yaml).contains("lint")
        assertThat(yaml).contains("testDebugUnitTest")
        assertThat(yaml).contains("assembleDebug")
    }
}
