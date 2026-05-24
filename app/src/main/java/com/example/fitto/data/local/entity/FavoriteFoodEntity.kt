package com.example.fitto.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_foods",
    indices = [
        Index(value = ["user_id", "food_id"], unique = true),
        Index(value = ["food_id"])
    ]
)
data class FavoriteFoodEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "food_id")
    val foodId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
