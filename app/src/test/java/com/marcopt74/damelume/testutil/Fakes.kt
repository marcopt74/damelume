package com.marcopt74.damelume.testutil

import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.domain.model.AppLocale
import com.marcopt74.damelume.domain.model.Mode
import com.marcopt74.damelume.domain.model.TimerState
import com.marcopt74.damelume.system.alarm.AlarmScheduler
import com.marcopt74.damelume.system.locale.LocaleApplicator
import com.marcopt74.damelume.system.notify.TimerNotifier
import com.marcopt74.damelume.system.sound.ChimePlayer
import com.marcopt74.damelume.system.sound.ForegroundTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

class MutableClock(
    var now: Long = 1_700_000_000_000L,
    private val zoneId: ZoneId = ZoneId.of("Europe/Lisbon"),
) : Clock {
    override fun nowMillis(): Long = now

    override fun zone(): ZoneId = zoneId
}

class RecordingAlarmScheduler : AlarmScheduler {
    val scheduled = mutableListOf<Long>()
    val cancelled = mutableListOf<Long?>()

    override fun schedule(endsAtEpochMs: Long) {
        scheduled += endsAtEpochMs
    }

    override fun cancel(endsAtEpochMs: Long?) {
        cancelled += endsAtEpochMs
    }
}

data class CompletionCall(
    val finished: Mode,
    val next: Mode,
    val nextMinutes: Int,
    val soundEnabled: Boolean,
)

class RecordingNotifier : TimerNotifier {
    var ongoing: TimerState? = null
    var cancelCount: Int = 0
    val completions = mutableListOf<CompletionCall>()

    override fun showOngoing(state: TimerState) {
        ongoing = state
    }

    override fun cancelOngoing() {
        cancelCount += 1
        ongoing = null
    }

    override fun showCompletion(
        finished: Mode,
        next: Mode,
        nextMinutes: Int,
        soundEnabled: Boolean,
    ) {
        completions += CompletionCall(finished, next, nextMinutes, soundEnabled)
    }
}

class RecordingChime : ChimePlayer {
    var plays: Int = 0

    override fun play() {
        plays += 1
    }
}

class FakeForeground(
    foreground: Boolean = true,
) : ForegroundTracker {
    var foreground: Boolean = foreground
    private val flow = MutableStateFlow(foreground)
    override val isForeground: StateFlow<Boolean> = flow

    override fun isInForeground(): Boolean = foreground
}

class FakeLocaleApplicator(
    private val system: AppLocale = AppLocale.EN,
) : LocaleApplicator {
    var applied: AppLocale? = null
    private var stored: AppLocale? = null

    override fun apply(locale: AppLocale) {
        applied = locale
        stored = locale
    }

    override fun current(): AppLocale? = stored

    override fun initialFromSystem(): AppLocale = system
}
