package com.example.fitto.feature_home.presentation.profile.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.fitto.R

class ProfileHeroBackgroundView @JvmOverloads constructor(
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

        val background = color(R.color.profile_screen_background)
        val green = color(R.color.fitto_green)
        val yellow = color(R.color.fitto_yellow)

        drawBackground(canvas, w, h, background)
        drawGreenLeaves(canvas, w, h, green, background)
        drawYellowLeaf(canvas, w, h, yellow, background)
    }

    private fun color(resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }

    private fun drawBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        background: Int
    ) {
        paint.shader = null
        paint.color = background
        paint.alpha = 255
        paint.style = Paint.Style.FILL

        canvas.drawRect(0f, 0f, w, h, paint)
    }

    private fun drawGreenLeaves(
        canvas: Canvas,
        w: Float,
        h: Float,
        green: Int,
        background: Int
    ) {
        // Arkadaki geniş yeşil yaprak
        path.reset()
        path.moveTo(w * -0.10f, h * 0.83f)
        path.cubicTo(w * 0.12f, h * 0.50f, w * 0.30f, h * 0.30f, w * 0.54f, h * 0.34f)
        path.cubicTo(w * 0.78f, h * 0.38f, w * 0.90f, h * 0.20f, w * 1.06f, h * 0.10f)
        path.cubicTo(w * 0.90f, h * 0.48f, w * 0.66f, h * 0.65f, w * 0.42f, h * 0.57f)
        path.cubicTo(w * 0.20f, h * 0.50f, w * 0.06f, h * 0.62f, w * -0.10f, h * 0.83f)
        path.close()

        paint.color = green
        paint.alpha = 45
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // Ana yeşil yaprak
        path.reset()
        path.moveTo(w * -0.06f, h * 0.74f)
        path.cubicTo(w * 0.10f, h * 0.28f, w * 0.32f, h * 0.06f, w * 0.57f, h * 0.17f)
        path.cubicTo(w * 0.75f, h * 0.25f, w * 0.88f, h * 0.17f, w * 1.08f, h * 0.03f)
        path.cubicTo(w * 0.88f, h * 0.39f, w * 0.67f, h * 0.51f, w * 0.47f, h * 0.43f)
        path.cubicTo(w * 0.28f, h * 0.35f, w * 0.12f, h * 0.48f, w * -0.06f, h * 0.74f)
        path.close()

        paint.shader = LinearGradient(
            0f,
            h * 0.72f,
            w,
            h * 0.05f,
            green,
            background,
            Shader.TileMode.CLAMP
        )
        paint.alpha = 165
        canvas.drawPath(path, paint)
        paint.shader = null

        // Yeşil yaprak damarı
        path.reset()
        path.moveTo(w * 0.10f, h * 0.58f)
        path.cubicTo(w * 0.24f, h * 0.30f, w * 0.40f, h * 0.18f, w * 0.61f, h * 0.24f)
        path.cubicTo(w * 0.76f, h * 0.28f, w * 0.88f, h * 0.19f, w * 0.98f, h * 0.10f)

        paint.color = background
        paint.alpha = 90
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.010f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)

        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun drawYellowLeaf(
        canvas: Canvas,
        w: Float,
        h: Float,
        yellow: Int,
        background: Int
    ) {
        // Ana sarı yaprak
        path.reset()
        path.moveTo(w * 1.06f, h * 0.90f)
        path.cubicTo(w * 0.92f, h * 0.72f, w * 0.78f, h * 0.58f, w * 0.60f, h * 0.50f)
        path.cubicTo(w * 0.50f, h * 0.46f, w * 0.40f, h * 0.48f, w * 0.30f, h * 0.54f)
        path.cubicTo(w * 0.42f, h * 0.64f, w * 0.56f, h * 0.72f, w * 0.70f, h * 0.72f)
        path.cubicTo(w * 0.84f, h * 0.72f, w * 0.96f, h * 0.82f, w * 1.06f, h * 0.90f)
        path.close()

        paint.shader = LinearGradient(
            w,
            h * 0.92f,
            w * 0.20f,
            h * 0.48f,
            yellow,
            background,
            Shader.TileMode.CLAMP
        )
        paint.alpha = 175
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        paint.shader = null

        // Sarı yaprak damarı
        path.reset()
        path.moveTo(w * 0.92f, h * 0.84f)
        path.cubicTo(w * 0.80f, h * 0.70f, w * 0.66f, h * 0.58f, w * 0.48f, h * 0.52f)
        path.cubicTo(w * 0.38f, h * 0.50f, w * 0.30f, h * 0.52f, w * 0.22f, h * 0.58f)

        paint.color = background
        paint.alpha = 75
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.010f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)

        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }
}