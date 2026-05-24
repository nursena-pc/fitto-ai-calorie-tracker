package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import kotlin.math.roundToInt

class ProfileDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()

    private var proteinPercent: Float = 0f
    private var carbPercent: Float = 0f
    private var fatPercent: Float = 0f
    private var centerTitle: String = context.getString(R.string.profile_weekly_summary_no_macro_short)
    private var centerSubtitle: String = context.getString(R.string.profile_weekly_summary_macro_center)

    fun setValues(
        proteinPercent: Float,
        carbPercent: Float,
        fatPercent: Float,
        centerTitle: String = this.centerTitle,
        centerSubtitle: String = this.centerSubtitle
    ) {
        this.proteinPercent = proteinPercent.coerceAtLeast(0f)
        this.carbPercent = carbPercent.coerceAtLeast(0f)
        this.fatPercent = fatPercent.coerceAtLeast(0f)
        this.centerTitle = centerTitle
        this.centerSubtitle = centerSubtitle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = width.coerceAtMost(height).toFloat()
        val strokeWidth = size * 0.16f
        val padding = strokeWidth / 2f + 2f
        val left = (width - size) / 2f
        val top = (height - size) / 2f

        arcBounds.set(
            left + padding,
            top + padding,
            left + size - padding,
            top + size - padding
        )

        arcPaint.strokeWidth = strokeWidth
        drawArc(canvas, R.color.profile_progress_track, -90f, 360f)

        val total = proteinPercent + carbPercent + fatPercent

        if (total > 0f) {
            var startAngle = -90f

            val proteinSweep = 360f * (proteinPercent / total)
            drawArc(canvas, R.color.reports_macro_protein, startAngle, proteinSweep)
            startAngle += proteinSweep

            val carbSweep = 360f * (carbPercent / total)
            drawArc(canvas, R.color.reports_macro_carbs, startAngle, carbSweep)
            startAngle += carbSweep

            val fatSweep = 360f * (fatPercent / total)
            drawArc(canvas, R.color.reports_macro_fat, startAngle, fatSweep)
        }

        drawCenterText(canvas, size)
    }

    private fun drawCenterText(
        canvas: Canvas,
        size: Float
    ) {
        val centerX = width / 2f
        val centerY = height / 2f

        titlePaint.color = ContextCompat.getColor(context, R.color.profile_text)
        titlePaint.textSize = size * 0.16f

        subtitlePaint.color = ContextCompat.getColor(context, R.color.profile_subtext)
        subtitlePaint.textSize = size * 0.10f

        canvas.drawText(
            centerTitle,
            centerX,
            centerY - size * 0.01f,
            titlePaint
        )

        canvas.drawText(
            centerSubtitle,
            centerX,
            centerY + size * 0.13f,
            subtitlePaint
        )
    }

    private fun drawArc(
        canvas: Canvas,
        colorResId: Int,
        startAngle: Float,
        sweepAngle: Float
    ) {
        if (sweepAngle <= 0f) return

        arcPaint.color = ContextCompat.getColor(context, colorResId)

        canvas.drawArc(
            arcBounds,
            startAngle,
            sweepAngle,
            false,
            arcPaint
        )
    }
}
