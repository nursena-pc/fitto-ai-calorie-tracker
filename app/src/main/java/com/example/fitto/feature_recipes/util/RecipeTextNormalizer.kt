package com.example.fitto.feature_recipes.util

import com.example.fitto.core.text.AppLocales
import java.util.Locale

fun String.normalizeRecipeText(): String {
    return trim()
        .lowercase(AppLocales.TURKISH)
        .replace("ı", "i")
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ş", "s")
        .replace("ö", "o")
        .replace("ç", "c")
}
