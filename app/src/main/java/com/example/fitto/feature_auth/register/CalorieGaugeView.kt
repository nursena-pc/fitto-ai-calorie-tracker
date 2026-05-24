package com.example.fitto.feature_auth.register

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import kotlin.math.cos
import kotlin.math.sin

class CalorieGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var targetCalories: Int = 1800
    private var maintenanceCalories: Int = 2200

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        // Kalınlığı 34f'den 52f'ye çıkardık (2. görseldeki dolgunluk için)
        strokeWidth = 52f
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80333333.toInt()
        strokeWidth = 6f // İğneyi de çok az kalınlaştırdık
        strokeCap = Paint.Cap.ROUND
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80333333.toInt()
        style = Paint.Style.FILL
    }

    private val caloriePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF212121.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val kcalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99212121.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setCalories(targetCalories: Int, maintenanceCalories: Int) {
        this.targetCalories = targetCalories
        this.maintenanceCalories = maintenanceCalories.coerceAtLeast(1)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val green = ContextCompat.getColor(context, R.color.fitto_green)
        val yellow = ContextCompat.getColor(context, R.color.fitto_yellow)
        val red = 0xFFE57373.toInt()

        // Kalınlık değerini burada da senkronize ettik
        val stroke = 52f
        arcPaint.strokeWidth = stroke

        val size = width.coerceAtMost(height * 2)
        // Yayı biraz daraltarak iç içe geçmeyi ve kenarlardan taşmayı önledik
        val arcWidth = size * 0.82f
        val left = (width - arcWidth) / 2f
        val top = 50f // Yukarıdan biraz daha boşluk bıraktık
        val rect = RectF(left, top, left + arcWidth, top + arcWidth)

        // Renkli yayların çizimi
        arcPaint.color = green
        canvas.drawArc(rect, 180f, 92f, false, arcPaint)

        arcPaint.color = yellow
        canvas.drawArc(rect, 272f, 38f, false, arcPaint)

        arcPaint.color = red
        canvas.drawArc(rect, 310f, 50f, false, arcPaint)

        // İğne hesaplaması
        val ratio = (targetCalories.toFloat() / maintenanceCalories.toFloat())
            .coerceIn(0.65f, 1.25f)

        val progress = ((ratio - 0.65f) / (1.25f - 0.65f)).coerceIn(0f, 1f)
        val angle = Math.toRadians((180f + progress * 180f).toDouble())

        val centerX = rect.centerX()
        val centerY = rect.centerY()
        // İğnenin boyunu kalınlaşan yayla orantılı kısalttık (taşmaması için)
        val radius = arcWidth / 2f - stroke * 0.8f

        val endX = centerX + radius * cos(angle).toFloat()
        val endY = centerY + radius * sin(angle).toFloat()

        canvas.drawLine(centerX, centerY, endX, endY, needlePaint)
        canvas.drawCircle(centerX, centerY, 8f, centerPaint)

        // Metinlerin boyutunu ve konumunu yeni kalınlığa göre optimize ettik
        caloriePaint.textSize = arcWidth * 0.15f
        kcalPaint.textSize = arcWidth * 0.06f

        val calorieY = centerY + caloriePaint.textSize * 0.20f
        val kcalY = calorieY + kcalPaint.textSize * 1.2f

        canvas.drawText(targetCalories.toString(), centerX, calorieY, caloriePaint)
        canvas.drawText("kcal", centerX, kcalY, kcalPaint)
    }
}