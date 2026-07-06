package com.example.fitto.feature_daily

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitto.R
import com.example.fitto.core.coroutines.CoroutineTaskRunner
import com.example.fitto.databinding.BottomSheetDailyAiRecommendationReasonBinding
import com.example.fitto.databinding.FragmentDailyBinding
import com.example.fitto.feature_daily.data.WaterTrackingStore
import com.example.fitto.feature_dietplans.model.ActiveDietPlanUiModel
import com.example.fitto.feature_dietplans.planning.DailyPlannedMealsUiModel
import com.example.fitto.feature_health.HealthConnectBottomSheetFragment
import com.example.fitto.feature_health.HealthConnectDailySummary
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.fittoAppContainer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDate
import kotlinx.coroutines.launch

class DailyFragment : Fragment(R.layout.fragment_daily) {

    private lateinit var healthConnectSummaryAction: DailyHealthConnectSummaryAction
    private lateinit var backgroundTaskRunner: CoroutineTaskRunner
    private var _binding: FragmentDailyBinding? = null
    private val binding get() = _binding!!
    private val dashboardStateStore = DailyDashboardStateStore(
        initialDate = LocalDate.now(),
        initialNutritionTargets = DailyDashboardNutritionTargets(
            calories = DEFAULT_DAILY_CALORIE_TARGET,
            proteinGrams = DEFAULT_PROTEIN_TARGET_GRAMS,
            carbGrams = DEFAULT_CARB_TARGET_GRAMS,
            fatGrams = DEFAULT_FAT_TARGET_GRAMS
        ),
        initialDisplayWeightKg = DEFAULT_PROFILE_WEIGHT_KG,
        initialWaterGoalMl = DEFAULT_WATER_GOAL_ML
    )
    private val dashboardState: DailyDashboardScreenState
        get() = dashboardStateStore.current()
    private val healthConnectSummary: HealthConnectDailySummary
        get() = dashboardState.healthConnectSummary
    private val selectedDate: LocalDate
        get() = dashboardState.selectedDate
    private val currentProfileWeightKg: Double?
        get() = dashboardState.profileWeightKg
    private val currentWeightKg: Double
        get() = dashboardState.displayWeightKg
    private val currentWaterMl: Int
        get() = dashboardState.waterMl
    private val localStepCount: Long
        get() = dashboardState.localSteps
    private val waterGoalMl: Int
        get() = dashboardState.waterGoalMl
    private val currentActivePlan: ActiveDietPlanUiModel?
        get() = dashboardState.activePlan
    private val currentDailyMealPlan: DailyPlannedMealsUiModel?
        get() = dashboardState.dailyMealPlan
    private lateinit var dashboardRefreshCoordinator: DailyDashboardRefreshCoordinator
    private lateinit var navigator: DailyNavigator
    private lateinit var sleepTrackingAction: DailySleepTrackingAction
    private lateinit var dailyAiDashboardCoordinator: DailyAiDashboardCoordinator
    private lateinit var profileMetricsLoadAction: DailyProfileMetricsLoadAction
    private lateinit var dailySummaryLoadAction: DailySummaryLoadAction
    private lateinit var weeklyProgressLoadAction: DailyWeeklyProgressLoadAction
    private lateinit var waterTrackingAction: DailyWaterTrackingAction
    private lateinit var activePlanLoadAction: DailyActivePlanLoadAction
    private lateinit var plannedMealAction: DailyPlannedMealAction
    private lateinit var activePlanApplyAction: DailyActivePlanApplyAction
    private lateinit var fastingAction: DailyFastingAction
    private lateinit var fastingCountdownCoordinator: DailyFastingCountdownCoordinator
    private var sleepWeightRenderer: DailySleepWeightRenderer? = null
    private var weeklyProgressRenderer: DailyWeeklyProgressRenderer? = null
    private var waterRenderer: DailyWaterRenderer? = null
    private var summaryRenderer: DailySummaryRenderer? = null
    private var activePlanRenderer: DailyActivePlanRenderer? = null
    private var plannedMealRenderer: DailyPlannedMealRenderer? = null
    private var fastingRenderer: DailyFastingRenderer? = null
    private var dailyAiCardRenderer: DailyAiCardRenderer? = null
    private var isDailyAiRecommendationApplied: Boolean = false
    private var dashboardClickBinder: DailyDashboardClickBinder? = null
    private var sleepDialogCoordinator: DailySleepDialogCoordinator? = null
    private lateinit var datePickerCoordinator: DailyDatePickerCoordinator

