package com.example.fitto.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fitto.data.local.dao.DietPlanDao
import com.example.fitto.data.local.dao.FavoriteFoodDao
import com.example.fitto.data.local.dao.FoodDao
import com.example.fitto.data.local.dao.MealDao
import com.example.fitto.data.local.dao.ProfileDao
import com.example.fitto.data.local.dao.RecipeDao
import com.example.fitto.data.local.entity.DietPlanEntity
import com.example.fitto.data.local.entity.FavoriteFoodEntity
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.data.local.entity.MealEntryFoodEntity
import com.example.fitto.data.local.entity.PlannedMealEntity
import com.example.fitto.data.local.entity.PlannedMealFoodEntity
import com.example.fitto.data.local.entity.RecipeEntity
import com.example.fitto.data.local.entity.UserProfileEntity

@Database(
    entities = [
        FoodEntity::class,
        FavoriteFoodEntity::class,
        MealEntryEntity::class,
        MealEntryFoodEntity::class,
        DietPlanEntity::class,
        PlannedMealEntity::class,
        PlannedMealFoodEntity::class,
        UserProfileEntity::class,
        RecipeEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(RecipeTypeConverters::class)
abstract class FittoDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun dietPlanDao(): DietPlanDao
    abstract fun favoriteFoodDao(): FavoriteFoodDao
    abstract fun profileDao(): ProfileDao
    abstract fun recipeDao(): RecipeDao
}
