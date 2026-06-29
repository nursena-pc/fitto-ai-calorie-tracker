package com.example.fitto.feature_recipes.data

import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeFilterResolverTest {

    @Test
    fun ingredientOptions_cleansDeduplicatesAndSortsIngredientNames() {
        val recipes = listOf(
            recipe(id = "1", ingredientNames = listOf(" domates (orta)", "Yumurta - protein")),
            recipe(id = "2", ingredientNames = listOf("Domates", "  ", "avokado"))
        )

        val options = RecipeFilterResolver.ingredientOptions(recipes)

        assertEquals(listOf("Avokado", "Domates", "Yumurta"), options.map { option -> option.label })
        assertEquals(listOf("avokado", "domates", "yumurta"), options.map { option -> option.key })
    }

    @Test
    fun filterRecipes_matchesNormalizedQueryAcrossRecipeSearchText() {
        val recipes = listOf(
            recipe(id = "fit", title = "Proteinli Kahvaltı", ingredientNames = listOf("Yumurta")),
            recipe(id = "light", title = "Yeşil Salata", steps = listOf("Doğra"))
        )

        val result = RecipeFilterResolver.filterRecipes(
            recipes = recipes,
            query = "proteinli kahvalti",
            filterState = RecipeFilterState()
        )

        assertEquals(listOf("fit"), result.map { recipe -> recipe.id })
    }

    @Test
    fun filterRecipes_appliesCategoryDietMethodDurationAndDifficultyFilters() {
        val recipes = listOf(
            recipe(
                id = "match",
                mainCategoryKey = "kahvalti",
                subcategoryKeys = setOf("sicak"),
                mainIngredientKeys = setOf("yumurta"),
                dietKeys = setOf("yuksek_protein", "glutensiz"),
                methodKeys = setOf("tava"),
                durationLabel = "15-30 dk",
                difficultyKey = "kolay"
            ),
            recipe(
                id = "wrongDiet",
                mainCategoryKey = "kahvalti",
                subcategoryKeys = setOf("sicak"),
                mainIngredientKeys = setOf("yumurta"),
                dietKeys = setOf("vegan"),
                methodKeys = setOf("tava"),
                durationLabel = "15-30 dk",
                difficultyKey = "kolay"
            ),
            recipe(
                id = "wrongCategory",
                mainCategoryKey = "ana_yemek",
                subcategoryKeys = setOf("sicak"),
                mainIngredientKeys = setOf("yumurta"),
                dietKeys = setOf("yuksek_protein", "glutensiz"),
                methodKeys = setOf("tava"),
                durationLabel = "15-30 dk",
                difficultyKey = "kolay"
            )
        )

        val result = RecipeFilterResolver.filterRecipes(
            recipes = recipes,
            query = "",
            filterState = RecipeFilterState(
                selectedMainCategoryKey = "kahvalti",
                selectedSubcategoryKey = "sicak",
                selectedMainIngredientKeys = setOf("yumurta"),
                selectedDietKeys = setOf("yuksek_protein", "glutensiz"),
                selectedMethodKeys = setOf("tava"),
                selectedDurationKey = "15-30 dk",
                selectedDifficultyKey = "kolay"
            )
        )

        assertEquals(listOf("match"), result.map { recipe -> recipe.id })
    }

    @Test
    fun filterRecipes_appliesIncludedAndExcludedIngredientFilters() {
        val recipes = listOf(
            recipe(id = "included", ingredientNames = listOf("Süzme yoğurt", "Yulaf")),
            recipe(id = "excluded", ingredientNames = listOf("Süzme yoğurt", "Şeker")),
            recipe(id = "missing", ingredientNames = listOf("Yulaf"))
        )

        val result = RecipeFilterResolver.filterRecipes(
            recipes = recipes,
            query = "",
            filterState = RecipeFilterState(
                selectedIncludedIngredientKeys = setOf("yogurt"),
                selectedExcludedIngredientKeys = setOf("seker")
            )
        )

        assertEquals(listOf("included"), result.map { recipe -> recipe.id })
    }

    private fun recipe(
        id: String,
        title: String = "Tarif $id",
        description: String = "Açıklama",
        mainCategoryKey: String = "kahvalti",
        subcategoryKeys: Set<String> = emptySet(),
        mainIngredientKeys: Set<String> = emptySet(),
        dietKeys: Set<String> = emptySet(),
        methodKeys: Set<String> = emptySet(),
        durationLabel: String = "15-30 dk",
        difficultyKey: String = "kolay",
        ingredientNames: List<String> = emptyList(),
        ingredients: List<String> = ingredientNames,
        steps: List<String> = emptyList()
    ): RecipeUiModel {
        return RecipeUiModel(
            id = id,
            title = title,
            description = description,
            mainCategoryKey = mainCategoryKey,
            subcategoryKey = subcategoryKeys.firstOrNull().orEmpty(),
            subcategoryKeys = subcategoryKeys,
            mainIngredientKeys = mainIngredientKeys,
            dietKeys = dietKeys,
            methodKeys = methodKeys,
            durationMinutes = 25,
            durationLabel = durationLabel,
            difficultyKey = difficultyKey,
            calories = 0,
            imageResId = 0,
            servings = 1,
            servingDisplay = "1 porsiyon",
            proteinGrams = 0,
            carbGrams = 0,
            fatGrams = 0,
            ingredientNames = ingredientNames,
            ingredients = ingredients,
            steps = steps,
            tip = null,
            isFavorite = false
        )
    }
}
