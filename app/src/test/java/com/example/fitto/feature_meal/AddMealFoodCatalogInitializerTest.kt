package com.example.fitto.feature_meal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AddMealFoodCatalogInitializerTest {

    @Test
    fun `food seed is imported only once after successful initialization`() = runBlocking {
        var importCount = 0
        val initializer = AddMealFoodCatalogInitializer {
            importCount += 1
        }

        initializer.ensureInitialized()
        initializer.ensureInitialized()
        initializer.ensureInitialized()

        assertEquals(1, importCount)
    }

    @Test
    fun `failed initialization is retried on the next request`() = runBlocking {
        val expectedError = IllegalStateException("seed import failed")
        var importCount = 0
        val initializer = AddMealFoodCatalogInitializer {
            importCount += 1
            if (importCount == 1) throw expectedError
        }

        val firstError = runCatching {
            initializer.ensureInitialized()
        }.exceptionOrNull()
        initializer.ensureInitialized()
        initializer.ensureInitialized()

        assertSame(expectedError, firstError)
        assertEquals(2, importCount)
    }
}