    private lateinit var stepCounterAction: DailyStepCounterAction
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDailyBinding.bind(view)
        val dateSelectionAction = DailyDateSelectionAction()
        navigator = DailyNavigator(
            fragmentManager = parentFragmentManager,
            containerId = R.id.homeTabContainer
        )

        val dependencies = requireContext().fittoAppContainer.createDailyDashboardDependencies(
            defaultTargets = DailyDashboardDefaultTargets(
                calories = DEFAULT_DAILY_CALORIE_TARGET,
                proteinGrams = DEFAULT_PROTEIN_TARGET_GRAMS,
                carbGrams = DEFAULT_CARB_TARGET_GRAMS,
                fatGrams = DEFAULT_FAT_TARGET_GRAMS
            ),
            onFastingTick = { planId, remainingMillis ->
                if (_binding != null) {
                    renderFastingCardState(
                        fastingAction.tick(planId, remainingMillis)
                    )
                }
            },
            onFastingFinish = { planId ->
                if (_binding != null) {
                    renderFastingCardState(fastingAction.complete(planId))
                    refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
                }
            }
        )
        dashboardStateStore.updateDailyCalorieTarget(
            dependencies.initialDailyTargetCalories
        )
        backgroundTaskRunner = dependencies.backgroundTaskRunner
        profileMetricsLoadAction = dependencies.profileMetricsLoadAction
        dailySummaryLoadAction = dependencies.dailySummaryLoadAction
        weeklyProgressLoadAction = dependencies.weeklyProgressLoadAction
        waterTrackingAction = dependencies.waterTrackingAction
        activePlanLoadAction = dependencies.activePlanLoadAction
        plannedMealAction = dependencies.plannedMealAction
        activePlanApplyAction = dependencies.activePlanApplyAction
        fastingAction = dependencies.fastingAction
        fastingCountdownCoordinator = dependencies.fastingCountdownCoordinator
        healthConnectSummaryAction = dependencies.healthConnectSummaryAction
        stepCounterAction = dependencies.stepCounterAction
        sleepTrackingAction = dependencies.sleepTrackingAction
        dailyAiDashboardCoordinator = dependencies.dailyAiDashboardCoordinator
        sleepWeightRenderer = DailySleepWeightRenderer(binding, requireContext())
        weeklyProgressRenderer = DailyWeeklyProgressRenderer(binding, requireContext())
        waterRenderer = DailyWaterRenderer(
            binding = binding,
            context = requireContext(),
            onRemoveBottle = { state ->
                val removed = waterTrackingAction.removeBottleSlot(
                    date = selectedDate,
                    state = state
                )
                if (removed) {
                    renderWaterTracking(DailyWaterScrollTarget.START)
                }
            },
            onToggleBottle = ::toggleWaterBottle
        )
        summaryRenderer = DailySummaryRenderer(
            binding = binding,
            context = requireContext(),
            onActivitySourceClick = ::showHealthConnectBottomSheet
        )
        activePlanRenderer = DailyActivePlanRenderer(binding)
        plannedMealRenderer = DailyPlannedMealRenderer(
            binding = binding,
            context = requireContext()
        )
        fastingRenderer = DailyFastingRenderer(
            binding = binding,
            context = requireContext(),
            onStart = ::startFasting,
            onFinish = ::finishFasting
        )
        dailyAiCardRenderer = DailyAiCardRenderer(binding, requireContext())
        sleepDialogCoordinator = DailySleepDialogCoordinator(
            context = requireContext(),
            fragmentManager = childFragmentManager,
            sleepTrackingAction = sleepTrackingAction,
            onSaved = { state ->
                if (_binding != null) {
                    renderSleepCard(state)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.sleep_saved_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        datePickerCoordinator = DailyDatePickerCoordinator(
            context = requireContext(),
            fragmentManager = parentFragmentManager,
            dateSelectionAction = dateSelectionAction,
            onDateSelected = { date, refreshPlan ->
                updateSelectedDate(date)
                refreshDashboard(refreshPlan)
            },
            onFutureDateSelected = {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.daily_future_date_blocked),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
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

        initializeDashboardView()
        bindDashboardClicks()
        registerDashboardResultListeners()

        refreshDashboard(DailyDashboardRefreshPlans.INITIAL)
    }
    private fun currentEntryDate(): String {
        return selectedDate.toString()
    }
    private fun loadProfileMetricsAndRender() {
        if (!::profileMetricsLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = backgroundTaskRunner.run {
                profileMetricsLoadAction.load(
                    fallbackWeightKg = DEFAULT_PROFILE_WEIGHT_KG
                )
            }
            if (_binding == null) return@launch

            dashboardStateStore.updateProfileMetrics(
                profileWeightKg = result.profileWeightKg,
                displayWeightKg = result.displayWeightKg,
                waterGoalMl = result.waterGoalMl
            )

            renderSleepWeightSummary()
            renderWaterTracking()
            renderMealTotals()
        }
    }

    private fun bindDashboardClicks() {
        dashboardClickBinder = DailyDashboardClickBinder(
            binding = binding,
            callbacks = DailyDashboardClickBinder.Callbacks(
                onDateClick = { datePickerCoordinator.show(selectedDate) },
                onQuickAddClick = ::showQuickAddBottomSheet,
                onDietPlansClick = ::openDietPlans,
                onApplyTodayPlanClick = ::applyActivePlanTodayOrOpenList,
                onActivePlanClick = ::handleActivePlanClick,
                onApplyPlannedMealClick = ::applyNextPlannedMeal,
                onMealDetailClick = ::openMealDetail,
                onAddMealClick = ::openAddMeal,
                onAddSmallWaterClick = {
                    addWater(WaterTrackingStore.BOTTLE_SIZE_ML)
                },
                onAddLargeWaterClick = {
                    addWater(WaterTrackingStore.BOTTLE_SIZE_ML * 2)
                },
                onAddBottleSlotClick = ::addBottleSlot,
                onActivityClick = ::openAddExercise,
                onHealthConnectClick = ::showHealthConnectBottomSheet,
                onSleepClick = {
                    sleepDialogCoordinator?.show(
                        entryDate = currentEntryDate(),
                        weightKg = currentWeightKg
                    )
                },
                onDailyAiClick = ::handleDailyAiCardClick,
                onDailyAiApplyClick = ::handleDailyAiSuggestionApply,
                onDailyAiReasonClick = ::showDailyAiRecommendationReason
            )
        ).also(DailyDashboardClickBinder::bind)
    }

    private fun registerDashboardResultListeners() {
        DailyDashboardResultRegistrar(
            parentFragmentManager = parentFragmentManager,
            childFragmentManager = childFragmentManager,
            lifecycleOwner = viewLifecycleOwner,
            callbacks = DailyDashboardResultRegistrar.Callbacks(
                currentDate = { selectedDate },
                onQuickActionDestination = ::handleQuickActionDestination,
                onDateSelection = ::applyDateSelectionEffect,
                onMealAdded = ::markNextPlannedMealCompletedIfMatches,
                onRefresh = ::refreshDashboard
            )
        ).register()
    }

    private fun handleQuickActionDestination(destination: DailyQuickActionDestination) {
        when (destination) {
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

    private fun showQuickAddBottomSheet() {
        DailyQuickAddBottomSheet().show(
            childFragmentManager,
            DailyQuickAddBottomSheet.TAG
        )
    }

    private fun handleActivePlanClick() {
        currentActivePlan
            ?.let { activePlan -> openActivePlanDetail(activePlan.id) }
            ?: openDietPlans()
    }

    private fun addBottleSlot() {
        waterTrackingAction.addBottleSlots(
            date = selectedDate,
            waterGoalMl = waterGoalMl
        )
        renderWaterTracking(DailyWaterScrollTarget.END)
    }

    private fun handleDailyAiCardClick() {
        if (!::dailyAiDashboardCoordinator.isInitialized) return
        renderDailyAiEffects(dailyAiDashboardCoordinator.onCardClicked())
    }

    private fun handleDailyAiSuggestionApply() {
        isDailyAiRecommendationApplied = true
        dailyAiCardRenderer?.renderAppliedState()
        Snackbar.make(
            binding.dailyRoot,
            R.string.daily_ai_recommendation_applied_snackbar,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showDailyAiRecommendationReason() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDailyAiRecommendationReasonBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        sheetBinding.dailyAiReasonRemainingMetricText.text = getString(
            R.string.daily_ai_reason_remaining_metric_format,
            binding.remainingCaloriesText.text
        )
        sheetBinding.dailyAiReasonProteinMetricText.text = binding.dailyProteinValueText.text
        sheetBinding.dailyAiReasonWaterMetricText.text = binding.waterAmountText.text

        sheetBinding.dailyAiReasonApplyButton.setOnClickListener {
            dialog.dismiss()
            handleDailyAiSuggestionApply()
        }
        sheetBinding.dailyAiReasonCloseButton.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun renderDailyAiEffects(
        effects: List<DailyAiDashboardCoordinator.Effect>
    ) {
        if (_binding == null) return

        effects.forEach { effect ->
            when (effect) {
                DailyAiDashboardCoordinator.Effect.Default -> {
                    dailyAiCardRenderer?.renderDefaultState()
                }

                DailyAiDashboardCoordinator.Effect.Preparing -> {
                    dailyAiCardRenderer?.renderPreparingState()
                }

                DailyAiDashboardCoordinator.Effect.RefreshSnapshot -> {
                    renderMealTotals()
                }

                is DailyAiDashboardCoordinator.Effect.Loading -> {
                    dailyAiCardRenderer?.renderLoading(effect.status)
                }

                is DailyAiDashboardCoordinator.Effect.Message -> {
                    val subtitle = when (effect.subtitle) {
                        DailyAiDashboardCoordinator.Subtitle.DEFAULT -> {
                            getString(R.string.daily_ai_default_subtitle)
                        }

                        DailyAiDashboardCoordinator.Subtitle.RESULT -> {
                            getString(R.string.daily_ai_result_subtitle)
                        }
                    }
                    dailyAiCardRenderer?.renderMessage(
                        message = effect.message,
                        subtitle = subtitle,
                        status = effect.status
                    )
                }

                is DailyAiDashboardCoordinator.Effect.RateLimited -> {
                    dailyAiCardRenderer?.renderMessage(
                        message = getString(R.string.daily_ai_error_rate_limited),
                        subtitle = effect.localMessage,
                        status = effect.status
                    )
                }

                is DailyAiDashboardCoordinator.Effect.Failure -> {
                    dailyAiCardRenderer?.renderMessage(
                        message = effect.localMessage,
                        subtitle = getString(
                            DailyAiMessageTextResolver.resolveErrorMessageRes(
                                effect.failureKind
                            )
                        ),
                        status = effect.status
                    )
                }

                is DailyAiDashboardCoordinator.Effect.StructuredResult -> {
                    dailyAiCardRenderer?.renderStructuredResult(
                        analysis = effect.analysis,
                        fallbackMessage = getString(
                            DailyAiMessageTextResolver.resolveLocalMessageRes(
                                effect.snapshot.triggerType
                            )
                        ),
                        subtitle = getString(R.string.daily_ai_result_subtitle),
                        status = DailyAiPromptDataMapper.localStatus(
                            effect.snapshot.triggerType
                        )
                    )
                }

                is DailyAiDashboardCoordinator.Effect.Execute -> {
                    launchDailyAiRequest(effect.request)
                }
            }
        }

        if (isDailyAiRecommendationApplied) {
            dailyAiCardRenderer?.renderAppliedState()
        }
    }

    private fun launchDailyAiRequest(
        request: DailyAiAnalysisAction.PreparedRequest
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = dailyAiDashboardCoordinator.execute(request)
            if (_binding == null) return@launch

            renderDailyAiEffects(
                dailyAiDashboardCoordinator.onRequestCompleted(
                    result = result,
                    selectedDate = selectedDate
                )
            )
        }
    }

    private fun renderSleepCard(
        state: DailySleepCardStateResolver.State? = null
    ) {
        if (_binding == null || !::sleepTrackingAction.isInitialized) return

        val sleepCardState = state ?: sleepTrackingAction.loadCard(currentEntryDate())
        sleepWeightRenderer?.renderSleep(sleepCardState)
    }

    private fun renderWeeklyCalorieProgress() {
        if (_binding == null || !::weeklyProgressLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val requestedDate = selectedDate
            val result = backgroundTaskRunner.run {
                weeklyProgressLoadAction.load(
                    DailyWeeklyProgressLoadRequest(
                        selectedDate = requestedDate,
                        minHeightPx = 18.dp,
                        maxHeightPx = 90.dp
                    )
                )
            }

            if (_binding == null || !dashboardStateStore.isSelectedDate(requestedDate)) return@launch

            weeklyProgressRenderer?.render(result)
        }
    }

    private fun renderSleepWeightSummary() {
        if (_binding == null) return

        renderSleepCard()
        sleepWeightRenderer?.renderWeight(
            DailyWeightCardStateResolver.resolve(currentProfileWeightKg)
        )
    }

    private fun renderWaterTracking(
        scrollTarget: DailyWaterScrollTarget = DailyWaterScrollTarget.NONE
    ) {
        if (_binding == null || !::waterTrackingAction.isInitialized) return

        val result = waterTrackingAction.load(
            DailyWaterTrackingRequest(
                date = selectedDate,
                waterGoalMl = waterGoalMl
            )
        )
        dashboardStateStore.updateWaterMl(result.waterState.normalizedWaterMl)

        waterRenderer?.render(
            result = result,
            waterGoalMl = waterGoalMl,
            scrollTarget = scrollTarget
        )

        refreshDailyAiSnapshotForWater()
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
            dashboardStateStore.updateLocalSteps(update.steps)
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

        summaryRenderer?.renderSteps(resolvedDisplaySteps())
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
            if (_binding == null) return@launch
            if (!dashboardStateStore.updateHealthConnectSummary(result.requestedDate, result.summary)) {
                return@launch
            }

            renderHealthConnectSummary()
        }
    }
    private fun renderHealthConnectSummary() {
        // Health Connect verisi geldikten sonra ana günlük hesapları tekrar çiziyoruz.
        // Böylece yakılan kalori, kalan kalori ve aktivite kartı aynı yerden güncelleniyor.
        renderMealTotals()
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
        dashboardStateStore.selectDate(date)
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

    private fun initializeDashboardView() = with(binding) {
        dailyCalorieProgress.progress = 0
        dailyCarbProgress.progress = 0
        dailyProteinProgress.progress = 0
        dailyFatProgress.progress = 0
        weeklyWaterProgress.progress = 0
        dailyAiCardRenderer?.renderDefaultState()
    }

    private fun renderActivePlan() {
        if (_binding == null || !::activePlanLoadAction.isInitialized) return

        val loadToken = dashboardStateStore.beginActivePlanLoad()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = backgroundTaskRunner.run {
                activePlanLoadAction.load(loadToken.targetCalories)
            }

            if (_binding == null) return@launch
            if (!dashboardStateStore.applyActivePlanLoad(loadToken, result)) return@launch

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
                renderFastingCardState(result.state)
                startFastingTimerIfNeeded(result.state)
            }
        }
    }

    private fun renderFastingCardState(state: DailyFastingCardState) {
        fastingRenderer?.render(state)
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

    private fun hideFastingCard() {
        fastingRenderer?.hide()
        fastingCountdownCoordinator.stop()
    }

    private fun renderBasicActivePlan(activePlan: ActiveDietPlanUiModel) {
        when (val effect = activePlanRenderer?.render(activePlan)) {
            is DailyActivePlanRenderer.FastingEffect.Show -> {
                renderFastingActionsIfNeeded(effect.planId)
            }

            DailyActivePlanRenderer.FastingEffect.Hide -> hideFastingCard()
            null -> Unit
        }
    }

    private fun renderDailyMealPlan(plan: DailyPlannedMealsUiModel) {
        hideFastingCard()
        plannedMealRenderer?.render(plan)
    }

    private fun renderMealTotals() {
        if (_binding == null || !::dailySummaryLoadAction.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val requestedDate = selectedDate
            val selectedDateIso = requestedDate.toString()
            val result = backgroundTaskRunner.run {
                dailySummaryLoadAction.load(
                    DailySummaryLoadRequest(
                        dateIso = selectedDateIso,
                        healthConnectSummary = healthConnectSummary,
                        displaySteps = resolvedDisplaySteps(),
                        fallbackWeightKg = currentWeightKg
                    )
                )
            }
            if (_binding == null || !dashboardStateStore.isSelectedDate(requestedDate)) return@launch

            val nutritionTargets = result.nutritionTargets
            val summaryStateChange = dashboardStateStore.updateSummary(
                nutritionTargets = DailyDashboardNutritionTargets(
                    calories = nutritionTargets.dailyCalories,
                    proteinGrams = nutritionTargets.proteinGrams,
                    carbGrams = nutritionTargets.carbGrams,
                    fatGrams = nutritionTargets.fatGrams
                ),
                profileWeightKg = result.resolvedWeightKg,
                displayWeightKg = result.displayWeightKg
            )

            val energyBalance = result.summaryState.energyBalance
            val activitySource = DailyActivityPresentationResolver.resolveSource(
                healthConnectSummary = healthConnectSummary,
                healthActiveCalories = energyBalance.syncedActivityCalories,
                localSteps = localStepCount,
                manualExerciseCalories = energyBalance.manualExerciseCalories
            )
            summaryRenderer?.render(
                result = result,
                activitySource = activitySource
            )
            renderDailyAiEffects(
                dailyAiDashboardCoordinator.onSummaryLoaded(
                    request = DailyAiSummarySnapshotRequest(
                        dateIso = selectedDateIso,
                        summary = result,
                        waterMl = currentWaterMl,
                        waterGoalMl = waterGoalMl
                    ),
                    selectedDate = selectedDate
                )
            )

            if (summaryStateChange.didCalorieTargetChange) {
                refreshDashboard(DailyDashboardRefreshPlans.ACTIVE_PLAN_PROGRESS_CHANGED)
            }
        }
    }

    private fun refreshDailyAiSnapshotForWater() {
        if (!::dailyAiDashboardCoordinator.isInitialized) return
        renderDailyAiEffects(
            dailyAiDashboardCoordinator.onWaterChanged(
                waterMl = currentWaterMl,
                waterGoalMl = waterGoalMl,
                selectedDate = selectedDate
            )
        )
    }

    private fun applyActivePlanTodayOrOpenList() {
        when (val result = activePlanApplyAction.execute(currentActivePlan?.title)) {
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
            val result = backgroundTaskRunner.run {
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
        dashboardClickBinder?.clear()
        dashboardClickBinder = null
        sleepDialogCoordinator?.clear()
        sleepDialogCoordinator = null
        fastingRenderer?.clear()
        fastingRenderer = null
        dailyAiCardRenderer = null
        plannedMealRenderer = null
        activePlanRenderer = null
        sleepWeightRenderer = null
        weeklyProgressRenderer = null
        waterRenderer = null
        summaryRenderer = null
        super.onDestroyView()
        _binding = null
    }
    companion object {
        private const val DEFAULT_DAILY_CALORIE_TARGET = 1800
        private const val DEFAULT_PROTEIN_TARGET_GRAMS = 120
        private const val DEFAULT_CARB_TARGET_GRAMS = 280
        private const val DEFAULT_FAT_TARGET_GRAMS = 70
        private const val DEFAULT_PROFILE_WEIGHT_KG = 70.0
        private const val DEFAULT_WATER_GOAL_ML = 2500
    }
}
