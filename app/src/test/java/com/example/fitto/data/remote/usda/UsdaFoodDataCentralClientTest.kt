package com.example.fitto.data.remote.usda

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaFoodDataCentralClientTest {

    @Test
    fun searchFoods_rejectsShortQueryWithoutCallingTransport() {
        val transport = FakeTransport("{}")
        val client = UsdaFoodDataCentralClient(transport, UsdaFoodResponseParser())

        val foods = client.searchFoods(" ap ")

        assertTrue(foods.isEmpty())
        assertEquals(0, transport.calls)
    }

    @Test
    fun searchFoods_trimsQueryAndForwardsPaging() {
        val transport = FakeTransport("{\"foods\":[]}")
        val client = UsdaFoodDataCentralClient(transport, UsdaFoodResponseParser())

        client.searchFoods(" apple ", pageSize = 12, pageNumber = 3)

        assertEquals("apple", transport.query)
        assertEquals(12, transport.pageSize)
        assertEquals(3, transport.pageNumber)
    }

    @Test
    fun searchFoods_returnsEmptyListForMalformedResponse() {
        val client = UsdaFoodDataCentralClient(
            FakeTransport("not-json"),
            UsdaFoodResponseParser()
        )

        assertTrue(client.searchFoods("apple").isEmpty())
    }

    private class FakeTransport(
        private val response: String?
    ) : UsdaFoodSearchTransport {
        var calls: Int = 0
        var query: String? = null
        var pageSize: Int? = null
        var pageNumber: Int? = null

        override fun search(query: String, pageSize: Int, pageNumber: Int): String? {
            calls += 1
            this.query = query
            this.pageSize = pageSize
            this.pageNumber = pageNumber
            return response
        }
    }
}
