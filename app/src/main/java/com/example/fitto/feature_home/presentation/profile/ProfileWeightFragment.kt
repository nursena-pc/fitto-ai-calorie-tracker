package com.example.fitto.feature_home.presentation.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.formatSignedWeightKg
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg
import com.example.fitto.feature_home.presentation.profile.reminders.ProfileRemindersActivity
import com.example.fitto.feature_home.presentation.profile.view.WeightTrendChartView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var historyRenderer: ProfileWeightHistoryRenderer

    private var historyMenuItem: MenuItem? = null
    private val weightController = WeightTrackingController()
    private var isBindingInput: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = obtainProfileViewModel()

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
        historyRenderer = ProfileWeightHistoryRenderer(
            context = requireContext(),
            container = root.findViewById(R.id.layoutWeightHistory),
            onItemSelected = ::onHistoryItemSelected
        )
    }

    private fun setupToolbar() {
        historyMenuItem = toolbar.menu.findItem(R.id.action_weight_history)

        toolbar.setNavigationOnClickListener {
            if (weightController.state.screen == WeightTrackingScreen.HISTORY) {
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
                    if (weightController.state.screen == WeightTrackingScreen.HISTORY) {
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

                val result = weightController.onWeightInput(
                    rawInput = weightInput.text?.toString().orEmpty()
                )
                updateSaveButtonState(result.state)
            }
        })

        weightInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !weightController.state.isReadOnly) {
                formatWeightInputAfterEditing()
            }
            updateSaveButtonState(weightController.state)
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
            if (!weightController.state.isReadOnly) {
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
        viewModel.uiState.observe(viewLifecycleOwner) { profileState ->
            val shouldReplaceDraft = !weightInput.hasFocus() ||
                !weightController.state.hasUnsavedWeightInput
            val weightState = weightController.updateProfile(
                healthData = profileState.healthData,
                allowDraftReplacement = shouldReplaceDraft
            )

            renderWeightState(
                state = weightState,
                forceInputBinding = shouldReplaceDraft || weightState.isReadOnly
            )
            renderHistory(weightState)
            bindChart(weightState)

            profileState.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderWeightState(
        state: WeightTrackingState,
        forceInputBinding: Boolean
    ) {
        if (state.isReadOnly) {
            bindHistoryPreview(state)
        } else {
            bindEditableWeight(state, forceInputBinding)
        }
    }

    private fun bindEditableWeight(
        state: WeightTrackingState,
        forceInputBinding: Boolean
    ) {
        val healthData = state.healthData
        bindWeightInput(
            value = state.draftWeightKg ?: 0.0,
            readOnly = false,
            force = forceInputBinding
        )

        textLastWeighDate.text = if (healthData.lastWeighDateText.isNotBlank()) {
            getString(R.string.weight_last_weigh_format, healthData.lastWeighDateText)
        } else {
            getString(R.string.weight_last_weigh_default)
        }

        textInitialWeightValue.text = formatWeightOrEmpty(healthData.initialWeightKg)
        textTargetWeightValue.text = formatWeightOrEmpty(healthData.targetWeightKg)
        textLostSoFar.text = getString(
            R.string.weight_lost_so_far_format,
            formatWeightKg(healthData.lostSoFarKg).ifBlank { "0" }
        )
        textRemainingWeight.text = getString(
            R.string.weight_remaining_format,
            formatWeightKg(healthData.remainingKg).ifBlank { "0" }
        )

        saveWeightButton.isVisible = true
        returnCurrentButton.isVisible = false
        weightInput.isFocusable = true
        weightInput.isFocusableInTouchMode = true
        weightInput.isCursorVisible = weightInput.hasFocus()
        updateSaveButtonState(state)
    }

    private fun bindHistoryPreview(state: WeightTrackingState) {
        val item = state.previewItem ?: return
        val healthData = state.healthData

        clearFocusedWeightInput()
        bindWeightInput(value = item.weightKg, readOnly = true, force = true)

        textLastWeighDate.text = getString(
            R.string.weight_preview_hint,
            formatDateOrEmpty(item.createdAt)
        )
        textInitialWeightValue.text = formatWeightOrEmpty(healthData.initialWeightKg)
        textTargetWeightValue.text = formatWeightOrEmpty(healthData.targetWeightKg)
        textLostSoFar.text = getString(
            R.string.weight_lost_so_far_format,
            formatWeightKg(healthData.lostSoFarKg).ifBlank { "0" }
        )
        textRemainingWeight.text = getString(
            R.string.weight_remaining_format,
            formatWeightKg(healthData.remainingKg).ifBlank { "0" }
        )

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
        isBindingInput = false
    }

    private fun bindChart(state: WeightTrackingState) {
        weightTrendChartView.submitData(state.healthData.chartPoints)

        val summary = state.chartSummary
        if (summary == null) {
            chartSummaryLayout.isVisible = false
            return
        }

        chartFirstValue.text = getString(
            R.string.weight_value_format,
            formatWeightKg(summary.firstWeightKg)
        )
        chartLastValue.text = getString(
            R.string.weight_value_format,
            formatWeightKg(summary.lastWeightKg)
        )
        chartChangeValue.text = getString(
            R.string.weight_value_format,
            formatSignedWeightKg(summary.changeKg)
        )
        chartSummaryLayout.isVisible = true
    }

    private fun updateSaveButtonState(state: WeightTrackingState) {
        if (state.isReadOnly) return

        saveWeightButton.isEnabled = state.canSave
        saveWeightButton.alpha = if (state.canSave) 1f else 0.58f
        saveWeightButton.text = if (state.isSavingWeight) {
            getString(R.string.weight_saving)
        } else {
            getString(R.string.weight_save)
        }
    }

    private fun saveWeight() {
        val currentState = weightController.state
        if (currentState.isReadOnly || currentState.isSavingWeight) return

        val weight = weightController.beginSave()
        if (weight == null) {
            Toast.makeText(requireContext(), R.string.weight_invalid, Toast.LENGTH_SHORT).show()
            focusWeightInput()
            return
        }

        clearFocusedWeightInput()
        updateSaveButtonState(weightController.state)

        viewModel.updateWeight(
            weightKg = weight,
            onSuccess = {
                val completedState = weightController.completeSave(weight)
                updateSaveButtonState(completedState)
                Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show()
            },
            onError = { message ->
                val failedState = weightController.failSave()
                updateSaveButtonState(failedState)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun formatWeightInputAfterEditing() {
        val value = weightController.formatInputAfterEditing(
            weightInput.text?.toString().orEmpty()
        ) ?: return
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

    private fun renderHistory(state: WeightTrackingState) {
        historyRenderer.render(
            items = state.visibleHistoryItems,
            selectedItemId = state.selectedHistoryItemId
        )
    }

    private fun onHistoryItemSelected(itemId: String) {
        val selectedState = weightController.selectHistoryItem(itemId)
        clearFocusedWeightInput()
        renderHistory(selectedState)
        showMainScreen(selectedState)
    }

    private fun clearHistoryPreview() {
        val state = weightController.clearHistoryPreview()
        clearFocusedWeightInput()
        bindEditableWeight(state, forceInputBinding = true)
        renderHistory(state)

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun showHistoryScreen() {
        clearFocusedWeightInput()
        val state = weightController.showHistory()
        renderScreen(state)

        historyScrollView.post {
            historyScrollView.scrollTo(0, 0)
        }
    }

    private fun showMainScreen(state: WeightTrackingState = weightController.showCurrent()) {
        val currentState = if (state.screen == WeightTrackingScreen.CURRENT) {
            state
        } else {
            weightController.showCurrent()
        }
        renderScreen(currentState)
        renderWeightState(currentState, forceInputBinding = true)

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun renderScreen(state: WeightTrackingState) {
        val isHistoryVisible = state.screen == WeightTrackingScreen.HISTORY
        toolbar.title = getString(
            if (isHistoryVisible) R.string.weight_history_title else R.string.profile_menu_weight
        )
        historyMenuItem?.isVisible = !isHistoryVisible
        mainScrollView.isVisible = !isHistoryVisible
        historyScrollView.isVisible = isHistoryVisible
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
