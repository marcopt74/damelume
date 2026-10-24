package com.marcopt74.damelume.domain.model

enum class Mode {
    WORK,
    BREAK,
    ;

    fun wireValue(): String =
        when (this) {
            WORK -> "work"
            BREAK -> "break"
        }

    companion object {
        fun fromWire(value: String?): Mode =
            when (value) {
                "break" -> BREAK
                else -> WORK
            }
    }
}
