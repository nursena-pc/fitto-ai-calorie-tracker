package com.example.fitto.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FittoDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun migrateEightToNine_preservesFoodsAndScopesOnlyLegacyCustomRows() {
        createVersionEightDatabase()
        val expectedLegacyOwner = FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf(String::isNotBlank)
            ?: FittoDatabaseMigrations.LEGACY_UNASSIGNED_OWNER_ID

        val roomDatabase = Room.databaseBuilder(
            context,
            FittoDatabase::class.java,
            TEST_DATABASE_NAME
        )
            .addMigrations(FittoDatabaseMigrations.MIGRATION_8_9)
            .build()

        val database = roomDatabase.openHelper.writableDatabase

        database.query(
            "SELECT owner_user_id FROM foods WHERE id = ?",
            arrayOf(CUSTOM_FOOD_ID)
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(
                expectedLegacyOwner,
                cursor.getString(0)
            )
        }

        database.query(
            "SELECT owner_user_id FROM foods WHERE id = ?",
            arrayOf(SEED_FOOD_ID)
        ).use { cursor ->
            cursor.moveToFirst()
            assertNull(cursor.getString(0))
        }

        database.query("SELECT COUNT(*) FROM foods").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }

        roomDatabase.close()
    }

    private fun createVersionEightDatabase() {
        val path = context.getDatabasePath(TEST_DATABASE_NAME)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
            VERSION_EIGHT_CREATE_STATEMENTS.forEach(database::execSQL)
            insertVersionEightFood(
                database = database,
                id = CUSTOM_FOOD_ID,
                source = "user_custom"
            )
            insertVersionEightFood(
                database = database,
                id = SEED_FOOD_ID,
                source = "seed"
            )
            database.version = 8
        }
    }

    private fun insertVersionEightFood(
        database: SQLiteDatabase,
        id: String,
        source: String
    ) {
        database.execSQL(
            """
            INSERT INTO foods (
                id, name_tr, name_en, category, default_unit, default_serving_amount,
                calories_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g,
                fiber_per_100g, source, is_verified, allergens, diet_tags, meal_types,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(
                id,
                "Test Yiyecek",
                "Test Food",
                "other",
                "g",
                100.0,
                100.0,
                5.0,
                10.0,
                3.0,
                1.0,
                source,
                1,
                "[]",
                "[]",
                "[]",
                1L,
                1L
            )
        )
    }

    companion object {
        private const val TEST_DATABASE_NAME = "fitto-migration-test.db"
        private const val CUSTOM_FOOD_ID = "user_custom_test"
        private const val SEED_FOOD_ID = "seed_test"

        private val VERSION_EIGHT_CREATE_STATEMENTS = listOf(
            """CREATE TABLE IF NOT EXISTS `foods` (`id` TEXT NOT NULL, `name_tr` TEXT NOT NULL, `name_en` TEXT, `category` TEXT NOT NULL, `default_unit` TEXT NOT NULL, `default_serving_amount` REAL NOT NULL, `calories_per_100g` REAL NOT NULL, `protein_per_100g` REAL NOT NULL, `carbs_per_100g` REAL NOT NULL, `fat_per_100g` REAL NOT NULL, `fiber_per_100g` REAL, `source` TEXT NOT NULL, `is_verified` INTEGER NOT NULL, `allergens` TEXT NOT NULL, `diet_tags` TEXT NOT NULL, `meal_types` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE TABLE IF NOT EXISTS `favorite_foods` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `food_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_foods_user_id_food_id` ON `favorite_foods` (`user_id`, `food_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_favorite_foods_food_id` ON `favorite_foods` (`food_id`)""",
            """CREATE TABLE IF NOT EXISTS `meal_entries` (`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `entry_date` TEXT NOT NULL, `meal_type` TEXT NOT NULL, `title` TEXT NOT NULL, `total_calories` INTEGER NOT NULL, `total_protein` REAL NOT NULL, `total_carbs` REAL NOT NULL, `total_fat` REAL NOT NULL, `source_type` TEXT NOT NULL, `source_plan_id` TEXT, `source_planned_meal_id` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_meal_entries_user_id_entry_date` ON `meal_entries` (`user_id`, `entry_date`)""",
            """CREATE INDEX IF NOT EXISTS `index_meal_entries_user_id_meal_type` ON `meal_entries` (`user_id`, `meal_type`)""",
            """CREATE INDEX IF NOT EXISTS `index_meal_entries_source_planned_meal_id` ON `meal_entries` (`source_planned_meal_id`)""",
            """CREATE TABLE IF NOT EXISTS `meal_entry_foods` (`id` TEXT NOT NULL, `meal_entry_id` TEXT NOT NULL, `food_id` TEXT, `food_name` TEXT NOT NULL, `amount_label` TEXT NOT NULL, `amount_grams` REAL NOT NULL, `calories` INTEGER NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`meal_entry_id`) REFERENCES `meal_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
            """CREATE INDEX IF NOT EXISTS `index_meal_entry_foods_meal_entry_id` ON `meal_entry_foods` (`meal_entry_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_meal_entry_foods_food_id` ON `meal_entry_foods` (`food_id`)""",
            """CREATE TABLE IF NOT EXISTS `diet_plans` (`id` TEXT NOT NULL, `name_tr` TEXT NOT NULL, `name_en` TEXT, `duration_days` INTEGER NOT NULL, `base_calories` INTEGER NOT NULL, `icon` TEXT NOT NULL, `category` TEXT NOT NULL, `description_tr` TEXT NOT NULL, `medical_notice_tr` TEXT NOT NULL, `is_restrictive` INTEGER NOT NULL, `source` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE TABLE IF NOT EXISTS `planned_meals` (`id` TEXT NOT NULL, `diet_plan_id` TEXT NOT NULL, `day_number` INTEGER NOT NULL, `meal_type` TEXT NOT NULL, `title_tr` TEXT NOT NULL, `base_calories` INTEGER NOT NULL, `order_index` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_planned_meals_diet_plan_id_day_number_meal_type` ON `planned_meals` (`diet_plan_id`, `day_number`, `meal_type`)""",
            """CREATE INDEX IF NOT EXISTS `index_planned_meals_diet_plan_id` ON `planned_meals` (`diet_plan_id`)""",
            """CREATE TABLE IF NOT EXISTS `planned_meal_foods` (`id` TEXT NOT NULL, `planned_meal_id` TEXT NOT NULL, `food_id` TEXT NOT NULL, `amount_grams` REAL NOT NULL, `display_amount_tr` TEXT NOT NULL, `scalable` INTEGER NOT NULL, `scale_group` TEXT NOT NULL, `min_amount_grams` REAL NOT NULL, `max_amount_grams` REAL NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""",
            """CREATE INDEX IF NOT EXISTS `index_planned_meal_foods_planned_meal_id` ON `planned_meal_foods` (`planned_meal_id`)""",
            """CREATE INDEX IF NOT EXISTS `index_planned_meal_foods_food_id` ON `planned_meal_foods` (`food_id`)""",
            """CREATE TABLE IF NOT EXISTS `user_profiles` (`user_id` TEXT NOT NULL, `email` TEXT NOT NULL, `display_name` TEXT NOT NULL, `user_name` TEXT NOT NULL, `height_cm` INTEGER NOT NULL, `weight_kg` INTEGER NOT NULL, `target_weight_kg` INTEGER, `age` INTEGER NOT NULL, `gender` TEXT NOT NULL, `goal_type` TEXT NOT NULL, `activity_level` TEXT NOT NULL, `bmr` INTEGER NOT NULL, `maintenance_calories` INTEGER NOT NULL, `target_calories` INTEGER NOT NULL, `recommended_calories` INTEGER NOT NULL, `daily_calorie_target` INTEGER NOT NULL, `protein_target_grams` INTEGER NOT NULL, `carb_target_grams` INTEGER NOT NULL, `fat_target_grams` INTEGER NOT NULL, `water_target_ml` INTEGER NOT NULL, `weekly_weight_change_kg` REAL NOT NULL, `estimated_weeks` INTEGER, `plan_status` TEXT NOT NULL, `profile_completed` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `synced_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`))""",
            """CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `ownerUid` TEXT, `sourceType` TEXT NOT NULL, `sourceRecipeId` TEXT, `sourceUserId` TEXT, `sourcePostId` TEXT, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `mainCategoryKey` TEXT NOT NULL, `subcategoryKey` TEXT NOT NULL, `subcategoryKeys` TEXT NOT NULL, `mainIngredientKeys` TEXT NOT NULL, `dietKeys` TEXT NOT NULL, `methodKeys` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `durationLabel` TEXT NOT NULL, `difficultyKey` TEXT NOT NULL, `calories` INTEGER NOT NULL, `imageResId` INTEGER NOT NULL, `servings` INTEGER NOT NULL, `servingDisplay` TEXT NOT NULL, `proteinGrams` INTEGER NOT NULL, `carbGrams` INTEGER NOT NULL, `fatGrams` INTEGER NOT NULL, `ingredientNames` TEXT NOT NULL, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `tip` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
        )
    }
}
