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

    private enum class ChartRange {
        ONE_MONTH,
        THREE_MONTHS,
        ALL
    }

    private lateinit var viewModel: ProfileViewModel

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

    private var latestGenderKey: String = ""
    private var currentMeasurements: BodyMeasurementsData = BodyMeasurementsData()
    private var draftMeasurements: BodyMeasurementsData = BodyMeasurementsData()
    private var historyItems: List<BodyMeasurementsHistoryItem> = emptyList()

    private var selectedChartField: BodyMeasurementField = BodyMeasurementField.WAIST
    private var selectedChartRange: ChartRange = ChartRange.ALL
    private var isChartPickerExpanded: Boolean = false
    private var selectedHistoryItemId: String? = null
    private var previewHistoryMeasurements: BodyMeasurementsData? = null
    private var previewHistoryTimestamp: Long = 0L

    private var hasUnsavedChanges: Boolean = false
    private var isHistoryScreenVisible: Boolean = false
    private var historyMenuItem: MenuItem? = null
    private var isSavingSnapshot: Boolean = false

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
                    if (previewHistoryMeasurements != null) return

                    val current = s?.toString().orEmpty()
                    val normalized = normalizeMeasurementInput(current)

                    if (normalized != current) {
                        ignoreMeasurementInputChanges = true
                        input.setText(normalized)
                        input.setSelection(normalized.length)
                        ignoreMeasurementInputChanges = false
                    }

                    applyMeasurementInput(field, normalized)
                }
            })

            input.setOnFocusChangeListener { _, hasFocus ->
                val readOnly = previewHistoryMeasurements != null
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
            if (isHistoryScreenVisible) {
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
                    if (isHistoryScreenVisible) {
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
            isChartPickerExpanded = !isChartPickerExpanded
            bindChartPickerVisibility()
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
            latestGenderKey = state.healthData.genderKey
            currentMeasurements = state.bodyMeasurements
            historyItems = state.bodyMeasurementsHistory

            if (!hasUnsavedChanges && !hasFocusedMeasurementInput()) {
                draftMeasurements = currentMeasurements
            }

            refreshSelectedHistoryPreview()

            if (previewHistoryMeasurements == null) {
                bindEditableMeasurements()
            } else {
                bindHistoryPreview()
            }

            bindHistoryList(historyItems)
            updateChart()

            state.errorMessage?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindEditableMeasurements() {
        diagramView.submitData(
            measurements = draftMeasurements,
            genderKey = latestGenderKey,
            readOnly = false
        )
        bindMeasurementInputValues(draftMeasurements, readOnly = false)

        textLastUpdate.text = getString(
            R.string.profile_body_measurements_last_update_compact_format,
            formatDateOrEmpty(currentMeasurements.updatedAt)
        )

        textMode.visibility = if (hasUnsavedChanges) View.VISIBLE else View.INVISIBLE
        textMode.text = getString(R.string.profile_body_measurements_unsaved_hint)

        saveButton.isVisible = true
        returnCurrentButton.isVisible = false

        updateSaveButtonState()
    }

    private fun bindHistoryPreview() {
        val data = previewHistoryMeasurements ?: return

        clearFocusedMeasurementInput()

        diagramView.submitData(
            measurements = data,
            genderKey = latestGenderKey,
            readOnly = true
        )
        bindMeasurementInputValues(data, readOnly = true)

        textLastUpdate.text = getString(
            R.string.profile_body_measurements_last_update_compact_format,
            formatDateOrEmpty(previewHistoryTimestamp)
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

    private fun updateSaveButtonState() {
        val canSave = previewHistoryMeasurements == null &&
            draftMeasurements.filledCount > 0 &&
            !isSavingSnapshot

        saveButton.isEnabled = canSave
        saveButton.alpha = if (canSave) 1f else 0.58f
        saveButton.text = if (isSavingSnapshot) {
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
        ChartRange.entries.forEach { range ->
            rangeFilterLayout.addView(createRangeItem(range))
        }

        bindChartPickerVisibility()
        refreshRangeFilters()
    }

    private fun bindChartPickerVisibility() {
        chartSelectedText.text = getString(selectedChartField.labelResId)
        filterGrid.isVisible = isChartPickerExpanded

        chartPickerArrow.animate()
            .rotation(if (isChartPickerExpanded) 180f else 0f)
            .setDuration(160L)
            .start()
    }

    private fun refreshFilters() {
        for (index in 0 until filterGrid.childCount) {
            val view = filterGrid.getChildAt(index) as? TextView ?: continue
            val field = view.tag as? BodyMeasurementField ?: continue
            bindFilterItem(view, field)
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
                selectedChartField = field
                isChartPickerExpanded = false
                bindChartPickerVisibility()
                refreshFilters()
                updateChart()
            }

            bindFilterItem(this, field)
        }
    }

    private fun bindFilterItem(view: TextView, field: BodyMeasurementField) {
        val selected = selectedChartField == field
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

    private fun createRangeItem(range: ChartRange): TextView {
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
                selectedChartRange = range
                refreshRangeFilters()
                updateChart()
            }

            bindRangeItem(this, range)
        }
    }

    private fun refreshRangeFilters() {
        for (index in 0 until rangeFilterLayout.childCount) {
            val view = rangeFilterLayout.getChildAt(index) as? TextView ?: continue
            val range = view.tag as? ChartRange ?: continue
            bindRangeItem(view, range)
        }
    }

    private fun bindRangeItem(view: TextView, range: ChartRange) {
        val selected = selectedChartRange == range
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

    private fun getRangeLabel(range: ChartRange): String {
        return when (range) {
            ChartRange.ONE_MONTH -> getString(R.string.profile_body_measurements_chart_range_1m)
            ChartRange.THREE_MONTHS -> getString(R.string.profile_body_measurements_chart_range_3m)
            ChartRange.ALL -> getString(R.string.profile_body_measurements_chart_range_all)
        }
    }

    private fun updateChart() {
        val allPoints = historyItems
            .asReversed()
            .mapNotNull { item ->
                val value = item.data.getValue(selectedChartField) ?: return@mapNotNull null
                item.createdAt to value
            }

        val minTimestamp = when (selectedChartRange) {
            ChartRange.ONE_MONTH -> System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
            ChartRange.THREE_MONTHS -> System.currentTimeMillis() - 90L * 24L * 60L * 60L * 1000L
            ChartRange.ALL -> 0L
        }

        val visiblePoints = if (minTimestamp > 0L) {
            allPoints.filter { it.first >= minTimestamp }
        } else {
            allPoints
        }

        trendChartView.submitData(visiblePoints)
        bindChartSummary(visiblePoints)
    }

    private fun bindChartSummary(points: List<Pair<Long, Float>>) {
        if (points.isEmpty()) {
            chartSummaryLayout.isVisible = false
            return
        }

        val first = points.first().second
        val last = points.last().second
        val change = last - first

        chartSummaryLayout.isVisible = true

        chartFirstValue.text = String.format(Locale.getDefault(), "%.2f cm", first)
        chartLastValue.text = String.format(Locale.getDefault(), "%.2f cm", last)
        chartChangeValue.text = String.format(Locale.getDefault(), "%+.2f cm", change)

        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val text = ContextCompat.getColor(requireContext(), R.color.profile_text)
        val subText = ContextCompat.getColor(requireContext(), R.color.profile_subtext)

        chartFirstValue.setTextColor(text)
        chartLastValue.setTextColor(text)

        chartChangeValue.setTextColor(
            when {
                change == 0f -> subText
                else -> green
            }
        )
    }
    private fun applyMeasurementInput(field: BodyMeasurementField, normalized: String) {
        val value = normalized.toFloatOrNull()
        val safeValue = when {
            normalized.isBlank() -> null
            normalized.endsWith(".") && value != null && value > 0f && value <= 300f -> value
            value != null && value > 0f && value <= 300f -> value
            else -> return
        }

        updateDraftFieldFromInput(field, safeValue)
    }

    private fun updateDraftFieldFromInput(field: BodyMeasurementField, value: Float?) {
        draftMeasurements = draftMeasurements.withValue(field, value)

        hasUnsavedChanges = hasMeasurementChanges(
            draft = draftMeasurements,
            current = currentMeasurements
        )

        textMode.visibility = if (hasUnsavedChanges) View.VISIBLE else View.INVISIBLE
        textMode.text = getString(R.string.profile_body_measurements_unsaved_hint)
        updateSaveButtonState()
    }

    private fun normalizeMeasurementInput(raw: String): String {
        val builder = StringBuilder()
        var hasDecimalSeparator = false
        var decimalCount = 0

        raw.replace(',', '.').forEach { char ->
            when {
                char.isDigit() && !hasDecimalSeparator && builder.length < 3 -> {
                    builder.append(char)
                }

                char.isDigit() && hasDecimalSeparator && decimalCount < 2 -> {
                    builder.append(char)
                    decimalCount++
                }

                char == '.' && !hasDecimalSeparator && builder.isNotEmpty() -> {
                    builder.append(char)
                    hasDecimalSeparator = true
                }

                char == '.' && !hasDecimalSeparator && builder.isEmpty() -> {
                    builder.append("0.")
                    hasDecimalSeparator = true
                }
            }
        }

        return builder.toString()
    }

    private fun formatInputAfterEditing(field: BodyMeasurementField) {
        val input = measurementInputs[field] ?: return
        val value = input.text?.toString()?.toFloatOrNull()
        val formatted = value?.takeIf { it > 0f && it <= 300f }?.formatInputDisplay().orEmpty()

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
        if (previewHistoryMeasurements != null) return
        if (draftMeasurements.filledCount == 0) return
        if (isSavingSnapshot) return

        val dataToSave = draftMeasurements.withUpdatedAtNow()

        isSavingSnapshot = true
        updateSaveButtonState()

        viewModel.updateBodyMeasurements(
            data = dataToSave,
            onSuccess = {
                selectedHistoryItemId = null
                previewHistoryMeasurements = null
                previewHistoryTimestamp = 0L

                currentMeasurements = dataToSave
                draftMeasurements = dataToSave
                hasUnsavedChanges = false
                isSavingSnapshot = false

                bindEditableMeasurements()
                updateChart()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile_body_measurements_saved),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { message ->
                isSavingSnapshot = false
                updateSaveButtonState()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun hasMeasurementChanges(
        draft: BodyMeasurementsData,
        current: BodyMeasurementsData
    ): Boolean {
        return BodyMeasurementField.entries.any { field ->
            draft.getValue(field) != current.getValue(field)
        }
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
                previewHistoryMeasurements = item.data
                previewHistoryTimestamp = item.createdAt

                clearFocusedMeasurementInput()
                diagramView.clearInlineEditing()
                bindHistoryList(historyItems)
                showMainScreen()
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

    private fun refreshSelectedHistoryPreview() {
        val selectedId = selectedHistoryItemId ?: return

        val selectedItem = historyItems.firstOrNull { it.id == selectedId }
        if (selectedItem == null) {
            selectedHistoryItemId = null
            previewHistoryMeasurements = null
            previewHistoryTimestamp = 0L
        } else {
            previewHistoryMeasurements = selectedItem.data
            previewHistoryTimestamp = selectedItem.createdAt
        }
    }

    private fun clearHistoryPreview() {
        selectedHistoryItemId = null
        previewHistoryMeasurements = null
        previewHistoryTimestamp = 0L

        clearFocusedMeasurementInput()
        diagramView.clearInlineEditing()
        bindEditableMeasurements()
        bindHistoryList(historyItems)

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun showHistoryScreen() {
        clearFocusedMeasurementInput()
        diagramView.clearInlineEditing()
        isHistoryScreenVisible = true

        toolbar.title = getString(R.string.profile_body_measurements_history)
        historyMenuItem?.isVisible = false

        mainScrollView.isVisible = false
        historyScrollView.isVisible = true

        historyScrollView.post {
            historyScrollView.scrollTo(0, 0)
        }
    }

    private fun showMainScreen() {
        isHistoryScreenVisible = false

        toolbar.title = getString(R.string.profile_body_measurements_title)
        historyMenuItem?.isVisible = true

        historyScrollView.isVisible = false
        mainScrollView.isVisible = true

        if (previewHistoryMeasurements == null) {
            bindEditableMeasurements()
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
