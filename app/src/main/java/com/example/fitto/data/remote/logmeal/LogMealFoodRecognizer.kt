package com.example.fitto.data.remote.logmeal

import java.io.File

interface LogMealFoodRecognizer {
    fun recognizeFood(
        imageFile: File,
        languageCode: String
    ): List<LogMealFoodCandidate>
}
