package com.example.fitto.feature_meal

import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddMealScreenStateStoreTest {

    @Test
    fun `search result stores foods and active online paging`() {
        val store = AddMealScreenStateStore()
        val token = store.beginFoodContentLoad()
        val foods = listOf(food("apple"))

        val applied = store.applySearchResult(
            token = token,
            result = searchResult(
                foods = foods,
                pagingState = AddMealOnlinePagingState.Active(
                    query = "apple",
                    nextPageNumber = 2,
                    canLoadMore = true
                )
            )
        )

        assertTrue(applied)
        assertEquals(foods, store.current().currentFoodResults)
        assertEquals(
            AddMealOnlinePagingScreenState(
                query = "apple",
                nextPageNumber = 2,
                canLoadMore = true
            ),
            store.current().onlinePaging
        )
    }

    @Test
    fun `new food content generation rejects stale search result`() {
        val store = AddMealScreenStateStore()
        val staleToken = store.beginFoodContentLoad()
        store.beginFoodContentLoad()

        val applied = store.applySearchResult(
            token = staleToken,
            result = searchResult(foods = listOf(food("stale")))
        )

        assertFalse(applied)
        assertTrue(store.current().currentFoodResults.isEmpty())
    }

    @Test
    fun `search reset clears previous online paging`() {
        val store = activePagingStore()
        val resetToken = store.beginFoodContentLoad()

        store.applySearchResult(
            token = resetToken,
            result = searchResult(
                foods = listOf(food("local")),
                pagingState = AddMealOnlinePagingState.Reset
            )
        )

        assertEquals(AddMealOnlinePagingScreenState(), store.current().onlinePaging)
        assertEquals(listOf("local"), store.current().currentFoodResults.map { it.id })
    }

    @Test
    fun `begin load more captures paging snapshot and renders loading state`() {
        val store = activePagingStore()

        val token = store.beginLoadMore()

        requireNotNull(token)
        assertEquals("apple", token.query)
        assertEquals(2, token.currentPage)
        assertEquals(listOf("page-1"), token.currentFoods.map { it.id })
        assertTrue(store.current().onlinePaging.isLoadingMore)
        assertEquals(
            AddMealLoadMoreUiState(visible = true, enabled = false),
            store.loadMoreUiState()
        )
        assertNull(store.beginLoadMore())
    }

    @Test
    fun `loaded page updates foods next page and load more state`() {
        val store = activePagingStore()
        val token = requireNotNull(store.beginLoadMore())
        val foods = listOf(food("page-1"), food("page-2"))

        val applied = store.applyLoadMoreResult(
            token = token,
            result = AddMealLoadMoreActionResult.Loaded(
                foods = foods,
                nextPageNumber = 3,
                canLoadMore = false
            )
        )

        assertTrue(applied)
        assertEquals(foods, store.current().currentFoodResults)
        assertEquals(3, store.current().onlinePaging.nextPageNumber)
        assertFalse(store.current().onlinePaging.canLoadMore)
        assertFalse(store.current().onlinePaging.isLoadingMore)
        assertEquals(
            AddMealLoadMoreUiState(visible = false, enabled = true),
            store.loadMoreUiState()
        )
    }

    @Test
    fun `no more foods stops loading and disables further paging`() {
        val store = activePagingStore()
        val token = requireNotNull(store.beginLoadMore())

        val applied = store.applyLoadMoreResult(
            token = token,
            result = AddMealLoadMoreActionResult.NoMoreFoods(canLoadMore = false)
        )

        assertTrue(applied)
        assertFalse(store.current().onlinePaging.isLoadingMore)
        assertFalse(store.current().onlinePaging.canLoadMore)
        assertNull(store.beginLoadMore())
    }

    @Test
    fun `stale load more result cannot replace newer favorite content`() {
        val store = activePagingStore()
        val staleToken = requireNotNull(store.beginLoadMore())
        val favoritesToken = store.beginFoodContentLoad()
        val favorite = food("favorite")
        store.applyFavoriteFoods(favoritesToken, listOf(favorite))

        val applied = store.applyLoadMoreResult(
            token = staleToken,
            result = AddMealLoadMoreActionResult.Loaded(
                foods = listOf(food("page-1"), food("stale-page-2")),
                nextPageNumber = 3,
                canLoadMore = true
            )
        )

        assertFalse(applied)
        assertEquals(listOf(favorite), store.current().currentFoodResults)
        assertEquals(AddMealOnlinePagingScreenState(), store.current().onlinePaging)
    }

    @Test
    fun `favorite content replaces results and removes online paging`() {
        val store = activePagingStore()
        val token = store.beginFoodContentLoad()
        val favorites = listOf(food("favorite-one"), food("favorite-two"))

        val applied = store.applyFavoriteFoods(token, favorites)

        assertTrue(applied)
        assertEquals(favorites, store.current().currentFoodResults)
        assertEquals(AddMealOnlinePagingScreenState(), store.current().onlinePaging)
        assertEquals(
            AddMealLoadMoreUiState(visible = false, enabled = true),
            store.loadMoreUiState()
        )
    }

    @Test
    fun `selection state is preserved while save prevents duplicate submission`() {
        val store = AddMealScreenStateStore()
        val selected = selectedFood("manual")
        store.addSelectedFood(selected)

        val firstSave = store.beginSave()
        val duplicateSave = store.beginSave()

        assertEquals(listOf(selected), firstSave?.selectedFoods)
        assertNull(duplicateSave)
        assertTrue(store.current().isSaving)
        assertFalse(store.selectionUiState().saveButtonEnabled)
        assertEquals(0.65f, store.selectionUiState().saveButtonAlpha)

        store.finishSave()

        assertFalse(store.current().isSaving)
        assertTrue(store.selectionUiState().saveButtonEnabled)
        assertEquals(1f, store.selectionUiState().saveButtonAlpha)
    }

    @Test
    fun `detected foods append and selected row removal uses selection resolver`() {
        val store = AddMealScreenStateStore()
        val manual = selectedFood("manual")
        val detectedOne = selectedFood("detected-one")
        val detectedTwo = selectedFood("detected-two")
        store.addSelectedFood(manual)
        store.addDetectedFoods(listOf(detectedOne, detectedTwo))

        store.removeSelectedFood(detectedOne)

        assertEquals(
            listOf("manual", "detected-two"),
            store.current().selectedFoods.map { it.id }
        )
        assertEquals(2, store.selectionUiState().selectedCount)
    }

    private fun activePagingStore(): AddMealScreenStateStore {
        return AddMealScreenStateStore().apply {
            val token = beginFoodContentLoad()
            applySearchResult(
                token = token,
                result = searchResult(
                    foods = listOf(food("page-1")),
                    pagingState = AddMealOnlinePagingState.Active(
                        query = "apple",
                        nextPageNumber = 2,
                        canLoadMore = true
                    )
                )
            )
        }
    }

    private fun searchResult(
        foods: List<FoodEntity>,
        pagingState: AddMealOnlinePagingState = AddMealOnlinePagingState.Keep
    ): AddMealFoodSearchResult {
        return AddMealFoodSearchResult(
            foods = foods,
            section = AddMealFoodSection.RESULTS,
            emptyFeedback = AddMealEmptySearchFeedback.NONE,
            pagingState = pagingState
        )
    }

    private fun food(id: String): FoodEntity {
        return FoodEntity(
            id = id,
            nameTr = id,
            nameEn = id,
            category = "test",
            defaultUnit = "g",
            defaultServingAmount = 100.0,
            caloriesPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 15.0,
            fatPer100g = 2.0,
            fiberPer100g = 1.0,
            source = "test",
            isVerified = true,
            allergens = "",
            dietTags = "",
            mealTypes = "breakfast",
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun selectedFood(id: String): SelectedFoodUiModel {
        return SelectedFoodUiModel(
            id = id,
            foodId = "food-$id",
            name = id,
            amountLabel = "100 g",
            amountGrams = 100.0,
            calories = 100,
            protein = 10.0,
            carbs = 15.0,
            fat = 2.0,
            emoji = ""
        )
    }
}
