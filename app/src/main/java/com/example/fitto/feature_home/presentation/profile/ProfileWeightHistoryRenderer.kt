package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ProfileWeightHistoryRenderer(
    private val context: Context,
    private val container: LinearLayout,
    private val onItemSelected: (String) -> Unit
) {
    fun render(
        items: List<WeightHistoryItem>,
        selectedItemId: String?
    ) {
        container.removeAllViews()

        if (items.isEmpty()) {
            container.addView(createEmptyView())
            return
        }

        items.forEach { item ->
            container.addView(
                createHistoryRow(
                    item = item,
                    isSelected = item.id == selectedItemId
                )
            )
        }
    }

    private fun createEmptyView(): View {
        return TextView(context).apply {
            text = context.getString(R.string.weight_history_empty)
            setTextColor(color(R.color.profile_subtext))
            textSize = 14f
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedDrawable(
                color = color(R.color.profile_card_background),
                strokeColor = color(R.color.profile_stroke),
                radiusDp = 22
            )
        }
    }

    private fun createHistoryRow(
        item: WeightHistoryItem,
        isSelected: Boolean
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(14), dp(14))
            background = roundedDrawable(
                color = color(R.color.profile_card_background),
                strokeColor = if (isSelected) color(R.color.fitto_green) else color(R.color.profile_stroke),
                radiusDp = 22
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { onItemSelected(item.id) }
        }

        row.addView(createTrendIcon(item.trend), LinearLayout.LayoutParams(dp(44), dp(44)))
        row.addView(
            createHistoryTexts(item),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        row.addView(
            createWeightValue(item.weightKg),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        row.addView(
            createChevron(),
            LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        return row
    }

    private fun createTrendIcon(trend: WeightTrend): ImageView {
        return ImageView(context).apply {
            setImageResource(
                when (trend) {
                    WeightTrend.UP -> R.drawable.ic_arrow_upward
                    WeightTrend.DOWN -> R.drawable.ic_arrow_downward
                    WeightTrend.SAME -> R.drawable.ic_history
                }
            )
            setColorFilter(
                when (trend) {
                    WeightTrend.UP -> color(R.color.fitto_orange)
                    WeightTrend.DOWN -> color(R.color.fitto_green)
                    WeightTrend.SAME -> color(R.color.profile_subtext)
                }
            )
            background = roundedDrawable(
                color = color(R.color.fitto_primary_soft),
                strokeColor = color(R.color.fitto_primary_soft),
                radiusDp = 18
            )
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
    }

    private fun createHistoryTexts(item: WeightHistoryItem): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(TextView(context).apply {
                text = formatDateOrEmpty(item.createdAt)
                setTextColor(color(R.color.profile_text))
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = context.getString(R.string.weight_history_item_subtitle)
                setTextColor(color(R.color.profile_subtext))
                textSize = 13f
            })
        }
    }

    private fun createWeightValue(weightKg: Double): TextView {
        return TextView(context).apply {
            text = context.getString(R.string.weight_value_format, formatWeightKg(weightKg))
            setTextColor(color(R.color.profile_text))
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
    }

    private fun createChevron(): TextView {
        return TextView(context).apply {
            text = "›"
            setTextColor(color(R.color.profile_subtext))
            textSize = 28f
            gravity = Gravity.CENTER
        }
    }

    private fun formatDateOrEmpty(timestamp: Long): String {
        return if (timestamp > 0L) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
        } else {
            context.getString(R.string.profile_body_measurements_no_date)
        }
    }

    private fun roundedDrawable(
        color: Int,
        strokeColor: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            setStroke(dp(1), strokeColor)
        }
    }

    private fun color(resourceId: Int): Int = ContextCompat.getColor(context, resourceId)

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
