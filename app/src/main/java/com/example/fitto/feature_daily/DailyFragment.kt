package com.example.fitto.feature_daily

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.example.fitto.feature_dietplans.DietPlansFragment
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_dietplans.planning.DietMealPlanCatalog
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
import com.example.fitto.feature_daily.ai.AiDailyStatusLevel
import com.example.fitto.feature_home.presentation.profile.ProfilePreferenceStore
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_exercise.data.ExerciseLogLocalModel
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
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

class DailyFragment : Fragment(R.layout.fragment_daily) {

    private lateinit var healthConnectSummaryAction: DailyHealthConnectSummaryAction
    private var healthConnectSummary: HealthConnectDailySummary = HealthConnectDailySummary()
    private var _binding: FragmentDailyBinding? = null
    private val binding get() = _binding!!
    private lateinit var exerciseLogStore: ExerciseLogStore
    private var selectedDate: LocalDate = LocalDate.now()
    private var hourglassAnimator: ObjectAnimator? = null
    private lateinit var dateSelectionAction: DailyDateSelectionAction
    private lateinit var dashboardRefreshCoordinator: DailyDashboardRefreshCoordinator
    private lateinit var navigator: DailyNavigator
    private var currentProfileWeightKg: Double? = null
    private lateinit var sleepTrackingAction: DailySleepTrackingAction
    private lateinit var dailyAiAnalysisAction: DailyAiAnalysisAction
    private lateinit var profileLoadAction: DailyProfileLoadAction
    private lateinit var profileMetricsLoadAction: DailyProfileMetricsLoadAction
    private lateinit var dailySummaryLoadAction: DailySummaryLoadAction
    private lateinit var weeklyProgressLoadAction: DailyWeeklyProgressLoadAction
    private lateinit var waterTrackingAction: DailyWaterTrackingAction
    private lateinit var activePlanLoadAction: DailyActivePlanLoadAction
    private lateinit var plannedMealAction: DailyPlannedMealAction
    private lateinit var fastingAction: DailyFastingAction
    private lateinit var fastingCountdownCoordinator: DailyFastingCountdownCoordinator

    private var currentWeightKg: Double = DEFAULT_PROFILE_WEIGHT_KG
    private var currentWaterMl: Int = 0
    private var currentDailyAiSnapshot: DailyAiSnapshot? = null
    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var dietPlanStore: DietPlanPreferenceStore
    private lateinit var nutritionTargetStore: UserNutritionTargetStore
    private lateinit var mealRepository: MealRepository
    private lateinit var stepCounterAction: DailyStepCounterAction
    private var localStepCount: Long = 0L
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
    private var activePlanLoadGeneration: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDailyBinding.bind(view)
        dateSelectionAction = DailyDateSelectionAction()
        navigator = DailyNavigator(
            fragmentManager = parentFragmentManager,
            containerId = R.id.homeTabContainer
        )

        val appContext = requireContext().applicationContext

        dietPlanStore = DietPlanPreferenceStore(requireContext())
        nutritionTargetStore = UserNutritionTargetStore(requireContext())
        mealRepository = MealRepository(appContext)
        val dietPlanSeedRepository = DietPlanSeedRepository(appContext)
        val profileCacheRepository = ProfileCacheRepository(appContext)
        val waterTrackingStore = WaterTrackingStore(appContext)
        customDietPlanStore = CustomDietPlanStore(appContext)
        val fastingSessionStore = FastingSessionStore(requireContext())
        exerciseLogStore = ExerciseLogStore(requireContext())

        profileLoadAction = DailyProfileLoadAction(
            getCachedProfile = profileCacheRepository::getCurrentUserProfile,
            syncProfile = profileCacheRepository::syncCurrentUserProfileFromFirebase
        )
        profileMetricsLoadAction = DailyProfileMetricsLoadAction(
            loadProfile = profileLoadAction::load
        )
        dailySummaryLoadAction = DailySummaryLoadAction(
            loadProfile = profileLoadAction::load,
            getMealSummariesForDate = mealRepository::getMealSummariesForDate,
            getNutritionSummaryForDate = mealRepository::getNutritionSummaryForDate,
            getExerciseSummaryForDate = exerciseLogStore::getSummaryForDate,
            getExerciseLogsForDate = exerciseLogStore::getLogsForDate,
            getMealEntryDetailsForDate = mealRepository::getMealEntryDetailsForDate,
            resolveNutritionTargets = ::resolveDailyNutritionTargets
        )
        weeklyProgressLoadAction = DailyWeeklyProgressLoadAction(
            loadProfile = profileLoadAction::load,
            getCaloriesForDate = { dateIso ->
                mealRepository.getNutritionSummaryForDate(dateIso).totalCalories
            },
            resolveNutritionTargets = ::resolveDailyNutritionTargets
        )
        waterTrackingAction = DailyWaterTrackingAction(
            getWaterMl = waterTrackingStore::getWaterMl,
            setWaterMl = waterTrackingStore::setWaterMl,
            addWaterMl = waterTrackingStore::addWaterMl,
            getVisibleBottleCount = waterTrackingStore::getVisibleBottleCount,
            increaseVisibleBottleCount = waterTrackingStore::increaseVisibleBottleCount,
            decreaseVisibleBottleCount = waterTrackingStore::decreaseVisibleBottleCount
        )

