package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_entry_foods",
    foreignKeys = [
        ForeignKey(
            entity = MealEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meal_entry_id"]),
        Index(value = ["food_id"])
    ]
)
data class MealEntryFoodEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "meal_entry_id")
    val mealEntryId: String,

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
