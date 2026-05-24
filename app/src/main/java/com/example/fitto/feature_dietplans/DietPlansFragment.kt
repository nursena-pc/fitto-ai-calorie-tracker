package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentDietPlansBinding
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.isVisible
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
class DietPlansFragment : Fragment(R.layout.fragment_diet_plans) {
    private lateinit var customDietPlanStore: CustomDietPlanStore
    private var _binding: FragmentDietPlansBinding? = null
    private val binding get() = _binding!!
    private var selectedTab: DietPlanTab = DietPlanTab.READY
    private var activeFilterState: DietPlanFilterState? = null
    private lateinit var adapter: DietPlansAdapter
    private lateinit var dietPlanStore: DietPlanPreferenceStore
    private lateinit var profileCacheRepository: ProfileCacheRepository
    private lateinit var nutritionTargetStore: UserNutritionTargetStore
    private var currentPlans: List<DietPlanUiModel> = emptyList()
    private var currentRecommendation: PersonalizedDietRecommendation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlansBinding.bind(view)
        customDietPlanStore = CustomDietPlanStore(requireContext().applicationContext)
        dietPlanStore = DietPlanPreferenceStore(requireContext())
        profileCacheRepository = ProfileCacheRepository(requireContext().applicationContext)
        nutritionTargetStore = UserNutritionTargetStore(requireContext())

