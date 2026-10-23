package com.marcopt74.damelume.domain.model

data class EngineResult(
    val state: TimerState,
    val session: SessionDraft? = null,
    val playChime: Boolean = false,
    val alarm: AlarmCommand = AlarmCommand.Unchanged,
    val ongoing: OngoingCommand = OngoingCommand.Unchanged,
    val sessionsResetAtEpochMs: Long? = null,
)

sealed interface AlarmCommand {
    data object Unchanged : AlarmCommand

    data object Cancel : AlarmCommand

    data class Schedule(
        val endsAtEpochMs: Long,
    ) : AlarmCommand
}

sealed interface OngoingCommand {
    data object Unchanged : OngoingCommand

    data object Cancel : OngoingCommand

    data object Update : OngoingCommand
}