        val customMealPlanGenerator = CustomDietMealPlanGenerator(requireContext())
        val customPlanLoadAction = DailyCustomPlanLoadAction(
            getAllFoods = mealRepository::getAllFoods,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds,
            createActivePlan = customMealPlanGenerator::createActivePlan,
            buildDailyPlan = customMealPlanGenerator::buildDailyPlan
        )
        val seedPlanLoadAction = DailySeedPlanLoadAction(
            getDietPlanById = dietPlanSeedRepository::getDietPlanById,
            getPlannedMealsForDay = dietPlanSeedRepository::getPlannedMealUiModelsForDay,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds
        )
        val catalogPlanLoadAction = DailyCatalogPlanLoadAction(
            getPlanDurationDays = DietMealPlanCatalog::getPlanDurationDays,
            getCurrentPlanDay = dietPlanStore::getCurrentPlanDay,
            getMealsForDay = DietMealPlanCatalog::getMealsForDay,
            getCompletedMealIds = dietPlanStore::getCompletedPlannedMealIds,
            getPlanTitle = { planId ->
                DietMealPlanCatalog.getPlanTitle(
                    requireContext(),
                    planId
                )
            },
            getPlanBadge = { planId ->
                DietMealPlanCatalog.getPlanBadge(
                    requireContext(),
                    planId
                )
            },
            getPlanIcon = DietMealPlanCatalog::getPlanIcon
        )
        activePlanLoadAction = DailyActivePlanLoadAction(
            getActivePlanId = dietPlanStore::getActivePlanId,
            getActiveFastingType = dietPlanStore::getActiveFastingType,
            getCustomFastingHours = dietPlanStore::getCustomFastingHours,
            getCustomPlan = customDietPlanStore::getPlan,
            createEmptyActivePlan = {
                DietPlanCatalog.createEmptyActivePlan(requireContext())
            },
            createCatalogActivePlan = { planId, fastingType, customFastingHours ->
                DietPlanCatalog.getActivePlan(
                    context = requireContext(),
                    planId = planId,
                    fastingType = fastingType,
                    customFastingHours = customFastingHours
                )
            },
            loadCustomPlan = customPlanLoadAction::load,
            loadSeedPlan = seedPlanLoadAction::load,
            loadCatalogPlan = catalogPlanLoadAction::load
        )
        plannedMealAction = DailyPlannedMealAction(
            addPlannedMealEntry = { request, entryDate ->
                mealRepository.addPlannedMealEntry(
                    mealType = request.mealType,
                    title = request.title,
                    targetCalories = request.targetCalories,
                    plannedMealCalories = request.plannedMealCalories,
                    suggestedFoodNames = request.suggestedFoodNames,
                    planId = request.planId,
                    plannedMealId = request.plannedMealId,
                    entryDate = entryDate
                )
            },
            markPlannedMealCompleted = { completion ->
                dietPlanStore.markPlannedMealCompleted(
                    planId = completion.planId,
                    dayNumber = completion.dayNumber,
                    plannedMealId = completion.plannedMealId
                )
            }
        )
        fastingAction = DailyFastingAction(
            isFastingPlan = DailyActivePlanPolicy::isFastingPlan,
            getFastingTypeName = { dietPlanStore.getActiveFastingType()?.name },
            getCustomFastingHours = dietPlanStore::getCustomFastingHours,
            getRemainingMillis = { planId ->
                fastingSessionStore.getRemainingMillis(planId)
            },
            startFasting = { planId, fastingHours ->
                fastingSessionStore.startFasting(
                    planId = planId,
                    fastingHours = fastingHours
                )
            },
            clearFasting = fastingSessionStore::clear
        )
        fastingCountdownCoordinator = DailyFastingCountdownCoordinator(
            timerFactory = AndroidDailyCountdownTimerFactory(),
            onTick = { planId, remainingMillis ->
                if (_binding != null) {
                    renderFastingCardState(
                        fastingAction.tick(planId, remainingMillis)
                    )
                }
            },
            onFinish = { planId ->
                if (_binding != null) {
                    renderFastingCardState(fastingAction.complete(planId))
                    refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
                }
            }
        )

