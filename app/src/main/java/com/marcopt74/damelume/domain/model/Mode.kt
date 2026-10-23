package com.marcopt74.damelume.domain.model

enum class Mode {
    WORK,
    BREAK,
    LONG_BREAK,
    ;

    fun wireValue(): String =
        when (this) {
            WORK -> "work"
            BREAK -> "break"
            LONG_BREAK -> "long_break"
        }

    companion object {
        fun fromWire(value: String?): Mode =
            when (value) {
                "break" -> BREAK
                "long_break" -> LONG_BREAK
                else -> WORK
            }
    }
}
