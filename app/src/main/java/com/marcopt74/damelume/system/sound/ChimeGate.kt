package com.marcopt74.damelume.system.sound

/**
 * SoundPool loads asynchronously. A completion that arrives first is queued
 * and played from the load callback. Later calls play immediately.
 */
internal class ChimeGate {
    private var loaded = false
    private var queued = false

    @Synchronized
    fun shouldPlayNow(): Boolean {
        if (loaded) return true
        queued = true
        return false
    }

    @Synchronized
    fun onLoadComplete(success: Boolean): Boolean {
        if (!success) {
            queued = false
            return false
        }
        loaded = true
        val play = queued
        queued = false
        return play
    }

    @Synchronized
    fun reset() {
        loaded = false
        queued = false
    }
}