        val healthConnectManager = HealthConnectManager(appContext)
        healthConnectSummaryAction = DailyHealthConnectSummaryAction(
            readDailySummary = healthConnectManager::readDailySummary
        )
        val stepCounterManager = FittoStepCounterManager(appContext)
        stepCounterAction = DailyStepCounterAction(
            getCachedSteps = stepCounterManager::getCachedSteps,
            isStepCounterAvailable = stepCounterManager::isStepCounterAvailable,
            startStepCounter = stepCounterManager::start,
            stopStepCounter = stepCounterManager::stop
        )
        val sleepLogStore = SleepLogStore(appContext)
        sleepTrackingAction = DailySleepTrackingAction(
            getLogForDate = sleepLogStore::getLogForDate,
            saveLog = sleepLogStore::saveLog
        )
        dailyAiAnalysisAction = DailyAiAnalysisActionFactory.create(appContext)
        dashboardRefreshCoordinator = DailyDashboardRefreshCoordinator(
            renderSelectedDate = ::renderSelectedDate,
            loadHealthConnectSummary = ::loadHealthConnectSummary,
            renderActivePlan = ::renderActivePlan,
            startLocalStepCounter = ::startLocalStepCounterIfNeeded,
            renderWaterTracking = { renderWaterTracking() },
            loadProfileMetrics = ::loadProfileMetricsAndRender,
            renderSleepCard = ::renderSleepCard,
            renderSleepWeightSummary = ::renderSleepWeightSummary,
            renderWeeklyCalorieProgress = ::renderWeeklyCalorieProgress,
            renderMealTotals = ::renderMealTotals
        )

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
        setupQuickActionResultListener()
        setupDietPlanResultListener()
        setupMealAddedResultListener()
        setupMealDetailChangedResultListener()
        setupExerciseAddedResultListener()
        setupClicks()
        setupSleepCard()
        setupDailyAiCard()

