package com.example.fitto.feature_daily.data

import android.content.Context
import com.example.fitto.data.local.session.UserSessionDataGuard
import java.time.LocalDate

class WaterTrackingStore(
    context: Context
) {

    private val prefs = UserSessionDataGuard.scopedSharedPreferences(context, PREF_NAME)

    fun getWaterMl(date: LocalDate): Int {
        return prefs.getInt(key(KEY_WATER_ML, date), 0)
            .coerceAtLeast(0)
    }

    fun setWaterMl(date: LocalDate, value: Int) {
        prefs.edit()
            .putInt(key(KEY_WATER_ML, date), value.coerceIn(0, MAX_WATER_ML))
            .apply()
    }

    fun addWaterMl(date: LocalDate, amountMl: Int): Int {
        val updated = (getWaterMl(date) + amountMl)
            .coerceIn(0, MAX_WATER_ML)

        setWaterMl(date, updated)
        return updated
    }

    fun getExtraBottleSlots(date: LocalDate): Int {
        return prefs.getInt(key(KEY_EXTRA_BOTTLES, date), 0)
            .coerceAtLeast(0)
    }

    fun addExtraBottleSlots(date: LocalDate, count: Int = 4): Int {
        val updated = (getExtraBottleSlots(date) + count)
            .coerceAtMost(40)

        prefs.edit()
            .putInt(key(KEY_EXTRA_BOTTLES, date), updated)
            .apply()

        return updated
    }
    fun getVisibleBottleCount(
        date: LocalDate,
        defaultCount: Int
    ): Int {
        return prefs.getInt(
            key(KEY_VISIBLE_BOTTLE_COUNT, date),
            defaultCount.coerceIn(MIN_BOTTLE_COUNT, MAX_BOTTLE_COUNT)
        ).coerceIn(MIN_BOTTLE_COUNT, MAX_BOTTLE_COUNT)
    }

    fun setVisibleBottleCount(
        date: LocalDate,
        count: Int
    ): Int {
        val safeCount = count.coerceIn(MIN_BOTTLE_COUNT, MAX_BOTTLE_COUNT)

        prefs.edit()
            .putInt(key(KEY_VISIBLE_BOTTLE_COUNT, date), safeCount)
            .apply()

        return safeCount
    }

    fun increaseVisibleBottleCount(
        date: LocalDate,
        defaultCount: Int,
        amount: Int = 4
    ): Int {
        val current = getVisibleBottleCount(date, defaultCount)
        return setVisibleBottleCount(date, current + amount)
    }

    fun decreaseVisibleBottleCount(
        date: LocalDate,
        defaultCount: Int,
        amount: Int = 1
    ): Int {
        val current = getVisibleBottleCount(date, defaultCount)
        return setVisibleBottleCount(date, current - amount)
    }
    private fun key(prefix: String, date: LocalDate): String {
        return "${prefix}_${date}"
    }

    companion object {
        const val BOTTLE_SIZE_ML = 250

        private const val PREF_NAME = "fitto_water_tracking"
        private const val KEY_WATER_ML = "water_ml"
        private const val KEY_EXTRA_BOTTLES = "extra_bottles"
        const val MIN_BOTTLE_COUNT = 7
        const val MAX_BOTTLE_COUNT = 16

        private const val KEY_VISIBLE_BOTTLE_COUNT = "visible_bottle_count"
        private const val MAX_WATER_ML = 10000
    }
}