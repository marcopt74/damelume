package com.marcopt74.damelume.system.locale

import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.marcopt74.damelume.domain.model.AppLocale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLocaleApplicator
    @Inject
    constructor() : LocaleApplicator {
        override fun apply(locale: AppLocale) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale.languageTag()))
        }

        override fun current(): AppLocale? {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return null
            val language = locales[0]?.language ?: return null
            return AppLocale.fromLanguage(language)
        }

        override fun initialFromSystem(): AppLocale {
            val primary = LocaleList.getDefault().get(0)
            return AppLocale.fromLanguage(primary?.language)
        }
    }
