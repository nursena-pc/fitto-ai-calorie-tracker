package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "height_cm")
    val heightCm: Int,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Int,

    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Int?,

    @ColumnInfo(name = "age")
    val age: Int,

    @ColumnInfo(name = "gender")
    val gender: String,

    @ColumnInfo(name = "goal_type")
    val goalType: String,

    @ColumnInfo(name = "activity_level")
    val activityLevel: String,

    @ColumnInfo(name = "bmr")
    val bmr: Int,

    @ColumnInfo(name = "maintenance_calories")
    val maintenanceCalories: Int,

    @ColumnInfo(name = "target_calories")
    val targetCalories: Int,

    @ColumnInfo(name = "recommended_calories")
    val recommendedCalories: Int,

    @ColumnInfo(name = "daily_calorie_target")
    val dailyCalorieTarget: Int,

    @ColumnInfo(name = "protein_target_grams")
    val proteinTargetGrams: Int,

    @ColumnInfo(name = "carb_target_grams")
    val carbTargetGrams: Int,

    @ColumnInfo(name = "fat_target_grams")
    val fatTargetGrams: Int,

    @ColumnInfo(name = "water_target_ml")
    val waterTargetMl: Int,

    @ColumnInfo(name = "weekly_weight_change_kg")
    val weeklyWeightChangeKg: Double,

    @ColumnInfo(name = "estimated_weeks")
    val estimatedWeeks: Int?,

    @ColumnInfo(name = "plan_status")
    val planStatus: String,

    @ColumnInfo(name = "profile_completed")
    val profileCompleted: Boolean,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long
)