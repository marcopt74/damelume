package com.marcopt74.damelume.system.alarm

interface AlarmScheduler {
    fun schedule(endsAtEpochMs: Long)

    fun cancel(endsAtEpochMs: Long?)
}
