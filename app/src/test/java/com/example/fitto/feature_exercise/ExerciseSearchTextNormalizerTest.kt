package com.example.fitto.feature_exercise

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseSearchTextNormalizerTest {

    @Test
    fun normalize_removesTurkishDiacriticsAndCollapsesSpaces() {
        assertEquals(
            "kosu yuruyusu",
            ExerciseSearchTextNormalizer.normalize("  Koşu   Yürüyüşü  ")
        )
    }

    @Test
    fun normalize_convertsDotlessTurkishI() {
        assertEquals(
            "agirlik",
            ExerciseSearchTextNormalizer.normalize("Ağırlık")
        )
    }
}
