package com.marcopt74.damelume.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcopt74.damelume.domain.model.TimerAction
import com.marcopt74.damelume.ui.timer.PrimaryControl
import com.marcopt74.damelume.ui.timer.TimerContent
import com.marcopt74.damelume.ui.timer.TimerStatus
import com.marcopt74.damelume.ui.timer.TimerUiState

/**
 * Debug-only host used by the rotation test. State lives in a retained [ViewModel].
 */
class TimerTestActivity : ComponentActivity() {
    lateinit var model: TimerHostViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[TimerHostViewModel::class.java]
        setContent {
            TimerContent(
                state = model.state,
                reducedMotion = true,
                onAction = model::onAction,
                onFrame = {},
            )
        }
    }
}

class TimerHostViewModel : ViewModel() {
    var state by mutableStateOf(TimerUiState())
        private set

    fun onAction(action: TimerAction) {
        if (action is TimerAction.Start) {
            state =
                state.copy(
                    isRunning = true,
                    primary = PrimaryControl.PAUSE,
                    status = TimerStatus.WORKING,
                )
        }
    }
}
