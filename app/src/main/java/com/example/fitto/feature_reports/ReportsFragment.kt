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
import com.example.fitto.fittoAppContainer
import com.example.fitto.feature_home.presentation.social.NotificationsFragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var dataCoordinator: ReportsDataCoordinator
    private lateinit var dateButton: MaterialButton
    private lateinit var viewPager: ViewPager2
    private lateinit var caloriePage: CaloriePageRefs
    private lateinit var macroPage: MacroPageRefs
    private lateinit var weightPage: WeightPageRefs
    private lateinit var pageRenderer: ReportsPageRenderer

    private val screenStateStore = ReportsScreenStateStore()

    private val selectedDate: LocalDate
        get() = dataCoordinator.state.selectedDate

    private val tabTitleResIds = intArrayOf(
        R.string.reports_tab_calorie,
        R.string.reports_tab_macro,
        R.string.reports_tab_weight
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataCoordinator = requireContext()
            .fittoAppContainer
            .reportsFeatureDependencies
            .createDataCoordinator(
                daysBack = resources.getInteger(R.integer.reports_days_back)
            )
    }

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
        pageRenderer = ReportsPageRenderer(
            context = context,
            caloriePage = caloriePage,
            macroPage = macroPage,
            weightPage = weightPage
        )

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
                    val previous = screenStateStore.selectPage(position)
                    scrollReportPageToTop(previous)
                    scrollReportPageToTop(screenStateStore.state.selectedPageIndex)
                }
            }
        )
        viewPager.setCurrentItem(screenStateStore.state.selectedPageIndex, false)
        updateDateButtonText()
        pageRenderer.setupCharts()
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
            dataCoordinator.selectDate(utcMillis.toUtcLocalDate())
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

    private fun loadReportData() {
        loadNutritionData()
        loadProfileData()
    }

    private fun loadNutritionData() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (dataCoordinator.loadNutrition(Locale.getDefault()) == null) return@launch
            if (!isAdded) return@launch

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
        dataCoordinator.loadHealthData {
            if (!isAdded) return@loadHealthData

            renderAll()
        }
    }

    private fun renderAll() {
        if (!::pageRenderer.isInitialized) return

        val renderState = ReportsRenderStateResolver.resolve(
            dataState = dataCoordinator.state,
            config = ReportsRenderConfig(
                defaultTargetCalories = resources.getInteger(
                    R.integer.reports_default_daily_calorie_target
                ),
                weightVisiblePointCount = resources.getInteger(
                    R.integer.reports_weight_visible_point_count
                ),
                weightAxisPaddingKg = resources.getInteger(
                    R.integer.reports_weight_axis_padding_kg
                ).toDouble()
            )
        )

        pageRenderer.render(renderState)
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

    private fun LocalDate.toUtcStartMillis(): Long {
        return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    private fun Long.toUtcLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
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

    private data class SmallStatRefs(
        val root: View,
        val valueText: MaterialTextView
    )

    private data class DetailCardRefs(
        val root: View,
        val container: LinearLayout
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
