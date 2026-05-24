package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_meals",
    indices = [
        Index(value = ["diet_plan_id", "day_number", "meal_type"]),
        Index(value = ["diet_plan_id"])
    ]
)
data class PlannedMealEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "diet_plan_id")
    val dietPlanId: String,

    @ColumnInfo(name = "day_number")
    val dayNumber: Int,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    @ColumnInfo(name = "title_tr")
    val titleTr: String,

    @ColumnInfo(name = "base_calories")
    val baseCalories: Int,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
