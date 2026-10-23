package com.marcopt74.damelume.domain.model

import java.time.LocalDate

data class SessionDraft(
    val mode: Mode,
    val plannedMs: Long,
    val endedAtEpochMs: Long,
    val localDate: LocalDate,
    val lateCompletion: Boolean,
)
