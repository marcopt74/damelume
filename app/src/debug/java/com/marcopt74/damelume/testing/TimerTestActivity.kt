package com.marcopt74.damelume.testing

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcopt74.damelume.data.TimerRepository
import com.marcopt74.damelume.domain.Clock
import com.marcopt74.damelume.system.notify.DevicePermissions
import com.marcopt74.damelume.system.sound.ForegroundTracker
import com.marcopt74.damelume.ui.timer.TimerScreen
import com.marcopt74.damelume.ui.timer.TimerViewModel

/**
 * Debug host for the rotation test. It installs the real [TimerViewModel]
 * from dependencies supplied by the test before launch.
 */
class TimerTestActivity : AppCompatActivity() {
    lateinit var viewModel: TimerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, TimerRotationHost.factory())[TimerViewModel::class.java]
        setContent {
            TimerScreen(viewModel = viewModel)
        }
    }
}

object TimerRotationHost {
    lateinit var repository: TimerRepository
    lateinit var clock: Clock
    lateinit var foreground: ForegroundTracker
    lateinit var permissions: DevicePermissions

    fun factory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TimerViewModel(repository, clock, foreground, permissions) as T
        }
}
