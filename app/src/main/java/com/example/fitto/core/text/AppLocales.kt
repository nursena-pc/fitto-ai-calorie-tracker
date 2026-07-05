package com.example.fitto.core.text

import java.util.Locale

object AppLocales {
    val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

    fun fromLanguageTag(languageTag: String): Locale {
        return Locale.forLanguageTag(languageTag)
    }

    fun forRegion(countryCode: String): Locale {
        return Locale.Builder()
            .setRegion(countryCode)
            .build()
    }
}
