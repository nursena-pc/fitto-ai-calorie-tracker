package com.example.fitto.feature_recipes.data

import android.content.res.Resources
import com.example.fitto.R
import com.example.fitto.feature_recipes.model.RecipeFilterOption

class RecipeFilterCatalog(
    private val resources: Resources
) {
    val mainCategories: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_main_category_keys,
        labelArrayRes = R.array.recipe_main_category_labels
    )

    val mainIngredients: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_main_ingredient_keys,
        labelArrayRes = R.array.recipe_main_ingredient_labels
    )

    val diets: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_diet_keys,
        labelArrayRes = R.array.recipe_diet_labels
    )

    val methods: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_method_keys,
        labelArrayRes = R.array.recipe_method_labels
    )

    val durations: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_duration_keys,
        labelArrayRes = R.array.recipe_duration_labels
    )

    val difficulties: List<RecipeFilterOption> = options(
        keyArrayRes = R.array.recipe_difficulty_keys,
        labelArrayRes = R.array.recipe_difficulty_labels
    )

    fun subcategoriesFor(mainCategoryKey: String?): List<RecipeFilterOption> {
        return when (mainCategoryKey) {
            KEY_MAIN_MEAL -> options(R.array.recipe_subcategory_keys_ana_yemek, R.array.recipe_subcategory_labels_ana_yemek)
            KEY_SOUP -> options(R.array.recipe_subcategory_keys_corba, R.array.recipe_subcategory_labels_corba)
            KEY_SALAD -> options(R.array.recipe_subcategory_keys_salata, R.array.recipe_subcategory_labels_salata)
            KEY_DESSERT -> options(R.array.recipe_subcategory_keys_tatli, R.array.recipe_subcategory_labels_tatli)
            KEY_BREAKFAST -> options(R.array.recipe_subcategory_keys_kahvalti, R.array.recipe_subcategory_labels_kahvalti)
            KEY_SNACK -> options(R.array.recipe_subcategory_keys_atistirmalik, R.array.recipe_subcategory_labels_atistirmalik)
            KEY_DRINK -> options(R.array.recipe_subcategory_keys_icecek, R.array.recipe_subcategory_labels_icecek)
            KEY_PICKLE -> options(R.array.recipe_subcategory_keys_tursu, R.array.recipe_subcategory_labels_tursu)
            KEY_SAUCE -> options(R.array.recipe_subcategory_keys_sos, R.array.recipe_subcategory_labels_sos)
            else -> emptyList()
        }
    }

    fun mainCategoryLabel(key: String): String = mainCategories.labelFor(key)
    fun subcategoryLabel(mainCategoryKey: String?, subcategoryKey: String): String =
        subcategoriesFor(mainCategoryKey).labelFor(subcategoryKey)

    fun difficultyLabel(key: String): String = difficulties.labelFor(key)

    private fun options(keyArrayRes: Int, labelArrayRes: Int): List<RecipeFilterOption> {
        val keys = resources.getStringArray(keyArrayRes)
        val labels = resources.getStringArray(labelArrayRes)
        return keys.mapIndexed { index, key ->
            RecipeFilterOption(
                key = key,
                label = labels.getOrElse(index) { key }
            )
        }
    }

    private fun List<RecipeFilterOption>.labelFor(key: String): String =
        firstOrNull { it.key == key }?.label ?: key

    companion object {
        const val KEY_MAIN_MEAL = "ana yemek"
        const val KEY_SOUP = "çorba"
        const val KEY_SALAD = "salata"
        const val KEY_DESSERT = "tatlı"
        const val KEY_BREAKFAST = "kahvaltı"
        const val KEY_SNACK = "atıştırmalık"
        const val KEY_DRINK = "içecek"
        const val KEY_PICKLE = "turşu"
        const val KEY_SAUCE = "sos"
    }
}
