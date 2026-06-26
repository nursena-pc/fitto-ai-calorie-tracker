    package com.example.fitto.feature_exercise

    import android.content.res.ColorStateList
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.view.View
    import android.widget.LinearLayout
    import android.widget.TextView
    import android.widget.Toast
    import androidx.core.content.ContextCompat
    import androidx.core.os.bundleOf
    import androidx.core.view.isVisible
    import androidx.fragment.app.Fragment
    import com.example.fitto.R
    import com.example.fitto.databinding.FragmentAddExerciseBinding
    import com.example.fitto.feature_exercise.data.ExerciseCatalog
    import com.example.fitto.feature_exercise.model.ExerciseCategory
    import com.example.fitto.feature_exercise.model.ExerciseIntensity
    import com.example.fitto.feature_exercise.model.ExerciseUiModel
    import com.google.android.material.card.MaterialCardView
    import kotlin.math.roundToInt
    import com.example.fitto.feature_exercise.data.ExerciseLogStore
    import java.time.LocalDate
    import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
    import android.widget.ImageButton
    import com.example.fitto.feature_exercise.model.ExerciseDurationRange
    import com.example.fitto.feature_exercise.model.ExerciseFilterState
    import com.example.fitto.feature_exercise.model.ExerciseSort
    import android.text.InputType
    import android.view.inputmethod.InputMethodManager
    import android.content.Context
    import androidx.lifecycle.lifecycleScope
    import com.example.fitto.data.local.repository.ProfileCacheRepository
    import com.google.android.material.dialog.MaterialAlertDialogBuilder
    import com.google.android.material.textfield.TextInputEditText
    import com.google.android.material.textfield.TextInputLayout
    import android.os.Handler
    import android.os.Looper
    import androidx.recyclerview.widget.LinearLayoutManager
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    class AddExerciseFragment : Fragment(R.layout.fragment_add_exercise) {

        private var _binding: FragmentAddExerciseBinding? = null
        private val binding get() = _binding!!
        private var filterState: ExerciseFilterState = ExerciseFilterState()
        private var selectedCategory: ExerciseCategory = ExerciseCategory.CARDIO
        private var selectedActivity: ExerciseUiModel? = null
        private var selectedIntensity: ExerciseIntensity = ExerciseIntensity.MODERATE
        private var durationMinutes: Int = 30
        private lateinit var exerciseLogStore: ExerciseLogStore
        private lateinit var profileCacheRepository: ProfileCacheRepository
        private var userWeightKg: Double = DEFAULT_USER_WEIGHT_KG
        private var editingLogId: String? = null
        private var isApplyingEditSelection: Boolean = false
        private var allActivities: List<ExerciseUiModel> = emptyList()
        private val searchHandler = Handler(Looper.getMainLooper())
        private var pendingSearchRunnable: Runnable? = null
        private var searchableTextById: Map<String, String> = emptyMap()
        private lateinit var activityAdapter: ExerciseActivityAdapter


        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            _binding = FragmentAddExerciseBinding.bind(view)
            exerciseLogStore = ExerciseLogStore(requireContext())
            profileCacheRepository = ProfileCacheRepository(requireContext().applicationContext)
            allActivities = ExerciseCatalog.load(
                context = requireContext(),
                forceReload = true
            )

            rebuildSearchCache()

            selectedActivity = allActivities
                .firstOrNull { it.category == selectedCategory }

            durationMinutes = selectedActivity?.defaultDurationMinutes ?: 30

            setupActivitiesRecyclerView()
            setupFilterResultListener()
            setupClicks()
            setupSearch()
            binding.categoryToggleGroup.check(R.id.cardioCategoryButton)

            renderCategoryChipColors()
            renderUserWeight()
            renderActivities()
            renderSelectedExercise()
            renderTodayExerciseLogs()
            loadUserWeightAndRender()
        }

        private fun setupActivitiesRecyclerView() = with(binding) {
            activityAdapter = ExerciseActivityAdapter(
                onActivityClick = { activity ->
                    selectActivityFromList(activity)
                },
                titleTextProvider = { activity ->
                    displayActivityTitle(activity)
                },
                descriptionTextProvider = { activity ->
                    displayActivityDescription(activity)
                },
                durationTextProvider = { minutes ->
                    getString(R.string.exercise_duration_value, minutes)
                },
                calorieTextProvider = { activity ->
                    getString(
                        R.string.exercise_kcal_value,
                        ExerciseCalorieCalculator.calculate(
                            activity = activity,
                            minutes = activity.defaultDurationMinutes,
                            intensity = filterState.intensity ?: selectedIntensity,
                            userWeightKg = userWeightKg
                        )
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
                    .newInstance(filterState)
                    .show(childFragmentManager, "exercise_filter")
            }

            categoryToggleGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                if (isApplyingEditSelection) return@setOnCheckedStateChangeListener
                val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

                selectedCategory = when (checkedId) {
                    R.id.strengthCategoryButton -> ExerciseCategory.STRENGTH
                    R.id.stretchingCategoryButton -> ExerciseCategory.STRETCHING
                    R.id.lifestyleCategoryButton -> ExerciseCategory.LIFESTYLE
                    else -> ExerciseCategory.CARDIO
                }

                val categoryActivities = allActivities
                    .filter { it.category == selectedCategory }

                selectedActivity = categoryActivities.firstOrNull()
                durationMinutes = selectedActivity?.defaultDurationMinutes ?: 30
                selectedIntensity = ExerciseIntensity.MODERATE

                renderCategoryChipColors()
                renderActivities()
                renderSelectedExercise()
                renderTodayExerciseLogs()
            }

            decreaseDurationButton.setOnClickListener {
                durationMinutes = (durationMinutes - 5).coerceAtLeast(5)
                renderSelectedExercise()
            }

            increaseDurationButton.setOnClickListener {
                durationMinutes = (durationMinutes + 5).coerceAtMost(240)
                renderSelectedExercise()
            }
            durationText.setOnClickListener {
                showDurationInputDialog()
            }

            intensityText.setOnClickListener {
                selectedIntensity = selectedIntensity.next()
                renderSelectedExercise()
            }
            intensityContainer.setOnClickListener {
                selectedIntensity = selectedIntensity.next()
                renderSelectedExercise()
            }

            addExerciseButton.setOnClickListener {
                addSelectedExercise()
            }
        }
        private fun renderUserWeight() = with(binding) {
            userWeightChipText.text = getString(
                R.string.exercise_weight_chip,
                ExerciseProfileWeightResolver.formatWeight(userWeightKg)
            )
        }
        private fun loadUserWeightAndRender() {
            viewLifecycleOwner.lifecycleScope.launch {
                val profile = withContext(Dispatchers.IO) {
                    profileCacheRepository.getCurrentUserProfile()
                        ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
                }

                userWeightKg = ExerciseProfileWeightResolver.resolveWeightKg(profile)
                    ?: DEFAULT_USER_WEIGHT_KG

                renderUserWeight()
                renderSelectedExercise()
            }
        }
        private fun rebuildSearchCache() {
            searchableTextById = ExerciseActivitySearchCache.build(allActivities)
        }
        private fun showDurationInputDialog() {
            val context = requireContext()

            val inputLayout = TextInputLayout(context).apply {
                hint = getString(R.string.exercise_duration_dialog_hint)
                boxBackgroundColor = ContextCompat.getColor(context, R.color.white)
                setPadding(0, 8.dp, 0, 0)
            }

            val editText = TextInputEditText(context).apply {
                setText(durationMinutes.toString())
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
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }, 180L)

                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = editText.text
                        ?.toString()
                        ?.trim()
                        ?.toIntOrNull()

                    if (value == null || value !in 1..240) {
                        inputLayout.error = getString(R.string.exercise_duration_invalid)
                        return@setOnClickListener
                    }

                    inputLayout.error = null
                    durationMinutes = value
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

                filterState = ExerciseFilterState(
                    durationRange = durationRange,
                    intensity = intensity,
                    sort = sort
                )

                filterState.intensity?.let {
                    selectedIntensity = it
                }

                val visibleActivities = filteredActivities()
                if (visibleActivities.none { it.id == selectedActivity?.id }) {
                    selectedActivity = visibleActivities.firstOrNull()
                    durationMinutes = selectedActivity?.defaultDurationMinutes ?: 30
                }

                renderActivities()
                renderSelectedExercise()
            }
        }

        private fun filteredActivities(): List<ExerciseUiModel> {
            val rawQuery = binding.exerciseSearchInput.text
                ?.toString()
                ?.trim()
                .orEmpty()

            return ExerciseActivityFilter.resolveVisibleActivities(
                activities = allActivities,
                searchableTextById = searchableTextById,
                selectedCategory = selectedCategory,
                filterState = filterState,
                selectedIntensity = selectedIntensity,
                rawQuery = rawQuery,
                userWeightKg = userWeightKg
            )
        }
        private fun resolveEditableActivityForLog(log: ExerciseLogLocalModel): ExerciseUiModel {
            return ExerciseLegacyActivityResolver.resolveActivityForLog(
                log = log,
                activities = allActivities,
                fallbackTitle = getString(R.string.exercise_add_title),
                fallbackCategoryName = getString(R.string.exercise_category_cardio),
                estimatedCaloriesText = getString(R.string.exercise_estimated_kcal)
            )
        }
        private fun renderCategoryChipColors() = with(binding) {
            val selectedBackground = ContextCompat.getColor(requireContext(), R.color.fitto_primary_soft)
            val normalBackground = ContextCompat.getColor(requireContext(), R.color.fitto_surface)
            val selectedStroke = ContextCompat.getColor(requireContext(), R.color.fitto_green)
            val normalStroke = ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft)

            val chips = listOf(
                cardioCategoryButton to ExerciseCategory.CARDIO,
                strengthCategoryButton to ExerciseCategory.STRENGTH,
                stretchingCategoryButton to ExerciseCategory.STRETCHING,
                lifestyleCategoryButton to ExerciseCategory.LIFESTYLE
            )

            chips.forEach { (chip, category) ->
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
            pendingSearchRunnable?.let { searchHandler.removeCallbacks(it) }

            pendingSearchRunnable = Runnable {
                renderActivities()
            }

            searchHandler.postDelayed(pendingSearchRunnable!!, 180L)
        }

        private fun renderActivities() = with(binding) {
            val activities = filteredActivities()

            exerciseEmptyText.isVisible = activities.isEmpty()
            activitiesRecyclerView.isVisible = activities.isNotEmpty()

            activityAdapter.submitActivities(
                activities = activities,
                selectedId = selectedActivity?.id
            )
        }
        private fun selectActivityFromList(activity: ExerciseUiModel) {
            editingLogId = null
            selectedCategory = activity.category
            selectedActivity = activity
            durationMinutes = activity.defaultDurationMinutes
            selectedIntensity = filterState.intensity ?: ExerciseIntensity.MODERATE

            checkCategoryChipForCategory(activity.category)
            renderCategoryChipColors()
            renderSelectedExercise()

            if (::activityAdapter.isInitialized) {
                activityAdapter.updateSelectedActivity(activity.id)
            }
        }
        private fun checkCategoryChipForCategory(category: ExerciseCategory) {
            isApplyingEditSelection = true

            binding.categoryToggleGroup.check(
                when (category) {
                    ExerciseCategory.CARDIO -> R.id.cardioCategoryButton
                    ExerciseCategory.STRENGTH -> R.id.strengthCategoryButton
                    ExerciseCategory.STRETCHING -> R.id.stretchingCategoryButton
                    ExerciseCategory.LIFESTYLE -> R.id.lifestyleCategoryButton
                }
            )

            isApplyingEditSelection = false
        }

        private fun renderSelectedExercise() = with(binding) {
            val activity = selectedActivity

            selectedExerciseCard.isVisible = activity != null
            addExerciseButton.isEnabled = activity != null

            if (activity == null) return@with

            selectedExerciseIconText.text = activity.iconEmoji
            selectedExerciseTitleText.text = displayActivityTitle(activity)
            selectedExerciseDescText.text = displayActivityDescription(activity)

            durationText.text = getString(R.string.exercise_duration_value, durationMinutes)
            intensityText.text = getString(selectedIntensity.titleRes)

            estimatedKcalText.text = getString(
                R.string.exercise_kcal_value,
                calculateCalories(activity, durationMinutes)
            )
            addExerciseButton.text = if (editingLogId == null) {
                getString(R.string.exercise_add_button)
            } else {
                getString(R.string.exercise_update_button)
            }
        }

        private fun calculateCalories(
            activity: ExerciseUiModel,
            minutes: Int
        ): Int {
            return ExerciseCalorieCalculator.calculate(
                activity = activity,
                minutes = minutes,
                intensity = selectedIntensity,
                userWeightKg = userWeightKg
            )
        }
        private fun renderTodayExerciseLogs() = with(binding) {
            val entryDate = currentEntryDate()
            val logs = exerciseLogStore.getLogsForDate(entryDate)
            val summary = exerciseLogStore.getSummaryForDate(entryDate)

            todayExerciseLogsContainer.removeAllViews()

            todayExerciseEmptyText.isVisible = logs.isEmpty()
            todayExerciseSummaryText.isVisible = logs.isNotEmpty()

            todayExerciseSummaryText.text = getString(
                R.string.exercise_today_summary,
                summary.logCount,
                summary.totalDurationMinutes,
                summary.totalCalories
            )

            logs.forEach { log ->
                todayExerciseLogsContainer.addView(createExerciseLogRow(log))
            }
        }

        private fun createExerciseLogRow(
            log: ExerciseLogLocalModel
        ): MaterialCardView {
            val context = requireContext()

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
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(12.dp, 10.dp, 8.dp, 10.dp)

                        addView(
                            TextView(context).apply {
                                layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp)
                                gravity = android.view.Gravity.CENTER
                                text = resolveExerciseEmoji(log.exerciseId)
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
                                        setTextColor(ContextCompat.getColor(context, R.color.fitto_text_dark))
                                        textSize = 14f
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        maxLines = 1
                                        ellipsize = android.text.TextUtils.TruncateAt.END
                                    }
                                )

                                addView(
                                    TextView(context).apply {
                                        text = getString(
                                            R.string.exercise_log_detail,
                                            log.durationMinutes,
                                            log.intensityName,
                                            log.calories
                                        )
                                        setTextColor(ContextCompat.getColor(context, R.color.fitto_text_secondary))
                                        textSize = 12f
                                        maxLines = 1
                                        ellipsize = android.text.TextUtils.TruncateAt.END
                                    }
                                )
                            }
                        )

                        val editButton = ImageButton(context).apply {
                            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp).apply {
                                marginEnd = 6.dp
                            }
                            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
                            contentDescription = getString(R.string.exercise_edit_log)
                            setImageResource(R.drawable.ic_edit)
                            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
                            imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.fitto_green)
                            )

                            setOnClickListener {
                                startEditingExerciseLog(log)
                            }
                        }

                        val deleteButton = ImageButton(context).apply {
                            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp)
                            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
                            contentDescription = getString(R.string.exercise_delete_log)
                            setImageResource(R.drawable.ic_delete)
                            setPadding(10.dp, 10.dp, 10.dp, 10.dp)
                            imageTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.fitto_orange)
                            )

                            setOnClickListener {
                                deleteExerciseLog(log.id)
                            }
                        }

                        addView(editButton)
                        addView(deleteButton)
                    }
                )
            }
        }
        private fun startEditingExerciseLog(log: ExerciseLogLocalModel) {
            val activity = resolveEditableActivityForLog(log)

            editingLogId = log.id
            selectedCategory = activity.category
            selectedActivity = activity
            durationMinutes = log.durationMinutes.coerceIn(1, 240)
            selectedIntensity = resolveIntensityByName(log.intensityName)

            // Keep search/filter state from hiding the activity being edited.
            filterState = ExerciseFilterState()
            if (!binding.exerciseSearchInput.text.isNullOrBlank()) {
                binding.exerciseSearchInput.setText("")
            }

            checkCategoryChipForCategory(selectedCategory)

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
        private fun resolveIntensityByName(intensityName: String): ExerciseIntensity {
            return ExerciseIntensity.entries.firstOrNull {
                getString(it.titleRes) == intensityName
            } ?: ExerciseIntensity.MODERATE
        }

        private fun deleteExerciseLog(logId: String) {
            exerciseLogStore.deleteLog(logId)

            Toast.makeText(
                requireContext(),
                getString(R.string.exercise_deleted_message),
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.setFragmentResult(
                REQUEST_EXERCISE_ADDED,
                bundleOf(
                    BUNDLE_EXERCISE_DATE to currentEntryDate()
                )
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
                activities = allActivities
            )
        }

        private fun addSelectedExercise() {
            val activity = selectedActivity ?: return
            val calories = calculateCalories(activity, durationMinutes)
            val entryDate = currentEntryDate()

            val activityTitle = displayActivityTitle(activity)
            val categoryName = getString(activity.category.titleRes)
            val intensityName = getString(selectedIntensity.titleRes)

            val currentEditingId = editingLogId

            if (currentEditingId == null) {
                exerciseLogStore.addLog(
                    entryDate = entryDate,
                    exerciseId = activity.id,
                    exerciseTitle = activityTitle,
                    categoryName = categoryName,
                    durationMinutes = durationMinutes,
                    intensityName = intensityName,
                    metValue = activity.metValue,
                    calories = calories
                )

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.exercise_added_message,
                        activityTitle,
                        calories
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                exerciseLogStore.updateLog(
                    logId = currentEditingId,
                    exerciseId = activity.id,
                    exerciseTitle = activityTitle,
                    categoryName = categoryName,
                    durationMinutes = durationMinutes,
                    intensityName = intensityName,
                    metValue = activity.metValue,
                    calories = calories
                )

                editingLogId = null

                Toast.makeText(
                    requireContext(),
                    getString(R.string.exercise_updated_message),
                    Toast.LENGTH_SHORT
                ).show()
            }

            parentFragmentManager.setFragmentResult(
                REQUEST_EXERCISE_ADDED,
                bundleOf(
                    BUNDLE_EXERCISE_ID to activity.id,
                    BUNDLE_EXERCISE_TITLE to activityTitle,
                    BUNDLE_EXERCISE_CALORIES to calories,
                    BUNDLE_EXERCISE_DURATION_MINUTES to durationMinutes,
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
            pendingSearchRunnable?.let { searchHandler.removeCallbacks(it) }
            pendingSearchRunnable = null

            binding.activitiesRecyclerView.adapter = null

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
            private const val DEFAULT_USER_WEIGHT_KG = 70.0
            private const val ARG_ENTRY_DATE = "arg_entry_date"

            fun newInstance(entryDate: String): AddExerciseFragment {
                return AddExerciseFragment().apply {
                    arguments = bundleOf(ARG_ENTRY_DATE to entryDate)
                }
            }
        }
    }
