package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R

class FittoBottomDecorationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val primary = ContextCompat.getColor(context, R.color.fitto_green)
        val secondary = ContextCompat.getColor(context, R.color.fitto_yellow)
        val background = ContextCompat.getColor(context, R.color.profile_screen_background)

        drawSoftBackWave(canvas, w, h, primary)
        drawYellowWave(canvas, w, h, secondary)
        drawFrontGreenHill(canvas, w, h, primary)
        drawDots(canvas, w, h, secondary)
        drawPlant(canvas, w, h, primary, background)
    }

    private fun drawSoftBackWave(canvas: Canvas, w: Float, h: Float, color: Int) {
        path.reset()
        path.moveTo(0f, h * 0.58f)
        path.quadTo(w * 0.18f, h * 0.42f, w * 0.36f, h * 0.60f)
        path.quadTo(w * 0.56f, h * 0.78f, w * 0.76f, h * 0.60f)
        path.quadTo(w * 0.90f, h * 0.47f, w, h * 0.56f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 36
        canvas.drawPath(path, paint)
        paint.alpha = 255
    }

    private fun drawYellowWave(canvas: Canvas, w: Float, h: Float, color: Int) {
        path.reset()
        path.moveTo(0f, h * 0.78f)
        path.quadTo(w * 0.20f, h * 0.70f, w * 0.40f, h * 0.82f)
        path.quadTo(w * 0.64f, h * 0.94f, w * 0.82f, h * 0.72f)
        path.quadTo(w * 0.92f, h * 0.60f, w, h * 0.68f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 242
        canvas.drawPath(path, paint)
        paint.alpha = 255
    }

    private fun drawFrontGreenHill(canvas: Canvas, w: Float, h: Float, color: Int) {
        path.reset()
        path.moveTo(0f, h * 0.82f)
        path.quadTo(w * 0.22f, h * 0.62f, w * 0.46f, h * 0.84f)
        path.quadTo(w * 0.66f, h, w * 0.84f, h * 0.82f)
        path.quadTo(w * 0.92f, h * 0.74f, w, h * 0.80f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 148
        canvas.drawPath(path, paint)
        paint.alpha = 255
    }

    private fun drawDots(canvas: Canvas, w: Float, h: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color

        paint.alpha = 217
        canvas.drawCircle(w * 0.08f, h * 0.63f, 9f, paint)

        paint.alpha = 191
        canvas.drawCircle(w * 0.16f, h * 0.74f, 11f, paint)

        paint.alpha = 178
        canvas.drawCircle(w * 0.68f, h * 0.70f, 9f, paint)

        paint.alpha = 255
    }

    private fun drawPlant(canvas: Canvas, w: Float, h: Float, primary: Int, background: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = primary
        paint.alpha = 191
        paint.strokeWidth = 6f
        paint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(w * 0.84f, h * 0.88f, w * 0.86f, h * 0.58f, paint)

        paint.style = Paint.Style.FILL
        paint.alpha = 198

        path.reset()
        path.moveTo(w * 0.86f, h * 0.62f)
        path.quadTo(w * 0.78f, h * 0.54f, w * 0.80f, h * 0.40f)
        path.quadTo(w * 0.89f, h * 0.42f, w * 0.86f, h * 0.62f)
        path.close()
        canvas.drawPath(path, paint)

        paint.alpha = 158
        path.reset()
        path.moveTo(w * 0.86f, h * 0.60f)
        path.quadTo(w * 0.93f, h * 0.50f, w * 0.94f, h * 0.36f)
        path.quadTo(w * 0.85f, h * 0.40f, w * 0.86f, h * 0.60f)
        path.close()
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.color = background
        paint.alpha = 77
        paint.strokeWidth = 2f
        paint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(w * 0.83f, h * 0.46f, w * 0.88f, h * 0.59f, paint)
        canvas.drawLine(w * 0.90f, h * 0.42f, w * 0.87f, h * 0.57f, paint)

        paint.alpha = 255
    }
}