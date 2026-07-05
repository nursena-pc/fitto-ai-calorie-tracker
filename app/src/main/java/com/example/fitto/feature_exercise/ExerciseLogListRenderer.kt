package com.example.fitto.feature_exercise

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.fitto.R
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

class ExerciseLogListRenderer(
    private val context: Context,
    private val logsContainer: LinearLayout,
    private val emptyText: TextView,
    private val summaryText: TextView,
    private val emojiProvider: (String) -> String,
    private val onEdit: (ExerciseLogLocalModel) -> Unit,
    private val onDelete: (String) -> Unit
) {

    fun render(snapshot: ExerciseLogSnapshot) {
        logsContainer.removeAllViews()
        emptyText.isVisible = snapshot.logs.isEmpty()
        summaryText.isVisible = snapshot.logs.isNotEmpty()
        summaryText.text = context.getString(
            R.string.exercise_today_summary,
            snapshot.summary.logCount,
            snapshot.summary.totalDurationMinutes,
            snapshot.summary.totalCalories
        )

        snapshot.logs.forEach { log ->
            logsContainer.addView(createLogRow(log))
        }
    }

    private fun createLogRow(log: ExerciseLogLocalModel): MaterialCardView {
        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dp
            }
            radius = 16.dp.toFloat()
            cardElevation = 0.dp.toFloat()
            strokeWidth = 1.dp
            setStrokeColor(ContextCompat.getColor(context, R.color.fitto_divider_soft))
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.fitto_background_light))

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12.dp, 10.dp, 8.dp, 10.dp)

                    addView(
                        TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp)
                            gravity = Gravity.CENTER
                            text = emojiProvider(log.exerciseId)
                            textSize = 22f
                            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
                        }
                    )

                    addView(
                        LinearLayout(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            ).apply {
                                marginStart = 10.dp
                            }
                            orientation = LinearLayout.VERTICAL

                            addView(
                                TextView(context).apply {
                                    text = log.exerciseTitle
                                    setTextColor(
                                        ContextCompat.getColor(context, R.color.fitto_text_dark)
                                    )
                                    textSize = 14f
                                    typeface = Typeface.DEFAULT_BOLD
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                }
                            )

                            addView(
                                TextView(context).apply {
                                    text = context.getString(
                                        R.string.exercise_log_detail,
                                        log.durationMinutes,
                                        log.intensityName,
                                        log.calories
                                    )
                                    setTextColor(
                                        ContextCompat.getColor(
                                            context,
                                            R.color.fitto_text_secondary
                                        )
                                    )
                                    textSize = 12f
                                    maxLines = 1
                                    ellipsize = TextUtils.TruncateAt.END
                                }
                            )
                        }
                    )

                    addView(createActionButton(
                        iconRes = R.drawable.ic_edit,
                        contentDescription = context.getString(R.string.exercise_edit_log),
                        tintRes = R.color.fitto_green,
                        addEndMargin = true,
                        onClick = { onEdit(log) }
                    ))
                    addView(createActionButton(
                        iconRes = R.drawable.ic_delete,
                        contentDescription = context.getString(R.string.exercise_delete_log),
                        tintRes = R.color.fitto_orange,
                        addEndMargin = false,
                        onClick = { onDelete(log.id) }
                    ))
                }
            )
        }
    }

    private fun createActionButton(
        iconRes: Int,
        contentDescription: String,
        tintRes: Int,
        addEndMargin: Boolean,
        onClick: () -> Unit
    ): ImageButton {
        return ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp).apply {
                if (addEndMargin) marginEnd = 6.dp
            }
            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
            this.contentDescription = contentDescription
            setImageResource(iconRes)
            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tintRes))
            setOnClickListener { onClick() }
        }
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).roundToInt()
}
