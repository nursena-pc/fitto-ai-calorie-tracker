package com.example.fitto.feature_home.presentation.profile

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
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
import android.widget.FrameLayout
import android.widget.GridLayout
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
import com.example.fitto.feature_home.presentation.profile.view.BodyMeasurementsDiagramView
import com.example.fitto.feature_home.presentation.profile.view.BodyMeasurementsTrendChartView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class ProfileBodyMeasurementsFragment : Fragment(R.layout.fragment_profile_body_measurements) {

    private lateinit var viewModel: ProfileViewModel
    private val measurementsController = BodyMeasurementsController()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var historyScrollView: NestedScrollView
    private lateinit var diagramFrame: FrameLayout
    private lateinit var diagramView: BodyMeasurementsDiagramView
    private lateinit var trendChartView: BodyMeasurementsTrendChartView
    private lateinit var textLastUpdate: TextView
    private lateinit var textMode: TextView
    private lateinit var historyLayout: LinearLayout
    private lateinit var filterGrid: GridLayout
    private lateinit var chartPickerHeader: LinearLayout
    private lateinit var chartSelectedText: TextView
    private lateinit var chartPickerArrow: ImageView
    private lateinit var rangeFilterLayout: LinearLayout
    private lateinit var chartSummaryLayout: LinearLayout
    private lateinit var chartFirstValue: TextView
    private lateinit var chartLastValue: TextView
    private lateinit var chartChangeValue: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var returnCurrentButton: MaterialButton

    private val measurementInputs = linkedMapOf<BodyMeasurementField, EditText>()
    private val measurementCards = linkedMapOf<BodyMeasurementField, LinearLayout>()
    private var ignoreMeasurementInputChanges: Boolean = false

    private var historyMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]

        bindViews(view)
        createMeasurementInputOverlays()
        setupToolbar()
        setupBackHandling()
        setupClicks()
        setupFilters()
        observeViewModel()

        viewModel.loadProfile()
    }

    private fun bindViews(root: View) {
        toolbar = root.findViewById(R.id.toolbarBodyMeasurements)
        mainScrollView = root.findViewById(R.id.scrollMainMeasurements)
        historyScrollView = root.findViewById(R.id.scrollMeasurementsHistory)
        diagramFrame = root.findViewById(R.id.frameBodyMeasurementsDiagram)
        diagramView = root.findViewById(R.id.bodyMeasurementsDiagramView)
        trendChartView = root.findViewById(R.id.bodyMeasurementsTrendChartView)
        textLastUpdate = root.findViewById(R.id.textBodyMeasurementsLastUpdate)
        textMode = root.findViewById(R.id.textBodyMeasurementsMode)
        historyLayout = root.findViewById(R.id.layoutMeasurementsHistory)
        filterGrid = root.findViewById(R.id.gridMeasurementFilters)
        chartPickerHeader = root.findViewById(R.id.layoutChartPickerHeader)
        chartSelectedText = root.findViewById(R.id.textSelectedChartField)
        chartPickerArrow = root.findViewById(R.id.imageChartPickerArrow)
        rangeFilterLayout = root.findViewById(R.id.layoutChartRangeFilters)
        chartSummaryLayout = root.findViewById(R.id.layoutChartSummary)
        chartFirstValue = root.findViewById(R.id.textChartFirstValue)
        chartLastValue = root.findViewById(R.id.textChartLastValue)
        chartChangeValue = root.findViewById(R.id.textChartChangeValue)
        saveButton = root.findViewById(R.id.buttonSaveBodyMeasurements)
        returnCurrentButton = root.findViewById(R.id.buttonReturnCurrentMeasurements)
    }

    private fun createMeasurementInputOverlays() {
        if (measurementInputs.isNotEmpty()) return

        BodyMeasurementField.entries.forEach { field ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(5), dp(8), dp(5))
                background = measurementCardBackground(active = false, readOnly = false)
                elevation = dp(1).toFloat()
                visibility = View.INVISIBLE
            }

            val title = TextView(requireContext()).apply {
                text = getString(field.labelResId)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_text))
                textSize = 11.6f
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                maxLines = 1
            }

            val valueRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, 0)
            }

            val input = EditText(requireContext()).apply {
                tag = field
                background = null
                includeFontPadding = false
                setPadding(0, 0, 0, 0)
                minHeight = 0
                textSize = 15.8f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_green))
                setHintTextColor(ContextCompat.getColor(requireContext(), R.color.profile_subtext))
                hint = "-"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setSingleLine(true)
                setSelectAllOnFocus(true)
                inputType = InputType.TYPE_CLASS_PHONE
                keyListener = DigitsKeyListener.getInstance("0123456789,.")
                imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }

            val unit = TextView(requireContext()).apply {
                text = "cm"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.profile_subtext))
                textSize = 10.8f
                includeFontPadding = false
                setPadding(dp(2), 0, 0, 0)
            }

            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    if (ignoreMeasurementInputChanges) return

                    val current = s?.toString().orEmpty()
                    val result = measurementsController.onMeasurementInput(field, current)

                    if (result.normalizedInput != current) {
                        ignoreMeasurementInputChanges = true
                        input.setText(result.normalizedInput)
                        input.setSelection(result.normalizedInput.length)
                        ignoreMeasurementInputChanges = false
                    }

                    if (result.accepted) {
                        bindDraftChangeState(result.state)
                    }
                }
            })

            input.setOnFocusChangeListener { _, hasFocus ->
                val readOnly = measurementsController.state.isReadOnly
                input.isCursorVisible = hasFocus && !readOnly
                bindMeasurementCardState(field, active = hasFocus && !readOnly, readOnly = readOnly)

                if (hasFocus && !readOnly) {
                    input.selectAll()
                } else if (!readOnly) {
                    formatInputAfterEditing(field)
                }
            }

            input.setOnEditorActionListener { _, actionId, event ->
                val isDone = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)

                if (isDone) {
                    input.clearFocus()
                    hideKeyboardFrom(input)
                    true
                } else {
                    false
                }
            }

            valueRow.addView(
                input,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            valueRow.addView(
                unit,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )

            card.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            card.addView(valueRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            diagramFrame.addView(card)
            measurementCards[field] = card
            measurementInputs[field] = input
        }
    }

    private fun setupToolbar() {
        toolbar.inflateMenu(R.menu.menu_body_measurements)
        historyMenuItem = toolbar.menu.findItem(R.id.menuBodyMeasurementsHistory)

        toolbar.setNavigationOnClickListener {
            if (measurementsController.state.screen == BodyMeasurementsScreen.HISTORY) {
                showMainScreen()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuBodyMeasurementsHistory -> {
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
                    if (measurementsController.state.screen == BodyMeasurementsScreen.HISTORY) {
                        showMainScreen()
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        )
    }

    private fun setupClicks() {
        diagramView.onLabelBoundsChanged = { bounds ->
            bounds.forEach { bound ->
                val card = measurementCards[bound.field] ?: return@forEach
                val params = FrameLayout.LayoutParams(bound.width, bound.height).apply {
                    leftMargin = bound.left
                    topMargin = bound.top
                }
                card.layoutParams = params
                card.visibility = View.VISIBLE
            }
        }

        chartPickerHeader.setOnClickListener {
            bindChartPickerVisibility(measurementsController.toggleChartPicker())
        }

        saveButton.setOnClickListener {
            clearFocusedMeasurementInput()
            saveDraftMeasurements()
        }

        returnCurrentButton.setOnClickListener {
            clearFocusedMeasurementInput()
            clearHistoryPreview()
        }

        mainScrollView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && isTouchOutsideView(event, trendChartView)) {
                trendChartView.clearSelectedPoint()
            }
            false
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val measurementState = measurementsController.updateProfile(
                genderKey = state.healthData.genderKey,
                currentMeasurements = state.bodyMeasurements,
                historyItems = state.bodyMeasurementsHistory,
                allowDraftReplacement = !hasFocusedMeasurementInput()
            )

            renderMeasurementState(measurementState)

            state.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderMeasurementState(state: BodyMeasurementsState) {
        if (state.isReadOnly) {
            bindHistoryPreview(state)
        } else {
            bindEditableMeasurements(state)
        }

        bindHistoryList(state.historyItems)
        updateChart(state)
    }

    private fun bindEditableMeasurements(state: BodyMeasurementsState) {
        diagramView.submitData(
            measurements = state.draftMeasurements,
            genderKey = state.genderKey,
            readOnly = false
        )
        bindMeasurementInputValues(state.draftMeasurements, readOnly = false)

        textLastUpdate.text = getString(
            R.string.profile_body_measurements_last_update_compact_format,
            formatDateOrEmpty(state.currentMeasurements.updatedAt)
        )

        textMode.visibility = if (state.hasUnsavedChanges) View.VISIBLE else View.INVISIBLE
        textMode.text = getString(R.string.profile_body_measurements_unsaved_hint)

        saveButton.isVisible = true
        returnCurrentButton.isVisible = false

        updateSaveButtonState(state)
    }

    private fun bindHistoryPreview(state: BodyMeasurementsState) {
        val previewItem = state.previewItem ?: return

        clearFocusedMeasurementInput()

        diagramView.submitData(
            measurements = previewItem.data,
            genderKey = state.genderKey,
            readOnly = true
        )
        bindMeasurementInputValues(previewItem.data, readOnly = true)

        textLastUpdate.text = getString(
            R.string.profile_body_measurements_last_update_compact_format,
            formatDateOrEmpty(previewItem.createdAt)
        )

        textMode.visibility = View.VISIBLE
        textMode.text = getString(R.string.profile_body_measurements_preview_hint)

        saveButton.isVisible = false
        returnCurrentButton.isVisible = true
    }

    private fun bindMeasurementInputValues(data: BodyMeasurementsData, readOnly: Boolean) {
        ignoreMeasurementInputChanges = true

        BodyMeasurementField.entries.forEach { field ->
            val input = measurementInputs[field] ?: return@forEach
            val focused = input.hasFocus()

            input.isFocusable = !readOnly
            input.isFocusableInTouchMode = !readOnly
            input.isCursorVisible = !readOnly && focused
            input.isLongClickable = !readOnly
            input.setTextIsSelectable(!readOnly)

            if (!focused || readOnly) {
                input.setText(data.getValue(field)?.formatInputDisplay().orEmpty())
                input.setSelection(input.text?.length ?: 0)
            }

            bindMeasurementCardState(field, active = focused && !readOnly, readOnly = readOnly)
        }

        ignoreMeasurementInputChanges = false
    }

    private fun bindMeasurementCardState(
        field: BodyMeasurementField,
        active: Boolean,
        readOnly: Boolean
    ) {
        val card = measurementCards[field] ?: return
        card.background = measurementCardBackground(active = active, readOnly = readOnly)
    }

    private fun updateSaveButtonState(state: BodyMeasurementsState) {
        saveButton.isEnabled = state.canSave
        saveButton.alpha = if (state.canSave) 1f else 0.58f
        saveButton.text = if (state.isSavingSnapshot) {
            getString(R.string.profile_body_measurements_saving)
        } else {
            getString(R.string.profile_body_measurements_save_snapshot)
        }
    }

    private fun setupFilters() {
        filterGrid.removeAllViews()
        BodyMeasurementField.entries.forEach { field ->
            filterGrid.addView(createFilterItem(field))
        }

        rangeFilterLayout.removeAllViews()
        BodyMeasurementsChartRange.entries.forEach { range ->
            rangeFilterLayout.addView(createRangeItem(range))
        }

        val state = measurementsController.state
        bindChartPickerVisibility(state)
        refreshFilters(state)
        refreshRangeFilters(state)
    }

    private fun bindChartPickerVisibility(state: BodyMeasurementsState) {
        chartSelectedText.text = getString(state.selectedChartField.labelResId)
        filterGrid.isVisible = state.isChartPickerExpanded

        chartPickerArrow.animate()
            .rotation(if (state.isChartPickerExpanded) 180f else 0f)
            .setDuration(160L)
            .start()
    }

    private fun refreshFilters(state: BodyMeasurementsState) {
        for (index in 0 until filterGrid.childCount) {
            val view = filterGrid.getChildAt(index) as? TextView ?: continue
            val field = view.tag as? BodyMeasurementField ?: continue
            bindFilterItem(view, field, state)
        }
    }

    private fun createFilterItem(field: BodyMeasurementField): TextView {
        return TextView(requireContext()).apply {
            tag = field
            text = getString(field.labelResId)
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(10), dp(12), dp(10))

            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }

            setOnClickListener {
                val state = measurementsController.selectChartField(field)
                bindChartPickerVisibility(state)
                refreshFilters(state)
                updateChart(state)
            }

            bindFilterItem(this, field, measurementsController.state)
        }
    }

    private fun bindFilterItem(
        view: TextView,
        field: BodyMeasurementField,
        state: BodyMeasurementsState
    ) {
        val selected = state.selectedChartField == field
        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)

        view.setTextColor(
            if (selected) {
                ContextCompat.getColor(requireContext(), android.R.color.white)
            } else {
                ContextCompat.getColor(requireContext(), R.color.profile_text)
            }
        )

        view.background = roundedDrawable(
            color = if (selected) green else ContextCompat.getColor(requireContext(), R.color.profile_screen_background),
            strokeColor = if (selected) green else ContextCompat.getColor(requireContext(), R.color.profile_stroke),
            radiusDp = 16
        )
    }

    private fun createRangeItem(range: BodyMeasurementsChartRange): TextView {
        return TextView(requireContext()).apply {
            tag = range
            text = getRangeLabel(range)
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), dp(9), dp(12), dp(9))

            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), dp(2), dp(4), dp(2))
            }

            setOnClickListener {
                val state = measurementsController.selectChartRange(range)
                refreshRangeFilters(state)
                updateChart(state)
            }

            bindRangeItem(this, range, measurementsController.state)
        }
    }

    private fun refreshRangeFilters(state: BodyMeasurementsState) {
        for (index in 0 until rangeFilterLayout.childCount) {
            val view = rangeFilterLayout.getChildAt(index) as? TextView ?: continue
            val range = view.tag as? BodyMeasurementsChartRange ?: continue
            bindRangeItem(view, range, state)
        }
    }

    private fun bindRangeItem(
        view: TextView,
        range: BodyMeasurementsChartRange,
        state: BodyMeasurementsState
    ) {
        val selected = state.selectedChartRange == range
        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)

        view.setTextColor(
            if (selected) ContextCompat.getColor(requireContext(), android.R.color.white)
            else ContextCompat.getColor(requireContext(), R.color.profile_text)
        )

        view.background = roundedDrawable(
            color = if (selected) green else ContextCompat.getColor(requireContext(), R.color.profile_screen_background),
            strokeColor = if (selected) green else ContextCompat.getColor(requireContext(), R.color.profile_stroke),
            radiusDp = 16
        )
    }

    private fun getRangeLabel(range: BodyMeasurementsChartRange): String {
        return when (range) {
            BodyMeasurementsChartRange.ONE_MONTH -> getString(R.string.profile_body_measurements_chart_range_1m)
            BodyMeasurementsChartRange.THREE_MONTHS -> getString(R.string.profile_body_measurements_chart_range_3m)
            BodyMeasurementsChartRange.ALL -> getString(R.string.profile_body_measurements_chart_range_all)
        }
    }

    private fun updateChart(state: BodyMeasurementsState) {
        trendChartView.submitData(
            state.chartPoints.map { point -> point.timestamp to point.value }
        )
        bindChartSummary(state.chartSummary)
    }

    private fun bindChartSummary(summary: BodyMeasurementsChartSummary?) {
        if (summary == null) {
            chartSummaryLayout.isVisible = false
            return
        }

        chartSummaryLayout.isVisible = true

        chartFirstValue.text = String.format(Locale.getDefault(), "%.2f cm", summary.firstValue)
        chartLastValue.text = String.format(Locale.getDefault(), "%.2f cm", summary.lastValue)
        chartChangeValue.text = String.format(Locale.getDefault(), "%+.2f cm", summary.change)

        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val text = ContextCompat.getColor(requireContext(), R.color.profile_text)
        val subText = ContextCompat.getColor(requireContext(), R.color.profile_subtext)

        chartFirstValue.setTextColor(text)
        chartLastValue.setTextColor(text)
        chartChangeValue.setTextColor(if (summary.change == 0f) subText else green)
    }

    private fun bindDraftChangeState(state: BodyMeasurementsState) {
        textMode.visibility = if (state.hasUnsavedChanges) View.VISIBLE else View.INVISIBLE
        textMode.text = getString(R.string.profile_body_measurements_unsaved_hint)
        updateSaveButtonState(state)
    }

    private fun formatInputAfterEditing(field: BodyMeasurementField) {
        val input = measurementInputs[field] ?: return
        val formatted = measurementsController.formatInputAfterEditing(
            input.text?.toString().orEmpty()
        )

        ignoreMeasurementInputChanges = true
        input.setText(formatted)
        input.setSelection(input.text?.length ?: 0)
        ignoreMeasurementInputChanges = false
    }

    private fun clearFocusedMeasurementInput() {
        val focused = measurementInputs.values.firstOrNull { it.hasFocus() } ?: return
        focused.clearFocus()
        hideKeyboardFrom(focused)
    }

    private fun hasFocusedMeasurementInput(): Boolean {
        return measurementInputs.values.any { it.hasFocus() }
    }

    private fun hideKeyboardFrom(view: View) {
        val imm = requireContext()
            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun saveDraftMeasurements() {
        val dataToSave = measurementsController.beginSave() ?: return
        updateSaveButtonState(measurementsController.state)

        viewModel.updateBodyMeasurements(
            data = dataToSave,
            onSuccess = {
                val state = measurementsController.completeSave(dataToSave)
                bindEditableMeasurements(state)
                updateChart(state)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_body_measurements_saved),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { message ->
                updateSaveButtonState(measurementsController.failSave())
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindHistoryList(items: List<BodyMeasurementsHistoryItem>) {
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
            text = getString(R.string.profile_body_measurements_empty_history)
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

    private fun createHistoryRow(item: BodyMeasurementsHistoryItem): View {
        val context = requireContext()
        val isSelected = item.id == measurementsController.state.selectedHistoryItemId

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
                val state = measurementsController.selectHistoryItem(item.id)

                clearFocusedMeasurementInput()
                diagramView.clearInlineEditing()
                bindHistoryList(state.historyItems)
                showMainScreen(state)
            }
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_history)
            setColorFilter(ContextCompat.getColor(context, R.color.fitto_green))
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
            text = resources.getQuantityString(
                R.plurals.profile_body_measurements_record_count,
                item.data.filledCount,
                item.data.filledCount
            )
            setTextColor(ContextCompat.getColor(context, R.color.profile_subtext))
            textSize = 13f
        })

        row.addView(
            texts,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
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

    private fun clearHistoryPreview() {
        val state = measurementsController.clearHistoryPreview()

        clearFocusedMeasurementInput()
        diagramView.clearInlineEditing()
        bindEditableMeasurements(state)
        bindHistoryList(state.historyItems)

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun showHistoryScreen() {
        measurementsController.showHistory()
        clearFocusedMeasurementInput()
        diagramView.clearInlineEditing()

        toolbar.title = getString(R.string.profile_body_measurements_history)
        historyMenuItem?.isVisible = false

        mainScrollView.isVisible = false
        historyScrollView.isVisible = true

        historyScrollView.post {
            historyScrollView.scrollTo(0, 0)
        }
    }

    private fun showMainScreen(
        state: BodyMeasurementsState = measurementsController.showEditor()
    ) {
        toolbar.title = getString(R.string.profile_body_measurements_title)
        historyMenuItem?.isVisible = true

        historyScrollView.isVisible = false
        mainScrollView.isVisible = true

        if (state.isReadOnly) {
            bindHistoryPreview(state)
        } else {
            bindEditableMeasurements(state)
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

    private fun formatDateOrEmpty(timestamp: Long): String {
        return if (timestamp > 0L) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(timestamp))
        } else {
            getString(R.string.profile_body_measurements_no_date)
        }
    }

    private fun Float.formatInputDisplay(): String {
        return String.format(Locale.US, "%.2f", this)
    }

    private fun measurementCardBackground(
        active: Boolean,
        readOnly: Boolean
    ): android.graphics.drawable.GradientDrawable {
        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val cardColor = ContextCompat.getColor(requireContext(), R.color.profile_card_background)
        val screenColor = ContextCompat.getColor(requireContext(), R.color.profile_screen_background)
        val strokeColor = ContextCompat.getColor(requireContext(), R.color.profile_stroke)

        return roundedDrawable(
            color = when {
                active -> android.graphics.Color.argb(24, 76, 175, 80)
                readOnly -> screenColor
                else -> cardColor
            },
            strokeColor = if (active) green else strokeColor,
            radiusDp = 14
        )
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
}
