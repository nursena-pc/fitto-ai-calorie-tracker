package com.example.fitto.feature_exercise

import java.text.Normalizer
import java.util.Locale

object ExerciseSearchTextNormalizer {

    fun normalize(value: String): String {
        val fixedTurkish = value
            .lowercase(TURKISH_LOCALE)
            .replace("ı", "i")

        return Normalizer.normalize(fixedTurkish, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private val TURKISH_LOCALE = Locale.forLanguageTag("tr-TR")
}
