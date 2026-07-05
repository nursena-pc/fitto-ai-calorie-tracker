package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class BodyMeasurementsTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<Pair<Long, Float>> = emptyList()
    private var coordinates: List<Pair<Float, Float>> = emptyList()
    private var selectedPointIndex: Int? = null

    private val green = ContextCompat.getColor(context, R.color.fitto_green)
    private val textColor = ContextCompat.getColor(context, R.color.profile_text)
    private val subTextColor = ContextCompat.getColor(context, R.color.profile_subtext)
    private val strokeColor = ContextCompat.getColor(context, R.color.profile_stroke)
    private val cardColor = ContextCompat.getColor(context, R.color.profile_card_background)

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.profile_screen_background)
        style = Paint.Style.FILL
    }

    private val panelStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        strokeWidth = dp(1f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        strokeWidth = dp(0.8f)
        alpha = 85
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        strokeWidth = dp(2.6f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.FILL
    }

    private val selectedPointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = green
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val markerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        strokeWidth = dp(1.1f)
        alpha = 120
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        textSize = sp(10.5f)
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        textSize = sp(13f)
        textAlign = Paint.Align.CENTER
    }

    private val tooltipBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardColor
        style = Paint.Style.FILL
        setShadowLayer(dp(3f), 0f, dp(1.5f), Color.argb(34, 0, 0, 0))
    }

    private val tooltipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val tooltipTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = sp(11.5f)
        isFakeBoldText = true
    }

    private val tooltipSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = subTextColor
        textSize = sp(10.5f)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun submitData(points: List<Pair<Long, Float>>) {
        this.points = points
            .filter { it.first > 0L }
            .sortedBy { it.first }

        selectedPointIndex = this.points.lastIndex.takeIf { it >= 0 }
        invalidate()
    }

    fun clearSelectedPoint() {
        if (selectedPointIndex == null) return
        selectedPointIndex = null
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.isEmpty() || coordinates.isEmpty()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                selectedPointIndex = findNearestPointIndex(event.x)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.isEmpty()) {
            coordinates = emptyList()
            canvas.drawText(
                context.getString(R.string.profile_body_measurements_chart_empty),
                width / 2f,
                height / 2f,
                emptyPaint
            )
            return
        }

        val chartLeft = paddingStart + dp(42f)
        val chartRight = width - paddingEnd - dp(12f)
        val chartTop = paddingTop + dp(22f)
        val chartBottom = height - paddingBottom - dp(42f)

        if (chartRight <= chartLeft || chartBottom <= chartTop) return

        val values = points.map { it.second }
        val rawMin = values.minOrNull() ?: return
        val rawMax = values.maxOrNull() ?: return

        val yPadding = max(1f, abs(rawMax - rawMin) * 0.15f)
        val minValue = rawMin - yPadding
        val maxValue = rawMax + yPadding
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        drawPanel(canvas, chartLeft, chartRight, chartTop, chartBottom)
        drawAxes(canvas, chartLeft, chartRight, chartTop, chartBottom)
        drawYLabels(canvas, chartTop, chartBottom, minValue, maxValue)
        drawXDateLabels(canvas, chartLeft, chartRight, chartBottom)

        coordinates = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                (chartLeft + chartRight) / 2f
            } else {
                chartLeft + ((chartRight - chartLeft) * index / (points.size - 1).toFloat())
            }

            val normalizedY = (point.second - minValue) / range
            val y = chartBottom - ((chartBottom - chartTop) * normalizedY)

            x to y
        }

        drawAreaFill(canvas, coordinates, chartBottom, chartTop)
        drawLine(canvas, coordinates)
        drawPoints(canvas, coordinates)
        drawSelectedMarker(canvas, chartTop, chartBottom)
    }

    private fun drawPanel(
        canvas: Canvas,
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float
    ) {
        val rect = RectF(
            chartLeft - dp(10f),
            chartTop - dp(12f),
            chartRight + dp(8f),
            chartBottom + dp(12f)
        )

        val radius = dp(20f)
        canvas.drawRoundRect(rect, radius, radius, panelPaint)
        canvas.drawRoundRect(rect, radius, radius, panelStrokePaint)
    }

    private fun drawAxes(
        canvas: Canvas,
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float
    ) {
        val midY = (chartTop + chartBottom) / 2f

        canvas.drawLine(chartLeft, chartTop, chartRight, chartTop, gridPaint)
        canvas.drawLine(chartLeft, midY, chartRight, midY, gridPaint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)
    }

    private fun drawAreaFill(
        canvas: Canvas,
        pointsToDraw: List<Pair<Float, Float>>,
        chartBottom: Float,
        chartTop: Float
    ) {
        if (pointsToDraw.size < 2) return

        val areaPath = Path()
        val first = pointsToDraw.first()
        val last = pointsToDraw.last()

        areaPath.moveTo(first.first, chartBottom)
        pointsToDraw.forEach { pair ->
            areaPath.lineTo(pair.first, pair.second)
        }
        areaPath.lineTo(last.first, chartBottom)
        areaPath.close()

        val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                chartTop,
                0f,
                chartBottom,
                Color.argb(26, 76, 175, 80),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawPath(areaPath, areaPaint)
    }

    private fun drawLine(
        canvas: Canvas,
        pointsToDraw: List<Pair<Float, Float>>
    ) {
        if (pointsToDraw.isEmpty()) return

        val path = Path()
        pointsToDraw.forEachIndexed { index, pair ->
            val (x, y) = pair
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, linePaint)
    }

    private fun drawPoints(
        canvas: Canvas,
        pointsToDraw: List<Pair<Float, Float>>
    ) {
        pointsToDraw.forEach { pair ->
            canvas.drawCircle(pair.first, pair.second, dp(3.8f), pointPaint)
        }
    }

    private fun drawSelectedMarker(
        canvas: Canvas,
        chartTop: Float,
        chartBottom: Float
    ) {
        val index = selectedPointIndex ?: return
        if (index !in points.indices || index !in coordinates.indices) return

        val point = points[index]
        val coordinate = coordinates[index]

        val x = coordinate.first
        val y = coordinate.second

        canvas.drawLine(x, chartTop, x, chartBottom, markerLinePaint)
        canvas.drawCircle(x, y, dp(5f), pointPaint)
        canvas.drawCircle(x, y, dp(8f), selectedPointStrokePaint)

        drawTooltip(canvas, x, y, point)
    }

    private fun drawTooltip(
        canvas: Canvas,
        pointX: Float,
        pointY: Float,
        point: Pair<Long, Float>
    ) {
        val dateText = DateFormat
            .getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(Date(point.first))

        val valueText = String.format(Locale.getDefault(), "%.2f cm", point.second)

        val horizontalPadding = dp(12f)
        val tooltipWidth = max(
            tooltipTitlePaint.measureText(valueText),
            tooltipSubPaint.measureText(dateText)
        ) + horizontalPadding * 2f

        val tooltipHeight = dp(52f)
        val tooltipLeft = when {
            pointX + tooltipWidth + dp(12f) <= width - paddingEnd -> {
                pointX + dp(10f)
            }

            else -> {
                pointX - tooltipWidth - dp(10f)
            }
        }.coerceIn(
            paddingStart.toFloat(),
            width - paddingEnd - tooltipWidth
        )

        val tooltipTop = (pointY - tooltipHeight - dp(12f)).let { preferredTop ->
            if (preferredTop >= paddingTop + dp(4f)) {
                preferredTop
            } else {
                pointY + dp(12f)
            }
        }.coerceIn(
            paddingTop + dp(4f),
            height - paddingBottom - tooltipHeight - dp(4f)
        )

        val rect = RectF(
            tooltipLeft,
            tooltipTop,
            tooltipLeft + tooltipWidth,
            tooltipTop + tooltipHeight
        )

        val radius = dp(14f)

        canvas.drawRoundRect(rect, radius, radius, tooltipBackgroundPaint)
        canvas.drawRoundRect(rect, radius, radius, tooltipStrokePaint)

        canvas.drawText(
            valueText,
            rect.left + horizontalPadding,
            rect.top + dp(21f),
            tooltipTitlePaint
        )

        canvas.drawText(
            dateText,
            rect.left + horizontalPadding,
            rect.top + dp(39f),
            tooltipSubPaint
        )
    }

    private fun drawYLabels(
        canvas: Canvas,
        chartTop: Float,
        chartBottom: Float,
        minValue: Float,
        maxValue: Float
    ) {
        val maxLabel = String.format(Locale.getDefault(), "%.1f", maxValue)
        val minLabel = String.format(Locale.getDefault(), "%.1f", minValue)

        canvas.drawText(maxLabel, paddingStart + dp(4f), chartTop + dp(4f), textPaint)
        canvas.drawText(minLabel, paddingStart + dp(4f), chartBottom, textPaint)
    }

    private fun drawXDateLabels(
        canvas: Canvas,
        chartLeft: Float,
        chartRight: Float,
        chartBottom: Float
    ) {
        val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

        val labelIndexes = when (points.size) {
            1 -> listOf(0)
            2 -> listOf(0, 1)
            else -> listOf(0, points.lastIndex / 2, points.lastIndex).distinct()
        }

        labelIndexes.forEach { index ->
            val x = if (points.size == 1) {
                (chartLeft + chartRight) / 2f
            } else {
                chartLeft + ((chartRight - chartLeft) * index / (points.size - 1).toFloat())
            }

            val label = dateFormat.format(Date(points[index].first))
            val textWidth = textPaint.measureText(label)

            val safeX = when (index) {
                0 -> x
                points.lastIndex -> x - textWidth
                else -> x - textWidth / 2f
            }.coerceIn(chartLeft, chartRight - textWidth)

            canvas.drawText(label, safeX, chartBottom + dp(24f), textPaint)
        }
    }

    private fun findNearestPointIndex(touchX: Float): Int {
        if (coordinates.size == 1) return 0

        return coordinates
            .mapIndexed { index, pair ->
                index to abs(pair.first - touchX)
            }
            .minByOrNull { it.second }
            ?.first ?: 0
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            value,
            resources.displayMetrics
        )
    }
}