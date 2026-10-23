package com.marcopt74.damelume.system.locale

import com.marcopt74.damelume.domain.model.AppLocale

interface LocaleApplicator {
    fun apply(locale: AppLocale)

    fun current(): AppLocale?

    fun initialFromSystem(): AppLocale
}
