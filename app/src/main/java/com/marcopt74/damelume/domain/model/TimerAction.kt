package com.marcopt74.damelume.domain.model

sealed interface TimerAction {
    data object Start : TimerAction

    data object Pause : TimerAction

    data object Reset : TimerAction

    data object SkipBreak : TimerAction

    data class Complete(
        val expectedEndsAt: Long? = null,
    ) : TimerAction

    data object ResetSessions : TimerAction

    data class SetWorkMinutes(
        val minutes: Int,
    ) : TimerAction

    data class SetBreakMinutes(
        val minutes: Int,
    ) : TimerAction

    data class SetLongBreakMinutes(
        val minutes: Int,
    ) : TimerAction

    data object Reconcile : TimerAction

    data class SetSoundEnabled(
        val enabled: Boolean,
    ) : TimerAction

    data class SetLocale(
        val locale: AppLocale,
    ) : TimerAction

    data object MarkNotificationAsked : TimerAction

    data object DismissNotificationBanner : TimerAction
}
