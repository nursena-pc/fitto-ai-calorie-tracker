package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.ranges.coerceIn

enum class ReminderClockMarkerKind {
    NORMAL,
    WATER
}

enum class ReminderClockMarkerCategory {
    MEAL,
    WATER,
    EXERCISE,
    WEIGHT,
    CUSTOM
}

data class ReminderClockMarker(
    val id: String,
    val minuteOfDay: Int,
    val kind: ReminderClockMarkerKind,
    val category: ReminderClockMarkerCategory,
    val title: String,
    val timeText: String,
    val isPast: Boolean,
    val isNext: Boolean
)

class ReminderPlanClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dottedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timeLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private val markerAreas = mutableListOf<MarkerArea>()

    private var timeText: String = "09:41"
    private var dayText: String = "Pazartesi"
    private var dateText: String = "20 Mayıs"
    private var currentMinuteOfDay: Int = 9 * 60 + 41
    private var markers: List<ReminderClockMarker> = emptyList()
    private var selectedMarkerId: String? = null
    private var onMarkerSelected: ((ReminderClockMarker?) -> Unit)? = null

    fun setOnMarkerSelectedListener(listener: ((ReminderClockMarker?) -> Unit)?) {
        onMarkerSelected = listener
    }

    fun clearSelection() {
        selectedMarkerId = null
        onMarkerSelected?.invoke(null)
        invalidate()
    }

    fun submitData(
        timeText: String,
        dayText: String,
        dateText: String,
        currentMinuteOfDay: Int,
        markers: List<ReminderClockMarker>
    ) {
        this.timeText = timeText
        this.dayText = dayText
        this.dateText = dateText
        this.currentMinuteOfDay = currentMinuteOfDay.coerceIn(0, 1439)
        this.markers = markers
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        markerAreas.clear()

        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val radius = size * 0.39f
        val outerRadius = size * 0.46f
        val stroke = size * 0.045f

        val trackColor = ContextCompat.getColor(context, R.color.profile_progress_track)
        val yellow = ContextCompat.getColor(context, R.color.fitto_yellow)
        val white = ContextCompat.getColor(context, R.color.fitto_white)
        val textColor = ContextCompat.getColor(context, R.color.fitto_white)

        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeWidth = stroke
        trackPaint.strokeCap = Paint.Cap.ROUND
        trackPaint.color = trackColor
        trackPaint.alpha = 115

        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = stroke
        progressPaint.strokeCap = Paint.Cap.ROUND
        progressPaint.color = yellow
        progressPaint.alpha = 245

        dottedPaint.style = Paint.Style.STROKE
        dottedPaint.strokeWidth = 3f
        dottedPaint.color = white
        dottedPaint.alpha = 120
        dottedPaint.pathEffect = DashPathEffect(floatArrayOf(2f, 12f), 0f)

        val left = cx - radius
        val top = cy - radius
        val right = cx + radius
        val bottom = cy + radius

        canvas.drawCircle(cx, cy, outerRadius, dottedPaint)
        canvas.drawCircle(cx, cy, radius, trackPaint)

        val sweep = (currentMinuteOfDay / 1440f) * 360f
        canvas.drawArc(left, top, right, bottom, -90f, sweep, false, progressPaint)

        drawTimeLabels(canvas, cx, cy, outerRadius, size)

        markers.forEach { marker ->
            drawMarker(
                canvas = canvas,
                marker = marker,
                cx = cx,
                cy = cy,
                radius = radius,
                size = size
            )
        }

        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = size * 0.165f
        textPaint.isFakeBoldText = true

        canvas.drawText(
            timeText,
            cx,
            cy - size * 0.015f,
            textPaint
        )

        subTextPaint.color = textColor
        subTextPaint.textAlign = Paint.Align.CENTER
        subTextPaint.textSize = size * 0.055f
        subTextPaint.isFakeBoldText = true

        canvas.drawText(
            dayText,
            cx,
            cy + size * 0.09f,
            subTextPaint
        )

        subTextPaint.isFakeBoldText = false

        canvas.drawText(
            dateText,
            cx,
            cy + size * 0.16f,
            subTextPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) {
            return true
        }

        val touchedMarker = markerAreas.firstOrNull { area ->
            val dx = event.x - area.cx
            val dy = event.y - area.cy
            dx.pow(2) + dy.pow(2) <= area.hitRadius.pow(2)
        }?.marker

        selectedMarkerId = touchedMarker?.id
        onMarkerSelected?.invoke(touchedMarker)
        invalidate()

        return true
    }

    private fun drawTimeLabels(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        size: Float
    ) {
        timeLabelPaint.color = ContextCompat.getColor(context, R.color.fitto_white)
        timeLabelPaint.alpha = 210
        timeLabelPaint.textAlign = Paint.Align.CENTER
        timeLabelPaint.textSize = size * 0.05f
        timeLabelPaint.isFakeBoldText = true

        drawTimeLabel(canvas, "00", cx, cy, radius, 0)
        drawTimeLabel(canvas, "06", cx, cy, radius, 6 * 60)
        drawTimeLabel(canvas, "12", cx, cy, radius, 12 * 60)
        drawTimeLabel(canvas, "18", cx, cy, radius, 18 * 60)

        timeLabelPaint.alpha = 255
    }

    private fun drawTimeLabel(
        canvas: Canvas,
        label: String,
        cx: Float,
        cy: Float,
        radius: Float,
        minute: Int
    ) {
        val angle = minuteToAngle(minute)
        val labelCx = cx + cos(angle).toFloat() * radius
        val labelCy = cy + sin(angle).toFloat() * radius + 8f

        canvas.drawText(label, labelCx, labelCy, timeLabelPaint)
    }

    private fun drawMarker(
        canvas: Canvas,
        marker: ReminderClockMarker,
        cx: Float,
        cy: Float,
        radius: Float,
        size: Float
    ) {
        val angle = minuteToAngle(marker.minuteOfDay)
        val markerCx = cx + cos(angle).toFloat() * radius
        val markerCy = cy + sin(angle).toFloat() * radius

        val selected = marker.id == selectedMarkerId
        val markerAlpha = when {
            selected -> 255
            marker.isNext -> 255
            marker.isPast -> 92
            else -> 220
        }

        val markerSize = when {
            selected -> size * 0.035f
            marker.isNext -> size * 0.032f
            else -> size * 0.024f
        }

        if (marker.kind == ReminderClockMarkerKind.WATER) {
            drawWaterDrop(
                canvas = canvas,
                cx = markerCx,
                cy = markerCy,
                size = markerSize * 1.12f,
                fill = ContextCompat.getColor(context, R.color.reminder_marker_water),
                stroke = ContextCompat.getColor(context, R.color.fitto_white),
                alpha = markerAlpha
            )
        } else {
            markerPaint.style = Paint.Style.FILL
            markerPaint.color = markerColor(marker.category)
            markerPaint.alpha = markerAlpha

            markerStrokePaint.style = Paint.Style.STROKE
            markerStrokePaint.strokeWidth = if (selected || marker.isNext) 4.2f else 3f
            markerStrokePaint.color = ContextCompat.getColor(context, R.color.fitto_white)
            markerStrokePaint.alpha = markerAlpha

            canvas.drawCircle(markerCx, markerCy, markerSize, markerPaint)
            canvas.drawCircle(markerCx, markerCy, markerSize, markerStrokePaint)

            markerPaint.alpha = 255
            markerStrokePaint.alpha = 255
        }

        markerAreas.add(
            MarkerArea(
                marker = marker,
                cx = markerCx,
                cy = markerCy,
                hitRadius = size * 0.07f
            )
        )
    }

    private fun markerColor(category: ReminderClockMarkerCategory): Int {
        return when (category) {
            ReminderClockMarkerCategory.MEAL ->
                ContextCompat.getColor(context, R.color.fitto_yellow)

            ReminderClockMarkerCategory.WATER ->
                ContextCompat.getColor(context, R.color.reminder_marker_water)

            ReminderClockMarkerCategory.EXERCISE ->
                ContextCompat.getColor(context, R.color.fitto_green)

            ReminderClockMarkerCategory.WEIGHT ->
                ContextCompat.getColor(context, R.color.reminder_marker_weight)

            ReminderClockMarkerCategory.CUSTOM ->
                ContextCompat.getColor(context, R.color.fitto_orange)
        }
    }

    private fun minuteToAngle(minute: Int): Double {
        val normalizedMinute = minute.coerceIn(0, 1439)
        val degrees = (normalizedMinute / 1440f) * 360f - 90f
        return Math.toRadians(degrees.toDouble())
    }

    private fun drawWaterDrop(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        fill: Int,
        stroke: Int,
        alpha: Int
    ) {
        path.reset()
        path.moveTo(cx, cy - size * 1.15f)
        path.cubicTo(
            cx - size * 0.90f,
            cy - size * 0.18f,
            cx - size * 0.72f,
            cy + size * 0.78f,
            cx,
            cy + size * 0.92f
        )
        path.cubicTo(
            cx + size * 0.72f,
            cy + size * 0.78f,
            cx + size * 0.90f,
            cy - size * 0.18f,
            cx,
            cy - size * 1.15f
        )
        path.close()

        waterPaint.style = Paint.Style.FILL
        waterPaint.color = fill
        waterPaint.alpha = alpha
        canvas.drawPath(path, waterPaint)

        waterStrokePaint.style = Paint.Style.STROKE
        waterStrokePaint.strokeWidth = 2.3f
        waterStrokePaint.color = stroke
        waterStrokePaint.alpha = alpha
        canvas.drawPath(path, waterStrokePaint)

        waterPaint.alpha = 255
        waterStrokePaint.alpha = 255
    }

    private data class MarkerArea(
        val marker: ReminderClockMarker,
        val cx: Float,
        val cy: Float,
        val hitRadius: Float
    )
}