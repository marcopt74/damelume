package com.marcopt74.damelume.ui.timer

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.data.db.LumeDatabase
import com.marcopt74.damelume.data.prefs.LumePreferencesDataSource
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.system.alarm.ExactAlarmCapability
import com.marcopt74.damelume.system.notify.DevicePermissions
import com.marcopt74.damelume.testutil.FakeForeground
import com.marcopt74.damelume.testutil.FakeLocaleApplicator
import com.marcopt74.damelume.testutil.MutableClock
import com.marcopt74.damelume.testutil.RecordingAlarmScheduler
import com.marcopt74.damelume.testutil.RecordingChime
import com.marcopt74.damelume.testutil.RecordingNotifier
import com.marcopt74.damelume.testutil.quietAppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TimerTickerTest {
    private val scheduler = TestCoroutineScheduler()
    private val main = UnconfinedTestDispatcher(scheduler)
    private val appScope = quietAppScope()
    private lateinit var database: LumeDatabase
    private lateinit var clock: MutableClock
    private lateinit var viewModel: TimerViewModel
    private lateinit var store: ViewModelStore
    private val collector = CoroutineScope(SupervisorJob() + main)
    private var ui = TimerUiState()

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, LumeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val file = File(context.filesDir, "ticker-${System.nanoTime()}.preferences_pb")
        val preferences = LumePreferencesDataSource(PreferenceDataStoreFactory.create { file })
        clock = MutableClock()
        val repository =
            TimerRepository(
                preferences = preferences,
                dao = database.completedSessionDao(),
                clock = clock,
                alarmScheduler = RecordingAlarmScheduler(),
                notifier = RecordingNotifier(),
                chimePlayer = RecordingChime(),
                foregroundTracker = FakeForeground(foreground = true),
                localeApplicator = FakeLocaleApplicator(),
                scope = appScope,
            )
        store = ViewModelStore()
        viewModel =
            ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return TimerViewModel(
                            repository,
                            clock,
                            FakeForeground(foreground = true),
                            object : DevicePermissions {
                                override fun sdkInt(): Int = 33

                                override fun notificationsGranted(): Boolean = true

                                override fun exactAlarmCapability(): ExactAlarmCapability = ExactAlarmCapability(33, true)
                            },
                        ) as T
                    }
                },
            )[TimerViewModel::class.java]
        collector.launch { viewModel.uiState.collect { ui = it } }
        scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        store.clear()
        collector.cancel()
        appScope.cancel()
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun ticker_runsOnlyWhileTheTimerIsRunning_andOnFrameReadsTheClock() {
        waitUntil { ui.timeText == "25:00" }
        assertThat(viewModel.isTicking).isFalse()

        viewModel.onAction(TimerAction.Start)
        waitUntil { ui.isRunning && viewModel.isTicking }
        assertThat(viewModel.isTicking).isTrue()

        viewModel.onAction(TimerAction.Pause)
        waitUntil { !ui.isRunning && !viewModel.isTicking }
        assertThat(viewModel.isTicking).isFalse()

        viewModel.onAction(TimerAction.Start)
        waitUntil { ui.isRunning && ui.endsAtEpochMs != null }
        clock.now = ui.endsAtEpochMs!! - 60_000
        viewModel.onFrame()
        scheduler.runCurrent()
        assertThat(ui.timeText).isEqualTo("01:00")
    }

    private fun waitUntil(predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!predicate()) {
            scheduler.runCurrent()
            if (System.currentTimeMillis() > deadline) {
                error("Timed out waiting for timer state $ui ticking=${viewModel.isTicking}")
            }
            Thread.sleep(10)
        }
    }
}
