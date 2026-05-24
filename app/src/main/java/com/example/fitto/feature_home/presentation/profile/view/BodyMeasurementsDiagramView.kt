package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.BodyMeasurementField
import com.example.fitto.feature_home.presentation.profile.BodyMeasurementsData
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

class BodyMeasurementsDiagramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class LabelBounds(
        val field: BodyMeasurementField,
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )

    var onLabelBoundsChanged: ((List<LabelBounds>) -> Unit)? = null

    private var genderKey: String = ""
    private var isReadOnly: Boolean = false
    private var lastBounds: List<LabelBounds> = emptyList()

    private val femaleBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.body_silhouette_female)
    }

    private val neutralBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.body_silhouette_neutral)
    }

    private val maleBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.body_silhouette_male)
    }

    private val green = ContextCompat.getColor(context, R.color.fitto_green)
    private val mutedGreen = android.graphics.Color.argb(190, 76, 175, 80)

    init {
        isFocusable = false
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }

    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.15f)
        color = mutedGreen
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = green
    }

    fun submitData(
        measurements: BodyMeasurementsData,
        genderKey: String,
        readOnly: Boolean = false
    ) {
        this.genderKey = genderKey
        this.isReadOnly = readOnly
        invalidate()
    }

    fun clearInlineEditing() = Unit
    fun finishExternalEditing() = Unit

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val contentWidth = width - paddingStart - paddingEnd
        val contentHeight = height - paddingTop - paddingBottom
        if (contentWidth <= 0 || contentHeight <= 0) return

        val labelW = min(contentWidth * 0.28f, dp(106f))
        val labelH = dp(48f)
        val labelGap = dp(5f)
        val totalLabelHeight = (labelH * 5f) + (labelGap * 4f)
        val labelTop = paddingTop + ((contentHeight - totalLabelHeight) / 2f).coerceAtLeast(dp(2f))

        val figure = createFigureRect(
            contentWidth = contentWidth.toFloat(),
            contentHeight = contentHeight.toFloat(),
            labelWidth = labelW
        )

        val bitmap = genderKey.toBodyBitmap()
        canvas.drawBitmap(bitmap, null, figure, bitmapPaint)

        val anchorSet = genderKey.toAnchorSet()
        val leftX = paddingStart + dp(6f)
        val rightX = width - paddingEnd - dp(6f) - labelW

        val leftItems = listOf(
            labelItem(BodyMeasurementField.SHOULDER, leftX, labelTop, labelH, labelGap, 0, figure, anchorSet.shoulder),
            labelItem(BodyMeasurementField.CHEST, leftX, labelTop, labelH, labelGap, 1, figure, anchorSet.chest),
            labelItem(BodyMeasurementField.WAIST, leftX, labelTop, labelH, labelGap, 2, figure, anchorSet.waist),
            labelItem(BodyMeasurementField.ABDOMEN, leftX, labelTop, labelH, labelGap, 3, figure, anchorSet.abdomen),
            labelItem(BodyMeasurementField.INNER_LEG, leftX, labelTop, labelH, labelGap, 4, figure, anchorSet.innerLeg)
        )

        val rightItems = listOf(
            labelItem(BodyMeasurementField.ARM, rightX, labelTop, labelH, labelGap, 0, figure, anchorSet.arm),
            labelItem(BodyMeasurementField.HIP, rightX, labelTop, labelH, labelGap, 1, figure, anchorSet.hip),
            labelItem(BodyMeasurementField.BASIN, rightX, labelTop, labelH, labelGap, 2, figure, anchorSet.basin),
            labelItem(BodyMeasurementField.THIGH, rightX, labelTop, labelH, labelGap, 3, figure, anchorSet.thigh),
            labelItem(BodyMeasurementField.CALF, rightX, labelTop, labelH, labelGap, 4, figure, anchorSet.calf)
        )

        val allItems = leftItems + rightItems
        allItems.forEach { item ->
            drawConnector(canvas, item, labelW, labelH)
        }

        val bounds = allItems.map { item ->
            LabelBounds(
                field = item.field,
                left = item.labelX.roundToInt(),
                top = item.labelY.roundToInt(),
                width = labelW.roundToInt(),
                height = labelH.roundToInt()
            )
        }

        if (bounds != lastBounds) {
            lastBounds = bounds
            post { onLabelBoundsChanged?.invoke(bounds) }
        }
    }

    private fun createFigureRect(
        contentWidth: Float,
        contentHeight: Float,
        labelWidth: Float
    ): RectF {
        val horizontalSafeSpace = (contentWidth - (labelWidth * 2f) - dp(18f)).coerceAtLeast(dp(92f))
        val maxFigureWidth = min(horizontalSafeSpace, dp(128f))
        val maxFigureHeight = contentHeight - dp(10f)
        val bitmap = genderKey.toBodyBitmap()
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()

        var figureHeight = maxFigureHeight
        var figureWidth = figureHeight * aspect

        if (figureWidth > maxFigureWidth) {
            figureWidth = maxFigureWidth
            figureHeight = figureWidth / aspect
        }

        val left = paddingStart + (contentWidth - figureWidth) / 2f
        val top = paddingTop + (contentHeight - figureHeight) / 2f

        return RectF(left, top, left + figureWidth, top + figureHeight)
    }

    private fun labelItem(
        field: BodyMeasurementField,
        x: Float,
        startY: Float,
        labelH: Float,
        gap: Float,
        index: Int,
        figure: RectF,
        anchor: AnchorPoint
    ): LabelItem {
        return LabelItem(
            field = field,
            labelX = x,
            labelY = startY + index * (labelH + gap),
            anchorX = figure.left + figure.width() * anchor.x,
            anchorY = figure.top + figure.height() * anchor.y
        )
    }

    private fun drawConnector(canvas: Canvas, item: LabelItem, labelW: Float, labelH: Float) {
        val isLeft = item.labelX < width / 2f
        val startX = if (isLeft) item.labelX + labelW else item.labelX
        val startY = item.labelY + labelH / 2f
        val elbowX = if (isLeft) startX + dp(12f) else startX - dp(12f)

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(elbowX, startY)
            lineTo(item.anchorX, item.anchorY)
        }

        canvas.drawPath(path, connectorPaint)
        canvas.drawCircle(item.anchorX, item.anchorY, dp(2.8f), dotPaint)
    }

    private fun String.toBodyBitmap(): Bitmap {
        return when (lowercase(Locale.US).trim()) {
            "female", "female_gender", "woman", "kadın", "kadin" -> femaleBitmap
            "male", "male_gender", "man", "erkek" -> maleBitmap
            "prefer_not_to_say",
            "neutral",
            "other",
            "unknown",
            "belirtmek_istemiyorum",
            "belirtmek istemiyorum",
            "" -> neutralBitmap
            else -> neutralBitmap
        }
    }

    private fun String.toAnchorSet(): AnchorSet {
        return when (lowercase(Locale.US).trim()) {
            "female", "female_gender", "woman", "kadın", "kadin" -> AnchorSet.Female
            "male", "male_gender", "man", "erkek" -> AnchorSet.Male
            else -> AnchorSet.Neutral
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private data class LabelItem(
        val field: BodyMeasurementField,
        val labelX: Float,
        val labelY: Float,
        val anchorX: Float,
        val anchorY: Float
    )

    private data class AnchorPoint(
        val x: Float,
        val y: Float
    )

    private data class AnchorSet(
        val shoulder: AnchorPoint,
        val chest: AnchorPoint,
        val waist: AnchorPoint,
        val abdomen: AnchorPoint,
        val innerLeg: AnchorPoint,
        val arm: AnchorPoint,
        val hip: AnchorPoint,
        val basin: AnchorPoint,
        val thigh: AnchorPoint,
        val calf: AnchorPoint
    ) {
        companion object {
            val Female = AnchorSet(
                shoulder = AnchorPoint(0.29f, 0.24f),
                chest = AnchorPoint(0.48f, 0.33f),
                waist = AnchorPoint(0.37f, 0.44f),
                abdomen = AnchorPoint(0.47f, 0.48f),
                innerLeg = AnchorPoint(0.50f, 0.64f),
                arm = AnchorPoint(0.79f, 0.35f),
                hip = AnchorPoint(0.70f, 0.56f),
                basin = AnchorPoint(0.66f, 0.61f),
                thigh = AnchorPoint(0.61f, 0.71f),
                calf = AnchorPoint(0.58f, 0.84f)
            )

            val Neutral = AnchorSet(
                shoulder = AnchorPoint(0.25f, 0.24f),
                chest = AnchorPoint(0.49f, 0.34f),
                waist = AnchorPoint(0.37f, 0.46f),
                abdomen = AnchorPoint(0.49f, 0.50f),
                innerLeg = AnchorPoint(0.50f, 0.65f),
                arm = AnchorPoint(0.79f, 0.36f),
                hip = AnchorPoint(0.67f, 0.57f),
                basin = AnchorPoint(0.64f, 0.62f),
                thigh = AnchorPoint(0.60f, 0.72f),
                calf = AnchorPoint(0.58f, 0.85f)
            )

            val Male = AnchorSet(
                shoulder = AnchorPoint(0.23f, 0.24f),
                chest = AnchorPoint(0.49f, 0.34f),
                waist = AnchorPoint(0.37f, 0.46f),
                abdomen = AnchorPoint(0.49f, 0.50f),
                innerLeg = AnchorPoint(0.50f, 0.65f),
                arm = AnchorPoint(0.78f, 0.37f),
                hip = AnchorPoint(0.65f, 0.57f),
                basin = AnchorPoint(0.62f, 0.62f),
                thigh = AnchorPoint(0.59f, 0.72f),
                calf = AnchorPoint(0.58f, 0.85f)
            )
        }
    }
}
