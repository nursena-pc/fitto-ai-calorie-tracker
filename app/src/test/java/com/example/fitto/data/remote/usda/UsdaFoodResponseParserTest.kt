package com.example.fitto.data.remote.usda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsdaFoodResponseParserTest {

    private val parser = UsdaFoodResponseParser()

    @Test
    fun parse_mapsKcalMacrosAndOptionalFiber() {
        val products = parser.parse(
            """
            {
              "foods": [
                {
                  "fdcId": 123,
                  "description": "GREEK YOGURT",
                  "dataType": "Branded",
                  "brandOwner": "Fit Brand",
                  "foodNutrients": [
                    {"nutrientNumber":"1008","nutrientName":"Energy","unitName":"KCAL","value":120},
                    {"nutrientNumber":"1003","nutrientName":"Protein","unitName":"G","value":"10,5"},
                    {"nutrientNumber":"1004","nutrientName":"Total lipid (fat)","unitName":"G","value":3.2},
                    {"nutrientNumber":"1005","nutrientName":"Carbohydrate","unitName":"G","value":12},
                    {"nutrientNumber":"1079","nutrientName":"Fiber","unitName":"G","value":1.4}
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val product = products.single()
        assertEquals(123L, product.fdcId)
        assertEquals("Greek yogurt", product.description)
        assertEquals("Fit Brand", product.brandOwner)
        assertEquals(120.0, product.caloriesPer100g, 0.0001)
        assertEquals(10.5, product.proteinPer100g, 0.0001)
        assertEquals(3.2, product.fatPer100g, 0.0001)
        assertEquals(12.0, product.carbsPer100g, 0.0001)
        assertEquals(1.4, product.fiberPer100g!!, 0.0001)
    }

    @Test
    fun parse_convertsKilojoulesAndUsesNutrientNamesAsFallback() {
        val product = parser.parse(
            """
            {
              "foods": [
                {
                  "fdcId": 456,
                  "description": "APPLE",
                  "dataType": "Foundation",
                  "foodNutrients": [
                    {"nutrientName":"Energy","unitName":"KJ","value":418.4},
                    {"nutrientName":"Protein","unitName":"G","value":1},
                    {"nutrientName":"Fat","unitName":"G","value":2},
                    {"nutrientName":"Carbohydrate","unitName":"G","value":20}
                  ]
                }
              ]
            }
            """.trimIndent()
        ).single()

        assertEquals(100.0, product.caloriesPer100g, 0.0001)
        assertEquals(1.0, product.proteinPer100g, 0.0001)
        assertNull(product.fiberPer100g)
    }

    @Test
    fun parse_skipsInvalidIdentityAndFoodsWithoutCalories() {
        val products = parser.parse(
            """
            {
              "foods": [
                {"fdcId":0,"description":"Invalid","foodNutrients":[]},
                {"fdcId":2,"description":"","foodNutrients":[]},
                {"fdcId":3,"description":"No energy","foodNutrients":[{"nutrientName":"Protein","value":5}]}
              ]
            }
            """.trimIndent()
        )

        assertTrue(products.isEmpty())
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }
}
