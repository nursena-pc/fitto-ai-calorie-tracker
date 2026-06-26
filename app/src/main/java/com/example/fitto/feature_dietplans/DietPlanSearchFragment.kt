package com.example.fitto.feature_dietplans

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDietPlanSearchBinding
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DietPlanSearchFragment : Fragment(R.layout.fragment_diet_plan_search) {

    private var _binding: FragmentDietPlanSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DietPlansAdapter
    private lateinit var preferenceStore: DietPlanPreferenceStore
    private lateinit var customDietPlanStore: CustomDietPlanStore
    private lateinit var customPlanDeletionAction: CustomDietPlanDeletionAction

    private var allPlans: List<DietPlanUiModel> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlanSearchBinding.bind(view)

        preferenceStore = DietPlanPreferenceStore(requireContext())
        customDietPlanStore = CustomDietPlanStore(requireContext().applicationContext)
        customPlanDeletionAction = CustomDietPlanDeletionAction.from(
            customPlanStore = customDietPlanStore,
            preferenceStore = preferenceStore
        )

        allPlans = createSearchPlans()

        setupRecyclerView()
        setupClicks()
        setupSearch()
        renderRecentSearches()

        adapter.submitList(allPlans)
    }

    private fun setupRecyclerView() = with(binding) {
        adapter = DietPlansAdapter(
            onDetailClick = { plan ->
                openPlanDetail(plan)
            },
            onApplyClick = { plan ->
                applyPlan(plan)
            },
            onFavoriteClick = { plan ->
                toggleFavorite(plan)
            },
            onDeleteClick = { plan ->
                confirmDeleteCustomPlan(plan)
            }
        )

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchResultsRecyclerView.adapter = adapter
    }
    private fun confirmDeleteCustomPlan(plan: DietPlanUiModel) {
        if (!CustomDietPlanDeletionAction.isCustomPlanId(plan.id)) return

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setTitle(getString(R.string.diet_plans_delete_custom_plan_title))
            .setMessage(
                getString(
                    R.string.diet_plans_delete_custom_plan_message,
                    titleTextForPlan(plan)
                )
            )
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
                    DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
                    Bundle().apply {
                        putString(
                            DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                            result.activePlanId
                        )
                    }
                )

                Toast.makeText(
                    requireContext(),
                    getString(R.string.diet_plans_delete_custom_plan_success),
                    Toast.LENGTH_SHORT
                ).show()

                refreshPlans()
                filterPlans(binding.searchInput.text?.toString().orEmpty())
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
    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        clearSearchButton.setOnClickListener {
            searchInput.text?.clear()
        }

    }

    private fun setupSearch() = with(binding) {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterPlans(text?.toString().orEmpty())
            }

            override fun afterTextChanged(editable: Editable?) = Unit
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            ) {
                submitSearch(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }
    private fun submitSearch(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        preferenceStore.saveRecentDietSearch(cleanQuery)
        renderRecentSearches()
        filterPlans(cleanQuery)
    }

    private fun rememberCurrentSearchIfNeeded() {
        val query = binding.searchInput.text?.toString().orEmpty().trim()
        if (query.isNotBlank()) {
            preferenceStore.saveRecentDietSearch(query)
            renderRecentSearches()
        }
    }

    private fun renderRecentSearches() = with(binding) {
        val recentSearches = preferenceStore.getRecentDietSearches()

        recentSearchesTitle.isVisible = recentSearches.isNotEmpty()
        recentSearchesScroll.isVisible = recentSearches.isNotEmpty()

        recentSearchesContainer.removeAllViews()

        recentSearches.forEach { query ->
            recentSearchesContainer.addView(createRecentSearchChip(query))
        }
    }

    private fun createRecentSearchChip(query: String): TextView {
        return TextView(requireContext()).apply {
            text = query
            setTextColor(ContextCompat.getColor(requireContext(), R.color.fitto_text_dark))
            textSize = 15f
            typeface = Typeface.DEFAULT
            setPadding(dp(16), dp(9), dp(16), dp(9))
            setBackgroundResource(R.drawable.bg_fitto_chip)

            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(10)
            }

            setOnClickListener {
                binding.searchInput.setText(query)
                binding.searchInput.setSelection(binding.searchInput.text?.length ?: 0)
                submitSearch(query)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun filterPlans(query: String) {
        adapter.submitList(
            DietPlanSearchResolver.filter(
                plans = allPlans,
                query = query,
                titleTextForPlan = ::titleTextForPlan,
                descriptionTextForPlan = ::descriptionTextForPlan
            )
        )
    }

    private fun openPlanDetail(plan: DietPlanUiModel) {
        rememberCurrentSearchIfNeeded()
        if (plan.id.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)) {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.homeTabContainer,
                    CustomDietPlanDetailFragment.newInstance(plan.id)
                )
                .addToBackStack("custom_diet_plan_detail")
                .commit()
            return
        }

        when (plan.id) {
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.homeTabContainer, IntermittentFastingDetailFragment())
                    .addToBackStack("if_detail")
                    .commit()
            }

            else -> {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(
                        R.id.homeTabContainer,
                        DietPlanDetailFragment.newInstance(plan.id)
                    )
                    .addToBackStack("diet_plan_detail")
                    .commit()
            }
        }
    }

    private fun applyPlan(plan: DietPlanUiModel) {
        rememberCurrentSearchIfNeeded()
        preferenceStore.saveActivePlan(plan.id)
        preferenceStore.applyActivePlanToday()

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, plan.id)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(
                R.string.diet_plans_apply_success_message,
                titleTextForPlan(plan)
            ),
            Toast.LENGTH_SHORT
        ).show()

        refreshPlans()
        parentFragmentManager.popBackStack()
    }

    private fun toggleFavorite(plan: DietPlanUiModel) {
        val isFavoriteNow = preferenceStore.toggleFavorite(plan.id)

        val messageRes = if (isFavoriteNow) {
            R.string.diet_plans_favorite_added_message
        } else {
            R.string.diet_plans_favorite_removed_message
        }

        Toast.makeText(
            requireContext(),
            getString(messageRes, titleTextForPlan(plan)),
            Toast.LENGTH_SHORT
        ).show()

        refreshPlans()
        filterPlans(binding.searchInput.text?.toString().orEmpty())
    }

    private fun refreshPlans() {
        allPlans = createSearchPlans()
    }

    private fun createSearchPlans(): List<DietPlanUiModel> {
        val favoriteIds = preferenceStore.getFavoriteIds()
        val activePlanId = preferenceStore.getActivePlanId()

        val readyPlans = DietPlanCatalog.getReadyPlans(
            favoriteIds = favoriteIds,
            activePlanId = activePlanId
        )

        val customPlans = customDietPlanStore
            .getPlans(includeDrafts = false)
            .map { customPlan ->
                customDietPlanStore.toDietPlanUiModel(
                    plan = customPlan,
                    isFavorite = customPlan.id in favoriteIds
                ).copy(
                    isApplied = customPlan.id == activePlanId
                )
            }

        return customPlans + readyPlans
    }

    private fun titleTextForPlan(plan: DietPlanUiModel): String {
        return DietPlanTextResolver.title(
            plan = plan,
            stringProvider = ::getString,
            fallback = getString(R.string.app_name)
        )
    }

    private fun descriptionTextForPlan(plan: DietPlanUiModel): String {
        return DietPlanTextResolver.description(
            plan = plan,
            stringProvider = ::getString,
            fallback = getString(R.string.diet_plan_custom_default_desc)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
