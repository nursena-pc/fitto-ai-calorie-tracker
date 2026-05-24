package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["owner_user_id"]),
        Index(value = ["source"])
    ]
)
data class FoodEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name_tr")
    val nameTr: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String?,

    val category: String,

    @ColumnInfo(name = "default_unit")
    val defaultUnit: String,

    @ColumnInfo(name = "default_serving_amount")
    val defaultServingAmount: Double,

    @ColumnInfo(name = "calories_per_100g")
    val caloriesPer100g: Double,

    @ColumnInfo(name = "protein_per_100g")
    val proteinPer100g: Double,

    @ColumnInfo(name = "carbs_per_100g")
    val carbsPer100g: Double,

    @ColumnInfo(name = "fat_per_100g")
    val fatPer100g: Double,

    @ColumnInfo(name = "fiber_per_100g")
    val fiberPer100g: Double?,

    val source: String,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean,

    val allergens: String,

    @ColumnInfo(name = "diet_tags")
    val dietTags: String,

    @ColumnInfo(name = "meal_types")
    val mealTypes: String,

    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
