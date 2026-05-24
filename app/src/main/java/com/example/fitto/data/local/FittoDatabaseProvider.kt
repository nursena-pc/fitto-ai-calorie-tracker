package com.example.fitto.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.auth.FirebaseAuth

object FittoDatabaseProvider {

    @Volatile
    private var instance: FittoDatabase? = null

    fun getDatabase(context: Context): FittoDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FittoDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_6_7_ADD_MEAL_ENTRY_USER_ID,
                    MIGRATION_7_8_ADD_FAVORITE_FOOD_USER_ID,
                    MIGRATION_8_9_ADD_FOOD_OWNER_USER_ID
                )
                // Geliştirme aşamasında diğer eski şema geçişleri için korunuyor; 6→7 geçişinde
                // yemek kayıtları silinmeden aktif kullanıcıya bağlanır.
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }

    private val MIGRATION_6_7_ADD_MEAL_ENTRY_USER_ID = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?.takeIf { it.isNotBlank() }
                ?: LOCAL_ANONYMOUS_USER_ID
            val escapedUid = uid.replace("'", "''")

            database.execSQL("ALTER TABLE meal_entries ADD COLUMN user_id TEXT NOT NULL DEFAULT '$escapedUid'")
            database.execSQL("DROP INDEX IF EXISTS index_meal_entries_entry_date")
            database.execSQL("DROP INDEX IF EXISTS index_meal_entries_meal_type")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_meal_entries_user_id_entry_date ON meal_entries(user_id, entry_date)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_meal_entries_user_id_meal_type ON meal_entries(user_id, meal_type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_meal_entries_source_planned_meal_id ON meal_entries(source_planned_meal_id)")
        }
    }



    private val MIGRATION_7_8_ADD_FAVORITE_FOOD_USER_ID = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?.takeIf { it.isNotBlank() }
                ?: LOCAL_ANONYMOUS_USER_ID
            val escapedUid = uid.replace("'", "''")

            database.execSQL("ALTER TABLE favorite_foods ADD COLUMN user_id TEXT NOT NULL DEFAULT '$escapedUid'")
            database.execSQL("DROP INDEX IF EXISTS index_favorite_foods_food_id")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_foods_food_id ON favorite_foods(food_id)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_favorite_foods_user_id_food_id ON favorite_foods(user_id, food_id)")
        }
    }


    private val MIGRATION_8_9_ADD_FOOD_OWNER_USER_ID = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?.takeIf { it.isNotBlank() }
            val ownerForLegacyCustomFoods = uid ?: LEGACY_UNASSIGNED_OWNER_ID
            val escapedOwner = ownerForLegacyCustomFoods.replace("'", "''")

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
            database.execSQL("CREATE INDEX IF NOT EXISTS index_foods_owner_user_id ON foods(owner_user_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_foods_source ON foods(source)")
        }
    }

    private const val LOCAL_ANONYMOUS_USER_ID = "local_anonymous"
    private const val LEGACY_UNASSIGNED_OWNER_ID = "__legacy_unassigned__"
    private const val DATABASE_NAME = "fitto.db"
}