        setupRecyclerView()
        setupClicks()
        setupFilterResultListener()
        setupDietPlanChangeListener()
        loadDietPlans()
        setupPersonalizedRecommendation()
    }

    private fun setupRecyclerView() = with(binding) {
        adapter = DietPlansAdapter(
            onDetailClick = { plan ->
                if (plan.isDraft) {
                    openDraftEditor(plan.id)
                } else {
                    openPlanDetail(plan.id)
                }
            },
            onApplyClick = { plan ->
                if (plan.isDraft) {
                    completeDraftPlan(plan)
                } else {
                    applyDietPlan(
                        planId = plan.id,
                        planTitle = plan.titleText
                            ?: plan.titleRes?.let { getString(it) }
                            ?: getString(R.string.app_name)
                    )
                }
            },
            onFavoriteClick = { plan ->
                val isSavedNow = dietPlanStore.toggleFavorite(plan.id)

                val planTitle = plan.titleText
                    ?: plan.titleRes?.let { getString(it) }
                    ?: getString(R.string.app_name)

                Toast.makeText(
                    requireContext(),
                    getString(
                        if (isSavedNow) {
                            R.string.diet_plans_favorite_added_message
                        } else {
                            R.string.diet_plans_favorite_removed_message
                        },
                        planTitle
                    ),
                    Toast.LENGTH_SHORT
                ).show()

                loadDietPlans()
                setupPersonalizedRecommendation()
            },
            onDeleteClick = { plan ->
                confirmDeleteCustomPlan(plan)
            }
        )

        dietPlansRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        dietPlansRecyclerView.adapter = adapter
    }

    private fun openDraftEditor(planId: String) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                CreateDietPlanFragment.newEditDraftInstance(planId)
            )
            .addToBackStack(BACK_STACK_CREATE_DIET_PLAN)
            .commit()
    }

    private fun completeDraftPlan(plan: DietPlanUiModel) {
        val draft = customDietPlanStore.getPlan(plan.id)
            ?.takeIf { it.isDraft }
            ?: return

        val completedPlan = draft.copy(
            isDraft = false,
            updatedAt = System.currentTimeMillis()
        )

        customDietPlanStore.savePlan(completedPlan)

        parentFragmentManager.setFragmentResult(
            REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(BUNDLE_SELECT_TAB, TAB_MY)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_draft_completed),
            Toast.LENGTH_SHORT
        ).show()

        selectedTab = DietPlanTab.MY
        binding.planChipGroup.check(R.id.chipMyPlans)

        loadDietPlans()
        setupPersonalizedRecommendation()
    }

    private fun setupClicks() = with(binding) {
        searchButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.homeTabContainer, DietPlanSearchFragment())
                .addToBackStack("diet_plan_search")
                .commit()
        }

        filterButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.homeTabContainer, DietPlanFilterFragment())
                .addToBackStack("diet_plan_filter")
                .commit()
        }
        planChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedTab = when (checkedIds.firstOrNull()) {
                R.id.chipMyPlans -> DietPlanTab.MY
                R.id.chipFavorites -> DietPlanTab.SAVED
                else -> DietPlanTab.READY
            }
            applyVisiblePlans()
        }

        applyTodayDietButton.setOnClickListener {
            val recommendation = currentRecommendation

            if (recommendation == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_apply_today_coming_soon),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                applyDietPlan(
                    planId = recommendation.planId,
                    planTitle = recommendation.titleText
                )
            }
        }
        planChipGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTab = when (checkedId) {
                R.id.chipMyPlans -> DietPlanTab.MY
                R.id.chipDrafts -> DietPlanTab.DRAFTS
                R.id.chipFavorites -> DietPlanTab.SAVED
                else -> DietPlanTab.READY
            }

            applyVisiblePlans()
        }

        createOwnPlanCard.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.homeTabContainer, CreateDietPlanFragment())
                .addToBackStack(BACK_STACK_CREATE_DIET_PLAN)
                .commit()
        }
    }

    private fun loadDietPlans() {
        val favoriteIds = dietPlanStore.getFavoriteIds()
        val activePlanId = dietPlanStore.getActivePlanId()

        val defaultPlans = createDietPlanItems(
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )

        val customPlans = customDietPlanStore
            .getPlans(includeDrafts = true)
            .map { customPlan ->
                customDietPlanStore.toDietPlanUiModel(
                    plan = customPlan,
                    isFavorite = customPlan.id in favoriteIds
                ).copy(
                    isApplied = customPlan.id == activePlanId
                )
            }

        currentPlans = customPlans + defaultPlans
        applyVisiblePlans()
    }
    private fun setupDietPlanChangeListener() {
        parentFragmentManager.setFragmentResultListener(
            REQUEST_DIET_PLAN_CHANGED,
            viewLifecycleOwner
        ) { _, bundle ->
            when (bundle.getString(BUNDLE_SELECT_TAB)) {
                TAB_DRAFTS -> {
                    selectedTab = DietPlanTab.DRAFTS
                    binding.planChipGroup.check(R.id.chipDrafts)
                }

                TAB_MY -> {
                    selectedTab = DietPlanTab.MY
                    binding.planChipGroup.check(R.id.chipMyPlans)
                }

                TAB_SAVED -> {
                    selectedTab = DietPlanTab.SAVED
                    binding.planChipGroup.check(R.id.chipFavorites)
                }

                else -> Unit
            }

            loadDietPlans()
            setupPersonalizedRecommendation()
        }
    }

    private fun setupFilterResultListener() {
        parentFragmentManager.setFragmentResultListener(
            DietPlanFilterFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            activeFilterState = if (bundle.getBoolean(DietPlanFilterFragment.BUNDLE_CLEAR_FILTER, false)) {
                null
            } else {
                DietPlanFilterState(
                    goal = bundle.getString(DietPlanFilterFragment.BUNDLE_GOAL).orEmpty(),
                    duration = bundle.getInt(DietPlanFilterFragment.BUNDLE_DURATION, 0),
                    type = bundle.getString(DietPlanFilterFragment.BUNDLE_TYPE).orEmpty(),
                    features = bundle
                        .getStringArrayList(DietPlanFilterFragment.BUNDLE_FEATURES)
                        .orEmpty()
                        .toSet(),
                    minCalories = bundle.getInt(DietPlanFilterFragment.BUNDLE_MIN_CALORIES, 0),
                    maxCalories = bundle.getInt(DietPlanFilterFragment.BUNDLE_MAX_CALORIES, Int.MAX_VALUE)
                )
            }

            applyVisiblePlans()
        }
    }

    private fun applyVisiblePlans() = with(binding) {
        val tabPlans = when (selectedTab) {
            DietPlanTab.READY -> currentPlans.filterNot { it.isCustomPlan() }

            DietPlanTab.MY -> currentPlans.filter {
                it.isCustomPlan() && !it.isDraft
            }

            DietPlanTab.DRAFTS -> currentPlans.filter {
                it.isCustomPlan() && it.isDraft
            }

            DietPlanTab.SAVED -> currentPlans.filter {
                it.isFavorite && !it.isDraft
            }
        }

        val filter = activeFilterState
        val visiblePlans = filter?.let { activeFilter ->
            tabPlans.filter { plan -> plan.matchesFilter(activeFilter) }
        } ?: tabPlans

        createOwnPlanCard.isVisible = selectedTab == DietPlanTab.READY || selectedTab == DietPlanTab.MY
        personalizedRecommendationCard.isVisible = selectedTab == DietPlanTab.READY && filter == null

        dietPlansEmptyText.isVisible = visiblePlans.isEmpty()
        dietPlansRecyclerView.isVisible = visiblePlans.isNotEmpty()

        dietPlansEmptyText.text = getString(
            if (filter != null) {
                R.string.diet_plans_empty_filtered
            } else {
                when (selectedTab) {
                    DietPlanTab.MY -> R.string.diet_plans_empty_my_plans
                    DietPlanTab.DRAFTS -> R.string.diet_plans_empty_drafts
                    DietPlanTab.SAVED -> R.string.diet_plans_empty_saved
                    DietPlanTab.READY -> R.string.diet_plans_empty_ready
                }
            }
        )

        adapter.submitList(visiblePlans)
    }

    private fun DietPlanUiModel.isCustomPlan(): Boolean {
        return id.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)
    }

    private fun DietPlanUiModel.matchesFilter(filter: DietPlanFilterState): Boolean {
        if (kcalPerDay !in filter.minCalories..filter.maxCalories) return false

        if (filter.duration > 0 && durationDays != filter.duration) return false

        if (filter.type.isNotBlank() && planTypeKey() != filter.type) return false

        if (filter.goal.isNotBlank() && !matchesGoal(filter.goal)) return false

        if (filter.features.isNotEmpty() && !filter.features.all { matchesFeature(it) }) {
            return false
        }

        return true
    }

    private fun DietPlanUiModel.planTypeKey(): String {
        return when (id) {
            DietPlanDetailFragment.PLAN_MEDITERRANEAN -> "mediterranean"
            DietPlanDetailFragment.PLAN_DASH -> "dash"
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> "intermittent_fasting"
            DietPlanDetailFragment.PLAN_KETOGENIC -> "ketogenic"
            DietPlanDetailFragment.PLAN_PLANT_BASED -> "plant_based"
            else -> if (isCustomPlan()) "custom" else id
        }
    }

    private fun DietPlanUiModel.matchesGoal(goal: String): Boolean {
        return when (goal) {
            "weight_loss" -> id != DietPlanDetailFragment.PLAN_WEIGHT_GAIN
            "weight_gain" -> id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN || isCustomPlan()
            "muscle_gain" -> id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN ||
                    id == DietPlanDetailFragment.PLAN_KETOGENIC ||
                    isCustomPlan()
            "balanced" -> true
            else -> true
        }
    }

    private fun DietPlanUiModel.matchesFeature(feature: String): Boolean {
        return when (feature) {
            "heart_friendly" -> id == DietPlanDetailFragment.PLAN_MEDITERRANEAN ||
                    id == DietPlanDetailFragment.PLAN_DASH ||
                    id == DietPlanDetailFragment.PLAN_MIND ||
                    id == DietPlanDetailFragment.PLAN_PLANT_BASED

            "low_sodium" -> id == DietPlanDetailFragment.PLAN_DASH

            "high_protein" -> id == DietPlanDetailFragment.PLAN_KETOGENIC ||
                    id == DietPlanDetailFragment.PLAN_WEIGHT_GAIN ||
                    isCustomPlan()

            "easy" -> durationDays <= 14 ||
                    id == DietPlanDetailFragment.PLAN_MEDITERRANEAN

            else -> true
        }
    }
    private fun setupPersonalizedRecommendation() {
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = withContext(Dispatchers.IO) {
                profileCacheRepository.getCurrentUserProfile()
                    ?: profileCacheRepository.syncCurrentUserProfileFromFirebase()
            }

            if (_binding == null) return@launch

            val recommendation = buildBestRecommendation(
                profile = profile,
                plans = currentPlans.ifEmpty {
                    createDietPlanItems(
                        favoriteIds = dietPlanStore.getFavoriteIds(),
                        activePlanId = dietPlanStore.getActivePlanId()
                    )                }
            )

            currentRecommendation = recommendation
            renderPersonalizedRecommendation(recommendation)
        }
    }

    private fun renderPersonalizedRecommendation(
        recommendation: PersonalizedDietRecommendation
    ) = with(binding) {
        personalizedPlanTitleText.text = recommendation.titleText
        personalizedPlanDescText.setText(recommendation.reasonRes)

        personalizedMatchText.text = getString(
            R.string.diet_plans_personalized_match,
            recommendation.matchPercent
        )

        personalizedReasonChip1.setText(recommendation.chip1Res)
        personalizedReasonChip2.setText(recommendation.chip2Res)
        personalizedReasonChip3.setText(recommendation.chip3Res)

        personalizedRecommendationCard.setOnClickListener {
            openRecommendedPlanDetail(recommendation.planId)
        }

        personalizedDetailButton.setOnClickListener {
            openRecommendedPlanDetail(recommendation.planId)
        }

        personalizedApplyButton.setOnClickListener {
            applyDietPlan(
                planId = recommendation.planId,
                planTitle = recommendation.titleText
            )
        }
    }

    private fun buildBestRecommendation(
        profile: UserProfileEntity?,
        plans: List<DietPlanUiModel>
    ): PersonalizedDietRecommendation {
        val safePlans = plans.ifEmpty {
            createDietPlanItems(
                favoriteIds = emptySet(),
                activePlanId = dietPlanStore.getActivePlanId()
            )
        }

        val scoredPlans = safePlans.map { plan ->
            plan to calculatePlanScore(
                profile = profile,
                plan = plan
            )
        }

        val bestPlan = scoredPlans
            .maxByOrNull { (_, score) -> score }
            ?.first
            ?: safePlans.first()

        val bestScore = scoredPlans
            .firstOrNull { (plan, _) -> plan.id == bestPlan.id }
            ?.second
            ?: 86

        return PersonalizedDietRecommendation(
            planId = bestPlan.id,
            titleText = bestPlan.titleText
                ?: bestPlan.titleRes?.let { getString(it) }
                ?: getString(R.string.app_name),
            reasonRes = reasonResForPlan(bestPlan.id),
            matchPercent = bestScore.coerceIn(72, 96),
            chip1Res = chipOneResForPlan(bestPlan.id),
            chip2Res = chipTwoResForGoal(profile?.goalType.orEmpty()),
            chip3Res = chipThreeResForProfile(profile)
        )
    }

    private fun calculatePlanScore(
        profile: UserProfileEntity?,
        plan: DietPlanUiModel
    ): Int {
        val goal = profile?.goalType.orEmpty().uppercase()
        val targetCalories = profile?.dailyCalorieTarget
            ?.takeIf { it > 0 }
            ?: DEFAULT_TARGET_CALORIES

        val planId = plan.id
        var score = 0

        score += goalFitScore(
            goal = goal,
            planId = planId
        )

        score += calorieFitScore(
            targetCalories = targetCalories,
            planCalories = plan.kcalPerDay
        )

        score += planSafetyScore(
            profile = profile,
            planId = planId
        )

        score += durationFitScore(
            profile = profile,
            durationDays = plan.durationDays
        )

        if (plan.isFavorite) {
            score += 4
        }

        return score.coerceIn(0, 100)
    }

    private fun goalFitScore(
        goal: String,
        planId: String
    ): Int {
        return when (goal) {
            "GAIN_WEIGHT" -> when (planId) {
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 50
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 24
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 20
                DietPlanDetailFragment.PLAN_DASH -> 16
                PLAN_INTERMITTENT_FASTING -> 4
                DietPlanDetailFragment.PLAN_KETOGENIC -> 4
                else -> 12
            }

            "LOSE_WEIGHT" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 38
                DietPlanDetailFragment.PLAN_DASH -> 34
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 32
                PLAN_INTERMITTENT_FASTING -> 28
                DietPlanDetailFragment.PLAN_KETOGENIC -> 24
                DietPlanDetailFragment.PLAN_MIND -> 22
                DietPlanDetailFragment.PLAN_ELIMINATION -> 16
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 0
                else -> 16
            }

            "MAINTAIN_WEIGHT", "EAT_HEALTHY" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 46
                DietPlanDetailFragment.PLAN_DASH -> 38
                DietPlanDetailFragment.PLAN_MIND -> 34
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 30
                DietPlanDetailFragment.PLAN_ELIMINATION -> 16
                PLAN_INTERMITTENT_FASTING -> 14
                DietPlanDetailFragment.PLAN_KETOGENIC -> 10
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 4
                else -> 20
            }

            "LEARN_NUTRITION" -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 44
                DietPlanDetailFragment.PLAN_DASH -> 36
                DietPlanDetailFragment.PLAN_MIND -> 32
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 30
                DietPlanDetailFragment.PLAN_ELIMINATION -> 20
                PLAN_INTERMITTENT_FASTING -> 12
                DietPlanDetailFragment.PLAN_KETOGENIC -> 10
                DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> 8
                else -> 20
            }

            else -> when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN -> 42
                DietPlanDetailFragment.PLAN_DASH -> 34
                DietPlanDetailFragment.PLAN_MIND -> 30
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 28
                else -> 16
            }
        }
    }

    private fun calorieFitScore(
        targetCalories: Int,
        planCalories: Int
    ): Int {
        val difference = abs(targetCalories - planCalories)

        return when {
            difference <= 100 -> 25
            difference <= 200 -> 22
            difference <= 300 -> 18
            difference <= 450 -> 12
            difference <= 650 -> 7
            else -> 2
        }
    }

    private fun planSafetyScore(
        profile: UserProfileEntity?,
        planId: String
    ): Int {
        val planStatus = profile?.planStatus.orEmpty().uppercase()
        val weeklyChange = profile?.weeklyWeightChangeKg ?: 0.0

        var score = 12

        if (planStatus == "AGGRESSIVE" || planStatus == "NOT_RECOMMENDED") {
            score += when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanDetailFragment.PLAN_DASH,
                DietPlanDetailFragment.PLAN_PLANT_BASED -> 10

                PLAN_INTERMITTENT_FASTING,
                DietPlanDetailFragment.PLAN_KETOGENIC -> -8

                else -> 0
            }
        }

        if (weeklyChange < -0.7) {
            score += when (planId) {
                DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                DietPlanDetailFragment.PLAN_DASH -> 6

                PLAN_INTERMITTENT_FASTING,
                DietPlanDetailFragment.PLAN_KETOGENIC -> -6

                else -> 0
            }
        }

        if (weeklyChange > 0.2 && planId == DietPlanDetailFragment.PLAN_WEIGHT_GAIN) {
            score += 8
        }

        return score.coerceIn(0, 22)
    }

    private fun durationFitScore(
        profile: UserProfileEntity?,
        durationDays: Int
    ): Int {
        val estimatedWeeks = profile?.estimatedWeeks

        return when {
            estimatedWeeks == null -> 6
            estimatedWeeks <= 4 && durationDays <= 14 -> 8
            estimatedWeeks > 4 && durationDays >= 14 -> 8
            else -> 5
        }
    }

    private fun reasonResForPlan(planId: String): Int {
        return when (planId) {
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN ->
                R.string.diet_plans_personalized_reason_weight_gain

            DietPlanDetailFragment.PLAN_PLANT_BASED ->
                R.string.diet_plans_personalized_reason_plant_based

            DietPlanDetailFragment.PLAN_ELIMINATION ->
                R.string.diet_plans_personalized_reason_elimination

            DietPlanDetailFragment.PLAN_DASH ->
                R.string.diet_plans_personalized_reason_dash

            DietPlanDetailFragment.PLAN_MIND ->
                R.string.diet_plans_personalized_reason_mind

            DietPlanDetailFragment.PLAN_KETOGENIC ->
                R.string.diet_plans_personalized_reason_keto

            else ->
                R.string.diet_plans_personalized_reason_mediterranean
        }
    }

    private fun chipOneResForPlan(planId: String): Int {
        return when (planId) {
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN ->
                R.string.diet_plan_tag_energy_focused

            DietPlanDetailFragment.PLAN_PLANT_BASED ->
                R.string.diet_plan_tag_plant_focused

            DietPlanDetailFragment.PLAN_ELIMINATION ->
                R.string.diet_plan_tag_sensitivity_tracking

            DietPlanDetailFragment.PLAN_DASH ->
                R.string.dash_detail_benefit_2_title

            DietPlanDetailFragment.PLAN_MIND ->
                R.string.diet_plan_tag_brain_friendly

            DietPlanDetailFragment.PLAN_KETOGENIC ->
                R.string.diet_plan_tag_low_carb

            PLAN_INTERMITTENT_FASTING ->
                R.string.diet_tag_fasting

            else ->
                R.string.diet_plans_personalized_chip_balanced
        }
    }

    private fun chipTwoResForGoal(goalType: String): Int {
        return when (goalType.uppercase()) {
            "GAIN_WEIGHT" -> R.string.diet_plan_tag_energy_focused
            "LOSE_WEIGHT" -> R.string.diet_plans_personalized_chip_easy_start
            "EAT_HEALTHY", "MAINTAIN_WEIGHT", "LEARN_NUTRITION" ->
                R.string.diet_plans_personalized_chip_balanced

            else -> R.string.diet_plans_personalized_chip_balanced
        }
    }

    private fun chipThreeResForProfile(profile: UserProfileEntity?): Int {
        val planStatus = profile?.planStatus.orEmpty().uppercase()

        return if (planStatus == "AGGRESSIVE" || planStatus == "NOT_RECOMMENDED") {
            R.string.diet_plans_personalized_chip_sustainable
        } else {
            R.string.diet_plans_personalized_chip_easy_start
        }
    }
    private fun confirmDeleteCustomPlan(plan: DietPlanUiModel) {
        if (!plan.id.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)) return

        val planTitle = plan.titleText
            ?: plan.titleRes?.let { getString(it) }
            ?: getString(R.string.diet_plan_tag_custom)

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(getString(R.string.diet_plans_delete_custom_plan_title))
            .setMessage(getString(R.string.diet_plans_delete_custom_plan_message, planTitle))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.common_delete)) { _, _ ->
                deleteCustomPlan(plan)
            }
            .show()
    }

    private fun deleteCustomPlan(plan: DietPlanUiModel) {
        runCatching {
            customDietPlanStore.deletePlan(plan.id)
            dietPlanStore.removeFavorite(plan.id)
            dietPlanStore.clearActivePlanIf(plan.id)
        }.onSuccess {
            parentFragmentManager.setFragmentResult(
                REQUEST_DIET_PLAN_CHANGED,
                Bundle().apply {
                    putString(BUNDLE_ACTIVE_PLAN_ID, dietPlanStore.getActivePlanId())
                }
            )

            Toast.makeText(
                requireContext(),
                getString(R.string.diet_plans_delete_custom_plan_success),
                Toast.LENGTH_SHORT
            ).show()

            loadDietPlans()
            setupPersonalizedRecommendation()
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.diet_plans_delete_custom_plan_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun applyDietPlan(
        planId: String,
        planTitle: String
    ) {
        dietPlanStore.saveActivePlan(planId)
        dietPlanStore.applyActivePlanToday()

        parentFragmentManager.setFragmentResult(
            REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(BUNDLE_ACTIVE_PLAN_ID, planId)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_apply_success, planTitle),
            Toast.LENGTH_SHORT
        ).show()

        loadDietPlans()
        setupPersonalizedRecommendation()
    }

    private fun openRecommendedPlanDetail(planId: String) {
        openPlanDetail(planId)
    }

    private fun openPlanDetail(planId: String) {
        when (planId) {
            PLAN_INTERMITTENT_FASTING -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.homeTabContainer, IntermittentFastingDetailFragment())
                    .addToBackStack("if_detail")
                    .commit()
            }

            DietPlanDetailFragment.PLAN_MEDITERRANEAN,
            DietPlanDetailFragment.PLAN_KETOGENIC,
            DietPlanDetailFragment.PLAN_DASH,
            DietPlanDetailFragment.PLAN_MIND,
            DietPlanDetailFragment.PLAN_ELIMINATION,
            DietPlanDetailFragment.PLAN_PLANT_BASED,
            DietPlanDetailFragment.PLAN_WEIGHT_GAIN -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.homeTabContainer,
                        DietPlanDetailFragment.newInstance(planId)
                    )
                    .addToBackStack(BACK_STACK_DIET_PLAN_DETAIL)
                    .commit()
            }

            else -> {
                if (planId.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)) {
                    parentFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(
                            R.id.homeTabContainer,
                            CustomDietPlanDetailFragment.newInstance(planId)
                        )
                        .addToBackStack("custom_diet_plan_detail")
                        .commit()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.diet_plans_detail_coming_soon, planId),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createDietPlanItems(
        favoriteIds: Set<String>,
        activePlanId: String? = dietPlanStore.getActivePlanId()
    ): List<DietPlanUiModel> {
        return listOf(
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                titleRes = R.string.diet_plan_mediterranean_title,
                descriptionRes = R.string.diet_plan_mediterranean_desc,
                kcalPerDay = 1800,
                durationDays = 7,
                isFavorite = DietPlanDetailFragment.PLAN_MEDITERRANEAN in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_MEDITERRANEAN,
                tagRes = R.string.diet_tag_popular,
                imageResId = R.drawable.akdeniz_detay_header
            ),
            DietPlanUiModel(
                id = PLAN_INTERMITTENT_FASTING,
                titleRes = R.string.diet_plan_if_title,
                descriptionRes = R.string.diet_plan_if_desc,
                kcalPerDay = 1600,
                durationDays = 14,
                isFavorite = PLAN_INTERMITTENT_FASTING in favoriteIds,
                isApplied = activePlanId == PLAN_INTERMITTENT_FASTING,
                tagRes = R.string.diet_tag_fasting,
                imageResId = R.drawable.intermittent_fasting_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_KETOGENIC,
                titleRes = R.string.diet_plan_keto_title,
                descriptionRes = R.string.diet_plan_keto_desc,
                kcalPerDay = 1700,
                durationDays = 7,
                isFavorite = DietPlanDetailFragment.PLAN_KETOGENIC in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_KETOGENIC,
                tagRes = R.string.diet_tag_low_carb,
                imageResId = R.drawable.ketogenic_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_DASH,
                titleRes = R.string.diet_plan_dash_title,
                descriptionRes = R.string.diet_plan_dash_desc,
                kcalPerDay = 1700,
                durationDays = 14,
                isFavorite = DietPlanDetailFragment.PLAN_DASH in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_DASH,
                tagRes = R.string.dash_detail_benefit_2_title,
                imageResId = R.drawable.dash_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_MIND,
                titleRes = R.string.diet_plan_mind_title,
                descriptionRes = R.string.diet_plan_mind_desc,
                kcalPerDay = 1700,
                durationDays = 14,
                isFavorite = DietPlanDetailFragment.PLAN_MIND in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_MIND,
                tagRes = R.string.diet_plan_tag_brain_friendly,
                imageResId = R.drawable.mind_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_ELIMINATION,
                titleRes = R.string.diet_plan_elimination_title,
                descriptionRes = R.string.diet_plan_elimination_desc,
                kcalPerDay = 1600,
                durationDays = 21,
                isFavorite = DietPlanDetailFragment.PLAN_ELIMINATION in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_ELIMINATION,
                tagRes = R.string.diet_plan_tag_sensitivity_tracking,
                imageResId = R.drawable.elimination_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_PLANT_BASED,
                titleRes = R.string.diet_plan_plant_based_title,
                descriptionRes = R.string.diet_plan_plant_based_desc,
                kcalPerDay = 1600,
                durationDays = 14,
                isFavorite = DietPlanDetailFragment.PLAN_PLANT_BASED in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_PLANT_BASED,
                tagRes = R.string.diet_plan_tag_plant_focused,
                imageResId = R.drawable.plant_based_diet_cover
            ),
            DietPlanUiModel(
                id = DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                titleRes = R.string.diet_plan_weight_gain_title,
                descriptionRes = R.string.diet_plan_weight_gain_desc,
                kcalPerDay = 2200,
                durationDays = 21,
                isFavorite = DietPlanDetailFragment.PLAN_WEIGHT_GAIN in favoriteIds,
                isApplied = activePlanId == DietPlanDetailFragment.PLAN_WEIGHT_GAIN,
                tagRes = R.string.diet_plan_tag_energy_focused,
                imageResId = R.drawable.weight_gain_diet_cover
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
    private enum class DietPlanTab {
        READY,
        MY,
        DRAFTS,
        SAVED
    }

    private data class DietPlanFilterState(
        val goal: String,
        val duration: Int,
        val type: String,
        val features: Set<String>,
        val minCalories: Int,
        val maxCalories: Int
    )
    companion object {
        const val REQUEST_DIET_PLAN_CHANGED = "request_diet_plan_changed"
        const val BUNDLE_ACTIVE_PLAN_ID = "bundle_active_plan_id"
        private const val PLAN_INTERMITTENT_FASTING = "intermittent_fasting"
        private const val BACK_STACK_CREATE_DIET_PLAN = "create_diet_plan"
        private const val BACK_STACK_DIET_PLAN_DETAIL = "diet_plan_detail"
        const val BUNDLE_SELECT_TAB = "bundle_select_tab"
        const val TAB_READY = "ready"
        const val TAB_MY = "my"
        const val TAB_DRAFTS = "drafts"
        const val TAB_SAVED = "saved"
        private const val DEFAULT_TARGET_CALORIES = 1800
    }

    private data class PersonalizedDietRecommendation(
        val planId: String,
        val titleText: String,
        val reasonRes: Int,
        val matchPercent: Int,
        val chip1Res: Int,
        val chip2Res: Int,
        val chip3Res: Int
    )
}