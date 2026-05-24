package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_meal_foods",
    indices = [
        Index(value = ["planned_meal_id"]),
        Index(value = ["food_id"])
    ]
)
data class PlannedMealFoodEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "planned_meal_id")
    val plannedMealId: String,

    @ColumnInfo(name = "food_id")
    val foodId: String,

    @ColumnInfo(name = "amount_grams")
    val amountGrams: Double,

    @ColumnInfo(name = "display_amount_tr")
    val displayAmountTr: String,

    val scalable: Boolean,

    @ColumnInfo(name = "scale_group")
    val scaleGroup: String,

    @ColumnInfo(name = "min_amount_grams")
    val minAmountGrams: Double,

    @ColumnInfo(name = "max_amount_grams")
    val maxAmountGrams: Double,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
