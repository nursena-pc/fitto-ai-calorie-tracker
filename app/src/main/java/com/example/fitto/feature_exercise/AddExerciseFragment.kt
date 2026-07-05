package com.example.fitto.feature_exercise

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentAddExerciseBinding
import com.example.fitto.fittoAppContainer
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.example.fitto.feature_exercise.model.ExerciseCategory
import com.example.fitto.feature_exercise.model.ExerciseDurationRange
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseSort
import com.example.fitto.feature_exercise.model.ExerciseUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

class AddExerciseFragment : Fragment(R.layout.fragment_add_exercise) {

    private var _binding: FragmentAddExerciseBinding? = null
    private val binding get() = _binding!!

    private val controller = AddExerciseController()
    private lateinit var coordinator: AddExerciseCoordinator
    private lateinit var activityAdapter: ExerciseActivityAdapter
    private var logListRenderer: ExerciseLogListRenderer? = null
    private var isApplyingCategorySelection: Boolean = false
    private var pendingSearchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddExerciseBinding.bind(view)

        coordinator = requireContext()
            .fittoAppContainer
            .createAddExerciseCoordinator()
        controller.initializeIfNeeded(coordinator::loadActivities)
        logListRenderer = ExerciseLogListRenderer(
            context = requireContext(),
            logsContainer = binding.todayExerciseLogsContainer,
            emptyText = binding.todayExerciseEmptyText,
            summaryText = binding.todayExerciseSummaryText,
            emojiProvider = ::resolveExerciseEmoji,
            onEdit = ::startEditingExerciseLog,
            onDelete = ::deleteExerciseLog
        )

        setupActivitiesRecyclerView()
        setupFilterResultListener()
        setupClicks()
        restoreControllerStateToView()
        setupSearch()

