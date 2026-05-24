package com.example.fitto.feature_recipes.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import kotlin.math.min

class RecipeMacroDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.fitto_surface)
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.fitto_divider_soft)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.fitto_text_dark)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.fitto_text_secondary)
        textAlign = Paint.Align.CENTER
    }

    private val green = ContextCompat.getColor(context, R.color.fitto_green)
    private val orange = ContextCompat.getColor(context, R.color.fitto_orange)
    private val yellow = ContextCompat.getColor(context, R.color.fitto_yellow)

    private var carbPercent: Int = 40
    private var proteinPercent: Int = 30
    private var fatPercent: Int = 30
    private var kcalText: String = "0"

    fun setNutrition(kcal: Int, carbs: Int, protein: Int, fat: Int) {
        kcalText = kcal.toString()
        val carbCalories = carbs * 4
        val proteinCalories = protein * 4
        val fatCalories = fat * 9
        val total = (carbCalories + proteinCalories + fatCalories).coerceAtLeast(1)
        carbPercent = ((carbCalories * 100f) / total).toInt().coerceIn(0, 100)
        proteinPercent = ((proteinCalories * 100f) / total).toInt().coerceIn(0, 100)
        fatPercent = (100 - carbPercent - proteinPercent).coerceIn(0, 100)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.12f
        strokePaint.strokeWidth = stroke
        bgPaint.strokeWidth = stroke
        val pad = stroke / 2f + 6f
        val rect = RectF(pad, pad, width - pad, height - pad)

        canvas.drawArc(rect, -90f, 360f, false, bgPaint)
        var start = -90f
        drawSegment(canvas, rect, start, carbPercent * 3.6f, green)
        start += carbPercent * 3.6f + 4f
        drawSegment(canvas, rect, start, proteinPercent * 3.6f, orange)
        start += proteinPercent * 3.6f + 4f
        drawSegment(canvas, rect, start, fatPercent * 3.6f, yellow)

        val radius = size * 0.26f
        canvas.drawCircle(width / 2f, height / 2f, radius, centerPaint)

        textPaint.textSize = size * 0.16f
        smallTextPaint.textSize = size * 0.085f
        canvas.drawText(kcalText, width / 2f, height / 2f - 2f, textPaint)
        canvas.drawText("kcal", width / 2f, height / 2f + size * 0.13f, smallTextPaint)
    }

    private fun drawSegment(canvas: Canvas, rect: RectF, start: Float, sweep: Float, color: Int) {
        if (sweep <= 0f) return
        strokePaint.color = color
        canvas.drawArc(rect, start, (sweep - 4f).coerceAtLeast(1f), false, strokePaint)
    }
}
