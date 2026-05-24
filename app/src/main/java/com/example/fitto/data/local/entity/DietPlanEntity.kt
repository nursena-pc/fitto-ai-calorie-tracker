package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_plans")
data class DietPlanEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name_tr")
    val nameTr: String,

    @ColumnInfo(name = "name_en")
    val nameEn: String?,

    @ColumnInfo(name = "duration_days")
    val durationDays: Int,

    @ColumnInfo(name = "base_calories")
    val baseCalories: Int,

    val icon: String,

    val category: String,

    @ColumnInfo(name = "description_tr")
    val descriptionTr: String,

    @ColumnInfo(name = "medical_notice_tr")
    val medicalNoticeTr: String,

    @ColumnInfo(name = "is_restrictive")
    val isRestrictive: Boolean,

    val source: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