        refreshDashboard(DailyDashboardRefreshPlans.INITIAL)
    }
    private fun currentEntryDate(): String {
        return selectedDate.toString()
    }
    private fun loadProfileMetricsAndRender() {
        if (!::profileMetricsLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                profileMetricsLoadAction.load(
                    fallbackWeightKg = DEFAULT_PROFILE_WEIGHT_KG
                )
            }
            if (_binding == null) return@launch

            currentProfileWeightKg = result.profileWeightKg
            currentWeightKg = result.displayWeightKg
            waterGoalMl = result.waterGoalMl

            renderSleepWeightSummary()
            renderWaterTracking()
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
            when (
                val decision = dailyAiAnalysisAction.onCardClicked(currentDailyAiSnapshot)
            ) {
                DailyAiAnalysisAction.CardClickDecision.PrepareSnapshot -> {
                    renderDailyAiPreparingState()
                    renderMealTotals()
                }

                is DailyAiAnalysisAction.CardClickDecision.Request -> {
                    launchDailyAiRequest(decision.intent)
                }
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
        if (_binding == null || !::dailyAiAnalysisAction.isInitialized) return

        val resolution = dailyAiAnalysisAction.resolveSnapshot(
            snapshot = snapshot,
            selectedDate = selectedDate,
            today = LocalDate.now()
        )
        renderDailyAiSnapshotPresentation(resolution.presentation)
        resolution.request?.let(::launchDailyAiRequest)
    }

    private fun renderDailyAiSnapshotPresentation(
        presentation: DailyAiAnalysisAction.SnapshotPresentation
    ) {
        when (presentation) {
            DailyAiAnalysisAction.SnapshotPresentation.NoChange -> Unit
            DailyAiAnalysisAction.SnapshotPresentation.Default -> renderDailyAiDefaultState()
            is DailyAiAnalysisAction.SnapshotPresentation.Message -> {
                val subtitle = when (presentation.subtitle) {
                    DailyAiAnalysisAction.MessageSubtitle.DEFAULT -> {
                        getString(R.string.daily_ai_default_subtitle)
                    }

                    DailyAiAnalysisAction.MessageSubtitle.RESULT -> {
                        getString(R.string.daily_ai_result_subtitle)
                    }
                }
                renderDailyAiMessage(
                    message = presentation.message,
                    subtitle = subtitle,
                    status = presentation.status
                )
            }
        }
    }

    private fun launchDailyAiRequest(
        intent: DailyAiAnalysisAction.RequestIntent
    ) {
        if (_binding == null || !::dailyAiAnalysisAction.isInitialized) return

        when (val preparation = dailyAiAnalysisAction.prepareRequest(intent)) {
            is DailyAiAnalysisAction.RequestPreparation.Cached -> {
                renderDailyAiMessage(
                    message = preparation.message,
                    subtitle = getString(R.string.daily_ai_result_subtitle),
                    status = preparation.status
                )
            }

            is DailyAiAnalysisAction.RequestPreparation.BlockedByCooldown -> {
                renderDailyAiMessage(
                    message = getString(R.string.daily_ai_error_rate_limited),
                    subtitle = preparation.localMessage,
                    status = preparation.status
                )
            }

            is DailyAiAnalysisAction.RequestPreparation.AlreadyLoading -> {
                renderDailyAiLoading(preparation.status)
            }

            is DailyAiAnalysisAction.RequestPreparation.Start -> {
                renderDailyAiLoading(
                    DailyAiPromptDataMapper.localStatus(preparation.request.snapshot.triggerType)
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = dailyAiAnalysisAction.execute(preparation.request)
                    if (_binding == null) return@launch

                    when (result) {
                        is DailyAiAnalysisAction.RequestResult.Success -> {
                            renderDailyAiStructuredResult(
                                analysis = result.analysis,
                                snapshot = result.snapshot,
                                subtitle = getString(R.string.daily_ai_result_subtitle)
                            )
                        }

                        is DailyAiAnalysisAction.RequestResult.Failure -> {
                            renderDailyAiMessage(
                                message = result.localMessage,
                                subtitle = getString(
                                    DailyAiMessageTextResolver.resolveErrorMessageRes(
                                        result.failureKind
                                    )
                                ),
                                status = DailyAiPromptDataMapper.localStatus(
                                    result.snapshot.triggerType
                                )
                            )
                        }

                        DailyAiAnalysisAction.RequestResult.Stale -> {
                            renderDailyAiState(currentDailyAiSnapshot)
                        }
                    }
                }
            }
        }
    }

    private fun renderDailyAiLoading(
        status: AiDailyStatusLevel = currentDailyAiSnapshot?.let { snapshot ->
            DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
        } ?: AiDailyStatusLevel.GREEN
    ) {
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
            fallbackMessage = getString(
                DailyAiMessageTextResolver.resolveLocalMessageRes(snapshot.triggerType)
            ),
            subtitle = subtitle,
            status = DailyAiPromptDataMapper.localStatus(snapshot.triggerType)
        )
    }

    private fun showSleepTimeDialog() {
        if (!::sleepTrackingAction.isInitialized) return

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

        var editor = sleepTrackingAction.startEditing(currentEntryDate())

        fun refreshPreview() {
            val preview = sleepTrackingAction.preview(
                editor = editor,
                weightKg = currentWeightKg
            )

            bedtimeValueText.text = DailyValueFormatter.formatHourMinute(
                preview.selection.bedtimeHour,
                preview.selection.bedtimeMinute
            )
            wakeTimeValueText.text = DailyValueFormatter.formatHourMinute(
                preview.selection.wakeHour,
                preview.selection.wakeMinute
            )
            durationResultText.text = getString(
                R.string.sleep_duration_result,
                DailyValueFormatter.formatDuration(preview.durationMinutes)
            )
            caloriesResultText.text = getString(
                R.string.sleep_calories_result,
                preview.caloriesBurned
            )
        }

        bedtimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_bed),
                initialHour = editor.selection.bedtimeHour,
                initialMinute = editor.selection.bedtimeMinute
            ) { hour, minute ->
                editor = sleepTrackingAction.updateBedtime(editor, hour, minute)
                refreshPreview()
            }
        }

        wakeTimeCard.setOnClickListener {
            showTimePicker(
                title = getString(R.string.sleep_time_picker_title_wake),
                initialHour = editor.selection.wakeHour,
                initialMinute = editor.selection.wakeMinute
            ) { hour, minute ->
                editor = sleepTrackingAction.updateWakeTime(editor, hour, minute)
                refreshPreview()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val result = sleepTrackingAction.save(
                editor = editor,
                weightKg = currentWeightKg
            )
            renderSleepCard(result.cardState)
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

    private fun renderSleepCard(
        state: DailySleepCardStateResolver.State? = null
    ) {
        if (_binding == null || !::sleepTrackingAction.isInitialized) return

        val sleepCardState = state ?: sleepTrackingAction.loadCard(currentEntryDate())

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
        if (_binding == null || !::weeklyProgressLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val requestedDate = selectedDate
            val result = withContext(Dispatchers.IO) {
                weeklyProgressLoadAction.load(
                    DailyWeeklyProgressLoadRequest(
                        selectedDate = requestedDate,
                        minHeightPx = 18.dp,
                        maxHeightPx = 90.dp
                    )
                )
            }

            if (_binding == null || selectedDate != requestedDate) return@launch

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

            val weeklyProgress = result.progress

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

            result.days.forEachIndexed { index, date ->
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

        when (val state = DailyWeightCardStateResolver.resolve(currentProfileWeightKg)) {
            DailyWeightCardStateResolver.State.Unknown -> {
                binding.weightValueText.text = getString(R.string.daily_weight_unknown_short)
                binding.weightStatusText.text = getString(R.string.daily_weight_not_saved)
            }

            is DailyWeightCardStateResolver.State.Available -> {
                binding.weightValueText.text = getString(
                    R.string.daily_weight_value_format,
                    DailyValueFormatter.formatWeightValue(state.weightKg)
                )
                binding.weightStatusText.text = getString(R.string.daily_weight_from_profile)
            }
        }
    }

    private fun renderWaterTracking(
        scrollToEnd: Boolean = false,
        scrollToStart: Boolean = false
    ) {
        if (_binding == null || !::waterTrackingAction.isInitialized) return

        val result = waterTrackingAction.load(
            DailyWaterTrackingRequest(
                date = selectedDate,
                waterGoalMl = waterGoalMl
            )
        )
        val waterState = result.waterState
        this.currentWaterMl = waterState.normalizedWaterMl

        binding.waterAmountText.text = getString(
            R.string.daily_water_value_format,
            waterState.normalizedWaterMl / 1000.0,
            waterGoalMl / 1000.0
        )

        binding.waterBottleContainer.removeAllViews()

        val waterBottleUiState = result.bottleUiState

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

                val removed = waterTrackingAction.removeBottleSlot(
                    date = selectedDate,
                    state = state
                )

                if (removed) {
                    renderWaterTracking(scrollToStart = true)
                }
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
        val added = waterTrackingAction.addWater(
            date = selectedDate,
            amountMl = amountMl
        )

        if (added) {
            renderWaterTracking()
        }
    }

    private fun toggleWaterBottle(isFilled: Boolean) {
        waterTrackingAction.toggleBottle(
            date = selectedDate,
            isFilled = isFilled
        )
        renderWaterTracking()
    }

    private fun startLocalStepCounterIfNeeded() {
        if (!::stepCounterAction.isInitialized) return

        val requiresActivityRecognitionPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val hasActivityRecognitionPermission = !requiresActivityRecognitionPermission ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        val effect = stepCounterAction.start(
            request = DailyStepCounterRequest(
                selectedDate = selectedDate,
                today = LocalDate.now(),
                requiresActivityRecognitionPermission = requiresActivityRecognitionPermission,
                hasActivityRecognitionPermission = hasActivityRecognitionPermission
            )
        ) { update ->
            localStepCount = update.steps
            renderActivityStepsOnly()

            if (update.shouldRefreshSummary) {
                renderMealTotals()
            }
        }

        when (effect) {
            DailyStepCounterEffect.None -> Unit
            DailyStepCounterEffect.SensorUnavailable -> Toast.makeText(
                requireContext(),
                getString(R.string.step_counter_sensor_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            DailyStepCounterEffect.RequestActivityRecognitionPermission -> {
                activityRecognitionPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
        }
    }

    private fun resolvedDisplaySteps(): Long {
        return DailyActivityPresentationResolver.resolveDisplaySteps(
            healthConnectSummary = healthConnectSummary,
            localSteps = localStepCount
        )
    }

    private fun renderActivityStepsOnly() {
        if (_binding == null) return

        binding.activityStepCountText.text = DailyValueFormatter.formatStepCount(
            resolvedDisplaySteps()
        )
    }

    private fun setupQuickActionResultListener() {
        childFragmentManager.setFragmentResultListener(
            DailyQuickAddBottomSheet.REQUEST_QUICK_ACTION,
            viewLifecycleOwner
        ) { _, bundle ->
            val action = DailyQuickAction.fromValue(
                bundle.getString(DailyQuickAddBottomSheet.BUNDLE_QUICK_ACTION)
            ) ?: return@setFragmentResultListener

            when (val destination = DailyQuickActionDestinationResolver.resolve(action)) {
                is DailyQuickActionDestination.AddMeal -> openAddMeal(destination.mealType)
                is DailyQuickActionDestination.CameraFood -> {
                    openCameraFoodEntry(destination.mealType)
                }
                DailyQuickActionDestination.AddWater -> {
                    addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
                }
                DailyQuickActionDestination.AddExercise -> openAddExercise()
                DailyQuickActionDestination.DietPlans -> openDietPlans()
            }
        }
    }

    private fun setupHealthConnectResultListener() {
        childFragmentManager.setFragmentResultListener(
            HealthConnectBottomSheetFragment.REQUEST_HEALTH_CONNECT_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            refreshDashboard(DailyDashboardRefreshPlans.HEALTH_CONNECT_CHANGED)
        }
    }

    private fun showHealthConnectBottomSheet() {
        HealthConnectBottomSheetFragment()
            .show(childFragmentManager, "health_connect")
    }

    private fun loadHealthConnectSummary() {
        if (!::healthConnectSummaryAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val requestedDate = selectedDate
            val result = healthConnectSummaryAction.load(requestedDate)
            if (_binding == null || selectedDate != result.requestedDate) return@launch

            healthConnectSummary = result.summary
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
            val effect = DailyResultEffectResolver.resolveExerciseAdded(
                currentDate = selectedDate,
                entryDate = bundle.getString(AddExerciseFragment.BUNDLE_EXERCISE_DATE)
            )

            applyDateSelectionEffect(effect.dateSelectionEffect)
            refreshDashboard(effect.refreshPlan)
        }
    }
    override fun onResume() {
        super.onResume()
        if (_binding != null && ::dashboardRefreshCoordinator.isInitialized) {
            refreshDashboard(DailyDashboardRefreshPlans.RESUME)
        }
    }
    override fun onPause() {
        if (::stepCounterAction.isInitialized) {
            stepCounterAction.stop()
        }
        super.onPause()
    }
    private fun openDatePicker() {
        val pickerState = dateSelectionAction.pickerState(selectedDate)

        val constraints = CalendarConstraints.Builder()
            .setEnd(pickerState.latestUtcMillis)
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlay_Fitto_MaterialDatePicker)
            .setTitleText(getString(R.string.daily_date_picker_title))
            .setSelection(pickerState.selectedUtcMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selectedUtcMillis ->
            val pickedDate = dateSelectionAction.dateFromUtcMillis(selectedUtcMillis)

            when (val result = dateSelectionAction.selectPickedDate(pickedDate)) {
                DailyDateSelectionAction.PickedDateResult.FutureDate -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.daily_future_date_blocked),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is DailyDateSelectionAction.PickedDateResult.Selected -> {
                    updateSelectedDate(result.date)
                    refreshDashboard(result.refreshPlan)
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

    private fun refreshDashboard(plan: DailyDashboardRefreshPlan) {
        if (_binding == null || !::dashboardRefreshCoordinator.isInitialized) return
        dashboardRefreshCoordinator.refresh(plan)
    }

    private fun updateSelectedDate(date: LocalDate) {
        selectedDate = date
        renderSelectedDate()
    }

    private fun renderSelectedDate() = with(binding) {
        val selectedDateText = DailySelectedDateTextResolver.resolve(selectedDate)
        dailySelectedDateText.text = if (selectedDateText.isToday) {
            getString(R.string.daily_selected_date_today, selectedDateText.formattedDate)
        } else {
            getString(R.string.daily_selected_date_format, selectedDateText.formattedDate)
        }
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
            refreshDashboard(DailyResultEffectResolver.resolveDietPlanChanged())
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
            refreshDashboard(effect.refreshPlan)
        }
    }

    private fun setupMealDetailChangedResultListener() {
        parentFragmentManager.setFragmentResultListener(
            MealDetailFragment.REQUEST_MEAL_DETAIL_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            refreshDashboard(DailyResultEffectResolver.resolveMealDetailChanged())
        }
    }

    private fun renderActivePlan() {
        if (_binding == null || !::activePlanLoadAction.isInitialized) return

        val loadGeneration = ++activePlanLoadGeneration
        val targetCalories = currentDailyTargetCalories

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                activePlanLoadAction.load(targetCalories)
            }

            if (_binding == null || loadGeneration != activePlanLoadGeneration) {
                return@launch
            }

            currentActivePlan = result.activePlan
            currentDailyMealPlan = result.dailyPlan

            when (result) {
                is DailyActivePlanLoadResult.Basic -> renderBasicActivePlan(result.displayPlan)
                is DailyActivePlanLoadResult.Planned -> renderDailyMealPlan(result.dailyPlan)
            }
        }
    }

    private fun renderFastingActionsIfNeeded(planId: String) {
        when (val result = fastingAction.load(planId)) {
            DailyFastingAction.LoadResult.Hidden -> hideFastingCard()
            is DailyFastingAction.LoadResult.Visible -> {
                binding.fastingActionContainer.isVisible = true
                binding.fastingAnalysisContainer.isVisible = true
                binding.fastingActionContainer.setOnClickListener {
                    // Keep taps in the fasting control area from opening plan details.
                }

                renderFastingCardState(result.state)
                startFastingTimerIfNeeded(result.state)
            }
        }
    }

    private fun renderFastingCardState(state: DailyFastingCardState) = with(binding) {
        fastingMealSuggestionText.text = buildString {
            append(getString(R.string.daily_fasting_meal_suggestion_title))
            append("\n")
            append(getString(state.mealSuggestionRes))
        }

        val countdownState = state.countdownState
        fastingCountdownTitleText.text = getString(countdownState.titleRes)
        fastingCountdownText.text = countdownState.messageRes?.let(::getString)
            ?: countdownState.remainingMillis
                ?.let(DailyValueFormatter::formatRemainingTime)
                .orEmpty()
        startFastingButton.isEnabled = countdownState.isButtonEnabled
        startFastingButton.text = getString(countdownState.buttonTextRes)
        startFastingButton.setOnClickListener {
            when (countdownState.buttonAction) {
                DailyFastingUiStateResolver.ButtonAction.START -> startFasting(state.planId)
                DailyFastingUiStateResolver.ButtonAction.FINISH -> finishFasting(state.planId)
                DailyFastingUiStateResolver.ButtonAction.NONE -> Unit
            }
        }

        if (countdownState.shouldAnimateHourglass) {
            startHourglassAnimation()
        } else {
            stopHourglassAnimation()
        }

        fastingAnalysisTitleText.text = getString(state.analysisState.titleRes)
        fastingAnalysisRangeText.text = getString(state.analysisState.rangeTitleRes)
        fastingAnalysisBodyText.text = getString(state.analysisState.bodyTextRes)
    }

    private fun startFasting(planId: String) {
        when (val result = fastingAction.start(planId)) {
            DailyFastingAction.StartResult.Unavailable -> hideFastingCard()
            is DailyFastingAction.StartResult.Started -> {
                renderFastingCardState(result.state)
                fastingCountdownCoordinator.start(planId, result.remainingMillis)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_fasting_started_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun finishFasting(planId: String) {
        fastingCountdownCoordinator.stop()
        renderFastingCardState(fastingAction.finish(planId))

        Toast.makeText(
            requireContext(),
            getString(R.string.daily_fasting_finished_message),
            Toast.LENGTH_SHORT
        ).show()

        refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
    }

    private fun startFastingTimerIfNeeded(state: DailyFastingCardState) {
        val remainingMillis = state.countdownState.remainingMillis
        if (remainingMillis != null && remainingMillis > 0L) {
            fastingCountdownCoordinator.start(state.planId, remainingMillis)
        } else {
            fastingCountdownCoordinator.stop()
        }
    }

    private fun hideFastingCard() = with(binding) {
        fastingActionContainer.isVisible = false
        fastingAnalysisContainer.isVisible = false
        fastingCountdownCoordinator.stop()
        stopHourglassAnimation()
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
            }

            is DailyBasicActivePlanUiStateResolver.Content.Metrics -> {
                activePlanMetricsContainer.isVisible = true
                hideFastingCard()

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
        hideFastingCard()

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
        if (_binding == null || !::dailySummaryLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val requestedDate = selectedDate
            val selectedDateIso = requestedDate.toString()
            val result = withContext(Dispatchers.IO) {
                dailySummaryLoadAction.load(
                    DailySummaryLoadRequest(
                        dateIso = selectedDateIso,
                        healthConnectSummary = healthConnectSummary,
                        displaySteps = resolvedDisplaySteps(),
                        fallbackWeightKg = currentWeightKg
                    )
                )
            }
            if (_binding == null || selectedDate != requestedDate) return@launch

            val profile = result.profile
            val nutritionTargets = result.nutritionTargets
            val targetCalories = nutritionTargets.dailyCalories

            val oldTargetCalories = currentDailyTargetCalories

            currentDailyTargetCalories = targetCalories
            currentProteinTargetGrams = nutritionTargets.proteinGrams
            currentCarbTargetGrams = nutritionTargets.carbGrams
            currentFatTargetGrams = nutritionTargets.fatGrams

            val byMealType = result.mealSummariesByType
            val mealCalories = result.mealCalories

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

            currentProfileWeightKg = result.resolvedWeightKg
            currentWeightKg = result.displayWeightKg

            val summaryState = result.summaryState
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
                mealDetailsByType = result.mealDetailsByType
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
                exerciseLogCount = result.exerciseLogCount,
                exerciseSummaryText = buildDailyAiExerciseSummary(
                    result.exerciseLogs,
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
                refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
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
            waterTrackingAction.addBottleSlots(
                date = selectedDate,
                waterGoalMl = waterGoalMl
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

                refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_APPLIED)
            }
        }
    }

    private fun applyNextPlannedMeal() {
        val applyRequest = plannedMealAction.prepare(currentDailyMealPlan) ?: return
        val entryDate = currentEntryDate()

        binding.applyPlannedMealButton.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                plannedMealAction.execute(
                    request = applyRequest,
                    entryDate = entryDate
                )
            }

            when (result) {
                is DailyPlannedMealExecutionResult.Success -> {
                    val effect = result.effect

                    Toast.makeText(
                        requireContext(),
                        getString(effect.messageRes, getString(effect.mealTypeTitleRes)),
                        Toast.LENGTH_SHORT
                    ).show()

                    val refreshSteps = buildList {
                        if (effect.refreshMealTotals) {
                            add(DailyDashboardRefreshStep.RENDER_MEAL_TOTALS)
                        }
                        if (effect.refreshActivePlan) {
                            add(DailyDashboardRefreshStep.RENDER_ACTIVE_PLAN)
                        }
                    }
                    refreshDashboard(
                        DailyDashboardRefreshPlan.of(*refreshSteps.toTypedArray())
                    )
                }

                is DailyPlannedMealExecutionResult.Failure -> {
                    Toast.makeText(
                        requireContext(),
                        getString(result.effect.messageRes),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            if (_binding != null) {
                binding.applyPlannedMealButton.isEnabled = true
            }
        }
    }

    private fun markNextPlannedMealCompletedIfMatches(mealType: MealType) {
        val wasCompleted = plannedMealAction.completeNextMealIfMatches(
            dailyPlan = currentDailyMealPlan,
            mealType = mealType
        )

        if (wasCompleted) {
            refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
        }
    }

    private fun openDietPlans() {
        navigator.openDietPlans()
    }

    private fun openActivePlanDetail(planId: String) {
        navigator.openActivePlanDetail(planId)
    }

    private fun openMealDetail(mealType: MealType) {
        navigator.openMealDetail(mealType)
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

    private fun openAddMeal(mealType: MealType = MealType.BREAKFAST) {
        navigator.openAddMeal(
            mealType = mealType,
            entryDate = currentEntryDate()
        )
    }

    private fun renderActivitySourceStatus(
        healthActiveCalories: Int,
        manualExerciseCalories: Int
    ) = with(binding) {
        val source = DailyActivityPresentationResolver.resolveSource(
            healthConnectSummary = healthConnectSummary,
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
    private fun openAddExercise() {
        navigator.openAddExercise(entryDate = currentEntryDate())
    }

    private fun openCameraFoodEntry(mealType: MealType = MealType.BREAKFAST) {
        navigator.openCameraFoodEntry(mealType)
    }


    override fun onDestroyView() {
        if (::stepCounterAction.isInitialized) {
            stepCounterAction.stop()
        }

        if (::fastingCountdownCoordinator.isInitialized) {
            fastingCountdownCoordinator.stop()
        }
        stopHourglassAnimation()
        super.onDestroyView()
        _binding = null
    }
    companion object {
        private const val DEFAULT_DAILY_CALORIE_TARGET = 1800
        private const val DEFAULT_PROTEIN_TARGET_GRAMS = 120
        private const val DEFAULT_CARB_TARGET_GRAMS = 280
        private const val DEFAULT_FAT_TARGET_GRAMS = 70
        private const val DEFAULT_PROFILE_WEIGHT_KG = 70.0
        private const val DAILY_AI_MEAL_SUMMARY_MAX_CHARS = 360
    }
}
