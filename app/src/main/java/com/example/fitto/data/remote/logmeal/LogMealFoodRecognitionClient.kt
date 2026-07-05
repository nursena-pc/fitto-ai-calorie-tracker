package com.example.fitto.data.remote.logmeal

import com.example.fitto.data.remote.AndroidRemoteClientLogger
import com.example.fitto.data.remote.NoOpRemoteClientLogger
import com.example.fitto.data.remote.RemoteClientLogger
import java.io.File

class LogMealFoodRecognitionClient internal constructor(
    private val imagePreparer: LogMealImagePreparer,
    private val apiSource: LogMealApiSource,
    private val responseParser: LogMealResponseParser,
    private val logger: RemoteClientLogger = NoOpRemoteClientLogger
) : LogMealFoodRecognizer {

    constructor(apiUserToken: String) : this(
        imagePreparer = AndroidLogMealImagePreparer(),
        apiSource = OkHttpLogMealApiSource(apiUserToken),
        responseParser = LogMealResponseParser(),
        logger = AndroidRemoteClientLogger
    )

    override fun recognizeFood(
        imageFile: File,
        languageCode: String
    ): List<LogMealFoodCandidate> {
        if (!apiSource.isConfigured) {
            logger.error(TAG, "LogMeal token is blank. Add LOGMEAL_API_USER_TOKEN to local.properties.")
            return emptyList()
        }

        val uploadFile = imagePreparer.prepare(imageFile) ?: return emptyList()
        val segmentationBody = apiSource.uploadForSegmentation(
            uploadFile = uploadFile,
            languageCode = languageCode
        ) ?: return emptyList()
        val segmentation = responseParser.parseSegmentation(segmentationBody)

        logger.debug(TAG, "LogMeal imageId=${segmentation.imageId}")
        logger.debug(TAG, "LogMeal parsed candidates=${segmentation.candidates.size}")

        val imageId = segmentation.imageId
        if (imageId.isNullOrBlank()) {
            logger.error(TAG, "LogMeal imageId not found. Nutrition request skipped.")
            return segmentation.candidates
        }

        val nutritionBody = apiSource.fetchNutrition(imageId)
        if (nutritionBody == null) {
            logger.error(TAG, "LogMeal nutrition is null. Returning candidates without nutrition.")
            return segmentation.candidates
        }

        return runCatching {
            responseParser.mergeNutrition(
                candidates = segmentation.candidates,
                nutritionJson = nutritionBody
            )
        }.onFailure { throwable ->
            logger.error(TAG, "LogMeal nutrition parsing failed. Returning candidates without nutrition.", throwable)
        }.getOrDefault(segmentation.candidates)
    }

    private companion object {
        const val TAG = "LogMealClient"
    }
}
