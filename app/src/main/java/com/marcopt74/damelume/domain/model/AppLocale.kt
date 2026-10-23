package com.marcopt74.damelume.domain.model

enum class AppLocale {
    PT,
    EN,
    ;

    fun languageTag(): String =
        when (this) {
            PT -> "pt-PT"
            EN -> "en"
        }

    companion object {
        fun fromWire(value: String?): AppLocale? =
            when (value) {
                "pt" -> PT
                "en" -> EN
                else -> null
            }

        fun fromLanguage(language: String?): AppLocale = if (language != null && language.startsWith("pt")) PT else EN
    }
}
