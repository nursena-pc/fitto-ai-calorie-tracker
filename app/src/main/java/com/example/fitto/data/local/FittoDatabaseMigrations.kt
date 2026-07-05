package com.example.fitto.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth

/**
 * Versioned Room migrations kept separately from database construction.
 *
 * Versions 1..5 predate the data-preserving migration chain and are the only versions that may
 * fall back to a destructive rebuild. Every supported version from 6 onward must have an explicit
 * migration path.
 */
object FittoDatabaseMigrations {

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val escapedUid = currentUserIdOrAnonymous().sqlEscaped()

            database.execSQL(
                "ALTER TABLE meal_entries ADD COLUMN user_id TEXT NOT NULL DEFAULT '$escapedUid'"
            )
            database.execSQL("DROP INDEX IF EXISTS index_meal_entries_entry_date")
            database.execSQL("DROP INDEX IF EXISTS index_meal_entries_meal_type")
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_meal_entries_user_id_entry_date " +
                    "ON meal_entries(user_id, entry_date)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_meal_entries_user_id_meal_type " +
                    "ON meal_entries(user_id, meal_type)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_meal_entries_source_planned_meal_id " +
                    "ON meal_entries(source_planned_meal_id)"
            )
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val escapedUid = currentUserIdOrAnonymous().sqlEscaped()

            database.execSQL(
                "ALTER TABLE favorite_foods ADD COLUMN user_id TEXT NOT NULL DEFAULT '$escapedUid'"
            )
            database.execSQL("DROP INDEX IF EXISTS index_favorite_foods_food_id")
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_favorite_foods_food_id " +
                    "ON favorite_foods(food_id)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_favorite_foods_user_id_food_id " +
                    "ON favorite_foods(user_id, food_id)"
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val ownerForLegacyCustomFoods = FirebaseAuth.getInstance().currentUser?.uid
                ?.takeIf(String::isNotBlank)
                ?: LEGACY_UNASSIGNED_OWNER_ID
            val escapedOwner = ownerForLegacyCustomFoods.sqlEscaped()

            database.execSQL("ALTER TABLE foods ADD COLUMN owner_user_id TEXT")
            database.execSQL(
                """
                UPDATE foods
                SET owner_user_id = '$escapedOwner'
                WHERE source IN ('user_custom', 'logmeal_camera')
                   OR id LIKE 'user_custom_%'
                   OR id LIKE 'logmeal_%'
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_foods_owner_user_id ON foods(owner_user_id)"
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS index_foods_source ON foods(source)")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )

    val LEGACY_DESTRUCTIVE_START_VERSIONS: IntArray = intArrayOf(1, 2, 3, 4, 5)

    private fun currentUserIdOrAnonymous(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf(String::isNotBlank)
            ?: LOCAL_ANONYMOUS_USER_ID
    }

    private fun String.sqlEscaped(): String = replace("'", "''")

    const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
    const val LEGACY_UNASSIGNED_OWNER_ID = "__legacy_unassigned__"
}
