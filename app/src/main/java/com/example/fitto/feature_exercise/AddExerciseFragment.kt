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
    import com.google.android.material.button.MaterialButton
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
    import com.example.fitto.data.local.entity.UserProfileEntity
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
    import java.text.Normalizer
    import java.util.Locale

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
                        calculateCaloriesFor(
                            activity = activity,
                            minutes = activity.defaultDurationMinutes,
                            intensity = filterState.intensity ?: selectedIntensity
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
                formatWeight(userWeightKg)
            )
        }
        private fun loadUserWeightAndRender() {
            viewLifecycleOwner.lifecycleScope.launch {
                val profile = withContext(Dispatchers.IO) {
                    profileCacheRepository.getCurrentUserProfile()
                        ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
                }

                userWeightKg = resolveProfileWeightKg(profile)
                    ?: DEFAULT_USER_WEIGHT_KG

                renderUserWeight()
                renderSelectedExercise()
            }
        }

        private fun resolveProfileWeightKg(profile: UserProfileEntity?): Double? {
            if (profile == null) return null

            val candidateNames = listOf(
                "weightKg",
                "currentWeightKg",
                "weight",
                "currentWeight",
                "initialWeightKg",
                "initialWeight"
            )

            candidateNames.forEach { name ->
                profile.readNumberProperty(name)
                    ?.takeIf { it in MIN_USER_WEIGHT_KG..MAX_USER_WEIGHT_KG }
                    ?.let { return it }
            }

            return null
        }
        private fun rebuildSearchCache() {
            searchableTextById = allActivities.associate { activity ->
                activity.id to listOf(
                    activity.id,
                    activity.title,
                    activity.description,
                    activity.searchKeywords.joinToString(" "),
                    activity.sourceDescription,
                    activity.sourceMajorHeading
                )
                    .joinToString(" ")
                    .normalizedForExerciseSearch()
            }
        }
        private fun Any.readNumberProperty(name: String): Double? {
            val fieldValue = runCatching {
                javaClass.getDeclaredField(name).apply {
                    isAccessible = true
                }.get(this)
            }.getOrNull()

            fieldValue.asDoubleOrNull()?.let { return it }

            val getterName = "get" + name.replaceFirstChar { it.uppercaseChar() }

            val getterValue = runCatching {
                javaClass.methods
                    .firstOrNull { method ->
                        method.name == getterName && method.parameterCount == 0
                    }
                    ?.invoke(this)
            }.getOrNull()

            return getterValue.asDoubleOrNull()
        }

        private fun Any?.asDoubleOrNull(): Double? {
            return when (this) {
                is Number -> toDouble()
                is String -> replace(",", ".").toDoubleOrNull()
                else -> null
            }
        }
        private fun formatWeight(value: Double): String {
            return if (value % 1.0 == 0.0) {
                value.roundToInt().toString()
            } else {
                String.format(Locale.getDefault(), "%.1f", value)
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

            val normalizedQuery = rawQuery.normalizedForExerciseSearch()
            val queryWords = buildExerciseSearchQueries(normalizedQuery)

            val isSearching = queryWords.isNotEmpty()
            val listIntensity = filterState.intensity ?: selectedIntensity

            val filtered = allActivities.filter { activity ->
                val searchableText = searchableTextById[activity.id].orEmpty()

                val categoryMatches = if (isSearching) {
                    true
                } else {
                    activity.category == selectedCategory
                }

                val durationMatches = if (isSearching) {
                    true
                } else {
                    filterState.durationRange.matches(activity.defaultDurationMinutes)
                }

                val searchMatches = if (!isSearching) {
                    true
                } else {
                    queryWords.any { query ->
                        searchableText.contains(query)
                    }
                }

                categoryMatches && durationMatches && searchMatches
            }

            return when (filterState.sort) {
                ExerciseSort.RECOMMENDED -> filtered

                ExerciseSort.KCAL_LOW_HIGH -> filtered.sortedBy {
                    calculateCaloriesFor(
                        activity = it,
                        minutes = it.defaultDurationMinutes,
                        intensity = listIntensity
                    )
                }

                ExerciseSort.KCAL_HIGH_LOW -> filtered.sortedByDescending {
                    calculateCaloriesFor(
                        activity = it,
                        minutes = it.defaultDurationMinutes,
                        intensity = listIntensity
                    )
                }

                ExerciseSort.DURATION_LOW_HIGH -> filtered.sortedBy {
                    it.defaultDurationMinutes
                }
            }
        }
        private fun buildExerciseSearchQueries(normalizedQuery: String): Set<String> {
            if (normalizedQuery.isBlank()) return emptySet()

            val queries = linkedSetOf(normalizedQuery)

            val aliasMap = mapOf(
                "kosu" to listOf("running", "run", "jogging", "jog"),
                "kosmak" to listOf("running", "run", "jogging", "jog"),
                "jogging" to listOf("kosu", "running", "run", "jog"),
                "running" to listOf("kosu", "run", "jogging", "jog"),
                "run" to listOf("kosu", "running", "jogging", "jog"),

                "yuruyus" to listOf("walking", "walk", "brisk", "steps"),
                "yuru" to listOf("walking", "walk", "brisk", "steps"),
                "walk" to listOf("yuruyus", "walking", "brisk", "steps"),
                "walking" to listOf("yuruyus", "walk", "brisk", "steps"),

                "bisiklet" to listOf("bicycling", "cycling", "cycle", "bike"),
                "bike" to listOf("bisiklet", "bicycling", "cycling"),
                "cycling" to listOf("bisiklet", "bicycling", "bike"),
                "bicycling" to listOf("bisiklet", "cycling", "bike"),

                "yuzme" to listOf("swimming", "swim", "water"),
                "swim" to listOf("yuzme", "swimming", "water"),
                "swimming" to listOf("yuzme", "swim", "water"),

                "dans" to listOf("dance", "dancing"),
                "dance" to listOf("dans", "dancing"),
                "dancing" to listOf("dans", "dance"),

                "yoga" to listOf("yoga"),
                "pilates" to listOf("pilates"),

                "esneme" to listOf("stretch", "stretching", "mobility"),
                "stretch" to listOf("esneme", "stretching", "mobility"),
                "stretching" to listOf("esneme", "stretch", "mobility"),
                "mobilite" to listOf("mobility", "stretching", "esneme"),
                "mobility" to listOf("mobilite", "stretching", "esneme"),

                "temizlik" to listOf("cleaning", "housework", "home"),
                "cleaning" to listOf("temizlik", "housework", "home"),
                "ev isi" to listOf("housework", "cleaning", "home"),
                "housework" to listOf("ev isi", "temizlik", "cleaning"),

                "market" to listOf("shopping", "grocery", "walking"),
                "alisveris" to listOf("shopping", "grocery"),
                "shopping" to listOf("market", "alisveris", "grocery"),

                "bahce" to listOf("gardening", "garden", "yard"),
                "gardening" to listOf("bahce", "garden", "yard"),

                "merdiven" to listOf("stairs", "stair", "climbing"),
                "stairs" to listOf("merdiven", "stair", "climbing"),

                "agirlik" to listOf("weight", "weights", "strength", "resistance"),
                "kuvvet" to listOf("strength", "weight", "resistance"),
                "strength" to listOf("kuvvet", "agirlik", "weight"),
                "weight" to listOf("agirlik", "kuvvet", "strength"),

                "karin" to listOf("core", "abs", "abdominal"),
                "core" to listOf("karin", "abs", "abdominal"),
                "abs" to listOf("karin", "core", "abdominal"),

                "ip atlama" to listOf("jump rope", "rope", "jumping"),
                "ip" to listOf("jump rope", "rope", "jumping"),
                "rope" to listOf("ip", "jump rope", "jumping")
            )

            aliasMap.forEach { (key, aliases) ->
                if (normalizedQuery.contains(key)) {
                    queries.add(key)
                    queries.addAll(aliases.map { it.normalizedForExerciseSearch() })
                }
            }

            // Kullanıcı iki kelime yazarsa her kelime için de alias denensin.
            normalizedQuery.split(" ")
                .filter { it.length >= 2 }
                .forEach { word ->
                    queries.add(word)
                    aliasMap[word]?.forEach { alias ->
                        queries.add(alias.normalizedForExerciseSearch())
                    }
                }

            return queries.filter { it.isNotBlank() }.toSet()
        }
        private fun resolveEditableActivityForLog(log: ExerciseLogLocalModel): ExerciseUiModel {
            allActivities.firstOrNull { it.id == log.exerciseId }?.let { return it }

            val normalizedLogTitle = log.exerciseTitle.normalizedForExerciseSearch()

            allActivities.firstOrNull { activity ->
                activity.title.normalizedForExerciseSearch() == normalizedLogTitle
            }?.let { return it }

            allActivities.firstOrNull { activity ->
                activity.title.normalizedForExerciseSearch().contains(normalizedLogTitle) ||
                        normalizedLogTitle.contains(activity.title.normalizedForExerciseSearch())
            }?.let { return it }

            return createLegacyActivityFromLog(log)
        }

        private fun createLegacyActivityFromLog(log: ExerciseLogLocalModel): ExerciseUiModel {
            return ExerciseUiModel(
                id = log.exerciseId.ifBlank { "legacy_${System.currentTimeMillis()}" },
                title = log.exerciseTitle.ifBlank { getString(R.string.exercise_add_title) },
                description = buildString {
                    append(log.categoryName.ifBlank { getString(R.string.exercise_category_cardio) })
                    append(" · ")
                    append(getString(R.string.exercise_estimated_kcal))
                },
                category = resolveLegacyCategory(log),
                metValue = log.metValue.takeIf { it > 0.0 } ?: 4.3,
                defaultDurationMinutes = log.durationMinutes.coerceIn(1, 240),
                iconEmoji = resolveLegacyEmoji(log.exerciseId),
                searchKeywords = listOf(
                    log.exerciseTitle,
                    log.categoryName,
                    log.exerciseId
                ),
                sourceCode = "",
                sourceMajorHeading = "Legacy",
                sourceDescription = log.exerciseTitle
            )
        }

        private fun resolveLegacyCategory(log: ExerciseLogLocalModel): ExerciseCategory {
            val raw = "${log.categoryName} ${log.exerciseId} ${log.exerciseTitle}"
                .normalizedForExerciseSearch()

            return when {
                raw.contains("kuvvet") ||
                        raw.contains("strength") ||
                        raw.contains("weight") ||
                        raw.contains("bodyweight") ||
                        raw.contains("calisthenics") -> ExerciseCategory.STRENGTH

                raw.contains("esneme") ||
                        raw.contains("stretch") ||
                        raw.contains("yoga") ||
                        raw.contains("pilates") ||
                        raw.contains("mobility") -> ExerciseCategory.STRETCHING

                raw.contains("yasam") ||
                        raw.contains("lifestyle") ||
                        raw.contains("cleaning") ||
                        raw.contains("cooking") ||
                        raw.contains("knitting") ||
                        raw.contains("stairs") -> ExerciseCategory.LIFESTYLE

                else -> ExerciseCategory.CARDIO
            }
        }

        private fun resolveLegacyEmoji(exerciseId: String): String {
            val id = exerciseId.normalizedForExerciseSearch()

            return when {
                id.contains("walk") || id.contains("walking") || id.contains("brisk") -> "🚶"
                id.contains("run") || id.contains("running") -> "🏃"
                id.contains("cycl") || id.contains("bicy") -> "🚴"
                id.contains("yoga") -> "🧘"
                id.contains("pilates") -> "🧎"
                id.contains("clean") -> "🧹"
                id.contains("cook") -> "🍳"
                id.contains("knit") -> "🧶"
                id.contains("stairs") -> "🪜"
                else -> "🔥"
            }
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


            private fun String.normalizedForExerciseSearch(): String {
                val fixedTurkish = lowercase(Locale("tr", "TR"))
                    .replace("ı", "i")

                return Normalizer.normalize(fixedTurkish, Normalizer.Form.NFD)
                    .replace("\\p{Mn}+".toRegex(), "")
                    .replace("ğ", "g")
                    .replace("ü", "u")
                    .replace("ş", "s")
                    .replace("ö", "o")
                    .replace("ç", "c")
                    .replace("\\s+".toRegex(), " ")
                    .trim()
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
            return calculateCaloriesFor(
                activity = activity,
                minutes = minutes,
                intensity = selectedIntensity
            )
        }

        private fun calculateCaloriesFor(
            activity: ExerciseUiModel,
            minutes: Int,
            intensity: ExerciseIntensity
        ): Int {
            val durationHours = minutes / 60.0

            return (activity.metValue * userWeightKg * durationHours * intensity.multiplier)
                .roundToInt()
                .coerceAtLeast(0)
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

            // Düzenleme sırasında arama/filtre eski aktiviteyi gizlemesin.
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
            val raw = activity.title.trim()
            val normalized = raw.normalizedForExerciseSearch()

            return when {
                normalized.contains("bicycl") ||
                        normalized.contains("cycling") ||
                        normalized.contains("bisiklet") -> {
                    "Bisiklet • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("running") ||
                        normalized.contains("jogging") ||
                        normalized.contains("kosu") ||
                        normalized.contains("run") -> {
                    "Koşu • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("walking") ||
                        normalized.contains("walk") ||
                        normalized.contains("yuruyus") -> {
                    "Yürüyüş • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("swimming") ||
                        normalized.contains("swim") ||
                        normalized.contains("yuzme") -> {
                    "Yüzme • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("yoga") -> "Yoga"
                normalized.contains("pilates") -> "Pilates"
                normalized.contains("stretch") ||
                        normalized.contains("stretching") ||
                        normalized.contains("esneme") -> "Esneme"

                normalized.contains("cleaning") ||
                        normalized.contains("housework") ||
                        normalized.contains("temizlik") -> {
                    "Ev temizliği • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("gardening") ||
                        normalized.contains("garden") ||
                        normalized.contains("bahce") -> {
                    "Bahçe işi • ${effortLabel(activity.metValue)}"
                }

                normalized.contains("shopping") ||
                        normalized.contains("grocery") ||
                        normalized.contains("market") -> {
                    "Alışveriş yürüyüşü"
                }

                else -> raw
                    .substringBefore(";")
                    .substringBefore(" veya ")
                    .substringBefore(",")
                    .trim()
                    .ifBlank { raw }
                    .take(42)
            }
        }

        private fun displayActivityDescription(activity: ExerciseUiModel): String {
            return "${getString(activity.category.titleRes)} • ${activityIntensityText(activity.metValue)}"
        }

        private fun effortLabel(metValue: Double): String {
            return when {
                metValue < 2.5 -> "Hafif tempo"
                metValue < 4.5 -> "Orta tempo"
                metValue < 7.0 -> "Canlı tempo"
                metValue < 10.0 -> "Yüksek tempo"
                else -> "Çok yüksek tempo"
            }
        }

        private fun activityIntensityText(metValue: Double): String {
            return when {
                metValue < 2.5 -> "Düşük efor"
                metValue < 4.5 -> "Orta efor"
                metValue < 7.0 -> "Aktif tempo"
                metValue < 10.0 -> "Yoğun efor"
                else -> "Çok yoğun efor"
            }
        }

        private fun resolveExerciseEmoji(exerciseId: String): String {
            return allActivities
                .firstOrNull { it.id == exerciseId }
                ?.iconEmoji
                ?: resolveLegacyEmoji(exerciseId)
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
            private const val MIN_USER_WEIGHT_KG = 25.0
            private const val MAX_USER_WEIGHT_KG = 300.0

            fun newInstance(entryDate: String): AddExerciseFragment {
                return AddExerciseFragment().apply {
                    arguments = bundleOf(ARG_ENTRY_DATE to entryDate)
                }
            }
        }
    }