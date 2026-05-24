package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.Gravity
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.ProfileHealthData
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import com.example.fitto.feature_home.presentation.profile.health.formatSignedWeightKg
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileRemindersActivity
import com.example.fitto.feature_home.presentation.profile.view.WeightTrendChartView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ProfileWeightFragment : Fragment(R.layout.fragment_profile_weight) {

    private lateinit var viewModel: ProfileViewModel

    private lateinit var toolbar: MaterialToolbar
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var historyScrollView: NestedScrollView
    private lateinit var currentWeightCard: View
    private lateinit var weightInput: EditText
    private lateinit var textLastWeighDate: TextView
    private lateinit var textInitialWeightValue: TextView
    private lateinit var textTargetWeightValue: TextView
    private lateinit var textLostSoFar: TextView
    private lateinit var textRemainingWeight: TextView
    private lateinit var saveWeightButton: MaterialButton
    private lateinit var returnCurrentButton: MaterialButton
    private lateinit var reminderCard: View
    private lateinit var reminderButton: View
    private lateinit var weightTrendChartView: WeightTrendChartView
    private lateinit var chartSummaryLayout: LinearLayout
    private lateinit var chartFirstValue: TextView
    private lateinit var chartLastValue: TextView
    private lateinit var chartChangeValue: TextView
    private lateinit var historyLayout: LinearLayout

    private var historyMenuItem: MenuItem? = null
    private var latestHealthData: ProfileHealthData = ProfileHealthData.Empty
    private var historyItems: List<WeightHistoryItem> = emptyList()
    private var selectedHistoryItemId: String? = null
    private var previewHistoryItem: WeightHistoryItem? = null
    private var isHistoryScreenVisible: Boolean = false
    private var isSavingWeight: Boolean = false
    private var isBindingInput: Boolean = false
    private var hasUnsavedWeightInput: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]

        bindViews(view)
        setupToolbar()
        setupBackHandling()
        setupInput()
        setupClicks()
        observeViewModel()

        viewModel.loadProfile(forceRefresh = true)
    }

    private fun bindViews(root: View) {
        toolbar = root.findViewById(R.id.toolbarWeight)
        mainScrollView = root.findViewById(R.id.scrollWeightMain)
        historyScrollView = root.findViewById(R.id.scrollWeightHistory)
        currentWeightCard = root.findViewById(R.id.cardCurrentWeight)
        weightInput = root.findViewById(R.id.editCurrentWeightValue)
        textLastWeighDate = root.findViewById(R.id.textLastWeighDate)
        textInitialWeightValue = root.findViewById(R.id.textInitialWeightValue)
        textTargetWeightValue = root.findViewById(R.id.textTargetWeightValue)
        textLostSoFar = root.findViewById(R.id.textLostSoFar)
        textRemainingWeight = root.findViewById(R.id.textRemainingWeight)
        saveWeightButton = root.findViewById(R.id.buttonSaveWeight)
        returnCurrentButton = root.findViewById(R.id.buttonReturnCurrentWeight)
        reminderCard = root.findViewById(R.id.cardReminderBanner)
        reminderButton = root.findViewById(R.id.buttonReminder)
        weightTrendChartView = root.findViewById(R.id.weightTrendChartView)
        chartSummaryLayout = root.findViewById(R.id.layoutWeightChartSummary)
        chartFirstValue = root.findViewById(R.id.textWeightChartFirstValue)
        chartLastValue = root.findViewById(R.id.textWeightChartLastValue)
        chartChangeValue = root.findViewById(R.id.textWeightChartChangeValue)
        historyLayout = root.findViewById(R.id.layoutWeightHistory)
    }

    private fun setupToolbar() {
        historyMenuItem = toolbar.menu.findItem(R.id.action_weight_history)

        toolbar.setNavigationOnClickListener {
            if (isHistoryScreenVisible) {
                showMainScreen()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_weight_history -> {
                    showHistoryScreen()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isHistoryScreenVisible) {
                        showMainScreen()
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        )
    }

    private fun setupInput() {
        weightInput.keyListener = DigitsKeyListener.getInstance("0123456789,.")
        weightInput.filters = arrayOf(WeightDecimalInputFilter())
        weightInput.setSelectAllOnFocus(true)

        weightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isBindingInput) return
                if (previewHistoryItem != null) return

                val value = parseWeightInput()
                hasUnsavedWeightInput = value != null && abs(value - latestHealthData.currentWeightKg) > 0.0005
                updateSaveButtonState()
            }
        })

        weightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && previewHistoryItem == null) {
                formatWeightInputAfterEditing()
            }
            updateSaveButtonState()
        }

        weightInput.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)

            if (isDone) {
                saveWeight()
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() {
        currentWeightCard.setOnClickListener {
            if (previewHistoryItem == null) {
                focusWeightInput()
            }
        }

        saveWeightButton.setOnClickListener {
            saveWeight()
        }

        returnCurrentButton.setOnClickListener {
            clearHistoryPreview()
        }

        reminderCard.setOnClickListener {
            openReminderScreen()
        }

        reminderButton.setOnClickListener {
            openReminderScreen()
        }

        mainScrollView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && isTouchOutsideView(event, weightTrendChartView)) {
                weightTrendChartView.clearSelectedPoint()
            }
            false
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestHealthData = state.healthData
            historyItems = state.healthData.history

            refreshSelectedHistoryPreview()

            if (previewHistoryItem == null) {
                bindEditableWeight()
            } else {
                bindHistoryPreview()
            }

            bindHistoryList(historyItems)
            bindChart(state.healthData)

            state.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindEditableWeight() {
        bindWeightInput(
            value = latestHealthData.currentWeightKg,
            readOnly = false,
            force = !weightInput.hasFocus() || !hasUnsavedWeightInput
        )

        textLastWeighDate.text = if (latestHealthData.lastWeighDateText.isNotBlank()) {
            getString(R.string.weight_last_weigh_format, latestHealthData.lastWeighDateText)
        } else {
            getString(R.string.weight_last_weigh_default)
        }

        textInitialWeightValue.text = formatWeightOrEmpty(latestHealthData.initialWeightKg)
        textTargetWeightValue.text = formatWeightOrEmpty(latestHealthData.targetWeightKg)
        textLostSoFar.text = getString(R.string.weight_lost_so_far_format, formatWeightKg(latestHealthData.lostSoFarKg).ifBlank { "0" })
        textRemainingWeight.text = getString(R.string.weight_remaining_format, formatWeightKg(latestHealthData.remainingKg).ifBlank { "0" })

        saveWeightButton.isVisible = true
        returnCurrentButton.isVisible = false
        weightInput.isFocusable = true
        weightInput.isFocusableInTouchMode = true
        weightInput.isCursorVisible = weightInput.hasFocus()
        updateSaveButtonState()
    }

    private fun bindHistoryPreview() {
        val item = previewHistoryItem ?: return

        clearFocusedWeightInput()
        bindWeightInput(value = item.weightKg, readOnly = true, force = true)

        textLastWeighDate.text = getString(R.string.weight_preview_hint, formatDateOrEmpty(item.createdAt))
        textInitialWeightValue.text = formatWeightOrEmpty(latestHealthData.initialWeightKg)
        textTargetWeightValue.text = formatWeightOrEmpty(latestHealthData.targetWeightKg)
        textLostSoFar.text = getString(R.string.weight_lost_so_far_format, formatWeightKg(latestHealthData.lostSoFarKg).ifBlank { "0" })
        textRemainingWeight.text = getString(R.string.weight_remaining_format, formatWeightKg(latestHealthData.remainingKg).ifBlank { "0" })

        saveWeightButton.isVisible = false
        returnCurrentButton.isVisible = true
        weightInput.isFocusable = false
        weightInput.isFocusableInTouchMode = false
        weightInput.isCursorVisible = false
    }

    private fun bindWeightInput(value: Double, readOnly: Boolean, force: Boolean) {
        if (!force && !readOnly) return

        isBindingInput = true
        weightInput.setText(formatWeightKg(value))
        weightInput.setSelection(weightInput.text?.length ?: 0)
        hasUnsavedWeightInput = false
        isBindingInput = false
    }

    private fun bindChart(healthData: ProfileHealthData) {
        weightTrendChartView.submitData(healthData.chartPoints)

        val points = healthData.chartPoints
        if (points.isEmpty()) {
            chartSummaryLayout.isVisible = false
            return
        }

        val first = points.first().weightKg
        val last = points.last().weightKg
        val change = last - first

        chartFirstValue.text = getString(R.string.weight_value_format, formatWeightKg(first))
        chartLastValue.text = getString(R.string.weight_value_format, formatWeightKg(last))
        chartChangeValue.text = getString(R.string.weight_value_format, formatSignedWeightKg(change))
        chartSummaryLayout.isVisible = true
    }

    private fun updateSaveButtonState() {
        if (previewHistoryItem != null) return

        val value = parseWeightInput()
        val canSave = value != null && value > 0.0 && !isSavingWeight
        saveWeightButton.isEnabled = canSave
        saveWeightButton.alpha = if (canSave) 1f else 0.58f
        saveWeightButton.text = if (isSavingWeight) {
            getString(R.string.weight_saving)
        } else {
            getString(R.string.weight_save)
        }
    }

    private fun saveWeight() {
        if (previewHistoryItem != null || isSavingWeight) return

        val weight = parseWeightInput()
        if (weight == null || weight <= 0.0) {
            Toast.makeText(requireContext(), R.string.weight_invalid, Toast.LENGTH_SHORT).show()
            focusWeightInput()
            return
        }

        clearFocusedWeightInput()
        isSavingWeight = true
        updateSaveButtonState()

        viewModel.updateWeight(
            weightKg = weight,
            onSuccess = {
                selectedHistoryItemId = null
                previewHistoryItem = null
                isSavingWeight = false
                hasUnsavedWeightInput = false

                Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show()
            },
            onError = { message ->
                isSavingWeight = false
                updateSaveButtonState()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun parseWeightInput(): Double? {
        val raw = weightInput.text?.toString().orEmpty().trim()
        if (raw.isBlank() || raw == "." || raw == ",") return null
        return raw.replace(',', '.').toDoubleOrNull()
    }

    private fun formatWeightInputAfterEditing() {
        val value = parseWeightInput() ?: return
        isBindingInput = true
        weightInput.setText(formatWeightKg(value))
        weightInput.setSelection(weightInput.text?.length ?: 0)
        isBindingInput = false
    }

    private fun focusWeightInput() {
        weightInput.post {
            weightInput.requestFocus()
            weightInput.selectAll()
            weightInput.isCursorVisible = true
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(weightInput, InputMethodManager.SHOW_IMPLICIT)
            mainScrollView.postDelayed({
                weightInput.requestRectangleOnScreen(
                    android.graphics.Rect(0, 0, weightInput.width, weightInput.height + dp(120)),
                    true
                )
            }, 180L)
        }
    }

    private fun clearFocusedWeightInput() {
        if (!weightInput.hasFocus()) return
        weightInput.clearFocus()
        hideKeyboardFrom(weightInput)
    }

    private fun hideKeyboardFrom(view: View) {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun openReminderScreen() {
        startActivity(Intent(requireContext(), ProfileRemindersActivity::class.java))
    }

    private fun bindHistoryList(items: List<WeightHistoryItem>) {
        historyLayout.removeAllViews()

        if (items.isEmpty()) {
            historyLayout.addView(createEmptyHistoryView())
            return
        }

        items.take(30).forEach { item ->
            historyLayout.addView(createHistoryRow(item))
        }
    }

    private fun createEmptyHistoryView(): View {
        return TextView(requireContext()).apply {
            text = getString(R.string.weight_history_empty)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_subtext))
            textSize = 14f
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedDrawable(
                color = ContextCompat.getColor(requireContext(), R.color.profile_card_background),
                strokeColor = ContextCompat.getColor(requireContext(), R.color.profile_stroke),
                radiusDp = 22
            )
        }
    }

    private fun createHistoryRow(item: WeightHistoryItem): View {
        val context = requireContext()
        val isSelected = item.id == selectedHistoryItemId

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(14), dp(14))
            background = roundedDrawable(
                color = ContextCompat.getColor(context, R.color.profile_card_background),
                strokeColor = if (isSelected) {
                    ContextCompat.getColor(context, R.color.fitto_green)
                } else {
                    ContextCompat.getColor(context, R.color.profile_stroke)
                },
                radiusDp = 22
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            setOnClickListener {
                selectedHistoryItemId = item.id
                previewHistoryItem = item
                clearFocusedWeightInput()
                bindHistoryList(historyItems)
                showMainScreen()
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(historyIconFor(item.trend))
            setColorFilter(historyColorFor(item.trend))
            background = roundedDrawable(
                color = ContextCompat.getColor(context, R.color.fitto_primary_soft),
                strokeColor = ContextCompat.getColor(context, R.color.fitto_primary_soft),
                radiusDp = 18
            )
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        row.addView(icon, LinearLayout.LayoutParams(dp(44), dp(44)))

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }

        texts.addView(TextView(context).apply {
            text = formatDateOrEmpty(item.createdAt)
            setTextColor(ContextCompat.getColor(context, R.color.profile_text))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        })

        texts.addView(TextView(context).apply {
            text = getString(R.string.weight_history_item_subtitle)
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            textSize = 13f
        })

        row.addView(
            texts,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        row.addView(
            TextView(context).apply {
                text = getString(R.string.weight_value_format, formatWeightKg(item.weightKg))
                setTextColor(ContextCompat.getColor(context, R.color.profile_text))
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        row.addView(
            TextView(context).apply {
                text = "›"
                setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
                textSize = 28f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        return row
    }

    private fun historyIconFor(trend: WeightTrend): Int {
        return when (trend) {
            WeightTrend.UP -> R.drawable.ic_arrow_upward
            WeightTrend.DOWN -> R.drawable.ic_arrow_downward
            WeightTrend.SAME -> R.drawable.ic_history
        }
    }

    private fun historyColorFor(trend: WeightTrend): Int {
        return when (trend) {
            WeightTrend.UP -> ContextCompat.getColor(requireContext(), R.color.fitto_orange)
            WeightTrend.DOWN -> ContextCompat.getColor(requireContext(), R.color.fitto_green)
            WeightTrend.SAME -> ContextCompat.getColor(requireContext(), R.color.profile_subtext)
        }
    }

    private fun refreshSelectedHistoryPreview() {
        val selectedId = selectedHistoryItemId ?: return
        val selectedItem = historyItems.firstOrNull { it.id == selectedId }
        if (selectedItem == null) {
            selectedHistoryItemId = null
            previewHistoryItem = null
        } else {
            previewHistoryItem = selectedItem
        }
    }

    private fun clearHistoryPreview() {
        selectedHistoryItemId = null
        previewHistoryItem = null
        clearFocusedWeightInput()
        bindEditableWeight()
        bindHistoryList(historyItems)

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun showHistoryScreen() {
        clearFocusedWeightInput()
        isHistoryScreenVisible = true

        toolbar.title = getString(R.string.weight_history_title)
        historyMenuItem?.isVisible = false

        mainScrollView.isVisible = false
        historyScrollView.isVisible = true

        historyScrollView.post {
            historyScrollView.scrollTo(0, 0)
        }
    }

    private fun showMainScreen() {
        isHistoryScreenVisible = false

        toolbar.title = getString(R.string.profile_menu_weight)
        historyMenuItem?.isVisible = true

        historyScrollView.isVisible = false
        mainScrollView.isVisible = true

        if (previewHistoryItem == null) {
            bindEditableWeight()
        } else {
            bindHistoryPreview()
        }

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun isTouchOutsideView(event: MotionEvent, target: View): Boolean {
        val rect = android.graphics.Rect()
        target.getGlobalVisibleRect(rect)
        return !rect.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    private fun formatWeightOrEmpty(value: Double): String {
        return if (value > 0.0) {
            getString(R.string.weight_value_format, formatWeightKg(value))
        } else {
            getString(R.string.profile_data_not_saved_yet)
        }
    }

    private fun formatDateOrEmpty(timestamp: Long): String {
        return if (timestamp > 0L) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
        } else {
            getString(R.string.profile_body_measurements_no_date)
        }
    }

    private fun roundedDrawable(
        color: Int,
        strokeColor: Int,
        radiusDp: Int
    ): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            setStroke(dp(1), strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private class WeightDecimalInputFilter : InputFilter {
        private val pattern = Regex("^\\d{0,3}([,.]\\d{0,3})?$")

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val current = dest?.toString().orEmpty()
            val incoming = source?.subSequence(start, end)?.toString().orEmpty()
            val result = current.substring(0, dstart) + incoming + current.substring(dend)

            if (result.isEmpty()) return null
            return if (pattern.matches(result)) null else ""
        }
    }
}
