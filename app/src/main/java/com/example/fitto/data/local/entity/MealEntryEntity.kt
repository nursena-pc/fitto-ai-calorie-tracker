package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_entries",
    indices = [
        Index(value = ["user_id", "entry_date"]),
        Index(value = ["user_id", "meal_type"]),
        Index(value = ["source_planned_meal_id"])
    ]
)
data class MealEntryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "entry_date")
    val entryDate: String,

    @ColumnInfo(name = "meal_type")
    val mealType: String,

    val title: String,

    @ColumnInfo(name = "total_calories")
    val totalCalories: Int,

    @ColumnInfo(name = "total_protein")
    val totalProtein: Double,

    @ColumnInfo(name = "total_carbs")
    val totalCarbs: Double,

    @ColumnInfo(name = "total_fat")
    val totalFat: Double,

    @ColumnInfo(name = "source_type")
    val sourceType: String,

    @ColumnInfo(name = "source_plan_id")
    val sourcePlanId: String?,

    @ColumnInfo(name = "source_planned_meal_id")
    val sourcePlannedMealId: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_DIET_PLAN = "diet_plan"
    }
}
