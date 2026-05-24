package com.example.fitto.feature_reports

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.fitto.R
import com.example.fitto.data.local.FittoDatabaseProvider
import com.example.fitto.data.local.session.UserSessionDataGuard
import com.example.fitto.feature_home.presentation.profile.ProfileRepository
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightChartPoint
import com.example.fitto.feature_home.presentation.social.NotificationsFragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class ReportsFragment : Fragment() {

    private val profileRepository = ProfileRepository()

    private lateinit var dateButton: MaterialButton
    private lateinit var viewPager: ViewPager2
    private lateinit var caloriePage: CaloriePageRefs
    private lateinit var macroPage: MacroPageRefs
    private lateinit var weightPage: WeightPageRefs

    private var selectedDate: LocalDate = LocalDate.now()
    private var nutritionDays: List<ReportDay> = emptyList()
    private var healthData: ProfileHealthData = ProfileHealthData.Empty
    private var lastReportPageIndex = 0

    private val tabTitleResIds = intArrayOf(
        R.string.reports_tab_calorie,
        R.string.reports_tab_macro,
        R.string.reports_tab_weight
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.reports_screen_background))
            setPadding(
                px(R.dimen.reports_screen_horizontal_padding),
                px(R.dimen.reports_screen_top_padding),
                px(R.dimen.reports_screen_horizontal_padding),
                0
            )
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(createDailyLikeHeader(context))
        root.addView(createDateSelector(context))

        val tabLayout = TabLayout(context).apply {
            setSelectedTabIndicatorColor(color(R.color.reports_tab_selected))
            setTabTextColors(
                color(R.color.reports_tab_unselected),
                color(R.color.reports_tab_selected)
            )
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            setBackgroundColor(color(android.R.color.transparent))
        }

        root.addView(
            tabLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(R.dimen.reports_tab_height)
            )
        )

        caloriePage = createCaloriePage(context)
        macroPage = createMacroPage(context)
        weightPage = createWeightPage(context)

        viewPager = ViewPager2(context).apply {
            adapter = StaticViewPagerAdapter(
                listOf(
                    caloriePage.root,
                    macroPage.root,
                    weightPage.root
                )
            )
            offscreenPageLimit = tabTitleResIds.size
            isUserInputEnabled = true
        }

        root.addView(
            viewPager,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getString(tabTitleResIds[position])
        }.attach()
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    scrollReportPageToTop(lastReportPageIndex)
                    scrollReportPageToTop(position)
                    lastReportPageIndex = position
                }
            }
        )
        updateDateButtonText()
        setupCharts()
        loadReportData()

        return root
    }

    private fun createDailyLikeHeader(context: Context): View {
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brandContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            contentDescription = getString(R.string.app_name)
        }

        brandContainer.addView(
            logo,
            LinearLayout.LayoutParams(
                px(R.dimen.reports_header_logo_size),
                px(R.dimen.reports_header_logo_size)
            ).apply {
                marginEnd = px(R.dimen.reports_header_logo_margin_end)
            }
        )

        val logoText = MaterialTextView(context).apply {
            text = getString(R.string.app_name)
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.reports_header_logo_text_size)
            )
            setTextColor(color(R.color.reports_brand_text))
            typeface = ResourcesCompat.getFont(context, R.font.fredoka_bold)
        }

        brandContainer.addView(logoText)

        header.addView(
            brandContainer,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val notificationButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_notifications)
            setColorFilter(color(R.color.reports_icon_tint))
            setBackgroundResource(resolveSelectableBorderless())
            contentDescription = getString(R.string.social_notifications)
            setPadding(
                px(R.dimen.reports_notification_button_padding),
                px(R.dimen.reports_notification_button_padding),
                px(R.dimen.reports_notification_button_padding),
                px(R.dimen.reports_notification_button_padding)
            )
            setOnClickListener {
                NotificationsFragment()
                    .show(childFragmentManager, NotificationsFragment::class.java.simpleName)
            }
        }

        header.addView(
            notificationButton,
            LinearLayout.LayoutParams(
                px(R.dimen.reports_notification_button_size),
                px(R.dimen.reports_notification_button_size)
            )
        )

        return header
    }

    private fun createDateSelector(context: Context): View {
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0,
                px(R.dimen.reports_date_selector_top_margin),
                0,
                px(R.dimen.reports_date_selector_bottom_margin)
            )
        }

        dateButton = MaterialButton(context).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setTextColor(color(R.color.reports_text_primary))
            icon = ContextCompat.getDrawable(context, R.drawable.ic_calendar_today)
            iconTint = ColorStateList.valueOf(color(R.color.reports_tab_selected))
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconPadding = px(R.dimen.reports_date_selector_icon_padding)
            cornerRadius = px(R.dimen.reports_date_selector_radius)
            strokeWidth = px(R.dimen.reports_card_stroke_width)
            strokeColor = ColorStateList.valueOf(color(R.color.reports_card_stroke))
            backgroundTintList = ColorStateList.valueOf(color(R.color.reports_card_background))
            minHeight = px(R.dimen.reports_date_selector_height)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                px(R.dimen.reports_date_selector_horizontal_padding),
                0,
                px(R.dimen.reports_date_selector_horizontal_padding),
                0
            )
            setOnClickListener {
                showDatePicker()
            }
        }

        wrapper.addView(
            dateButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(R.dimen.reports_date_selector_height)
            )
        )

        return wrapper
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.reports_date_picker_title))
            .setSelection(selectedDate.toUtcStartMillis())
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedDate = utcMillis.toUtcLocalDate()
            updateDateButtonText()
            loadReportData()
        }

        picker.show(parentFragmentManager, "ReportsDatePicker")
    }

    private fun updateDateButtonText() {
        if (!::dateButton.isInitialized) return

        val pattern = getString(R.string.reports_date_format_pattern)
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        dateButton.text = selectedDate.format(formatter)
    }

    private fun createCaloriePage(context: Context): CaloriePageRefs {
        val scroll = pageScroll(context)
        val content = pageContent(context)
        val card = reportCard(context)

        val body = cardBody(context)
        val value = bigValue(context)
        val info = infoText(context)

        val statRow = statRow(context)
        val todayStat = smallStatCard(context, R.string.reports_stat_today)
        val averageStat = smallStatCard(context, R.string.reports_stat_average)
        val targetStat = smallStatCard(context, R.string.reports_stat_target)

        statRow.addView(todayStat.root, statLayoutParams(start = false, end = true))
        statRow.addView(averageStat.root, statLayoutParams(start = true, end = true))
        statRow.addView(targetStat.root, statLayoutParams(start = true, end = false))

        val chart = LineChart(context)
        val details = detailCard(context, R.string.reports_calorie_details_title)

        body.addView(sectionTitle(context, R.string.reports_calorie_title))
        body.addView(sectionSubtitle(context, R.string.reports_calorie_subtitle))
        body.addView(value)
        body.addView(info)
        body.addView(statRow)
        body.addView(
            chart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(R.dimen.reports_line_chart_height)
            )
        )

        card.addView(body)
        content.addView(card)
        content.addView(details.root, detailCardLayoutParams())
        scroll.addView(content)

        return CaloriePageRefs(
            root = scroll,
            valueText = value,
            infoText = info,
            todayStat = todayStat.valueText,
            averageStat = averageStat.valueText,
            targetStat = targetStat.valueText,
            chart = chart,
            detailsContainer = details.container
        )
    }

    private fun createMacroPage(context: Context): MacroPageRefs {
        val scroll = pageScroll(context)
        val content = pageContent(context)
        val card = reportCard(context)

        val body = cardBody(context)
        val value = bigValue(context)
        val info = infoText(context)

        val statRow = statRow(context)
        val proteinStat = smallStatCard(context, R.string.reports_stat_protein)
        val carbStat = smallStatCard(context, R.string.reports_stat_carbs)
        val fatStat = smallStatCard(context, R.string.reports_stat_fat)

        statRow.addView(proteinStat.root, statLayoutParams(start = false, end = true))
        statRow.addView(carbStat.root, statLayoutParams(start = true, end = true))
        statRow.addView(fatStat.root, statLayoutParams(start = true, end = false))

        val chart = PieChart(context)
        val details = detailCard(context, R.string.reports_macro_details_title)

        body.addView(sectionTitle(context, R.string.reports_macro_title))
        body.addView(sectionSubtitle(context, R.string.reports_macro_subtitle))
        body.addView(value)
        body.addView(info)
        body.addView(statRow)
        body.addView(
            chart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(R.dimen.reports_pie_chart_height)
            )
        )

        card.addView(body)
        content.addView(card)
        content.addView(details.root, detailCardLayoutParams())
        scroll.addView(content)

        return MacroPageRefs(
            root = scroll,
            valueText = value,
            infoText = info,
            proteinStat = proteinStat.valueText,
            carbStat = carbStat.valueText,
            fatStat = fatStat.valueText,
            chart = chart,
            detailsContainer = details.container
        )
    }

    private fun createWeightPage(context: Context): WeightPageRefs {
        val scroll = pageScroll(context)
        val content = pageContent(context)
        val card = reportCard(context)

        val body = cardBody(context)
        val value = bigValue(context)
        val info = infoText(context)

        val statRow = statRow(context)
        val startStat = smallStatCard(context, R.string.reports_stat_start)
        val currentStat = smallStatCard(context, R.string.reports_stat_current)
        val targetStat = smallStatCard(context, R.string.reports_stat_target)

        statRow.addView(startStat.root, statLayoutParams(start = false, end = true))
        statRow.addView(currentStat.root, statLayoutParams(start = true, end = true))
        statRow.addView(targetStat.root, statLayoutParams(start = true, end = false))

        val chart = LineChart(context)
        val details = detailCard(context, R.string.reports_weight_details_title)

        body.addView(sectionTitle(context, R.string.reports_weight_title))
        body.addView(sectionSubtitle(context, R.string.reports_weight_subtitle))
        body.addView(value)
        body.addView(info)
        body.addView(statRow)
        body.addView(
            chart,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                px(R.dimen.reports_line_chart_height)
            )
        )

        card.addView(body)
        content.addView(card)
        content.addView(details.root, detailCardLayoutParams())
        scroll.addView(content)

        return WeightPageRefs(
            root = scroll,
            valueText = value,
            infoText = info,
            startStat = startStat.valueText,
            currentStat = currentStat.valueText,
            targetStat = targetStat.valueText,
            chart = chart,
            detailsContainer = details.container
        )
    }

    private fun setupCharts() {
        setupLineChart(
            chart = caloriePage.chart,
            noDataText = getString(R.string.reports_no_calorie_data)
        )
        setupPieChart(macroPage.chart)
        setupLineChart(
            chart = weightPage.chart,
            noDataText = getString(R.string.reports_no_weight_data)
        )
    }

    private fun setupLineChart(
        chart: LineChart,
        noDataText: String
    ) {
        chart.apply {
            setBackgroundColor(color(R.color.reports_chart_background))
            description.isEnabled = false
            setNoDataText(noDataText)
            setNoDataTextColor(color(R.color.reports_text_secondary))

            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            extraBottomOffset = chartDp(R.dimen.reports_chart_extra_bottom_offset)
            extraTopOffset = chartDp(R.dimen.reports_chart_extra_top_offset)

            axisRight.isEnabled = false

            axisLeft.textColor = color(R.color.reports_chart_axis_text)
            axisLeft.gridColor = color(R.color.reports_chart_grid)
            axisLeft.axisLineColor = color(R.color.reports_chart_grid)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = resources.getInteger(R.integer.reports_chart_x_granularity).toFloat()
            xAxis.textColor = color(R.color.reports_chart_axis_text)
            xAxis.axisLineColor = color(R.color.reports_chart_grid)
            xAxis.setDrawGridLines(false)

            legend.isEnabled = false
        }
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            setBackgroundColor(color(R.color.reports_chart_background))
            description.isEnabled = false
            setNoDataText(getString(R.string.reports_no_macro_data))
            setNoDataTextColor(color(R.color.reports_text_secondary))

            setUsePercentValues(true)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = resources.getInteger(R.integer.reports_pie_hole_radius_percent).toFloat()
            transparentCircleRadius = resources.getInteger(R.integer.reports_pie_transparent_radius_percent).toFloat()
            setHoleColor(color(R.color.reports_chart_background))
            centerText = getString(R.string.reports_macro_center_text)
            setCenterTextSize(chartSp(R.dimen.reports_pie_center_text_size))
            setCenterTextColor(color(R.color.reports_text_primary))

            legend.isEnabled = true
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textColor = color(R.color.reports_text_secondary)
            legend.textSize = chartSp(R.dimen.reports_chart_legend_text_size)
        }
    }

    private fun loadReportData() {
        loadNutritionData()
        loadProfileData()
    }

    private fun loadNutritionData() {
        val appContext = requireContext().applicationContext
        val database = FittoDatabaseProvider.getDatabase(appContext)
        val activeUserId = UserSessionDataGuard.getActiveUid(appContext) ?: "local_anonymous"
        val dates = selectedWeekDates()

        viewLifecycleOwner.lifecycleScope.launch {
            val loadedDays = withContext(Dispatchers.IO) {
                dates.map { date ->
                    val summary = database.mealDao()
                        .getNutritionSummaryForDate(activeUserId, date.toString())

                    ReportDay(
                        date = date,
                        label = date.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        ),
                        calories = summary.totalCalories,
                        protein = summary.totalProtein,
                        carbs = summary.totalCarbs,
                        fat = summary.totalFat
                    )
                }
            }

            if (!isAdded) return@launch

            nutritionDays = loadedDays
            renderAll()
        }
    }

    private fun scrollReportPageToTop(position: Int) {
        val page = when (position) {
            0 -> if (::caloriePage.isInitialized) caloriePage.root else null
            1 -> if (::macroPage.isInitialized) macroPage.root else null
            2 -> if (::weightPage.isInitialized) weightPage.root else null
            else -> null
        }

        (page as? NestedScrollView)?.post {
            page.scrollTo(0, 0)
        }
    }
    private fun loadProfileData() {
        profileRepository.loadProfile(
            onSuccess = { state ->
                if (!isAdded) return@loadProfile

                healthData = state.healthData
                renderAll()
            },
            onError = {
                if (!isAdded) return@loadProfile

                healthData = ProfileHealthData.Empty
                renderAll()
            }
        )
    }

    private fun renderAll() {
        renderCaloriePage()
        renderMacroPage()
        renderWeightPage()
    }

    private fun renderCaloriePage() {
        if (!::caloriePage.isInitialized || nutritionDays.isEmpty()) return

        val targetCalories = healthData.dailyTargetCalories
            .takeIf { it > 0 }
            ?: resources.getInteger(R.integer.reports_default_daily_calorie_target)

        val todayCalories = nutritionDays.lastOrNull()?.calories ?: 0
        val averageCalories = nutritionDays
            .map { it.calories }
            .average()
            .takeIf { !it.isNaN() }
            ?.roundToInt()
            ?: 0

        caloriePage.valueText.text = getString(R.string.reports_format_kcal, todayCalories)
        caloriePage.infoText.text = getString(R.string.reports_calorie_info_selected_date)
        caloriePage.todayStat.text = getString(R.string.reports_format_kcal, todayCalories)
        caloriePage.averageStat.text = getString(R.string.reports_format_kcal, averageCalories)
        caloriePage.targetStat.text = getString(R.string.reports_format_kcal, targetCalories)

        val entries = nutritionDays.mapIndexed { index, day ->
            Entry(index.toFloat(), day.calories.toFloat())
        }

        val labels = nutritionDays.map { it.label }

        val dataSet = LineDataSet(entries, getString(R.string.reports_calorie_title)).apply {
            color = color(R.color.reports_calorie_line)
            lineWidth = chartDp(R.dimen.reports_chart_line_width)
            circleRadius = chartDp(R.dimen.reports_chart_circle_radius)
            circleHoleRadius = chartDp(R.dimen.reports_chart_circle_hole_radius)
            setCircleColor(color(R.color.reports_calorie_line))
            valueTextColor = color(R.color.reports_chart_value_text)
            valueTextSize = chartSp(R.dimen.reports_chart_value_text_size)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color(R.color.reports_calorie_fill)
            fillAlpha = resources.getInteger(R.integer.reports_chart_fill_alpha)
        }

        caloriePage.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        caloriePage.chart.axisLeft.removeAllLimitLines()

        caloriePage.chart.axisLeft.addLimitLine(
            LimitLine(
                targetCalories.toFloat(),
                getString(R.string.reports_chart_target_label)
            ).apply {
                lineColor = color(R.color.reports_target_line)
                lineWidth = chartDp(R.dimen.reports_chart_limit_line_width)
                textColor = color(R.color.reports_target_line)
                textSize = chartSp(R.dimen.reports_chart_value_text_size)
                enableDashedLine(
                    chartDp(R.dimen.reports_chart_dash_line_length),
                    chartDp(R.dimen.reports_chart_dash_gap_length),
                    0f
                )
            }
        )

        val maxCalorie = nutritionDays.maxOfOrNull { it.calories } ?: 0
        val axisMultiplier = resources.getFraction(
            R.fraction.reports_axis_max_padding_multiplier,
            1,
            1
        )

        caloriePage.chart.axisLeft.axisMinimum = 0f
        caloriePage.chart.axisLeft.axisMaximum = maxOf(
            targetCalories.toFloat(),
            maxCalorie.toFloat(),
            resources.getInteger(R.integer.reports_chart_min_axis_max).toFloat()
        ) * axisMultiplier

        caloriePage.chart.data = LineData(dataSet)
        caloriePage.chart.invalidate()

        caloriePage.detailsContainer.removeAllViews()
        nutritionDays.forEach { day ->
            caloriePage.detailsContainer.addView(
                detailRow(
                    title = day.date.displayDate(),
                    subtitle = getString(
                        R.string.reports_format_macro_detail,
                        day.protein.roundToInt(),
                        day.carbs.roundToInt(),
                        day.fat.roundToInt()
                    ),
                    value = getString(R.string.reports_format_kcal, day.calories)
                )
            )
        }
    }

    private fun renderMacroPage() {
        if (!::macroPage.isInitialized) return

        val selectedDay = nutritionDays.lastOrNull()
        val protein = selectedDay?.protein ?: 0.0
        val carbs = selectedDay?.carbs ?: 0.0
        val fat = selectedDay?.fat ?: 0.0
        val total = protein + carbs + fat

        macroPage.valueText.text = getString(R.string.reports_format_gram, total.roundToInt())
        macroPage.infoText.text = getString(R.string.reports_macro_info_from_meals)
        macroPage.proteinStat.text = getString(R.string.reports_format_gram, protein.roundToInt())
        macroPage.carbStat.text = getString(R.string.reports_format_gram, carbs.roundToInt())
        macroPage.fatStat.text = getString(R.string.reports_format_gram, fat.roundToInt())

        macroPage.detailsContainer.removeAllViews()

        if (total <= 0.0) {
            macroPage.chart.clear()
            macroPage.chart.invalidate()
            macroPage.detailsContainer.addView(emptyDetailRow())
            return
        }

        val entries = mutableListOf<PieEntry>()

        if (protein > 0.0) {
            entries.add(PieEntry(protein.toFloat(), getString(R.string.reports_stat_protein)))
        }
        if (carbs > 0.0) {
            entries.add(PieEntry(carbs.toFloat(), getString(R.string.reports_stat_carbs)))
        }
        if (fat > 0.0) {
            entries.add(PieEntry(fat.toFloat(), getString(R.string.reports_stat_fat)))
        }

        val dataSet = PieDataSet(entries, getString(R.string.reports_macro_title)).apply {
            setColors(
                listOf(
                    color(R.color.reports_macro_protein),
                    color(R.color.reports_macro_carbs),
                    color(R.color.reports_macro_fat)
                )
            )
            sliceSpace = chartDp(R.dimen.reports_pie_slice_space)
            valueTextColor = color(R.color.reports_text_primary)
            valueTextSize = chartSp(R.dimen.reports_pie_value_text_size)
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(macroPage.chart))
        }

        macroPage.chart.data = data
        macroPage.chart.invalidate()

        nutritionDays.forEach { day ->
            val dayTotal = day.protein + day.carbs + day.fat

            macroPage.detailsContainer.addView(
                detailRow(
                    title = day.date.displayDate(),
                    subtitle = getString(
                        R.string.reports_format_macro_detail,
                        day.protein.roundToInt(),
                        day.carbs.roundToInt(),
                        day.fat.roundToInt()
                    ),
                    value = getString(R.string.reports_format_gram, dayTotal.roundToInt())
                )
            )
        }
    }

    private fun renderWeightPage() {
        if (!::weightPage.isInitialized) return

        val points = resolveWeightPoints()

        if (points.isEmpty()) {
            weightPage.valueText.text = getString(R.string.reports_value_empty)
            weightPage.infoText.text = getString(R.string.reports_weight_missing)
            weightPage.startStat.text = getString(R.string.reports_value_empty)
            weightPage.currentStat.text = getString(R.string.reports_value_empty)
            weightPage.targetStat.text = formatWeightOrDash(healthData.targetWeightKg)
            weightPage.detailsContainer.removeAllViews()
            weightPage.detailsContainer.addView(emptyDetailRow())

            weightPage.chart.clear()
            weightPage.chart.invalidate()
            return
        }

        val visibleCount = resources.getInteger(R.integer.reports_weight_visible_point_count)
        val visiblePoints = points.takeLast(visibleCount)

        val firstWeight = points.firstOrNull()?.weightKg ?: 0.0
        val currentWeight = points.lastOrNull()?.weightKg ?: 0.0

        val targetWeight = healthData.targetWeightKg

        weightPage.valueText.text = formatWeightOrDash(currentWeight)
        weightPage.infoText.text = if (targetWeight > 0.0) {
            getString(R.string.reports_weight_goal_info)
        } else {
            getString(R.string.reports_weight_subtitle)
        }
        weightPage.startStat.text = formatWeightOrDash(firstWeight)
        weightPage.currentStat.text = formatWeightOrDash(currentWeight)
        weightPage.targetStat.text = formatWeightOrDash(targetWeight)

        val entries = visiblePoints.mapIndexed { index, point ->
            Entry(index.toFloat(), point.weightKg.toFloat())
        }

        val labels = visiblePoints.map { point ->
            point.label.ifBlank { getString(R.string.reports_value_empty) }
        }

        val dataSet = LineDataSet(entries, getString(R.string.reports_weight_title)).apply {
            color = color(R.color.reports_weight_line)
            lineWidth = chartDp(R.dimen.reports_chart_line_width)
            circleRadius = chartDp(R.dimen.reports_chart_circle_radius)
            circleHoleRadius = chartDp(R.dimen.reports_chart_circle_hole_radius)
            setCircleColor(color(R.color.reports_weight_line))
            valueTextColor = color(R.color.reports_chart_value_text)
            valueTextSize = chartSp(R.dimen.reports_chart_value_text_size)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color(R.color.reports_weight_fill)
            fillAlpha = resources.getInteger(R.integer.reports_chart_fill_alpha)
        }

        val minWeight = visiblePoints.minOfOrNull { it.weightKg } ?: currentWeight
        val maxWeight = visiblePoints.maxOfOrNull { it.weightKg } ?: currentWeight
        val axisPadding = resources.getInteger(R.integer.reports_weight_axis_padding_kg).toDouble()

        weightPage.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        weightPage.chart.axisLeft.axisMinimum = (minWeight - axisPadding)
            .coerceAtLeast(0.0)
            .toFloat()
        weightPage.chart.axisLeft.axisMaximum = (maxWeight + axisPadding).toFloat()

        weightPage.chart.data = LineData(dataSet)
        weightPage.chart.invalidate()

        weightPage.detailsContainer.removeAllViews()
        visiblePoints.reversed().forEach { point ->
            weightPage.detailsContainer.addView(
                detailRow(
                    title = point.label,
                    subtitle = getString(R.string.reports_weight_detail_measurement),
                    value = formatWeightOrDash(point.weightKg)
                )
            )
        }
    }

    private fun resolveWeightPoints(): List<WeightChartPoint> {
        val profilePoints = healthData.chartPoints
            .filter { it.weightKg > 0.0 }
            .sortedBy { it.timestamp }

        return profilePoints
    }

    private fun pageScroll(context: Context): NestedScrollView {
        return NestedScrollView(context).apply {
            setFillViewport(true)
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(color(R.color.reports_screen_background))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun pageContent(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0,
                px(R.dimen.reports_page_top_padding),
                0,
                px(R.dimen.reports_page_bottom_padding)
            )
        }
    }

    private fun reportCard(context: Context): MaterialCardView {
        return MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.reports_card_radius)
            cardElevation = resources.getDimension(R.dimen.reports_card_elevation)
            strokeWidth = px(R.dimen.reports_card_stroke_width)
            setStrokeColor(color(R.color.reports_card_stroke))
            setCardBackgroundColor(color(R.color.reports_card_background))
            useCompatPadding = true
        }
    }

    private fun cardBody(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                px(R.dimen.reports_card_content_padding),
                px(R.dimen.reports_card_content_padding),
                px(R.dimen.reports_card_content_padding),
                px(R.dimen.reports_card_content_padding)
            )
        }
    }

    private fun detailCard(
        context: Context,
        titleResId: Int
    ): DetailCardRefs {
        val card = reportCard(context)
        val body = cardBody(context)

        body.addView(
            MaterialTextView(context).apply {
                text = getString(titleResId)
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(color(R.color.reports_text_primary))
            }
        )

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, px(R.dimen.reports_detail_list_top_padding), 0, 0)
        }

        body.addView(container)
        card.addView(body)

        return DetailCardRefs(
            root = card,
            container = container
        )
    }

    private fun detailCardLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = px(R.dimen.reports_detail_card_top_margin)
        }
    }

    private fun detailRow(
        title: String,
        subtitle: String,
        value: String
    ): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                0,
                px(R.dimen.reports_detail_row_vertical_padding),
                0,
                px(R.dimen.reports_detail_row_vertical_padding)
            )
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        textColumn.addView(
            MaterialTextView(context).apply {
                text = title
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setTextColor(color(R.color.reports_text_primary))
            }
        )

        textColumn.addView(
            MaterialTextView(context).apply {
                text = subtitle
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
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
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
                setTextColor(color(R.color.reports_tab_selected))
                gravity = Gravity.END
            }
        )

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(row)
            addView(
                MaterialDivider(context).apply {
                    setDividerColor(color(R.color.reports_divider))
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    px(R.dimen.reports_detail_divider_height)
                )
            )
        }

        return wrapper
    }

    private fun emptyDetailRow(): View {
        return MaterialTextView(requireContext()).apply {
            text = getString(R.string.reports_detail_no_data)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(color(R.color.reports_text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, px(R.dimen.reports_detail_empty_vertical_padding), 0, px(R.dimen.reports_detail_empty_vertical_padding))
        }
    }

    private fun sectionTitle(
        context: Context,
        titleResId: Int
    ): MaterialTextView {
        return MaterialTextView(context).apply {
            text = getString(titleResId)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            setTextColor(color(R.color.reports_text_primary))
        }
    }

    private fun sectionSubtitle(
        context: Context,
        subtitleResId: Int
    ): MaterialTextView {
        return MaterialTextView(context).apply {
            text = getString(subtitleResId)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(color(R.color.reports_text_secondary))
            setPadding(0, px(R.dimen.reports_section_subtitle_top_padding), 0, px(R.dimen.reports_section_subtitle_bottom_padding))
        }
    }

    private fun bigValue(context: Context): MaterialTextView {
        return MaterialTextView(context).apply {
            text = getString(R.string.reports_value_empty)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall)
            setTextColor(color(R.color.reports_text_primary))
        }
    }

    private fun infoText(context: Context): MaterialTextView {
        return MaterialTextView(context).apply {
            text = getString(R.string.reports_loading)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(color(R.color.reports_text_secondary))
        }
    }

    private fun statRow(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, px(R.dimen.reports_stat_row_top_padding), 0, px(R.dimen.reports_stat_row_bottom_padding))
        }
    }

    private fun statLayoutParams(
        start: Boolean,
        end: Boolean
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            px(R.dimen.reports_stat_card_height),
            1f
        ).apply {
            if (start) marginStart = px(R.dimen.reports_stat_card_spacing)
            if (end) marginEnd = px(R.dimen.reports_stat_card_spacing)
        }
    }

    private fun smallStatCard(
        context: Context,
        labelResId: Int
    ): SmallStatRefs {
        val card = MaterialCardView(context).apply {
            radius = resources.getDimension(R.dimen.reports_stat_card_radius)
            cardElevation = 0f
            strokeWidth = px(R.dimen.reports_stat_card_stroke_width)
            setStrokeColor(color(R.color.reports_stat_card_stroke))
            setCardBackgroundColor(color(R.color.reports_stat_card_background))
        }

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                px(R.dimen.reports_stat_card_padding),
                px(R.dimen.reports_stat_card_padding),
                px(R.dimen.reports_stat_card_padding),
                px(R.dimen.reports_stat_card_padding)
            )
        }

        val labelText = MaterialTextView(context).apply {
            text = getString(labelResId)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
            gravity = Gravity.CENTER
            setTextColor(color(R.color.reports_text_secondary))
        }

        val valueText = MaterialTextView(context).apply {
            text = getString(R.string.reports_value_empty)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            gravity = Gravity.CENTER
            setTextColor(color(R.color.reports_text_primary))
            maxLines = resources.getInteger(R.integer.reports_stat_value_max_lines)
        }

        body.addView(labelText)
        body.addView(valueText)
        card.addView(body)

        return SmallStatRefs(
            root = card,
            valueText = valueText
        )
    }

    private fun selectedWeekDates(): List<LocalDate> {
        return (resources.getInteger(R.integer.reports_days_back) downTo 0).map { offset ->
            selectedDate.minusDays(offset.toLong())
        }
    }

    private fun LocalDate.displayDate(): String {
        val pattern = getString(R.string.reports_short_date_format_pattern)
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return format(formatter)
    }

    private fun LocalDate.toUtcStartMillis(): Long {
        return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    private fun Long.toUtcLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    }

    private fun formatWeightOrDash(value: Double): String {
        if (value <= 0.0) return getString(R.string.reports_value_empty)

        val formattedValue = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(
                Locale.getDefault(),
                getString(R.string.reports_decimal_one_pattern),
                value
            )
        }

        return getString(R.string.reports_format_kg, formattedValue)
    }

    private fun resolveSelectableBorderless(): Int {
        val outValue = TypedValue()
        requireContext().theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless,
            outValue,
            true
        )
        return outValue.resourceId
    }

    private fun color(@ColorRes colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    private fun px(@DimenRes dimenResId: Int): Int {
        return resources.getDimensionPixelSize(dimenResId)
    }

    private fun chartDp(@DimenRes dimenResId: Int): Float {
        return resources.getDimension(dimenResId) / resources.displayMetrics.density
    }

    private fun chartSp(@DimenRes dimenResId: Int): Float {
        return resources.getDimension(dimenResId) / resources.displayMetrics.scaledDensity
    }

    private data class ReportDay(
        val date: LocalDate,
        val label: String,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )

    private data class SmallStatRefs(
        val root: View,
        val valueText: MaterialTextView
    )

    private data class DetailCardRefs(
        val root: View,
        val container: LinearLayout
    )

    private data class CaloriePageRefs(
        val root: View,
        val valueText: MaterialTextView,
        val infoText: MaterialTextView,
        val todayStat: MaterialTextView,
        val averageStat: MaterialTextView,
        val targetStat: MaterialTextView,
        val chart: LineChart,
        val detailsContainer: LinearLayout
    )

    private data class MacroPageRefs(
        val root: View,
        val valueText: MaterialTextView,
        val infoText: MaterialTextView,
        val proteinStat: MaterialTextView,
        val carbStat: MaterialTextView,
        val fatStat: MaterialTextView,
        val chart: PieChart,
        val detailsContainer: LinearLayout
    )

    private data class WeightPageRefs(
        val root: View,
        val valueText: MaterialTextView,
        val infoText: MaterialTextView,
        val startStat: MaterialTextView,
        val currentStat: MaterialTextView,
        val targetStat: MaterialTextView,
        val chart: LineChart,
        val detailsContainer: LinearLayout
    )

    private class StaticViewPagerAdapter(
        private val pages: List<View>
    ) : RecyclerView.Adapter<StaticViewPagerAdapter.PageViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PageViewHolder {
            val container = FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            return PageViewHolder(container)
        }

        override fun onBindViewHolder(
            holder: PageViewHolder,
            position: Int
        ) {
            val page = pages[position]

            (page.parent as? ViewGroup)?.removeView(page)
            holder.container.removeAllViews()
            holder.container.addView(
                page,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        override fun getItemCount(): Int = pages.size

        class PageViewHolder(
            val container: FrameLayout
        ) : RecyclerView.ViewHolder(container)
    }
}
