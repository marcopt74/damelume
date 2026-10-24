package com.marcopt74.damelume.system.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import com.marcopt74.damelume.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidChimePlayer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ChimePlayer {
        private var current: MediaPlayer? = null

        override fun play() {
            val player = MediaPlayer()
            val previous =
                synchronized(this) {
                    val old = current
                    current = player
                    old
                }
            previous?.runCatching { release() }
            try {
                context.resources.openRawResourceFd(R.raw.lume_chime).use { descriptor ->
                    player.setAudioAttributes(ALARM)
                    player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    player.setOnCompletionListener { release(it) }
                    player.setOnErrorListener { failed, _, _ ->
                        release(failed)
                        true
                    }
                    player.prepare()
                }
                player.start()
            } catch (_: Exception) {
                release(player)
            }
        }

        private fun release(player: MediaPlayer) {
            val owned =
                synchronized(this) {
                    val matches = current === player
                    if (matches) current = null
                    matches
                }
            if (owned) player.runCatching { release() }
        }

        private companion object {
            val ALARM: AudioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
        }
    }
