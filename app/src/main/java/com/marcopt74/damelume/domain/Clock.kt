package com.marcopt74.damelume.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface Clock {
    fun nowMillis(): Long

    fun zone(): ZoneId

    fun today(): LocalDate = Instant.ofEpochMilli(nowMillis()).atZone(zone()).toLocalDate()
}

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
