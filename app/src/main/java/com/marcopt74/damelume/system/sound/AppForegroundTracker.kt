package com.marcopt74.damelume.system.sound

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppForegroundTracker
    @Inject
    constructor() : ForegroundTracker {
        private val foreground = MutableStateFlow(false)

        override val isForeground: StateFlow<Boolean> = foreground.asStateFlow()

        override fun isInForeground(): Boolean = foreground.value

        fun start() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        foreground.value = true
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        foreground.value = false
                    }
                },
            )
        }
    }

interface ForegroundTracker {
    val isForeground: StateFlow<Boolean>

    fun isInForeground(): Boolean
}
