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
import com.example.fitto.databinding.FragmentDietPlansBinding
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import com.example.fitto.fittoAppContainer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DietPlansFragment : Fragment(R.layout.fragment_diet_plans) {
    private var _binding: FragmentDietPlansBinding? = null
    private val binding get() = _binding!!

    private lateinit var dependencies: DietPlanFeatureDependencies
    private lateinit var adapter: DietPlansAdapter
    private val screenStateStore = DietPlansScreenStateStore()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlansBinding.bind(view)
        dependencies = requireContext().fittoAppContainer.createDietPlanFeatureDependencies()

        setupRecyclerView()
        setupClicks()
        setupFilterResultListener()
        setupDietPlanChangeListener()
        restoreSelectedTab()
        loadDietPlans()
        setupPersonalizedRecommendation()
    }

    private fun setupRecyclerView() = with(binding) {
        adapter = DietPlansAdapter(
            onDetailClick = { plan ->
                if (plan.isDraft) openDraftEditor(plan.id) else openPlanDetail(plan.id)
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
                val favoriteResult = dependencies.preferenceAction.toggleFavorite(plan.id)
                renderListEffect(
                    DietPlansListEffectResolver.favoriteToggled(
                        planTitle = titleTextForPlan(plan),
                        result = favoriteResult
                    )
                )
            },
            onDeleteClick = ::confirmDeleteCustomPlan
        )

        dietPlansRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        dietPlansRecyclerView.adapter = adapter
    }

    private fun openDraftEditor(planId: String) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeTabContainer, CreateDietPlanFragment.newEditDraftInstance(planId))
            .addToBackStack(BACK_STACK_CREATE_DIET_PLAN)
            .commit()
    }

    private fun completeDraftPlan(plan: DietPlanUiModel) {
        val result = dependencies.draftCompletionAction.complete(plan.id)
        if (result is DietPlanDraftCompletionResult.Completed) {
            renderListEffect(DietPlansListEffectResolver.draftCompleted())
        }
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
            screenStateStore.selectTab(DietPlanTabResolver.fromChipId(checkedIds.firstOrNull()))
            applyVisiblePlans()
        }

        applyTodayDietButton.setOnClickListener {
            val recommendation = screenStateStore.state.recommendation
            if (recommendation == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_apply_today_coming_soon),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                applyDietPlan(recommendation.planId, recommendation.titleText)
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

    private fun restoreSelectedTab() {
        val chipId = when (screenStateStore.state.selectedTab) {
            DietPlanTab.READY -> R.id.chipReadyPlans
            DietPlanTab.MY -> R.id.chipMyPlans
            DietPlanTab.DRAFTS -> R.id.chipDrafts
            DietPlanTab.SAVED -> R.id.chipFavorites
        }
        binding.planChipGroup.check(chipId)
    }

    private fun loadDietPlans() {
        screenStateStore.setPlans(
            dependencies.listLoader.load(includeDrafts = true).allPlans
        )
        applyVisiblePlans()
    }

    private fun setupDietPlanChangeListener() {
        parentFragmentManager.setFragmentResultListener(
            REQUEST_DIET_PLAN_CHANGED,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedTab = when (bundle.getString(BUNDLE_SELECT_TAB)) {
                TAB_DRAFTS -> DietPlanTab.DRAFTS
                TAB_MY -> DietPlanTab.MY
                TAB_SAVED -> DietPlanTab.SAVED
                else -> null
            }
            selectedTab?.let {
                screenStateStore.selectTab(it)
                restoreSelectedTab()
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
            screenStateStore.setFilter(
                DietPlanFilterResultMapper.fromResult(
                    clearFilters = bundle.getBoolean(DietPlanFilterFragment.BUNDLE_CLEAR_FILTER, false),
                    goal = bundle.getString(DietPlanFilterFragment.BUNDLE_GOAL),
                    duration = bundle.getInt(DietPlanFilterFragment.BUNDLE_DURATION, 0),
                    type = bundle.getString(DietPlanFilterFragment.BUNDLE_TYPE),
                    features = bundle.getStringArrayList(DietPlanFilterFragment.BUNDLE_FEATURES),
                    minCalories = bundle.getInt(DietPlanFilterFragment.BUNDLE_MIN_CALORIES, 0),
                    maxCalories = bundle.getInt(DietPlanFilterFragment.BUNDLE_MAX_CALORIES, Int.MAX_VALUE)
                )
            )
            applyVisiblePlans()
        }
    }

    private fun applyVisiblePlans() = with(binding) {
        val visibilityState = screenStateStore.visibleState()
        val visiblePlans = visibilityState.visiblePlans

        createOwnPlanCard.isVisible = visibilityState.showCreateOwnPlan
        personalizedRecommendationCard.isVisible = visibilityState.showPersonalizedRecommendation
        dietPlansEmptyText.isVisible = visiblePlans.isEmpty()
        dietPlansRecyclerView.isVisible = visiblePlans.isNotEmpty()
        dietPlansEmptyText.text = getString(visibilityState.emptyTextRes)
        adapter.submitList(visiblePlans)
    }

    private fun setupPersonalizedRecommendation() {
        val generation = screenStateStore.beginRecommendationLoad()
        viewLifecycleOwner.lifecycleScope.launch {
            val profile = dependencies.profileLoader.loadCurrentProfile()
            if (_binding == null) return@launch

            val plans = screenStateStore.state.plans.ifEmpty {
                dependencies.listLoader.loadReadyPlans()
            }
            val recommendation = buildBestRecommendation(profile, plans)
            if (screenStateStore.applyRecommendation(generation, recommendation)) {
                renderPersonalizedRecommendation(recommendation)
            }
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
            applyDietPlan(recommendation.planId, recommendation.titleText)
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
                fallbackPlans = dependencies.listLoader.loadReadyPlans(
                    favoriteIds = emptySet(),
                    activePlanId = dependencies.preferenceStore.getActivePlanId()
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

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Fitto_WhiteAlertDialog)
            .setTitle(getString(R.string.diet_plans_delete_custom_plan_title))
            .setMessage(getString(R.string.diet_plans_delete_custom_plan_message, planTitle))
            .setNegativeButton(getString(R.string.common_cancel), null)
            .setPositiveButton(getString(R.string.common_delete)) { _, _ -> deleteCustomPlan(plan) }
            .show()
    }

    private fun deleteCustomPlan(plan: DietPlanUiModel) {
        renderListEffect(
            DietPlansListEffectResolver.customPlanDeleted(
                dependencies.deletionAction.delete(plan.id)
            )
        )
    }

    private fun applyDietPlan(planId: String, planTitle: String) {
        renderListEffect(
            DietPlansListEffectResolver.planApplied(
                planTitle = planTitle,
                result = dependencies.preferenceAction.applyPlan(planId)
            )
        )
    }

    private fun renderListEffect(effect: DietPlansListEffect) {
        if (effect.shouldNotifyDietPlanChanged) {
            parentFragmentManager.setFragmentResult(
                REQUEST_DIET_PLAN_CHANGED,
                Bundle().apply {
                    effect.activePlanId?.let { putString(BUNDLE_ACTIVE_PLAN_ID, it) }
                    effect.selectTab?.let { putString(BUNDLE_SELECT_TAB, it) }
                }
            )
        }

        effect.toastMessageRes?.let { messageRes ->
            Toast.makeText(
                requireContext(),
                effect.toastPlanTitle?.let { getString(messageRes, it) } ?: getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
        }

        effect.selectedTab?.let { screenStateStore.selectTab(it) }
        effect.checkedChipId?.let { binding.planChipGroup.check(it) }
        if (effect.shouldReloadPlans) loadDietPlans()
        if (effect.shouldRefreshRecommendation) setupPersonalizedRecommendation()
    }

    private fun openRecommendedPlanDetail(planId: String) = openPlanDetail(planId)

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
                    .replace(R.id.homeTabContainer, DietPlanDetailFragment.newInstance(destination.planId))
                    .addToBackStack(BACK_STACK_DIET_PLAN_DETAIL)
                    .commit()
            }
            is DietPlanDestination.CustomDetail -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.homeTabContainer, CustomDietPlanDetailFragment.newInstance(destination.planId))
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

    override fun onDestroyView() {
        binding.dietPlansRecyclerView.adapter = null
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
