package com.example.fitto.feature_health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDate

class FittoStepCounterManager(
    context: Context
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val store = FittoStepCounterStore(appContext)

    private val sensorManager: SensorManager by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val stepCounterSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    private var activeDate: LocalDate = LocalDate.now()
    private var onStepsChanged: ((Long) -> Unit)? = null

    fun isStepCounterAvailable(): Boolean {
        return stepCounterSensor != null
    }

    fun getCachedSteps(date: LocalDate): Long {
        return store.getCachedSteps(date)
    }

    fun start(
        date: LocalDate,
        onStepsChanged: (Long) -> Unit
    ) {
        stop()

        activeDate = date
        this.onStepsChanged = onStepsChanged

        onStepsChanged(store.getCachedSteps(date))

        // Geçmiş günlerde canlı sensör dinlemiyoruz.
        if (date != LocalDate.now()) return

        val sensor = stepCounterSensor ?: return

        sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        onStepsChanged = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsSinceReboot = event.values
            .firstOrNull()
            ?.toLong()
            ?: return

        val todaySteps = store.updateFromSensorTotal(
            date = activeDate,
            sensorTotalSteps = totalStepsSinceReboot
        )

        onStepsChanged?.invoke(todaySteps)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}