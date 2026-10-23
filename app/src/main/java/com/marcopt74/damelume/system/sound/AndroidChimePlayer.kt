package com.marcopt74.damelume.system.sound

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.SoundPool
import com.marcopt74.damelume.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidChimePlayer
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ChimePlayer,
        Closeable {
        private val appContext = context.applicationContext
        private val gate = ChimeGate()
        private var pool: SoundPool? = null
        private var soundId: Int = 0

        private val callbacks =
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) release()
                }

                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                override fun onLowMemory() = release()
            }

        init {
            appContext.registerComponentCallbacks(callbacks)
            load()
        }

        override fun play() {
            val current = pool
            if (current == null) {
                load()
                gate.shouldPlayNow()
                return
            }
            if (gate.shouldPlayNow() && soundId != 0) {
                current.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }

        override fun close() {
            release()
            appContext.unregisterComponentCallbacks(callbacks)
        }

        fun release() {
            gate.reset()
            pool?.release()
            pool = null
            soundId = 0
        }

        private fun load() {
            val created =
                SoundPool
                    .Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    ).build()
            pool = created
            created.setOnLoadCompleteListener { soundPool, sampleId, status ->
                if (sampleId == soundId && gate.onLoadComplete(status == 0)) {
                    soundPool.play(sampleId, 1f, 1f, 1, 0, 1f)
                }
            }
            soundId = created.load(appContext, R.raw.lume_chime, 1)
        }
    }
