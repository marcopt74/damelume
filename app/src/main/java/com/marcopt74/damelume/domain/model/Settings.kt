package com.marcopt74.damelume.domain.model

data class Settings(
    val soundEnabled: Boolean = true,
    val locale: AppLocale? = null,
)
