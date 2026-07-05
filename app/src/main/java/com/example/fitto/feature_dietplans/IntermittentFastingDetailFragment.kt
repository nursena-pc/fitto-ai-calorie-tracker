package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.FragmentIntermittentFastingDetailBinding
import com.example.fitto.fittoAppContainer
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.FastingPlanType
import com.example.fitto.feature_dietplans.model.FastingPlanUiModel
import com.google.android.material.card.MaterialCardView
import android.graphics.Typeface
import android.view.ViewGroup

class IntermittentFastingDetailFragment :
    Fragment(R.layout.fragment_intermittent_fasting_detail) {

    private var _binding: FragmentIntermittentFastingDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceStore: DietPlanPreferenceStore
    private lateinit var dietPlanPreferenceAction: DietPlanPreferenceAction
    private var selectedPlanType: FastingPlanType = FastingPlanType.PLAN_16_8
    private var customFastingHours: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentIntermittentFastingDetailBinding.bind(view)
        val dependencies = requireContext().fittoAppContainer.createDietPlanFeatureDependencies()
        preferenceStore = dependencies.preferenceStore
        dietPlanPreferenceAction = dependencies.preferenceAction

        setupClicks()
        setupFaqAccordion()
        setupPlanSelection()
        renderSelectedPlan(FastingPlanType.PLAN_16_8)
        renderSaveState()
        setupCustomPlanResult()
    }
    private fun toggleSaveFastingPlan() {
        val favoriteResult = dietPlanPreferenceAction.toggleFavorite(
            DietPlanCatalog.PLAN_INTERMITTENT_FASTING
        )

        renderSaveState()

        Toast.makeText(
            requireContext(),
            getString(
                if (favoriteResult.isFavorite) {
                    R.string.diet_plans_favorite_added_message
                } else {
                    R.string.diet_plans_favorite_removed_message
                },
                getString(R.string.diet_plan_if_title)
            ),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    favoriteResult.activePlanId
                )
            }
        )
    }

    private fun renderSaveState() = with(binding) {
        val isSaved = preferenceStore.isFavorite(DietPlanCatalog.PLAN_INTERMITTENT_FASTING)

        savePlanButton.text = getString(
            if (isSaved) R.string.diet_plan_detail_saved_plan
            else R.string.if_detail_save
        )

        savePlanButton.alpha = if (isSaved) 0.86f else 1f
    }
    private fun setupCustomPlanResult() {
        childFragmentManager.setFragmentResultListener(
            CustomFastingPlanBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val fastingHours = bundle.getInt(
                CustomFastingPlanBottomSheet.BUNDLE_FASTING_HOURS,
                CustomFastingPlanRules.DEFAULT_FASTING_HOURS
            )
            renderCustomPlan(fastingHours)
        }
    }
    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        applyPlanButton.setOnClickListener {
            applySelectedFastingPlan()
        }

        savePlanButton.setOnClickListener {
            toggleSaveFastingPlan()
        }
    }

    private fun setupPlanSelection() = with(binding) {
        fastingPlan12Card.setOnClickListener {
            renderSelectedPlan(FastingPlanType.PLAN_12_12)
        }

        fastingPlan15Card.setOnClickListener {
            renderSelectedPlan(FastingPlanType.PLAN_15_9)
        }

        fastingPlan16Card.setOnClickListener {
            renderSelectedPlan(FastingPlanType.PLAN_16_8)
        }

        fastingPlan61Card.setOnClickListener {
            renderSelectedPlan(FastingPlanType.PLAN_6_1)
        }

        fastingPlan52Card.setOnClickListener {
            renderSelectedPlan(FastingPlanType.PLAN_5_2)
        }
        fastingPlanCustomCard.setOnClickListener {
            CustomFastingPlanBottomSheet
                .newInstance(customFastingHours ?: CustomFastingPlanRules.DEFAULT_FASTING_HOURS)
                .show(childFragmentManager, CustomFastingPlanBottomSheet.TAG)
        }
    }

    private fun renderSelectedPlan(planType: FastingPlanType) {
        selectedPlanType = planType
        customFastingHours = null

        val plan = FastingPlanContentCatalog.get(planType) ?: return

        updatePlanSelectionUi(planType)
        bindPlanContent(plan)
        collapseFaqs()
    }

    private fun updatePlanSelectionUi(planType: FastingPlanType) = with(binding) {
        resetPlanCardStyles()

        when (planType) {
            FastingPlanType.PLAN_12_12 -> highlightPlanCard(fastingPlan12Card)
            FastingPlanType.PLAN_15_9 -> highlightPlanCard(fastingPlan15Card)
            FastingPlanType.PLAN_16_8 -> highlightPlanCard(fastingPlan16Card)
            FastingPlanType.PLAN_6_1 -> highlightPlanCard(fastingPlan61Card)
            FastingPlanType.PLAN_5_2 -> highlightPlanCard(fastingPlan52Card)
            FastingPlanType.PLAN_CUSTOM -> highlightPlanCard(fastingPlanCustomCard)
        }
    }


    private fun renderCustomPlan(fastingHours: Int) = with(binding) {
        val schedule = CustomFastingPlanRules.resolveSchedule(fastingHours)

        selectedPlanType = FastingPlanType.PLAN_CUSTOM
        customFastingHours = schedule.fastingHours

        resetPlanCardStyles()
        highlightPlanCard(fastingPlanCustomCard)

        selectedPlanLabelText.text = getString(R.string.if_custom_selected_label)
        selectedPlanRatioText.text = getString(
            R.string.if_custom_selected_ratio,
            schedule.fastingHours,
            schedule.eatingHours
        )

        timeStartText.text = schedule.fastingStartText
        timeStartSubText.text = getString(R.string.if_custom_time_start_sub)

        timeEatText.text = schedule.eatingStartText
        timeEatSubText.text = getString(R.string.if_custom_time_eat_sub)

        timeEndText.text = schedule.fastingEndText
        timeEndSubText.text = getString(R.string.if_custom_time_end_sub)

        aboutDescText.text = getString(
            R.string.if_custom_about_desc,
            schedule.fastingHours,
            schedule.eatingHours
        )

        benefit1TitleText.text = getString(R.string.if_custom_benefit_1_title)
        benefit1DescText.text = getString(R.string.if_custom_benefit_1_desc)

        benefit2TitleText.text = getString(R.string.if_custom_benefit_2_title)
        benefit2DescText.text = getString(R.string.if_custom_benefit_2_desc)

        benefit3TitleText.text = getString(R.string.if_custom_benefit_3_title)
        benefit3DescText.text = getString(R.string.if_custom_benefit_3_desc)

        forWhomDescText.text = getString(
            R.string.if_custom_for_whom_desc,
            schedule.fastingHours,
            schedule.eatingHours
        )

        weeklyFlowDescText.text = getString(
            R.string.if_custom_weekly_flow_desc,
            schedule.fastingHours,
            schedule.eatingHours
        )

        faq1QuestionText.text = getString(
            R.string.if_custom_faq_1,
            schedule.fastingHours,
            schedule.eatingHours
        )
        faq1Answer.text = getString(R.string.if_custom_faq_1_answer)

        faq2QuestionText.text = getString(R.string.if_custom_faq_2)
        faq2Answer.text = getString(R.string.if_custom_faq_2_answer)

        faq3QuestionText.text = getString(R.string.if_custom_faq_3)
        faq3Answer.text = getString(R.string.if_custom_faq_3_answer)

        collapseFaqs()
    }

    private fun resetPlanCardStyles() = with(binding) {
        val defaultBg = ContextCompat.getColor(requireContext(), R.color.fitto_surface)
        val defaultStroke = ContextCompat.getColor(requireContext(), R.color.fitto_divider_soft)

        val cards: List<MaterialCardView> = listOf(
            fastingPlan12Card,
            fastingPlan15Card,
            fastingPlan16Card,
            fastingPlan61Card,
            fastingPlan52Card,
            fastingPlanCustomCard
        )

        cards.forEach { card ->
            card.setCardBackgroundColor(defaultBg)
            card.strokeColor = defaultStroke
            card.strokeWidth = dp(1)
            updatePlanCardTextStyle(card, isSelected = false)
        }
    }

    private fun highlightPlanCard(card: MaterialCardView) {
        card.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )
        card.strokeColor = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        card.strokeWidth = dp(2)
        updatePlanCardTextStyle(card, isSelected = true)
    }

    private fun updatePlanCardTextStyle(card: MaterialCardView, isSelected: Boolean) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.fitto_green)
        val titleColor = ContextCompat.getColor(requireContext(), R.color.fitto_text_dark)
        val subtitleColor = ContextCompat.getColor(requireContext(), R.color.fitto_text_secondary)

        val textViews = mutableListOf<TextView>()
        collectTextViews(card, textViews)

        textViews.forEachIndexed { index, textView ->
            val color = when {
                isSelected -> selectedColor
                index == 0 -> titleColor
                else -> subtitleColor
            }

            textView.setTextColor(color)
            textView.setTypeface(
                textView.typeface,
                if (isSelected || index == 0) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun collectTextViews(view: View, result: MutableList<TextView>) {
        if (view is TextView) {
            result.add(view)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectTextViews(view.getChildAt(index), result)
            }
        }
    }

    private fun bindPlanContent(plan: FastingPlanUiModel) = with(binding) {
        selectedPlanLabelText.setText(plan.selectedPlanLabelRes)
        selectedPlanRatioText.setText(plan.selectedPlanRatioRes)

        timeStartText.setText(plan.timeStartRes)
        timeStartSubText.setText(plan.timeStartSubRes)

        timeEatText.setText(plan.timeEatRes)
        timeEatSubText.setText(plan.timeEatSubRes)

        timeEndText.setText(plan.timeEndRes)
        timeEndSubText.setText(plan.timeEndSubRes)

        aboutDescText.setText(plan.aboutDescRes)

        benefit1TitleText.setText(plan.benefit1TitleRes)
        benefit1DescText.setText(plan.benefit1DescRes)

        benefit2TitleText.setText(plan.benefit2TitleRes)
        benefit2DescText.setText(plan.benefit2DescRes)

        benefit3TitleText.setText(plan.benefit3TitleRes)
        benefit3DescText.setText(plan.benefit3DescRes)

        forWhomDescText.setText(plan.forWhomDescRes)
        weeklyFlowDescText.setText(plan.weeklyFlowDescRes)

        faq1QuestionText.setText(plan.faq1Res)
        faq1Answer.setText(plan.faq1AnswerRes)

        faq2QuestionText.setText(plan.faq2Res)
        faq2Answer.setText(plan.faq2AnswerRes)

        faq3QuestionText.setText(plan.faq3Res)
        faq3Answer.setText(plan.faq3AnswerRes)
    }

    private fun setupFaqAccordion() = with(binding) {
        faq1Header.setOnClickListener {
            toggleFaq(faq1Answer, faq1Arrow)
        }

        faq2Header.setOnClickListener {
            toggleFaq(faq2Answer, faq2Arrow)
        }

        faq3Header.setOnClickListener {
            toggleFaq(faq3Answer, faq3Arrow)
        }
    }

    private fun toggleFaq(answerView: TextView, arrowView: TextView) {
        val willOpen = answerView.visibility != View.VISIBLE
        answerView.visibility = if (willOpen) View.VISIBLE else View.GONE
        arrowView.text = if (willOpen) "⌃" else "⌄"
    }

    private fun collapseFaqs() = with(binding) {
        faq1Answer.visibility = View.GONE
        faq2Answer.visibility = View.GONE
        faq3Answer.visibility = View.GONE

        faq1Arrow.text = "⌄"
        faq2Arrow.text = "⌄"
        faq3Arrow.text = "⌄"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }


    private fun applySelectedFastingPlan() {
        val applyResult = dietPlanPreferenceAction.applyPlan(
            planId = DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
            fastingType = selectedPlanType,
            customFastingHours = customFastingHours
        )

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    applyResult.activePlanId
                )
            }
        )

        val appliedPlan = DietPlanCatalog.getActivePlan(
            context = requireContext(),
            planId = DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
            fastingType = selectedPlanType,
            customFastingHours = customFastingHours
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_apply_success_message, appliedPlan.title),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
