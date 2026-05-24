package com.example.fitto.feature_recipes.util

import java.util.Locale

fun String.normalizeRecipeText(): String {
    return trim()
        .lowercase(Locale("tr", "TR"))
        .replace("ı", "i")
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ş", "s")
        .replace("ö", "o")
        .replace("ç", "c")
}
