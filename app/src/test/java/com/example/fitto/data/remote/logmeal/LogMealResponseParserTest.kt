package com.example.fitto.data.remote.logmeal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogMealResponseParserTest {

    private val parser = LogMealResponseParser()

    @Test
    fun parseSegmentation_findsNestedImageIdDeduplicatesAndSortsCandidates() {
        val result = parser.parseSegmentation(
            """
            {
              "metadata":{"image_id":"image-42"},
              "segmentation_results":[
                {
                  "food_item_position":1,
                  "recognition_results":[
                    {"name":"Pasta","probability":0.70},
                    {"name":"PASTA","probability":0.40}
                  ]
                },
                {
                  "food_item_position":2,
                  "recognition_results":[
                    {"food_name":"Salad","confidence":0.90,"serving_size":120}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("image-42", result.imageId)
        assertEquals(listOf("Salad", "Pasta"), result.candidates.map { it.name })
        assertEquals(listOf(2, 1), result.candidates.map { it.foodItemPosition })
        assertEquals(120.0, result.candidates.first().servingGrams!!, 0.0001)
    }

    @Test
    fun parseSegmentation_readsEmbeddedNutritionValues() {
        val candidate = parser.parseSegmentation(
            """
            {
              "imageId":"image-1",
              "name":"Soup",
              "score":0.8,
              "nutrition":{
                "quantity_g":250,
                "energy":{"value":836.8,"unit":"kJ"},
                "protein":12,
                "carbohydrate":"30,5",
                "fat":7,
                "fiber":4
              }
            }
            """.trimIndent()
        ).candidates.single()

        assertEquals(250.0, candidate.servingGrams!!, 0.0001)
        assertEquals(200.0, candidate.calories!!, 0.0001)
        assertEquals(12.0, candidate.protein!!, 0.0001)
        assertEquals(30.5, candidate.carbs!!, 0.0001)
        assertEquals(7.0, candidate.fat!!, 0.0001)
        assertEquals(4.0, candidate.fiber!!, 0.0001)
    }

    @Test
    fun mergeNutrition_prefersPerItemAndUsesOverallForFirstUnmatchedCandidate() {
        val candidates = listOf(
            candidate("Pasta", position = 1),
            candidate("Salad", position = 2),
            candidate("Bread", position = 3)
        )

        val merged = parser.mergeNutrition(
            candidates = candidates,
            nutritionJson = """
                {
                  "serving_size":100,
                  "calories":300,
                  "protein":9,
                  "nutritional_info_per_item":{
                    "2":{
                      "servingSize":180,
                      "calories":220,
                      "protein":6,
                      "carbs":25,
                      "fat":8,
                      "fiber":3
                    }
                  }
                }
            """.trimIndent()
        )

        assertEquals(100.0, merged[0].servingGrams!!, 0.0001)
        assertEquals(300.0, merged[0].calories!!, 0.0001)
        assertEquals(180.0, merged[1].servingGrams!!, 0.0001)
        assertEquals(220.0, merged[1].calories!!, 0.0001)
        assertNull(merged[2].calories)
    }

    @Test
    fun mergeNutrition_keepsCandidateWhenNutritionHasNoMacroValues() {
        val original = candidate("Apple", position = 1).copy(servingGrams = 80.0)

        val merged = parser.mergeNutrition(
            candidates = listOf(original),
            nutritionJson = "{\"serving_size\":150}"
        )

        assertEquals(original, merged.single())
    }

    private fun candidate(name: String, position: Int): LogMealFoodCandidate {
        return LogMealFoodCandidate(
            name = name,
            confidence = 0.8,
            foodItemPosition = position,
            servingGrams = null,
            calories = null,
            protein = null,
            carbs = null,
            fat = null,
            fiber = null
        )
    }
}
