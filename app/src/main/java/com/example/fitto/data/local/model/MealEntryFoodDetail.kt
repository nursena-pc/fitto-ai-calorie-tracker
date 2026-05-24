package com.example.fitto.data.local.model

import androidx.room.ColumnInfo

data class MealEntryFoodDetail(
    @ColumnInfo(name = "meal_entry_id")
    val mealEntryId: String,

    @ColumnInfo(name = "entry_title")
    val entryTitle: String,

    @ColumnInfo(name = "source_type")
    val sourceType: String,

    @ColumnInfo(name = "food_id")
    val foodId: String?,

    @ColumnInfo(name = "food_name")
    val foodName: String,

    @ColumnInfo(name = "amount_label")
    val amountLabel: String,

    @ColumnInfo(name = "amount_grams")
    val amountGrams: Double,

    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
