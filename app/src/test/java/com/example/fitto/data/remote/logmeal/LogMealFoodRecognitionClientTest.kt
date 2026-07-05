package com.example.fitto.data.remote.logmeal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LogMealFoodRecognitionClientTest {

    @Test
    fun recognizeFood_returnsEmptyWithoutPreparingWhenTokenIsMissing() {
        val preparer = FakeImagePreparer()
        val source = FakeApiSource(isConfigured = false)
        val client = client(preparer, source)

        val result = client.recognizeFood(File("source.jpg"), "tr")

        assertTrue(result.isEmpty())
        assertFalse(preparer.wasCalled)
        assertEquals(0, source.uploadCalls)
    }

    @Test
    fun recognizeFood_returnsCandidatesWithoutNutritionWhenImageIdIsMissing() {
        val preparer = FakeImagePreparer()
        val source = FakeApiSource(
            segmentationBody = "{\"name\":\"Apple\",\"confidence\":0.7}"
        )
        val client = client(preparer, source)

        val result = client.recognizeFood(File("source.jpg"), "tr")

        assertEquals(listOf("Apple"), result.map { it.name })
        assertEquals(0, source.nutritionCalls)
        assertEquals("tr", source.languageCode)
    }

    @Test
    fun recognizeFood_mergesNutritionWhenBothRequestsSucceed() {
        val source = FakeApiSource(
            segmentationBody = """
                {
                  "imageId":"image-1",
                  "segmentation_results":[
                    {"food_item_position":1,"name":"Pasta","confidence":0.8}
                  ]
                }
            """.trimIndent(),
            nutritionBody = """
                {
                  "nutritional_info_per_item":{
                    "1":{"serving_size":250,"calories":500,"protein":20,"carbs":70,"fat":12}
                  }
                }
            """.trimIndent()
        )
        val client = client(FakeImagePreparer(), source)

        val candidate = client.recognizeFood(File("source.jpg"), "eng").single()

        assertEquals("image-1", source.nutritionImageId)
        assertEquals(250.0, candidate.servingGrams!!, 0.0001)
        assertEquals(500.0, candidate.calories!!, 0.0001)
        assertEquals(20.0, candidate.protein!!, 0.0001)
    }

    @Test
    fun recognizeFood_keepsCandidatesWhenNutritionResponseIsMalformed() {
        val source = FakeApiSource(
            segmentationBody = "{\"imageId\":\"image-1\",\"name\":\"Apple\",\"confidence\":0.7}",
            nutritionBody = "not-json"
        )
        val client = client(FakeImagePreparer(), source)

        val candidate = client.recognizeFood(File("source.jpg"), "eng").single()

        assertEquals("Apple", candidate.name)
        assertEquals(null, candidate.calories)
    }

    private fun client(
        preparer: LogMealImagePreparer,
        source: LogMealApiSource
    ): LogMealFoodRecognitionClient {
        return LogMealFoodRecognitionClient(
            imagePreparer = preparer,
            apiSource = source,
            responseParser = LogMealResponseParser()
        )
    }

    private class FakeImagePreparer : LogMealImagePreparer {
        var wasCalled: Boolean = false

        override fun prepare(sourceFile: File): File {
            wasCalled = true
            return File("prepared.jpg")
        }
    }

    private class FakeApiSource(
        override val isConfigured: Boolean = true,
        private val segmentationBody: String? = null,
        private val nutritionBody: String? = null
    ) : LogMealApiSource {
        var uploadCalls: Int = 0
        var nutritionCalls: Int = 0
        var languageCode: String? = null
        var nutritionImageId: String? = null

        override fun uploadForSegmentation(uploadFile: File, languageCode: String): String? {
            uploadCalls += 1
            this.languageCode = languageCode
            return segmentationBody
        }

        override fun fetchNutrition(imageId: String): String? {
            nutritionCalls += 1
            nutritionImageId = imageId
            return nutritionBody
        }
    }
}