        renderCategoryChipColors()
        renderUserWeight()
        renderActivities()
        renderSelectedExercise()
        renderTodayExerciseLogs()
        loadUserWeightAndRender()
    }

    private fun restoreControllerStateToView() {
        checkCategoryChipForCategory(controller.state.selectedCategory)
        binding.exerciseSearchInput.setText(controller.state.rawSearchQuery)
    }

    private fun setupActivitiesRecyclerView() = with(binding) {
        activityAdapter = ExerciseActivityAdapter(
            onActivityClick = ::selectActivityFromList,
            titleTextProvider = ::displayActivityTitle,
            descriptionTextProvider = ::displayActivityDescription,
            durationTextProvider = { minutes ->
                getString(R.string.exercise_duration_value, minutes)
            },
            calorieTextProvider = { activity ->
                getString(
                    R.string.exercise_kcal_value,
                    controller.caloriesForListActivity(activity)
                )
            }
        )

        activitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activityAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(12)
            itemAnimator = null
        }
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        filterButton.setOnClickListener {
            ExerciseFilterBottomSheetFragment
                .newInstance(controller.state.filterState)
                .show(childFragmentManager, "exercise_filter")
        }
        categoryToggleGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isApplyingCategorySelection) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            controller.selectCategory(categoryForCheckedId(checkedId))
            renderCategoryChipColors()
            renderActivities()
            renderSelectedExercise()
            renderTodayExerciseLogs()
        }
        decreaseDurationButton.setOnClickListener {
            controller.decreaseDuration()
            renderSelectedExercise()
        }
        increaseDurationButton.setOnClickListener {
            controller.increaseDuration()
            renderSelectedExercise()
        }
        durationText.setOnClickListener {
            showDurationInputDialog()
        }
        intensityText.setOnClickListener {
            cycleIntensityAndRender()
        }
        intensityContainer.setOnClickListener {
            cycleIntensityAndRender()
        }
        addExerciseButton.setOnClickListener {
            saveSelectedExercise()
        }
    }

    private fun categoryForCheckedId(checkedId: Int): ExerciseCategory {
        return when (checkedId) {
            R.id.strengthCategoryButton -> ExerciseCategory.STRENGTH
            R.id.stretchingCategoryButton -> ExerciseCategory.STRETCHING
            R.id.lifestyleCategoryButton -> ExerciseCategory.LIFESTYLE
            else -> ExerciseCategory.CARDIO
        }
    }

    private fun cycleIntensityAndRender() {
        controller.cycleIntensity()
        renderSelectedExercise()
    }

    private fun renderUserWeight() = with(binding) {
        userWeightChipText.text = getString(
            R.string.exercise_weight_chip,
            ExerciseProfileWeightResolver.formatWeight(controller.state.userWeightKg)
        )
    }

    private fun loadUserWeightAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            controller.updateUserWeight(coordinator.loadUserWeightKg())
            renderUserWeight()
            renderSelectedExercise()
        }
    }

    private fun showDurationInputDialog() {
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            hint = getString(R.string.exercise_duration_dialog_hint)
            boxBackgroundColor = ContextCompat.getColor(context, R.color.white)
            setPadding(0, 8.dp, 0, 0)
        }
        val editText = TextInputEditText(context).apply {
            setText(controller.state.durationMinutes.toString())
            setSelectAllOnFocus(true)
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
        }
        inputLayout.addView(editText)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.exercise_duration_dialog_title))
            .setMessage(getString(R.string.exercise_duration_dialog_message))
            .setView(inputLayout)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.common_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.white)
            editText.requestFocus()
            editText.postDelayed({
                val inputMethodManager = context.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }, 180L)

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!controller.setDuration(editText.text?.toString().orEmpty())) {
                    inputLayout.error = getString(R.string.exercise_duration_invalid)
                    return@setOnClickListener
                }

                inputLayout.error = null
                renderSelectedExercise()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun setupFilterResultListener() {
        childFragmentManager.setFragmentResultListener(
            ExerciseFilterBottomSheetFragment.REQUEST_EXERCISE_FILTER,
            viewLifecycleOwner
        ) { _, bundle ->
            val durationRange = bundle
                .getString(ExerciseFilterBottomSheetFragment.BUNDLE_DURATION_RANGE)
                ?.let { runCatching { ExerciseDurationRange.valueOf(it) }.getOrNull() }
                ?: ExerciseDurationRange.ALL
            val intensity = bundle
                .getString(ExerciseFilterBottomSheetFragment.BUNDLE_INTENSITY)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { ExerciseIntensity.valueOf(it) }.getOrNull() }
            val sort = bundle
                .getString(ExerciseFilterBottomSheetFragment.BUNDLE_SORT)
                ?.let { runCatching { ExerciseSort.valueOf(it) }.getOrNull() }
                ?: ExerciseSort.RECOMMENDED

            syncSearchQueryFromView()
            controller.applyFilter(
                ExerciseFilterState(
                    durationRange = durationRange,
                    intensity = intensity,
                    sort = sort
                )
            )
            renderActivities()
            renderSelectedExercise()
        }
    }

    private fun renderCategoryChipColors() = with(binding) {
        val selectedBackground = ContextCompat.getColor(
            requireContext(),
            R.color.fitto_primary_soft
        )
        val normalBackground = ContextCompat.getColor(requireContext(), R.color.fitto_surface)
        val selectedStroke = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val normalStroke = ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft)
        val selectedCategory = controller.state.selectedCategory

        listOf(
            cardioCategoryButton to ExerciseCategory.CARDIO,
            strengthCategoryButton to ExerciseCategory.STRENGTH,
            stretchingCategoryButton to ExerciseCategory.STRETCHING,
            lifestyleCategoryButton to ExerciseCategory.LIFESTYLE
        ).forEach { (chip, category) ->
            val isSelected = category == selectedCategory
            chip.chipBackgroundColor = ColorStateList.valueOf(
                if (isSelected) selectedBackground else normalBackground
            )
            chip.chipStrokeColor = ColorStateList.valueOf(
                if (isSelected) selectedStroke else normalStroke
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.fitto_green else R.color.fitto_text_dark
                )
            )
        }
    }

    private fun setupSearch() {
        binding.exerciseSearchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    scheduleRenderActivities()
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )
    }

    private fun scheduleRenderActivities() {
        pendingSearchJob?.cancel()
        pendingSearchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            renderActivities()
        }
    }

    private fun syncSearchQueryFromView() {
        controller.updateSearchQuery(
            binding.exerciseSearchInput.text?.toString().orEmpty()
        )
    }

    private fun renderActivities() = with(binding) {
        syncSearchQueryFromView()
        val activities = controller.visibleActivities()
        exerciseEmptyText.isVisible = activities.isEmpty()
        activitiesRecyclerView.isVisible = activities.isNotEmpty()
        activityAdapter.submitActivities(
            activities = activities,
            selectedId = controller.state.selectedActivity?.id
        )
    }

    private fun selectActivityFromList(activity: ExerciseUiModel) {
        controller.selectActivity(activity)
        checkCategoryChipForCategory(activity.category)
        renderCategoryChipColors()
        renderSelectedExercise()
        activityAdapter.updateSelectedActivity(activity.id)
    }

    private fun checkCategoryChipForCategory(category: ExerciseCategory) {
        isApplyingCategorySelection = true
        binding.categoryToggleGroup.check(
            when (category) {
                ExerciseCategory.CARDIO -> R.id.cardioCategoryButton
                ExerciseCategory.STRENGTH -> R.id.strengthCategoryButton
                ExerciseCategory.STRETCHING -> R.id.stretchingCategoryButton
                ExerciseCategory.LIFESTYLE -> R.id.lifestyleCategoryButton
            }
        )
        isApplyingCategorySelection = false
    }

    private fun renderSelectedExercise() = with(binding) {
        val state = controller.state
        val activity = state.selectedActivity
        selectedExerciseCard.isVisible = activity != null
        addExerciseButton.isEnabled = activity != null
        if (activity == null) return@with

        selectedExerciseIconText.text = activity.iconEmoji
        selectedExerciseTitleText.text = displayActivityTitle(activity)
        selectedExerciseDescText.text = displayActivityDescription(activity)
        durationText.text = getString(
            R.string.exercise_duration_value,
            state.durationMinutes
        )
        intensityText.text = getString(state.selectedIntensity.titleRes)
        estimatedKcalText.text = getString(
            R.string.exercise_kcal_value,
            controller.selectedCalories()
        )
        addExerciseButton.text = getString(
            if (state.isEditing) {
                R.string.exercise_update_button
            } else {
                R.string.exercise_add_button
            }
        )
    }

    private fun renderTodayExerciseLogs() {
        logListRenderer?.render(coordinator.loadLogs(currentEntryDate()))
    }

    private fun startEditingExerciseLog(log: ExerciseLogLocalModel) {
        controller.startEditing(
            log = log,
            fallbackTitle = getString(R.string.exercise_add_title),
            fallbackCategoryName = getString(R.string.exercise_category_cardio),
            estimatedCaloriesText = getString(R.string.exercise_estimated_kcal),
            intensityTitleProvider = { intensity -> getString(intensity.titleRes) }
        )

        if (!binding.exerciseSearchInput.text.isNullOrBlank()) {
            binding.exerciseSearchInput.setText("")
        }
        checkCategoryChipForCategory(controller.state.selectedCategory)
        renderCategoryChipColors()
        renderActivities()
        renderSelectedExercise()

        binding.addExerciseScrollView.post {
            binding.addExerciseScrollView.smoothScrollTo(
                0,
                (binding.selectedExerciseCard.top - 16.dp).coerceAtLeast(0)
            )
        }
        Toast.makeText(
            requireContext(),
            getString(R.string.exercise_update_started),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun deleteExerciseLog(logId: String) {
        coordinator.deleteLog(logId)
        Toast.makeText(
            requireContext(),
            getString(R.string.exercise_deleted_message),
            Toast.LENGTH_SHORT
        ).show()
        parentFragmentManager.setFragmentResult(
            REQUEST_EXERCISE_ADDED,
            bundleOf(BUNDLE_EXERCISE_DATE to currentEntryDate())
        )
        renderTodayExerciseLogs()
    }

    private fun currentEntryDate(): String {
        return requireArguments()
            .getString(ARG_ENTRY_DATE)
            ?.takeIf { it.isNotBlank() }
            ?: LocalDate.now().toString()
    }

    private fun displayActivityTitle(activity: ExerciseUiModel): String {
        return ExerciseDisplayTextResolver.resolveTitle(activity)
    }

    private fun displayActivityDescription(activity: ExerciseUiModel): String {
        return ExerciseDisplayTextResolver.resolveDescription(
            activity = activity,
            categoryName = getString(activity.category.titleRes)
        )
    }

    private fun resolveExerciseEmoji(exerciseId: String): String {
        return ExerciseLegacyActivityResolver.resolveEmoji(
            exerciseId = exerciseId,
            activities = controller.state.activities
        )
    }

    private fun saveSelectedExercise() {
        val state = controller.state
        val activity = state.selectedActivity ?: return
        val activityTitle = displayActivityTitle(activity)
        val calories = controller.selectedCalories()
        val entryDate = currentEntryDate()
        val result = coordinator.saveLog(
            ExerciseLogSaveRequest(
                editingLogId = state.editingLogId,
                entryDate = entryDate,
                exerciseId = activity.id,
                exerciseTitle = activityTitle,
                categoryName = getString(activity.category.titleRes),
                durationMinutes = state.durationMinutes,
                intensityName = getString(state.selectedIntensity.titleRes),
                metValue = activity.metValue,
                calories = calories
            )
        )
        controller.completeSave()

        val message = when (result) {
            is ExerciseLogSaveResult.Added -> getString(
                R.string.exercise_added_message,
                activityTitle,
                calories
            )
            ExerciseLogSaveResult.Updated -> getString(R.string.exercise_updated_message)
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        parentFragmentManager.setFragmentResult(
            REQUEST_EXERCISE_ADDED,
            bundleOf(
                BUNDLE_EXERCISE_ID to activity.id,
                BUNDLE_EXERCISE_TITLE to activityTitle,
                BUNDLE_EXERCISE_CALORIES to calories,
                BUNDLE_EXERCISE_DURATION_MINUTES to state.durationMinutes,
                BUNDLE_EXERCISE_DATE to entryDate
            )
        )
        renderTodayExerciseLogs()
        renderActivities()
        renderSelectedExercise()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()

    override fun onDestroyView() {
        pendingSearchJob?.cancel()
        pendingSearchJob = null
        binding.activitiesRecyclerView.adapter = null
        logListRenderer = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_EXERCISE_ADDED = "request_exercise_added"
        const val BUNDLE_EXERCISE_ID = "bundle_exercise_id"
        const val BUNDLE_EXERCISE_TITLE = "bundle_exercise_title"
        const val BUNDLE_EXERCISE_CALORIES = "bundle_exercise_calories"
        const val BUNDLE_EXERCISE_DURATION_MINUTES = "bundle_exercise_duration_minutes"
        const val BUNDLE_EXERCISE_DATE = "bundle_exercise_date"
        private const val ARG_ENTRY_DATE = "arg_entry_date"
        private const val SEARCH_DEBOUNCE_MILLIS = 180L

        fun newInstance(entryDate: String): AddExerciseFragment {
            return AddExerciseFragment().apply {
                arguments = bundleOf(ARG_ENTRY_DATE to entryDate)
            }
        }
    }
}
