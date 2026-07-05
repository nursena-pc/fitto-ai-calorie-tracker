package com.example.fitto.data.local

import android.content.Context
import androidx.room.Room

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
                .addMigrations(*FittoDatabaseMigrations.ALL)
                .fallbackToDestructiveMigrationFrom(
                    true,
                    *FittoDatabaseMigrations.LEGACY_DESTRUCTIVE_START_VERSIONS
                )
                .build()
                .also { instance = it }
        }
    }

    private const val DATABASE_NAME = "fitto.db"
}
