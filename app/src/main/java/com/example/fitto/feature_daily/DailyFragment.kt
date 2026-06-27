package com.example.fitto.feature_daily

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.data.local.repository.DietPlanSeedRepository
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_dietplans.DietPlanDetailFragment
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_dietplans.IntermittentFastingDetailFragment
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.DietMealPlanCatalog
import com.example.fitto.feature_dietplans.planning.PlannedMealProgressResolver
import com.example.fitto.feature_meal.AddMealFragment
import com.example.fitto.feature_meal.MealDetailFragment
import com.example.fitto.feature_meal.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.model.CustomDietPlanLocalModel
import com.example.fitto.feature_dietplans.planning.CustomDietMealPlanGenerator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.example.fitto.feature_daily.data.FastingSessionStore
import com.example.fitto.feature_daily.ai.AiDailyAnalysisCache
import com.example.fitto.feature_daily.ai.AiDailyPromptBuilder
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_daily.ai.CachedDailyAiMessage
import com.example.fitto.feature_daily.ai.GeminiDailyAiRepository
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import com.example.fitto.feature_exercise.AddExerciseFragment
import com.example.fitto.feature_exercise.data.ExerciseLogStore
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_health.HealthConnectManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.fitto.feature_health.FittoStepCounterManager
import com.example.fitto.feature_daily.data.WaterTrackingStore
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import android.app.Dialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.example.fitto.feature_sleep.data.SleepLogStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.format.TextStyle
import com.example.fitto.feature_meal.CameraFoodEntryFragment

class DailyFragment : Fragment(R.layout.fragment_daily) {

    private lateinit var healthConnectManager: HealthConnectManager
    private var healthConnectSummary: HealthConnectDailySummary = HealthConnectDailySummary()
    private var _binding: FragmentDailyBinding? = null
    private val binding get() = _binding!!
    private lateinit var fastingSessionStore: FastingSessionStore
    private lateinit var exerciseLogStore: ExerciseLogStore
    private var selectedDate: LocalDate = LocalDate.now()
    private var fastingTimer: android.os.CountDownTimer? = null
    private var hourglassAnimator: ObjectAnimator? = null
    private var selectedDateIso: String = LocalDate.now().toString()
    private var currentProfileWeightKg: Double? = null
    private lateinit var sleepLogStore: SleepLogStore
    private lateinit var dailyAiRepository: GeminiDailyAiRepository
    private lateinit var dailyAiCache: AiDailyAnalysisCache

    private var currentWeightKg: Double = 70.0
    private var currentWaterMl: Int = 0
    private var currentDailyAiSnapshot: DailyAiSnapshot? = null
    private var dailyAiLoading: Boolean = false
    private var dailyAiRequestInFlightKey: String? = null
    private var pendingDailyAiManualRequest: Boolean = false
    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var dietPlanStore: DietPlanPreferenceStore
    private lateinit var nutritionTargetStore: UserNutritionTargetStore
    private lateinit var mealRepository: MealRepository
    private lateinit var dietPlanSeedRepository: DietPlanSeedRepository
    private lateinit var profileCacheRepository: ProfileCacheRepository
    private lateinit var stepCounterManager: FittoStepCounterManager
    private var localStepCount: Long = 0L
    private lateinit var waterTrackingStore: WaterTrackingStore
    private var waterGoalMl: Int = 2500
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val effect = DailyActivityRecognitionPermissionResultResolver.resolve(granted)

        Toast.makeText(
            requireContext(),
            getString(effect.messageRes),
            Toast.LENGTH_SHORT
        ).show()

        if (effect.shouldStartStepCounter) {
            startLocalStepCounterIfNeeded()
        }

