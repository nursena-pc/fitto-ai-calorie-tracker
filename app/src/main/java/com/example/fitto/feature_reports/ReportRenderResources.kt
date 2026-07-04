package com.example.fitto.feature_reports

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.example.fitto.R
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textview.MaterialTextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class ReportRenderResources(
    private val context: Context
) {
    private val resources = context.resources

    fun string(resId: Int, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *formatArgs)
        }
    }

    fun integer(resId: Int): Int = resources.getInteger(resId)

    fun fraction(resId: Int): Float = resources.getFraction(resId, 1, 1)

    fun color(@ColorRes colorResId: Int): Int {
        return ContextCompat.getColor(context, colorResId)
    }

    fun chartDp(@DimenRes dimenResId: Int): Float {
        return resources.getDimension(dimenResId) / resources.displayMetrics.density
    }

    fun chartSp(@DimenRes dimenResId: Int): Float {
        val scaledDensity = resources.displayMetrics.density * resources.configuration.fontScale
        return resources.getDimension(dimenResId) / scaledDensity
    }

    fun displayDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern(
            string(R.string.reports_short_date_format_pattern),
            Locale.getDefault()
        )
        return date.format(formatter)
    }

    fun formatWeightOrDash(value: Double): String {
        if (value <= 0.0) return string(R.string.reports_value_empty)

        val formattedValue = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(
                Locale.getDefault(),
                string(R.string.reports_decimal_one_pattern),
                value
            )
        }

        return string(R.string.reports_format_kg, formattedValue)
    }

    fun macroLabel(type: ReportMacroSummaryResolver.MacroType): String {
        return when (type) {
            ReportMacroSummaryResolver.MacroType.PROTEIN -> {
                string(R.string.reports_stat_protein)
            }
            ReportMacroSummaryResolver.MacroType.CARBS -> {
                string(R.string.reports_stat_carbs)
            }
            ReportMacroSummaryResolver.MacroType.FAT -> {
                string(R.string.reports_stat_fat)
            }
        }
    }

    fun detailRow(
        title: String,
        subtitle: String,
        value: String
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                0,
                resources.getDimensionPixelSize(R.dimen.reports_detail_row_vertical_padding),
                0,
                resources.getDimensionPixelSize(R.dimen.reports_detail_row_vertical_padding)
            )
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        textColumn.addView(
            MaterialTextView(context).apply {
                text = title
                setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
                )
                setTextColor(color(R.color.reports_text_primary))
            }
        )
        textColumn.addView(
            MaterialTextView(context).apply {
                text = subtitle
                setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodySmall
                )
                setTextColor(color(R.color.reports_text_secondary))
            }
        )

        row.addView(
            textColumn,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        row.addView(
            MaterialTextView(context).apply {
                text = value
                setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_TitleSmall
                )
                setTextColor(color(R.color.reports_tab_selected))
                gravity = Gravity.END
            }
        )

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(row)
            addView(
                MaterialDivider(context).apply {
                    setDividerColor(color(R.color.reports_divider))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.reports_detail_divider_height)
                )
            )
        }
    }

    fun emptyDetailRow(): View {
        return MaterialTextView(context).apply {
            text = string(R.string.reports_detail_no_data)
            setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            )
            setTextColor(color(R.color.reports_text_secondary))
            gravity = Gravity.CENTER
            setPadding(
                0,
                resources.getDimensionPixelSize(R.dimen.reports_detail_empty_vertical_padding),
                0,
                resources.getDimensionPixelSize(R.dimen.reports_detail_empty_vertical_padding)
            )
        }
    }
}
