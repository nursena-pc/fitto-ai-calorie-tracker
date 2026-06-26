package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.entity.UserProfileEntity
import com.example.fitto.data.local.repository.ProfileCacheRepository
import com.example.fitto.databinding.FragmentDietPlansBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.data.UserNutritionTargetStore
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private lateinit var customPlanDeletionAction: CustomDietPlanDeletionAction
    private var currentPlans: List<DietPlanUiModel> = emptyList()
    private var currentRecommendation: PersonalizedDietRecommendation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlansBinding.bind(view)
        customDietPlanStore = CustomDietPlanStore(requireContext().applicationContext)
        dietPlanStore = DietPlanPreferenceStore(requireContext())
        profileCacheRepository = ProfileCacheRepository(requireContext().applicationContext)
        nutritionTargetStore = UserNutritionTargetStore(requireContext())
        customPlanDeletionAction = CustomDietPlanDeletionAction.from(
            customPlanStore = customDietPlanStore,
            preferenceStore = dietPlanStore
        )

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
            selectedTab = DietPlanTabResolver.fromChipId(checkedIds.firstOrNull())
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
        val visibilityState = DietPlanVisibilityResolver.resolve(
            plans = currentPlans,
            selectedTab = selectedTab,
            filter = activeFilterState
        )
        val visiblePlans = visibilityState.visiblePlans

        createOwnPlanCard.isVisible = visibilityState.showCreateOwnPlan
        personalizedRecommendationCard.isVisible = visibilityState.showPersonalizedRecommendation

        dietPlansEmptyText.isVisible = visiblePlans.isEmpty()
        dietPlansRecyclerView.isVisible = visiblePlans.isNotEmpty()
        dietPlansEmptyText.text = getString(visibilityState.emptyTextRes)

        adapter.submitList(visiblePlans)
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
        return checkNotNull(
            DietPlanRecommendationResolver.resolve(
                profile = profile,
                plans = plans,
                fallbackPlans = createDietPlanItems(
                    favoriteIds = emptySet(),
                    activePlanId = dietPlanStore.getActivePlanId()
                ),
                titleTextForPlan = ::titleTextForPlan
            )
        )
    }

    private fun titleTextForPlan(plan: DietPlanUiModel): String {
        return DietPlanTextResolver.title(
            plan = plan,
            stringProvider = ::getString,
            fallback = getString(R.string.app_name)
        )
    }

    private fun confirmDeleteCustomPlan(plan: DietPlanUiModel) {
        if (!CustomDietPlanDeletionAction.isCustomPlanId(plan.id)) return

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
        when (val result = customPlanDeletionAction.delete(plan.id)) {
            is CustomDietPlanDeletionResult.Deleted -> {
                parentFragmentManager.setFragmentResult(
                    REQUEST_DIET_PLAN_CHANGED,
                    Bundle().apply {
                        putString(BUNDLE_ACTIVE_PLAN_ID, result.activePlanId)
                    }
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_delete_custom_plan_success),
                    Toast.LENGTH_SHORT
                ).show()

                loadDietPlans()
                setupPersonalizedRecommendation()
            }

            is CustomDietPlanDeletionResult.Failed -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_delete_custom_plan_error),
                    Toast.LENGTH_SHORT
                ).show()
            }

            CustomDietPlanDeletionResult.NotCustomPlan -> Unit
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
        when (val destination = DietPlanDestinationResolver.resolve(planId)) {
            DietPlanDestination.IntermittentFasting -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.homeTabContainer, IntermittentFastingDetailFragment())
                    .addToBackStack("if_detail")
                    .commit()
            }

            is DietPlanDestination.ReadyDetail -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.homeTabContainer,
                        DietPlanDetailFragment.newInstance(destination.planId)
                    )
                    .addToBackStack(BACK_STACK_DIET_PLAN_DETAIL)
                    .commit()
            }

            is DietPlanDestination.CustomDetail -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.homeTabContainer,
                        CustomDietPlanDetailFragment.newInstance(destination.planId)
                    )
                    .addToBackStack("custom_diet_plan_detail")
                    .commit()
            }

            is DietPlanDestination.Unknown -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_detail_coming_soon, destination.planId),
                    Toast.LENGTH_SHORT
                ).show()
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
                id = DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
                titleRes = R.string.diet_plan_if_title,
                descriptionRes = R.string.diet_plan_if_desc,
                kcalPerDay = 1600,
                durationDays = 14,
                isFavorite = DietPlanCatalog.PLAN_INTERMITTENT_FASTING in favoriteIds,
                isApplied = activePlanId == DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
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

    companion object {
        const val REQUEST_DIET_PLAN_CHANGED = "request_diet_plan_changed"
        const val BUNDLE_ACTIVE_PLAN_ID = "bundle_active_plan_id"
        private const val BACK_STACK_CREATE_DIET_PLAN = "create_diet_plan"
        private const val BACK_STACK_DIET_PLAN_DETAIL = "diet_plan_detail"
        const val BUNDLE_SELECT_TAB = "bundle_select_tab"
        const val TAB_READY = "ready"
        const val TAB_MY = "my"
        const val TAB_DRAFTS = "drafts"
        const val TAB_SAVED = "saved"
    }
}