        if (effect.shouldRenderSteps) {
            renderActivityStepsOnly()
        }
    }
    private var currentDailyTargetCalories: Int = DEFAULT_DAILY_CALORIE_TARGET
    private var currentProteinTargetGrams: Int = DEFAULT_PROTEIN_TARGET_GRAMS
    private var currentCarbTargetGrams: Int = DEFAULT_CARB_TARGET_GRAMS
    private var currentFatTargetGrams: Int = DEFAULT_FAT_TARGET_GRAMS

    private var currentActivePlan: ActiveDietPlanUiModel? = null
    private var currentDailyMealPlan: DailyPlannedMealsUiModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDailyBinding.bind(view)

        val appContext = requireContext().applicationContext

        dietPlanStore = DietPlanPreferenceStore(requireContext())
        nutritionTargetStore = UserNutritionTargetStore(requireContext())
        mealRepository = MealRepository(appContext)
        dietPlanSeedRepository = DietPlanSeedRepository(appContext)
        profileCacheRepository = ProfileCacheRepository(appContext)
        customDietPlanStore = CustomDietPlanStore(appContext)
        fastingSessionStore = FastingSessionStore(requireContext())
        exerciseLogStore = ExerciseLogStore(requireContext())
        waterTrackingStore = WaterTrackingStore(appContext)
        healthConnectManager = HealthConnectManager(appContext)
        stepCounterManager = FittoStepCounterManager(appContext)
        sleepLogStore = SleepLogStore(requireContext())
        dailyAiRepository = GeminiDailyAiRepository()
        dailyAiCache = AiDailyAnalysisCache(appContext)

        currentDailyTargetCalories = nutritionTargetStore
            .getDailyTargetCalories()
            .takeIf { it > 0 }
            ?: DEFAULT_DAILY_CALORIE_TARGET

        binding.dateSelectorContainer.setOnClickListener {
            openDatePicker()
        }

        binding.dailySelectedDateText.setOnClickListener {
            openDatePicker()
        }

        setupMockUi()
        setupHealthConnectResultListener()
        setupDietPlanResultListener()
        setupMealAddedResultListener()
        setupMealDetailChangedResultListener()
        setupExerciseAddedResultListener()
        setupClicks()
        setupSleepCard()
        setupDailyAiCard()

        renderSelectedDate()
        renderActivePlan()
        renderWaterTracking()
        renderSleepCard()
        renderSleepWeightSummary()
        startLocalStepCounterIfNeeded()
        loadWaterGoalAndRender()
        loadSleepWeightSummary()
        loadHealthConnectSummary()
        renderWeeklyCalorieProgress()
    }
    private fun currentEntryDate(): String {
        return selectedDate.toString()
    }
    private fun loadSleepWeightSummary() {
        if (!::profileCacheRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val resolvedWeight = DailyProfileMetricsResolver.resolveWeightKg(profile)

            currentProfileWeightKg = resolvedWeight
            currentWeightKg = resolvedWeight ?: 70.0

            renderSleepWeightSummary()
            renderSleepCard()
            renderMealTotals()
        }
    }

    private fun setupSleepCard() {
        binding.sleepCard.setOnClickListener {
            showSleepTimeDialog()
        }
    }
    private fun setupDailyAiCard() {
        binding.dailyAiCard.setOnClickListener {
            val snapshot = currentDailyAiSnapshot
            Log.d(
                FITTO_AI_LOG_TAG,
                "Daily AI card clicked. snapshotReady=${snapshot != null}, loading=$dailyAiLoading"
            )

            if (snapshot == null) {
                pendingDailyAiManualRequest = true
                renderDailyAiPreparingState()
                renderMealTotals()
            } else {
                // Kullanıcı dokunduğunda kart sessiz kalmasın; cache/cooldown varsa hemen sonra güncellenir.
                renderDailyAiLoading()
                requestDailyAiAnalysis(snapshot, isAuto = false)
            }
        }
    }

    private fun dailyAiCardRenderer(): DailyAiCardRenderer {
        return DailyAiCardRenderer(binding, requireContext())
    }

    private fun renderDailyAiDefaultState() {
        dailyAiCardRenderer().renderDefaultState()
    }

    private fun renderDailyAiPreparingState() {
        dailyAiCardRenderer().renderPreparingState()
    }

    private fun renderDailyAiState(snapshot: DailyAiSnapshot?) {
        if (_binding == null || dailyAiLoading) return

        if (snapshot == null) {
            renderDailyAiDefaultState()
            return
        }

        currentDailyAiSnapshot = snapshot

        if (pendingDailyAiManualRequest) {
            pendingDailyAiManualRequest = false
            requestDailyAiAnalysis(snapshot, isAuto = false)
            return
        }

        val cachedMessage = dailyAiCache.get(snapshot.dateIso, snapshot.triggerType)
        if (cachedMessage != null) {
            val cachedAiMessage = cachedMessage.aiMessage.trim()
            if (isUsableDailyAiCachedMessage(cachedMessage, snapshot)) {
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI render cache hit. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, message=${cachedAiMessage.take(80)}"
                )
                renderDailyAiMessage(
                    message = cachedAiMessage,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
                return
            } else {
                Log.w(
                    FITTO_AI_LOG_TAG,
                    "Daily AI stale cache ignored in render. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, source=${cachedMessage.source}, message=${cachedAiMessage.take(80)}"
                )
                dailyAiCache.clear(snapshot.dateIso, snapshot.triggerType)
            }
        }

        val localMessage = localDailyAiMessage(snapshot)
        renderDailyAiMessage(
            message = localMessage,
            subtitle = getString(R.string.daily_ai_default_subtitle)
        )

        if (shouldAutoRequestDailyAi(snapshot) && !dailyAiCache.hasAutoAttempted(snapshot.dateIso, snapshot.triggerType)) {
            requestDailyAiAnalysis(snapshot, isAuto = true)
        }
    }

    private fun renderDailyAiLoading() {
        val status = currentDailyAiSnapshot?.let { snapshot ->
            DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
        } ?: AiDailyStatusLevel.GREEN
        dailyAiCardRenderer().renderLoading(status)
    }

    private fun renderDailyAiMessage(
        message: String,
        subtitle: String = getString(R.string.daily_ai_default_subtitle),
        status: AiDailyStatusLevel = currentDailyAiSnapshot?.let { snapshot ->
            DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
        } ?: AiDailyStatusLevel.GREEN
    ) {
        dailyAiCardRenderer().renderMessage(
            message = message,
            subtitle = subtitle,
            status = status
        )
    }

    private fun renderDailyAiStructuredResult(
        analysis: com.example.fitto.feature_daily.ai.AiDailyAnalysisResult,
        snapshot: DailyAiSnapshot,
        subtitle: String = getString(R.string.daily_ai_result_subtitle)
    ) {
        dailyAiCardRenderer().renderStructuredResult(
            analysis = analysis,
            fallbackMessage = localDailyAiMessage(snapshot),
            subtitle = subtitle,
            status = DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
        )
    }

    private fun requestDailyAiAnalysis(
        snapshot: DailyAiSnapshot,
        isAuto: Boolean
    ) {
        if (_binding == null) {
            Log.w(FITTO_AI_LOG_TAG, "Daily AI request ignored: binding is null.")
            return
        }

        if (!::dailyAiRepository.isInitialized || !::dailyAiCache.isInitialized) {
            Log.w(FITTO_AI_LOG_TAG, "Daily AI request ignored: repository/cache is not ready.")
            renderDailyAiMessage(
                message = localDailyAiMessage(snapshot),
                subtitle = getString(R.string.daily_ai_error_general)
            )
            return
        }

        dailyAiCache.get(snapshot.dateIso, snapshot.triggerType)?.let { cached ->
            val cachedAiMessage = cached.aiMessage.trim()
            if (isUsableDailyAiCachedMessage(cached, snapshot)) {
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI cache hit. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, message=${cachedAiMessage.take(80)}"
                )
                renderDailyAiMessage(
                    message = cachedAiMessage,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
                return
            } else {
                Log.w(
                    FITTO_AI_LOG_TAG,
                    "Daily AI stale cache cleared. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, source=${cached.source}, message=${cachedAiMessage.take(80)}"
                )
                dailyAiCache.clear(snapshot.dateIso, snapshot.triggerType)
            }
        }

        val requestKey = DailyAiCachePolicy.requestKey(snapshot)
        when (
            val decision = DailyAiRequestPolicy.resolve(
                cooldownUntilMillis = dailyAiCache.getCooldownUntil(),
                nowMillis = System.currentTimeMillis(),
                isLoading = dailyAiLoading,
                inFlightRequestKey = dailyAiRequestInFlightKey,
                requestKey = requestKey
            )
        ) {
            is DailyAiRequestPolicy.Decision.BlockedByCooldown -> {
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI request blocked by cooldown. remaining=${decision.remainingSeconds}s"
                )
                renderDailyAiMessage(
                    message = getString(R.string.daily_ai_error_rate_limited),
                    subtitle = localDailyAiMessage(snapshot)
                )
                return
            }

            DailyAiRequestPolicy.Decision.AlreadyLoading -> {
                Log.d(FITTO_AI_LOG_TAG, "Daily AI request ignored: already loading.")
                renderDailyAiLoading()
                return
            }

            DailyAiRequestPolicy.Decision.SameRequestInFlight -> {
                Log.d(FITTO_AI_LOG_TAG, "Daily AI request ignored: same request is already in flight.")
                renderDailyAiLoading()
                return
            }

            DailyAiRequestPolicy.Decision.Start -> Unit
        }

        if (isAuto) {
            dailyAiCache.markAutoAttempted(snapshot.dateIso, snapshot.triggerType)
        }

        Log.d(
            FITTO_AI_LOG_TAG,
            "Daily AI request started. trigger=${snapshot.triggerType.name}, date=${snapshot.dateIso}, isAuto=$isAuto"
        )

        dailyAiLoading = true
        dailyAiRequestInFlightKey = requestKey
        renderDailyAiLoading()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                val language = currentAppLanguage()
                val prompt = AiDailyPromptBuilder.build(toPromptData(snapshot, language))
                dailyAiRepository.requestDailyAnalysis(prompt, language)
            }

            if (_binding == null) {
                dailyAiLoading = false
                dailyAiRequestInFlightKey = null
                return@launch
            }

            result.onSuccess { analysis ->
                Log.d(
                    FITTO_AI_LOG_TAG,
                    "Daily AI request succeeded. localStatus=${DailyAiPromptDataMapper.localStatus(snapshot.triggerType).name}, aiStatus=${analysis.status.name}"
                )

                val message = dailyAiCardRenderer()
                    .buildStructuredMessage(analysis)
                    .ifBlank { localDailyAiMessage(snapshot) }

                dailyAiCache.save(
                    dateIso = snapshot.dateIso,
                    triggerType = snapshot.triggerType,
                    aiMessage = message,
                    totalCalories = snapshot.totalCalories,
                    mealCount = snapshot.meals.size,
                    waterMl = snapshot.waterMl,
                    proteinGrams = snapshot.proteinGrams,
                    exerciseSignature = DailyAiCachePolicy.exerciseSignature(snapshot),
                    nutritionPreferenceSignature = DailyAiCachePolicy.nutritionPreferenceSignature(snapshot)
                )

                renderDailyAiStructuredResult(
                    analysis = analysis,
                    snapshot = snapshot,
                    subtitle = getString(R.string.daily_ai_result_subtitle)
                )
            }.onFailure { throwable ->
                Log.w(FITTO_AI_LOG_TAG, "Daily AI request failed: ${throwable.message}")

                if (throwable.message.orEmpty().contains(GeminiDailyAiRepository.ERROR_RATE_LIMIT)) {
                    dailyAiCache.setCooldownFor(DAILY_AI_RATE_LIMIT_COOLDOWN_MS)
                }

                renderDailyAiMessage(
                    message = localDailyAiMessage(snapshot),
                    subtitle = getString(DailyAiMessageTextResolver.resolveErrorMessageRes(throwable))
                )
            }

            dailyAiLoading = false
            dailyAiRequestInFlightKey = null
        }
    }

    private fun localDailyAiMessage(snapshot: DailyAiSnapshot): String {
        return getString(DailyAiMessageTextResolver.resolveLocalMessageRes(snapshot.triggerType))
    }

    private fun isUsableDailyAiCachedMessage(
        cachedMessage: CachedDailyAiMessage,
        snapshot: DailyAiSnapshot
    ): Boolean {
        if (!DailyAiCachePolicy.matchesSnapshot(cachedMessage, snapshot)) {
            Log.d(
                FITTO_AI_LOG_TAG,
                "Daily AI cache data mismatch. cached=${DailyAiCachePolicy.cachedMessageDebugText(cachedMessage)}, current=${DailyAiCachePolicy.snapshotDebugText(snapshot)}"
            )
        }

        val localTexts = DailyAiMessageTextResolver
            .resolveCacheLocalMessageResList(snapshot.triggerType)
            .map(::getString)

        return DailyAiCachePolicy.isUsableCachedMessage(
            cachedMessage = cachedMessage,
            snapshot = snapshot,
            localMessages = localTexts
        )
    }

    private fun currentAppLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]?.language ?: Locale.getDefault().language
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.language ?: Locale.getDefault().language
        }
    }

    private fun shouldAutoRequestDailyAi(snapshot: DailyAiSnapshot): Boolean {
        return DailyAiPromptDataMapper.shouldAutoRequestAi(
            snapshot = snapshot,
            selectedDate = selectedDate,
            today = LocalDate.now()
        )
    }

    private fun toPromptData(
        snapshot: DailyAiSnapshot,
        appLanguage: String
    ) = DailyAiPromptDataMapper.toPromptData(
        snapshot = snapshot,
        appLanguage = appLanguage,
        goalLabel = getString(DailyAiGoalTextResolver.resolveLabelRes(snapshot.goalType)),
        goalGuidance = DailyAiGoalTextResolver.resolveGuidance(snapshot.goalType, appLanguage)
    )

    private fun showSleepTimeDialog() {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_sleep_time, null, false)

        dialog.setContentView(view)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val bedtimeCard = view.findViewById<MaterialCardView>(R.id.bedtimeCard)
        val wakeTimeCard = view.findViewById<MaterialCardView>(R.id.wakeTimeCard)
        val bedtimeValueText = view.findViewById<TextView>(R.id.bedtimeValueText)
        val wakeTimeValueText = view.findViewById<TextView>(R.id.wakeTimeValueText)
        val durationResultText = view.findViewById<TextView>(R.id.sleepDurationResultText)
        val caloriesResultText = view.findViewById<TextView>(R.id.sleepCaloriesResultText)
        val cancelButton = view.findViewById<MaterialButton>(R.id.sleepCancelButton)
        val saveButton = view.findViewById<MaterialButton>(R.id.sleepSaveButton)

        val currentDate = currentEntryDate()
        val currentLog = sleepLogStore.getLogForDate(currentDate)

        var sleepSelection = DailySleepTimeSelection.from(currentLog)

        fun refreshPreview() {
            bedtimeValueText.text = DailyValueFormatter.formatHourMinute(
                sleepSelection.bedtimeHour,
                sleepSelection.bedtimeMinute
            )
            wakeTimeValueText.text = DailyValueFormatter.formatHourMinute(
                sleepSelection.wakeHour,
                sleepSelection.wakeMinute
            )

            val durationMinutes = sleepSelection.durationMinutes()
            val calories = sleepSelection.calories(currentWeightKg)

            durationResultText.text = getString(
                R.string.sleep_duration_result,
                DailyValueFormatter.formatDuration(durationMinutes)
            )

            caloriesResultText.text = getString(
                R.string.sleep_calories_result,
                calories
            )
        }

        bedtimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_bed),
                initialHour = sleepSelection.bedtimeHour,
                initialMinute = sleepSelection.bedtimeMinute
            ) { hour, minute ->
                sleepSelection = sleepSelection.withBedtime(hour, minute)
                refreshPreview()
            }
        }

        wakeTimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_wake),
                initialHour = sleepSelection.wakeHour,
                initialMinute = sleepSelection.wakeMinute
            ) { hour, minute ->
                sleepSelection = sleepSelection.withWakeTime(hour, minute)
                refreshPreview()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            sleepLogStore.saveLog(sleepSelection.toLog(currentDate, currentWeightKg))
            renderSleepCard()
            dialog.dismiss()

            Toast.makeText(
                requireContext(),
                getString(R.string.sleep_saved_message),
                Toast.LENGTH_SHORT
            ).show()
        }

        refreshPreview()
        dialog.show()
    }


    private fun showTimePicker(
        initialHour: Int,
        initialMinute: Int,
        title: String,
        onTimeSelected: (hour: Int, minute: Int) -> Unit
    ) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText(title)
            .setTheme(R.style.ThemeOverlay_Fitto_WhiteTimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            onTimeSelected(picker.hour, picker.minute)
        }

        picker.show(childFragmentManager, "fitto_time_picker")
    }

    private fun renderSleepCard() {
        if (_binding == null || !::sleepLogStore.isInitialized) return

        val sleepCardState = DailySleepCardStateResolver.resolve(
            sleepLogStore.getLogForDate(currentEntryDate())
        )

        when (sleepCardState) {
            DailySleepCardStateResolver.State.Empty -> {
                binding.sleepValueText.text = getString(R.string.daily_no_sleep_data_short)
                binding.sleepStatusText.text = getString(R.string.daily_no_sleep_data_desc)
            }

            is DailySleepCardStateResolver.State.Logged -> {
                binding.sleepValueText.text = DailyValueFormatter.formatDuration(
                    sleepCardState.durationMinutes
                )
                binding.sleepStatusText.text = getString(
                    R.string.sleep_time_range_format,
                    DailyValueFormatter.formatHourMinute(
                        sleepCardState.selection.bedtimeHour,
                        sleepCardState.selection.bedtimeMinute
                    ),
                    DailyValueFormatter.formatHourMinute(
                        sleepCardState.selection.wakeHour,
                        sleepCardState.selection.wakeMinute
                    )
                )
            }
        }
    }

    private fun renderWeeklyCalorieProgress() {
        if (_binding == null || !::mealRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val targetCalories = resolveDailyNutritionTargets(profile)
                .dailyCalories
                .coerceAtLeast(1)

            val days = (6 downTo 0).map { offset ->
                selectedDate.minusDays(offset.toLong())
            }

            val dailyCalories = withContext(Dispatchers.IO) {
                days.map { date ->
                    val summary = mealRepository.getNutritionSummaryForDate(date.toString())
                    summary.totalCalories.coerceAtLeast(0)
                }
            }

            if (_binding == null) return@launch

            val barViews: List<View> = listOf(
                binding.weekBar1,
                binding.weekBar2,
                binding.weekBar3,
                binding.weekBar4,
                binding.weekBar5,
                binding.weekBar6,
                binding.weekBar7
            )

            val dayTexts: List<TextView> = listOf(
                binding.weekDay1,
                binding.weekDay2,
                binding.weekDay3,
                binding.weekDay4,
                binding.weekDay5,
                binding.weekDay6,
                binding.weekDay7
            )

            val valueTexts: List<TextView> = listOf(
                binding.weekValue1,
                binding.weekValue2,
                binding.weekValue3,
                binding.weekValue4,
                binding.weekValue5,
                binding.weekValue6,
                binding.weekValue7
            )

            val weeklyProgress = DailyWeeklyCalorieProgressCalculator.calculate(
                dailyCalories = dailyCalories,
                targetCalories = targetCalories,
                minHeightPx = 18.dp,
                maxHeightPx = 90.dp
            )

            binding.weeklyProgressDescriptionText.text = if (weeklyProgress.daysWithFood == 0) {
                getString(R.string.daily_no_weekly_data)
            } else {
                getString(
                    R.string.daily_weekly_calorie_summary,
                    weeklyProgress.daysWithFood,
                    weeklyProgress.averageProgress
                )
            }

            binding.weeklyWaterProgress.progress = weeklyProgress.progressBarPercent

            days.forEachIndexed { index, date ->
                val dayProgress = weeklyProgress.days[index]

                val params = barViews[index].layoutParams
                params.height = dayProgress.heightPx
                barViews[index].layoutParams = params

                dayTexts[index].text = date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                )

                valueTexts[index].text = if (dayProgress.hasFood) {
                    getString(R.string.daily_weekly_calorie_percent, dayProgress.progressPercent)
                } else {
                    "-"
                }

                barViews[index].alpha = dayProgress.alpha
            }
        }
    }
    private fun renderSleepWeightSummary() {
        if (_binding == null) return

        renderSleepCard()

        val weightKg = currentProfileWeightKg

        if (weightKg == null) {
            binding.weightValueText.text = getString(R.string.daily_weight_unknown_short)
            binding.weightStatusText.text = getString(R.string.daily_weight_not_saved)
        } else {
            binding.weightValueText.text = getString(
                R.string.daily_weight_value_format,
                DailyValueFormatter.formatWeightValue(weightKg)
            )
            binding.weightStatusText.text = getString(R.string.daily_weight_from_profile)
        }
    }

    private fun loadWaterGoalAndRender() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            waterGoalMl = DailyProfileMetricsResolver.resolveWaterGoalMl(profile)
            renderWaterTracking()
        }
    }

    private fun renderWaterTracking(
        scrollToEnd: Boolean = false,
        scrollToStart: Boolean = false
    ) {
        if (_binding == null || !::waterTrackingStore.isInitialized) return

        val rawWaterMl = waterTrackingStore.getWaterMl(selectedDate)
        val defaultBottleCount = DailyWaterTrackingCalculator.defaultBottleCount(waterGoalMl)
        val visibleBottleCount = waterTrackingStore.getVisibleBottleCount(
            date = selectedDate,
            defaultCount = defaultBottleCount
        )
        val waterState = DailyWaterTrackingCalculator.calculateState(
            rawWaterMl = rawWaterMl,
            waterGoalMl = waterGoalMl,
            visibleBottleCount = visibleBottleCount
        )
        this.currentWaterMl = waterState.normalizedWaterMl

        if (waterState.normalizedWaterMl != rawWaterMl) {
            waterTrackingStore.setWaterMl(selectedDate, waterState.normalizedWaterMl)
        }

        binding.waterAmountText.text = getString(
            R.string.daily_water_value_format,
            waterState.normalizedWaterMl / 1000.0,
            waterGoalMl / 1000.0
        )

        binding.waterBottleContainer.removeAllViews()

        val waterBottleUiState = DailyWaterBottleUiStateResolver.resolve(waterState)

        binding.waterBottleContainer.addView(
            createRemoveBottleView(waterBottleUiState.removeBottle)
        )

        waterBottleUiState.bottles.forEach { bottleState ->
            binding.waterBottleContainer.addView(
                createBottleView(bottleState)
            )
        }

        if (scrollToEnd) {
            binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_RIGHT)
            }
        }

        if (scrollToStart) {
            binding.waterBottleScrollView.post {
                binding.waterBottleScrollView.fullScroll(View.FOCUS_LEFT)
            }
        }

        refreshDailyAiSnapshotForWater()
    }

    private fun createRemoveBottleView(
        state: DailyWaterBottleUiStateResolver.RemoveBottleState
    ): ImageView {
        val context = requireContext()

        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(38.dp, 38.dp).apply {
                marginEnd = 10.dp
                topMargin = 8.dp
                bottomMargin = 8.dp
            }

            background = ContextCompat.getDrawable(context, R.drawable.bg_fitto_chip)
            setPadding(9.dp, 9.dp, 9.dp, 9.dp)

            setImageResource(R.drawable.ic_remove)

            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    state.tintColorRes
                )
            )

            alpha = state.alpha

            contentDescription = getString(state.contentDescriptionRes)

            setOnClickListener {
                if (!state.isEnabled) return@setOnClickListener

                val rawWaterMl = waterTrackingStore.getWaterMl(selectedDate)

                // Keep visible bottles at or above the filled bottle count.
                val canRemoveBottle = DailyWaterTrackingCalculator.canRemoveVisibleBottle(
                    visibleBottleCount = state.visibleBottleCount,
                    rawWaterMl = rawWaterMl
                )

                if (!canRemoveBottle) return@setOnClickListener

                waterTrackingStore.decreaseVisibleBottleCount(
                    date = selectedDate,
                    defaultCount = state.defaultBottleCount,
                    amount = 1
                )

                renderWaterTracking(scrollToStart = true)
            }
        }
    }

    private fun createBottleView(
        state: DailyWaterBottleUiStateResolver.BottleState
    ): ImageView {
        val context = requireContext()

        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(32.dp, 54.dp).apply {
                marginEnd = 10.dp
            }

            setImageResource(R.drawable.ic_water_bottle)

            imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    state.tintColorRes
                )
            )

            alpha = state.alpha

            contentDescription = getString(state.contentDescriptionRes, state.number)

            setOnClickListener {
                toggleWaterBottle(state.isFilled)
            }
        }
    }

    private fun addWater(amountMl: Int) {
        val normalizedAmount = DailyWaterTrackingCalculator.normalizeAddAmount(amountMl)

        if (normalizedAmount <= 0) return

        waterTrackingStore.addWaterMl(
            date = selectedDate,
            amountMl = normalizedAmount
        )

        renderWaterTracking()
    }
    private fun toggleWaterBottle(isFilled: Boolean) {
        val updatedWaterMl = DailyWaterTrackingCalculator.toggleBottleWaterMl(
            rawWaterMl = waterTrackingStore.getWaterMl(selectedDate),
            isFilled = isFilled
        )

        waterTrackingStore.setWaterMl(
            date = selectedDate,
            value = updatedWaterMl
        )

        renderWaterTracking()
    }

    private fun startLocalStepCounterIfNeeded() {
        if (!::stepCounterManager.isInitialized) return

        localStepCount = stepCounterManager.getCachedSteps(selectedDate)
        renderActivityStepsOnly()

        val requiresActivityRecognitionPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val hasActivityRecognitionPermission = !requiresActivityRecognitionPermission ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        when (
            DailyStepCounterStartPolicy.resolve(
                selectedDate = selectedDate,
                today = LocalDate.now(),
                isStepCounterAvailable = stepCounterManager.isStepCounterAvailable(),
                requiresActivityRecognitionPermission = requiresActivityRecognitionPermission,
                hasActivityRecognitionPermission = hasActivityRecognitionPermission
            )
        ) {
            DailyStepCounterStartPolicy.Decision.StopForPastDate -> stepCounterManager.stop()
            DailyStepCounterStartPolicy.Decision.SensorUnavailable -> Toast.makeText(
                requireContext(),
                getString(R.string.step_counter_sensor_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            DailyStepCounterStartPolicy.Decision.RequestActivityRecognitionPermission -> {
                activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
            DailyStepCounterStartPolicy.Decision.Start -> stepCounterManager.start(selectedDate) { steps ->
                localStepCount = steps
                renderActivityStepsOnly()
                renderMealTotals()
            }
        }
    }

    private fun resolvedDisplaySteps(): Long {
        return DailyActivityPolicy.resolveDisplaySteps(
            isHealthConnectAvailable = healthConnectSummary.isAvailable,
            hasHealthConnectPermission = healthConnectSummary.hasPermission,
            healthSteps = healthConnectSummary.steps,
            localSteps = localStepCount
        )
    }

    private fun renderActivityStepsOnly() {
        if (_binding == null) return

        binding.activityStepCountText.text = DailyValueFormatter.formatStepCount(
            resolvedDisplaySteps()
        )
    }

    private fun setupHealthConnectResultListener() {
        childFragmentManager.setFragmentResultListener(
            HealthConnectBottomSheetFragment.REQUEST_HEALTH_CONNECT_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            loadHealthConnectSummary()
        }
    }

    private fun showHealthConnectBottomSheet() {
        HealthConnectBottomSheetFragment()
            .show(childFragmentManager, "health_connect")
    }

    private fun loadHealthConnectSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            healthConnectSummary = healthConnectManager.readDailySummary(selectedDate)

            renderHealthConnectSummary()
        }
    }
    private fun renderHealthConnectSummary() {
        // Health Connect verisi geldikten sonra ana günlük hesapları tekrar çiziyoruz.
        // Böylece yakılan kalori, kalan kalori ve aktivite kartı aynı yerden güncelleniyor.
        renderMealTotals()
    }

    private fun setupExerciseAddedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddExerciseFragment.REQUEST_EXERCISE_ADDED,
            viewLifecycleOwner
        ) { _, bundle ->
            val (dateEffect, refreshEffect) = DailyResultEffectResolver.resolveExerciseAdded(
                currentDate = selectedDate,
                entryDate = bundle.getString(AddExerciseFragment.BUNDLE_EXERCISE_DATE)
            )

            applyDateSelectionEffect(dateEffect)
            applyRefreshEffect(refreshEffect)
        }
    }
    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            renderSelectedDate()
            loadHealthConnectSummary()
            renderActivePlan()
            startLocalStepCounterIfNeeded()
            renderWaterTracking()
            loadSleepWeightSummary()
            renderSleepCard()
            renderWeeklyCalorieProgress()
        }
    }
    override fun onPause() {
        if (::stepCounterManager.isInitialized) {
            stepCounterManager.stop()
        }
        super.onPause()
    }
    private fun openDatePicker() {
        val todayMillis = LocalDate.now()
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val selectedMillis = selectedDate
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        val constraints = CalendarConstraints.Builder()
            .setEnd(todayMillis)
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(getString(R.string.daily_date_picker_title))
            .setSelection(selectedMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
            val pickedDate = Instant.ofEpochMilli(selectedUtcMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()

            when (DailyDateSelectionResolver.resolvePickedDate(pickedDate)) {
                DailyDateSelectionResolver.PickedDateResult.FutureDate -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.daily_future_date_blocked),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is DailyDateSelectionResolver.PickedDateResult.Selected -> {
                    updateSelectedDate(pickedDate)
                    reloadDailyDataForSelectedDate()
                }
            }
        }

        picker.show(parentFragmentManager, "daily_date_picker")
    }


    private fun applyDateSelectionEffect(effect: DailyResultEffectResolver.DateSelectionEffect) {
        effect.selection?.let { selection ->
            updateSelectedDate(selection.date)
        }
    }

    private fun applyRefreshEffect(effect: DailyResultEffectResolver.RefreshEffect) {
        if (effect.shouldLoadHealthConnectSummary) {
            loadHealthConnectSummary()
        }

        if (effect.shouldRenderMealTotals) {
            renderMealTotals()
        }

        if (effect.shouldRenderActivePlan) {
            renderActivePlan()
        }

        if (effect.shouldRenderWeeklyCalorieProgress) {
            renderWeeklyCalorieProgress()
        }
    }

    private fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        selectedDateIso = date.toString()
        renderSelectedDate()
    }

    private fun renderSelectedDate() = with(binding) {
        selectedDateIso = selectedDate.toString()

        val selectedDateText = DailySelectedDateTextResolver.resolve(selectedDate)
        dailySelectedDateText.text = if (selectedDateText.isToday) {
            getString(R.string.daily_selected_date_today, selectedDateText.formattedDate)
        } else {
            getString(R.string.daily_selected_date_format, selectedDateText.formattedDate)
        }
    }

    private fun reloadDailyDataForSelectedDate() {
        selectedDateIso = selectedDate.toString()
        loadHealthConnectSummary()
        renderActivePlan()
        startLocalStepCounterIfNeeded()
        renderWaterTracking()
        renderSleepCard()
        renderWeeklyCalorieProgress()
    }

    private fun setupMockUi() = with(binding) {
        dailyCalorieProgress.progress = 0
        dailyCarbProgress.progress = 0
        dailyProteinProgress.progress = 0
        dailyFatProgress.progress = 0
        weeklyWaterProgress.progress = 0
        renderDailyAiDefaultState()
    }

    private fun setupDietPlanResultListener() {
        parentFragmentManager.setFragmentResultListener(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            applyRefreshEffect(DailyResultEffectResolver.resolveDietPlanChanged())
        }
    }

    private fun setupMealAddedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddMealFragment.REQUEST_MEAL_ENTRY_ADDED,
            viewLifecycleOwner
        ) { _, bundle ->
            val effect = DailyResultEffectResolver.resolveMealAdded(
                currentDate = selectedDate,
                mealTypeArgument = bundle.getString(AddMealFragment.BUNDLE_MEAL_TYPE),
                entryDate = bundle.getString(AddMealFragment.BUNDLE_ENTRY_DATE)
            )

            applyDateSelectionEffect(
                DailyResultEffectResolver.DateSelectionEffect(effect.dateSelection)
            )

            markNextPlannedMealCompletedIfMatches(effect.mealType)
            applyRefreshEffect(effect.refreshEffect)
        }
    }

    private fun setupMealDetailChangedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            MealDetailFragment.REQUEST_MEAL_DETAIL_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            applyRefreshEffect(DailyResultEffectResolver.resolveMealDetailChanged())
        }
    }

    private fun renderActivePlan() {
        if (_binding == null || !::dietPlanStore.isInitialized) return

        val activePlanId = dietPlanStore.getActivePlanId()
        if (activePlanId?.let(DailyActivePlanPolicy::isCustomPlan) == true) {
            val customPlan = customDietPlanStore.getPlan(activePlanId)

            if (customPlan != null) {
                renderGeneratedCustomPlan(customPlan)
                return
            }
        }

        val activePlan = if (activePlanId == null) {
            DietPlanCatalog.createEmptyActivePlan(requireContext())
        } else {
            DietPlanCatalog.getActivePlan(
                context = requireContext(),
                planId = activePlanId,
                fastingType = dietPlanStore.getActiveFastingType(),
                customFastingHours = dietPlanStore.getCustomFastingHours()
            )
        }

        currentActivePlan = activePlan.takeUnless { plan ->
            DailyActivePlanPolicy.isEmptyPlan(plan.id)
        }

        if (DailyActivePlanPolicy.shouldUseBasicPlanRenderer(activePlan.id)) {
            currentDailyMealPlan = null
            renderBasicActivePlan(activePlan)
            return
        }

        val targetCalories = currentDailyTargetCalories.coerceAtLeast(1)

        viewLifecycleOwner.lifecycleScope.launch {
            val dailyPlan = withContext(Dispatchers.IO) {
                buildDailyMealPlanFromRoom(activePlan, targetCalories)
            }

            if (_binding == null) return@launch

            val safeDailyPlan = dailyPlan ?: buildDailyMealPlanFromCatalog(
                activePlan = activePlan,
                targetCalories = targetCalories
            )

            if (safeDailyPlan == null) {
                currentDailyMealPlan = null
                renderBasicActivePlan(activePlan)
            } else {
                currentDailyMealPlan = safeDailyPlan
                renderDailyMealPlan(safeDailyPlan)
            }
        }
    }

    private fun renderFastingActionsIfNeeded(planId: String) = with(binding) {
        if (!DailyActivePlanPolicy.isFastingPlan(planId)) {
            fastingActionContainer.visibility = View.GONE
            fastingAnalysisContainer.visibility = View.GONE
            stopFastingTimer()
            stopHourglassAnimation()
            return
        }

        fastingActionContainer.visibility = View.VISIBLE
        fastingAnalysisContainer.visibility = View.VISIBLE

        fastingActionContainer.setOnClickListener {
            // Keep taps in the fasting control area from opening plan details.
        }

        val fastingHours = resolveSelectedFastingHours()
        val remainingMillis = fastingSessionStore.getRemainingMillis(planId)

        fastingMealSuggestionText.text = buildString {
            append(getString(R.string.daily_fasting_meal_suggestion_title))
            append("\n")
            append(getFastingMealSuggestion(fastingHours))
        }

        updateFastingAnalysis(planId, remainingMillis)

        if (remainingMillis > 0L) {
            startFastingCountdown(planId)
        } else {
            renderFastingCountdownState(
                state = DailyFastingUiStateResolver.resolveIdleCountdownState(),
                onStartClick = {
                    fastingSessionStore.startFasting(
                        planId = planId,
                        fastingHours = fastingHours
                    )

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.daily_fasting_started_message),
                        Toast.LENGTH_SHORT
                    ).show()

                    startFastingCountdown(planId)
                }
            )
        }
    }
    private fun startFastingCountdown(planId: String) {
        stopFastingTimer()

        val remainingMillis = fastingSessionStore.getRemainingMillis(planId)

        if (remainingMillis <= 0L) {
            renderFastingCountdownState(
                DailyFastingUiStateResolver.resolveCompletedCountdownState()
            )
            updateFastingAnalysis(planId)
            return
        }

        renderFastingCountdownState(
            state = DailyFastingUiStateResolver.resolveRunningCountdownState(remainingMillis),
            onFinishClick = ::finishFastingManually
        )
        updateFastingAnalysis(planId, remainingMillis)

        fastingTimer = object : android.os.CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (_binding == null) return

                binding.fastingCountdownText.text = DailyValueFormatter.formatRemainingTime(millisUntilFinished)
                updateFastingAnalysis(planId, millisUntilFinished)
            }

            override fun onFinish() {
                if (_binding == null) return

                renderFastingCountdownState(
                    DailyFastingUiStateResolver.resolveCompletedCountdownState()
                )

                fastingSessionStore.clear()
                renderActivePlan()
            }
        }.start()
    }
    private fun finishFastingManually() {
        stopFastingTimer()
        fastingSessionStore.clear()

        renderFastingCountdownState(
            DailyFastingUiStateResolver.resolveIdleCountdownState()
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.daily_fasting_finished_message),
            Toast.LENGTH_SHORT
        ).show()

        renderActivePlan()
    }

    private fun stopFastingTimer() {
        fastingTimer?.cancel()
        fastingTimer = null
    }

    private fun renderFastingCountdownState(
        state: DailyFastingUiStateResolver.CountdownState,
        onStartClick: (() -> Unit)? = null,
        onFinishClick: (() -> Unit)? = null
    ) = with(binding) {
        fastingCountdownTitleText.text = getString(state.titleRes)
        fastingCountdownText.text = state.messageRes?.let(::getString)
            ?: state.remainingMillis?.let(DailyValueFormatter::formatRemainingTime)
            .orEmpty()
        startFastingButton.isEnabled = state.isButtonEnabled
        startFastingButton.text = getString(state.buttonTextRes)

        val clickAction = when (state.buttonAction) {
            DailyFastingUiStateResolver.ButtonAction.START -> onStartClick
            DailyFastingUiStateResolver.ButtonAction.FINISH -> onFinishClick
            DailyFastingUiStateResolver.ButtonAction.NONE -> null
        }

        startFastingButton.setOnClickListener {
            clickAction?.invoke()
        }

        if (state.shouldAnimateHourglass) {
            startHourglassAnimation()
        } else {
            stopHourglassAnimation()
        }
    }

    private fun updateFastingAnalysis(
        planId: String,
        remainingMillisOverride: Long? = null
    ) = with(binding) {
        val remainingMillis = remainingMillisOverride
            ?: fastingSessionStore.getRemainingMillis(planId)
        val analysisState = DailyFastingUiStateResolver.resolveAnalysisState(
            fastingHours = resolveSelectedFastingHours(),
            remainingMillis = remainingMillis
        )

        fastingAnalysisTitleText.text = getString(analysisState.titleRes)
        fastingAnalysisRangeText.text = getString(analysisState.rangeTitleRes)
        fastingAnalysisBodyText.text = getString(analysisState.bodyTextRes)
    }
    private fun startHourglassAnimation() {
        if (hourglassAnimator?.isRunning == true) return

        hourglassAnimator = ObjectAnimator.ofFloat(
            binding.fastingHourglassText,
            View.ROTATION,
            0f,
            180f,
            360f
        ).apply {
            duration = 1400L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        binding.fastingHourglassText.animate()
            .alpha(0.65f)
            .setDuration(700L)
            .withEndAction {
                binding.fastingHourglassText.animate()
                    .alpha(1f)
                    .setDuration(700L)
                    .start()
            }
            .start()
    }

    private fun stopHourglassAnimation() {
        hourglassAnimator?.cancel()
        hourglassAnimator = null

        binding.fastingHourglassText.rotation = 0f
        binding.fastingHourglassText.alpha = 1f
    }

    private fun getFastingMealSuggestion(fastingHours: Int): String {
        return getString(DailyFastingTextResolver.resolveMealSuggestionRes(fastingHours))
    }

    private fun resolveSelectedFastingHours(): Int {
        return DailyFastingPolicy.resolveFastingHours(
            fastingTypeName = dietPlanStore.getActiveFastingType()?.name,
            customFastingHours = dietPlanStore.getCustomFastingHours()
        )
    }

    private fun renderGeneratedCustomPlan(
        customPlan: CustomDietPlanLocalModel
    ) {
        val generator = CustomDietMealPlanGenerator(requireContext())
        val activePlan = generator.createActivePlan(customPlan)
        currentActivePlan = activePlan

        viewLifecycleOwner.lifecycleScope.launch {
            val foods = withContext(Dispatchers.IO) {
                mealRepository.getAllFoods()
            }

            if (_binding == null) return@launch

            val dayNumber = dietPlanStore.getCurrentPlanDay(customPlan.durationDays)
            val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
                planId = customPlan.id,
                dayNumber = dayNumber
            )
            val dailyPlan = generator.buildDailyPlan(
                plan = customPlan,
                allFoods = foods,
                dayNumber = dayNumber,
                completedMealIds = completedMealIds
            )

            currentDailyMealPlan = dailyPlan

            if (dailyPlan.meals.isEmpty()) {
                renderBasicActivePlan(activePlan)
            } else {
                renderDailyMealPlan(dailyPlan)
            }
        }
    }

    private suspend fun buildDailyMealPlanFromRoom(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val roomPlan = dietPlanSeedRepository.getDietPlanById(activePlan.id) ?: return null
        if (roomPlan.durationDays <= 0) return null

        val dayNumber = dietPlanStore.getCurrentPlanDay(roomPlan.durationDays)
        val plannedMeals = dietPlanSeedRepository.getPlannedMealUiModelsForDay(
            planId = activePlan.id,
            dayNumber = dayNumber
        )

        val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
            activePlan.id,
            dayNumber
        )

        val nextMeal = PlannedMealProgressResolver.nextMeal(
            meals = plannedMeals,
            completedMealIds = completedMealIds
        )

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = roomPlan.nameTr,
            planBadge = activePlan.badge,
            planIcon = roomPlan.icon.ifBlank { activePlan.icon },
            dayNumber = dayNumber,
            durationDays = roomPlan.durationDays,
            targetCalories = targetCalories,
            meals = plannedMeals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun buildDailyMealPlanFromCatalog(
        activePlan: ActiveDietPlanUiModel,
        targetCalories: Int
    ): DailyPlannedMealsUiModel? {
        val durationDays = DietMealPlanCatalog.getPlanDurationDays(activePlan.id)
        if (durationDays <= 0) return null

        val dayNumber = dietPlanStore.getCurrentPlanDay(durationDays)
        val plannedMeals = DietMealPlanCatalog.getMealsForDay(activePlan.id, dayNumber)
        if (plannedMeals.isEmpty()) return null

        val completedMealIds = dietPlanStore.getCompletedPlannedMealIds(
            activePlan.id,
            dayNumber
        )

        val nextMeal = PlannedMealProgressResolver.nextMeal(
            meals = plannedMeals,
            completedMealIds = completedMealIds
        )

        return DailyPlannedMealsUiModel(
            planId = activePlan.id,
            planTitle = DietMealPlanCatalog.getPlanTitle(requireContext(), activePlan.id),
            planBadge = DietMealPlanCatalog.getPlanBadge(requireContext(), activePlan.id),
            planIcon = DietMealPlanCatalog.getPlanIcon(activePlan.id),
            dayNumber = dayNumber,
            durationDays = durationDays,
            targetCalories = targetCalories,
            meals = plannedMeals,
            nextMeal = nextMeal,
            completedMealIds = completedMealIds
        )
    }

    private fun renderBasicActivePlan(activePlan: ActiveDietPlanUiModel) = with(binding) {
        val planState = DailyBasicActivePlanUiStateResolver.resolve(activePlan)

        activePlanTitleText.text = planState.title
        activePlanBadgeText.text = planState.badge

        when (val content = planState.content) {
            DailyBasicActivePlanUiStateResolver.Content.Fasting -> {
                activePlanMetricsContainer.isVisible = false
                fastingActionContainer.isVisible = true
                fastingAnalysisContainer.isVisible = true

                renderFastingActionsIfNeeded(planState.planId)
                updateFastingAnalysis(planState.planId)
            }

            is DailyBasicActivePlanUiStateResolver.Content.Metrics -> {
                activePlanMetricsContainer.isVisible = true
                fastingActionContainer.isVisible = false
                fastingAnalysisContainer.isVisible = false

                activePlanIconText.isVisible = true
                activePlanDivider.isVisible = true
                activePlanMetricOneLabelText.isVisible = true
                activePlanMetricOneValueText.isVisible = true
                activePlanMetricTwoLabelText.isVisible = true
                activePlanMetricTwoValueText.isVisible = true

                activePlanMetricOneLabelText.text = content.primaryLabel
                activePlanMetricOneValueText.text = content.primaryValue
                activePlanMetricTwoLabelText.text = content.secondaryLabel
                activePlanMetricTwoValueText.text = content.secondaryValue
            }
        }

        plannedMealFoodsText.isVisible = false
        applyPlannedMealButton.isVisible = false
        fastingProgress.progress = planState.progress
    }

    private fun renderDailyMealPlan(plan: DailyPlannedMealsUiModel) = with(binding) {
        fastingActionContainer.visibility = View.GONE
        fastingAnalysisContainer.visibility = View.GONE
        stopFastingTimer()
        stopHourglassAnimation()

        activePlanMetricsContainer.isVisible = true
        activePlanIconText.isVisible = true
        activePlanDivider.isVisible = true
        activePlanMetricOneLabelText.isVisible = true
        activePlanMetricOneValueText.isVisible = true
        activePlanMetricTwoLabelText.isVisible = true
        activePlanMetricTwoValueText.isVisible = true

        val planState = DailyPlannedMealUiStateResolver.resolve(plan)

        activePlanTitleText.text = planState.planTitle

        activePlanBadgeText.text = getString(
            R.string.daily_active_plan_day_badge,
            planState.dayNumber,
            planState.durationDays,
            planState.targetCalories
        )

        activePlanIconText.text = planState.planIcon
        activePlanMetricOneLabelText.text = getString(R.string.daily_next_planned_meal_label)
        activePlanMetricTwoLabelText.text = getString(R.string.daily_plan_status_label)
        activePlanMetricTwoValueText.text = getString(
            R.string.daily_plan_status_value,
            planState.completedMealCount,
            planState.totalMealCount
        )
        fastingProgress.progress = planState.progressPercent

        when (val nextMealState = planState.nextMealState) {
            DailyPlannedMealUiStateResolver.NextMealState.Completed -> {
                activePlanMetricOneValueText.text = getString(R.string.daily_plan_completed_value)
                plannedMealFoodsText.text = getString(R.string.daily_plan_completed_description)
                plannedMealFoodsText.isVisible = true
                applyPlannedMealButton.isVisible = false
            }

            is DailyPlannedMealUiStateResolver.NextMealState.Available -> {
                activePlanMetricOneValueText.text = getString(
                    R.string.daily_next_planned_meal_value,
                    getString(nextMealState.mealTypeTitleRes),
                    nextMealState.calories
                )

                plannedMealFoodsText.text = buildRecommendationText(
                    mealTitle = nextMealState.mealTitle,
                    foodSummary = nextMealState.foodSummary
                )

                plannedMealFoodsText.isVisible = true
                applyPlannedMealButton.isVisible = true
            }
        }
    }

    private fun buildRecommendationText(
        mealTitle: String,
        foodSummary: String
    ): SpannableStringBuilder {
        val green = ContextCompat.getColor(requireContext(), R.color.fitto_green)

        return SpannableStringBuilder().apply {
            val mealLabelStart = length
            append(getString(R.string.daily_meal_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                mealLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(mealTitle)

            append("\n\n")

            val foodLabelStart = length
            append(getString(R.string.daily_food_recommendation_label))
            setSpan(
                ForegroundColorSpan(green),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                foodLabelStart,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("\n")
            append(foodSummary)
        }
    }

    private fun renderMealTotals() {
        if (_binding == null || !::mealRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            val selectedDateIso = selectedDate.toString()

            val mealSummaries = withContext(Dispatchers.IO) {
                mealRepository.getMealSummariesForDate(selectedDateIso)
            }

            val nutritionSummary = withContext(Dispatchers.IO) {
                mealRepository.getNutritionSummaryForDate(selectedDateIso)
            }
            val exerciseSummary = withContext(Dispatchers.IO) {
                exerciseLogStore.getSummaryForDate(selectedDateIso)
            }
            val exerciseLogs = withContext(Dispatchers.IO) {
                exerciseLogStore.getLogsForDate(selectedDateIso)
            }

            val mealDetailsByType = withContext(Dispatchers.IO) {
                MealType.entries.associateWith { mealType ->
                    mealRepository.getMealEntryDetailsForDate(
                        dateIso = selectedDateIso,
                        mealType = mealType
                    )
                }
            }
            if (_binding == null) return@launch

            val nutritionTargets = resolveDailyNutritionTargets(profile)
            val targetCalories = nutritionTargets.dailyCalories

            val oldTargetCalories = currentDailyTargetCalories

            currentDailyTargetCalories = targetCalories
            currentProteinTargetGrams = nutritionTargets.proteinGrams
            currentCarbTargetGrams = nutritionTargets.carbGrams
            currentFatTargetGrams = nutritionTargets.fatGrams

            val byMealType = mealSummaries.associateBy { it.mealType }
            val mealCalories = DailyMealCaloriesResolver.resolve(mealSummaries)

            binding.breakfastCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                mealCalories.breakfast
            )

            binding.snackCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                mealCalories.snack
            )

            binding.lunchCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                mealCalories.lunch
            )

            binding.dinnerCaloriesText.text = getString(
                R.string.daily_meal_calories_value,
                mealCalories.dinner
            )

            val resolvedWeight = DailyProfileMetricsResolver.resolveWeightKg(profile)
            currentProfileWeightKg = resolvedWeight
            currentWeightKg = resolvedWeight ?: currentWeightKg

            val healthActiveCalories = DailyHealthActiveCaloriesResolver.resolve(
                isAvailable = healthConnectSummary.isAvailable,
                hasPermission = healthConnectSummary.hasPermission,
                activeCaloriesKcal = healthConnectSummary.activeCaloriesKcal
            )

            val displaySteps = resolvedDisplaySteps()
            val summaryState = DailySummaryUiStateResolver.resolve(
                nutritionSummary = nutritionSummary,
                nutritionTargets = nutritionTargets,
                exerciseSummary = exerciseSummary,
                healthActiveCalories = healthActiveCalories,
                displaySteps = displaySteps,
                weightKg = currentWeightKg
            )
            val energyBalance = summaryState.energyBalance

            binding.remainingCaloriesText.text = energyBalance.remainingCalories.toString()

            binding.dailyIntakeText.text = getString(
                R.string.daily_summary_intake_value,
                energyBalance.eatenCalories
            )

            binding.dailyBurnedText.text = getString(
                R.string.daily_summary_burned_value,
                energyBalance.burnedCalories
            )
            binding.activityBurnedCaloriesText.text = energyBalance.burnedCalories.toString()

            renderActivitySourceStatus(
                healthActiveCalories = energyBalance.syncedActivityCalories,
                manualExerciseCalories = energyBalance.manualExerciseCalories
            )

            binding.activityStepCountText.text = DailyValueFormatter.formatStepCount(
                summaryState.displaySteps
            )

            binding.activityDurationText.text = getString(
                R.string.exercise_duration_value,
                summaryState.activityDurationMinutes
            )

            binding.dailyRemainingText.text = getString(
                R.string.daily_summary_remaining_value,
                energyBalance.remainingCalories
            )

            binding.dailyCalorieProgress.progress = summaryState.calorieProgressPercent
            binding.dailyCarbValueText.text = getString(
                R.string.daily_macro_value,
                DailyValueFormatter.formatGram(summaryState.carbs.currentGrams),
                summaryState.carbs.targetGrams
            )

            binding.dailyProteinValueText.text = getString(
                R.string.daily_macro_value,
                DailyValueFormatter.formatGram(summaryState.protein.currentGrams),
                summaryState.protein.targetGrams
            )

            binding.dailyFatValueText.text = getString(
                R.string.daily_macro_value,
                DailyValueFormatter.formatGram(summaryState.fat.currentGrams),
                summaryState.fat.targetGrams
            )

            binding.dailyCarbProgress.progress = summaryState.carbs.progressPercent
            binding.dailyProteinProgress.progress = summaryState.protein.progressPercent
            binding.dailyFatProgress.progress = summaryState.fat.progressPercent

            val dailyAiMeals = buildDailyAiMeals(
                mealSummariesByType = byMealType,
                mealDetailsByType = mealDetailsByType
            )

            currentDailyAiSnapshot = DailyAiSnapshotResolver.resolve(
                dateIso = selectedDateIso,
                userName = DailyAiUserNameResolver.resolve(
                    displayName = profile?.displayName,
                    userName = profile?.userName,
                    fallbackName = getString(R.string.app_name)
                ),
                goalType = profile?.goalType.orEmpty(),
                nutritionTargets = nutritionTargets,
                summaryState = summaryState,
                waterMl = currentWaterMl,
                waterGoalMl = waterGoalMl,
                meals = dailyAiMeals,
                exerciseLogCount = exerciseSummary.logCount,
                exerciseSummaryText = buildDailyAiExerciseSummary(
                    exerciseLogs,
                    energyBalance.syncedActivityCalories
                ),
                dietPreferences = ProfilePreferenceStore.nutritionDietPreferencesText(
                    requireContext()
                ),
                allergies = ProfilePreferenceStore.getNutritionAllergies(requireContext()),
                dislikedFoods = ProfilePreferenceStore.getNutritionDislikedFoods(requireContext()),
                mealRoutine = ProfilePreferenceStore.nutritionMealRoutineText(
                    requireContext()
                ),
                snackMealLabel = getString(R.string.daily_snack)
            )

            renderDailyAiState(currentDailyAiSnapshot)

            if (oldTargetCalories != targetCalories) {
                renderActivePlan()
            }
        }
    }

    private fun buildDailyAiMeals(
        mealSummariesByType: Map<String, com.example.fitto.data.local.model.DailyMealSummary>,
        mealDetailsByType: Map<MealType, List<MealEntryDetailUiModel>>
    ): List<DailyAiMealSnapshot> {
        return DailyAiMealSnapshotMapper.buildMeals(
            mealDefinitions = DailyAiMealDefinitionResolver.resolveMealDefinitions().map { definition ->
                DailyAiMealSnapshotMapper.MealDefinition(
                    key = definition.key,
                    label = getString(definition.labelRes)
                )
            },
            mealSummariesByType = mealSummariesByType,
            mealDetailsByType = mealDetailsByType.mapKeys { (mealType, _) ->
                mealType.argumentValue
            },
            unavailableDetailsText = getString(R.string.daily_ai_entry_details_unavailable),
            maxFoodSummaryChars = DAILY_AI_MEAL_SUMMARY_MAX_CHARS
        )
    }

    private fun buildDailyAiExerciseSummary(
        logs: List<ExerciseLogLocalModel>,
        syncedActivityCalories: Int
    ): String {
        return DailyAiExerciseSummaryBuilder.build(
            logs = DailyAiExerciseLogMapper.toExerciseLogs(logs),
            syncedActivityCalories = syncedActivityCalories,
            itemText = { title, durationMinutes, calories ->
                getString(
                    R.string.daily_ai_exercise_summary_item,
                    title,
                    durationMinutes,
                    calories
                )
            },
            withSyncedText = { manualSummary, syncedCalories ->
                getString(R.string.daily_ai_exercise_summary_with_steps, manualSummary, syncedCalories)
            },
            syncedOnlyText = { syncedCalories ->
                getString(R.string.daily_ai_exercise_summary_synced, syncedCalories)
            }
        )
    }

    private fun refreshDailyAiSnapshotForWater() {
        val currentSnapshot = currentDailyAiSnapshot ?: return
        val updatedTrigger = DailyAiTriggerResolver.resolve(
            totalCalories = currentSnapshot.totalCalories,
            targetCalories = currentSnapshot.dailyCalorieTarget,
            waterMl = currentWaterMl,
            waterGoalMl = waterGoalMl,
            proteinGrams = currentSnapshot.proteinGrams,
            proteinTargetGrams = currentSnapshot.proteinTargetGrams,
            meals = currentSnapshot.meals,
            exerciseLogCount = currentSnapshot.exerciseLogCount,
            snackMealLabel = getString(R.string.daily_snack)
        )

        val updatedSnapshot = currentSnapshot.copy(
            waterMl = currentWaterMl,
            waterGoalMl = waterGoalMl,
            triggerType = updatedTrigger
        )

        currentDailyAiSnapshot = updatedSnapshot
        renderDailyAiState(updatedSnapshot)
    }

    private fun getActiveCustomPlanOrNull(): CustomDietPlanLocalModel? {
        val activePlanId = dietPlanStore.getActivePlanId() ?: return null

        return activePlanId
            .takeIf(DailyActivePlanPolicy::isCustomPlan)
            ?.let { customDietPlanStore.getPlan(it) }
    }

    private fun resolveDailyNutritionTargets(
        profile: UserProfileEntity?
    ): DailyNutritionTargetResolver.Targets {
        return DailyNutritionTargetResolver.resolveTargets(
            activeCustomPlanCalories = activeCustomPlanCalories(),
            profileDailyCalories = profile?.dailyCalorieTarget,
            storedDailyCalories = nutritionTargetStore.getDailyTargetCalories(),
            defaultDailyCalories = DEFAULT_DAILY_CALORIE_TARGET,
            profileProteinGrams = profile?.proteinTargetGrams,
            defaultProteinGrams = DEFAULT_PROTEIN_TARGET_GRAMS,
            profileCarbGrams = profile?.carbTargetGrams,
            defaultCarbGrams = DEFAULT_CARB_TARGET_GRAMS,
            profileFatGrams = profile?.fatTargetGrams,
            defaultFatGrams = DEFAULT_FAT_TARGET_GRAMS
        )
    }

    private fun activeCustomPlanCalories(): Int? {
        return getActiveCustomPlanOrNull()
            ?.dailyCalories
            ?.takeIf { it > 0 }
    }

    private fun setupClicks() = with(binding) {
        dailyQuickAddFab.setOnClickListener {
            DailyQuickAddBottomSheet().show(
                childFragmentManager,
                DailyQuickAddBottomSheet.TAG
            )
        }

        dietPlansButton.setOnClickListener {
            openDietPlans()
        }

        applyTodayPlanButton.setOnClickListener {
            applyActivePlanTodayOrOpenList()
        }

        activePlanCard.setOnClickListener {
            val activePlan = currentActivePlan
            if (activePlan == null) {
                openDietPlans()
            } else {
                openActivePlanDetail(activePlan.id)
            }
        }

        applyPlannedMealButton.setOnClickListener {
            applyNextPlannedMeal()
        }

        breakfastMealRow.setOnClickListener {
            openMealDetail(MealType.BREAKFAST)
        }

        snackMealRow.setOnClickListener {
            openMealDetail(MealType.SNACK)
        }

        lunchMealRow.setOnClickListener {
            openMealDetail(MealType.LUNCH)
        }

        dinnerMealRow.setOnClickListener {
            openMealDetail(MealType.DINNER)
        }

        breakfastAddButton.setOnClickListener {
            openAddMeal(MealType.BREAKFAST)
        }

        snackAddButton.setOnClickListener {
            openAddMeal(MealType.SNACK)
        }

        lunchAddButton.setOnClickListener {
            openAddMeal(MealType.LUNCH)
        }

        dinnerAddButton.setOnClickListener {
            openAddMeal(MealType.DINNER)
        }

        addWater250Button.setOnClickListener {
            addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
        }

        addWater500Button.setOnClickListener {
            addWater(WaterTrackingStore.BOTTLE_SIZE_ML * 2)
        }
        addWaterBottleSlotButton.setOnClickListener {
            val defaultBottleCount = DailyWaterTrackingCalculator.defaultBottleCount(waterGoalMl)

            waterTrackingStore.increaseVisibleBottleCount(
                date = selectedDate,
                defaultCount = defaultBottleCount,
                amount = 4
            )

            renderWaterTracking(scrollToEnd = true)
        }
        activitySummaryCard.setOnClickListener {
            openAddExercise()
        }

        activitySummaryCard.setOnLongClickListener {
            showHealthConnectBottomSheet()
            true
        }

        activityBurnedCaloriesText.setOnClickListener {
            showHealthConnectBottomSheet()
        }

        activityDurationText.setOnClickListener {
            showHealthConnectBottomSheet()
        }


    }

    private fun applyActivePlanTodayOrOpenList() {
        when (val result = DailyActivePlanApplyAction(
            applyActivePlanToday = { dietPlanStore.applyActivePlanToday() }
        ).execute(currentActivePlan?.title)) {
            DailyActivePlanApplyAction.Result.OpenPlanList -> openDietPlans()
            is DailyActivePlanApplyAction.Result.Applied -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_active_plan_applied_today, result.planTitle),
                    Toast.LENGTH_SHORT
                ).show()

                renderActivePlan()
                renderMealTotals()
            }
        }
    }

    private fun applyNextPlannedMeal() {
        val applyRequest = DailyPlannedMealActionResolver.resolveApplyRequest(
            currentDailyMealPlan
        ) ?: return

        binding.applyPlannedMealButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.addPlannedMealEntry(
                        mealType = applyRequest.mealType,
                        title = applyRequest.title,
                        targetCalories = applyRequest.targetCalories,
                        plannedMealCalories = applyRequest.plannedMealCalories,
                        suggestedFoodNames = applyRequest.suggestedFoodNames,
                        planId = applyRequest.planId,
                        plannedMealId = applyRequest.plannedMealId,
                        entryDate = selectedDateIso
                    )
                }
            }.onSuccess {
                markPlannedMealCompleted(applyRequest.completion)

                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.daily_planned_meal_applied_message,
                        getString(applyRequest.mealTypeTitleRes)
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                renderMealTotals()
                renderActivePlan()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_planned_meal_save_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            }

            if (_binding != null) {
                binding.applyPlannedMealButton.isEnabled = true
            }
        }
    }

    private fun markNextPlannedMealCompletedIfMatches(mealType: MealType) {
        val completion = DailyPlannedMealActionResolver.resolveCompletionForMealType(
            dailyPlan = currentDailyMealPlan,
            mealType = mealType
        ) ?: return

        markPlannedMealCompleted(completion)
        renderActivePlan()
    }

    private fun markPlannedMealCompleted(
        completion: DailyPlannedMealActionResolver.Completion
    ) {
        dietPlanStore.markPlannedMealCompleted(
            planId = completion.planId,
            dayNumber = completion.dayNumber,
            plannedMealId = completion.plannedMealId
        )
    }

    fun openDietPlans() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, DietPlansFragment())
            .addToBackStack("diet_plans")
            .commit()
    }

    private fun openActivePlanDetail(planId: String) {
        if (DailyActivePlanPolicy.isCustomPlan(planId)) {
            openDietPlans()
            return
        }
        val fragment = when (planId) {
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> IntermittentFastingDetailFragment()
            else -> DietPlanDetailFragment.newInstance(planId)
        }

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, fragment)
            .addToBackStack("active_diet_plan_detail")
            .commit()
    }

    private fun openMealDetail(mealType: MealType) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, MealDetailFragment.newInstance(mealType))
            .addToBackStack(MealDetailFragment.BACK_STACK_NAME)
            .commit()
    }


    fun openReminderTarget(
        categoryName: String?,
        reminderId: String?
    ) {
        when (val target = DailyReminderTargetResolver.resolve(categoryName, reminderId)) {
            is DailyReminderTargetResolver.Target.Meal -> openAddMeal(target.mealType)
            DailyReminderTargetResolver.Target.Water -> {
                scrollToDailyCard(binding.waterTrackingCard)
                Toast.makeText(requireContext(), R.string.daily_reminder_water_focus_message, Toast.LENGTH_SHORT).show()
            }
            DailyReminderTargetResolver.Target.Exercise -> {
                scrollToDailyCard(binding.activitySummaryCard)
                Toast.makeText(requireContext(), R.string.daily_reminder_exercise_focus_message, Toast.LENGTH_SHORT).show()
            }
            DailyReminderTargetResolver.Target.DailySummary -> scrollToDailyCard(binding.dailySummaryCard)
        }
    }

    private fun scrollToDailyCard(target: View) {
        binding.dailyScrollView.post {
            val top = (target.top - 18.dp).coerceAtLeast(0)
            binding.dailyScrollView.smoothScrollTo(0, top)
            target.animate()
                .scaleX(1.015f)
                .scaleY(1.015f)
                .setDuration(120L)
                .withEndAction {
                    target.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140L)
                        .start()
                }
                .start()
        }
    }

    fun openAddMeal(mealType: MealType = MealType.BREAKFAST) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                AddMealFragment.newInstance(
                    mealType = mealType,
                    entryDate = selectedDate.toString()
                )
            )
            .addToBackStack(AddMealFragment.BACK_STACK_NAME)
            .commit()
    }
    private fun renderActivitySourceStatus(
        healthActiveCalories: Int,
        manualExerciseCalories: Int
    ) = with(binding) {
        val source = DailyActivityPolicy.resolveActivitySource(
            isHealthConnectAvailable = healthConnectSummary.isAvailable,
            hasHealthConnectPermission = healthConnectSummary.hasPermission,
            healthSteps = healthConnectSummary.steps,
            healthActiveCalories = healthActiveCalories,
            localSteps = localStepCount,
            manualExerciseCalories = manualExerciseCalories
        )

        activitySyncStatusText.text = getString(
            DailyActivitySourceTextResolver.resolveStatusTextRes(source)
        )

        activitySyncStatusText.setOnClickListener {
            showHealthConnectBottomSheet()
        }
    }
    fun openAddExercise() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                AddExerciseFragment.newInstance(
                    entryDate = selectedDate.toString()
                )
            )
            .addToBackStack("add_exercise")
            .commit()
    }
    fun openCameraFoodEntry(mealType: MealType = MealType.BREAKFAST) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                CameraFoodEntryFragment.newInstance(
                    mealType = mealType
                )
            )
            .addToBackStack(CameraFoodEntryFragment.BACK_STACK_NAME)
            .commit()
    }

    fun addQuickWater() {
        addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
    }


    override fun onDestroyView() {
        if (::stepCounterManager.isInitialized) {
            stepCounterManager.stop()
        }

        stopFastingTimer()
        stopHourglassAnimation()
        super.onDestroyView()
        _binding = null
    }
    companion object {
        private const val DEFAULT_DAILY_CALORIE_TARGET = 1800
        private const val DEFAULT_PROTEIN_TARGET_GRAMS = 120
        private const val DEFAULT_CARB_TARGET_GRAMS = 280
        private const val DEFAULT_FAT_TARGET_GRAMS = 70
        private const val DAILY_AI_MEAL_SUMMARY_MAX_CHARS = 360
        private const val DAILY_AI_RATE_LIMIT_COOLDOWN_MS = 60_000L
        private const val FITTO_AI_LOG_TAG = "FittoAI"
    }
}
