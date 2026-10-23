package com.marcopt74.damelume.system.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.marcopt74.damelume.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidChimePlayer
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ChimePlayer {
        private val pool =
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

        private val soundId = pool.load(context, R.raw.lume_chime, 1)

        override fun play() {
            if (soundId == 0) return
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
