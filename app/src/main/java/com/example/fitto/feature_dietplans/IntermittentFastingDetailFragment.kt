package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.FragmentIntermittentFastingDetailBinding
import com.example.fitto.feature_dietplans.data.DietPlanCatalog
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore
import com.example.fitto.feature_dietplans.model.FastingPlanType
import com.example.fitto.feature_dietplans.model.FastingPlanUiModel
import com.google.android.material.card.MaterialCardView
import android.graphics.Typeface
import android.view.ViewGroup
import java.util.Locale

class IntermittentFastingDetailFragment :
    Fragment(R.layout.fragment_intermittent_fasting_detail) {

    private var _binding: FragmentIntermittentFastingDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceStore: DietPlanPreferenceStore
    private var selectedPlanType: FastingPlanType = FastingPlanType.PLAN_16_8
    private var customFastingHours: Int? = null

    private val fastingPlans by lazy {
        mapOf(
            FastingPlanType.PLAN_12_12 to FastingPlanUiModel(
                type = FastingPlanType.PLAN_12_12,
                selectedPlanLabelRes = R.string.if_plan_12_12_selected_label,
                selectedPlanRatioRes = R.string.if_plan_12_12_selected_ratio,

                timeStartRes = R.string.if_plan_12_12_time_start,
                timeStartSubRes = R.string.if_plan_12_12_time_start_sub,
                timeEatRes = R.string.if_plan_12_12_time_eat,
                timeEatSubRes = R.string.if_plan_12_12_time_eat_sub,
                timeEndRes = R.string.if_plan_12_12_time_end,
                timeEndSubRes = R.string.if_plan_12_12_time_end_sub,

                aboutDescRes = R.string.if_plan_12_12_about_desc,

                benefit1TitleRes = R.string.if_plan_12_12_benefit_1_title,
                benefit1DescRes = R.string.if_plan_12_12_benefit_1_desc,
                benefit2TitleRes = R.string.if_plan_12_12_benefit_2_title,
                benefit2DescRes = R.string.if_plan_12_12_benefit_2_desc,
                benefit3TitleRes = R.string.if_plan_12_12_benefit_3_title,
                benefit3DescRes = R.string.if_plan_12_12_benefit_3_desc,

                forWhomDescRes = R.string.if_plan_12_12_for_whom_desc,
                weeklyFlowDescRes = R.string.if_plan_12_12_weekly_flow_desc,

                faq1Res = R.string.if_plan_12_12_faq_1,
                faq1AnswerRes = R.string.if_plan_12_12_faq_1_answer,
                faq2Res = R.string.if_plan_12_12_faq_2,
                faq2AnswerRes = R.string.if_plan_12_12_faq_2_answer,
                faq3Res = R.string.if_plan_12_12_faq_3,
                faq3AnswerRes = R.string.if_plan_12_12_faq_3_answer
            ),
            FastingPlanType.PLAN_15_9 to FastingPlanUiModel(
                type = FastingPlanType.PLAN_15_9,
                selectedPlanLabelRes = R.string.if_plan_15_9_selected_label,
                selectedPlanRatioRes = R.string.if_plan_15_9_selected_ratio,

                timeStartRes = R.string.if_plan_15_9_time_start,
                timeStartSubRes = R.string.if_plan_15_9_time_start_sub,
                timeEatRes = R.string.if_plan_15_9_time_eat,
                timeEatSubRes = R.string.if_plan_15_9_time_eat_sub,
                timeEndRes = R.string.if_plan_15_9_time_end,
                timeEndSubRes = R.string.if_plan_15_9_time_end_sub,

                aboutDescRes = R.string.if_plan_15_9_about_desc,

                benefit1TitleRes = R.string.if_plan_15_9_benefit_1_title,
                benefit1DescRes = R.string.if_plan_15_9_benefit_1_desc,
                benefit2TitleRes = R.string.if_plan_15_9_benefit_2_title,
                benefit2DescRes = R.string.if_plan_15_9_benefit_2_desc,
                benefit3TitleRes = R.string.if_plan_15_9_benefit_3_title,
                benefit3DescRes = R.string.if_plan_15_9_benefit_3_desc,

                forWhomDescRes = R.string.if_plan_15_9_for_whom_desc,
                weeklyFlowDescRes = R.string.if_plan_15_9_weekly_flow_desc,

                faq1Res = R.string.if_plan_15_9_faq_1,
                faq1AnswerRes = R.string.if_plan_15_9_faq_1_answer,
                faq2Res = R.string.if_plan_15_9_faq_2,
                faq2AnswerRes = R.string.if_plan_15_9_faq_2_answer,
                faq3Res = R.string.if_plan_15_9_faq_3,
                faq3AnswerRes = R.string.if_plan_15_9_faq_3_answer
            ),

            FastingPlanType.PLAN_6_1 to FastingPlanUiModel(
                type = FastingPlanType.PLAN_6_1,
                selectedPlanLabelRes = R.string.if_plan_6_1_selected_label,
                selectedPlanRatioRes = R.string.if_plan_6_1_selected_ratio,

                timeStartRes = R.string.if_plan_6_1_time_start,
                timeStartSubRes = R.string.if_plan_6_1_time_start_sub,
                timeEatRes = R.string.if_plan_6_1_time_eat,
                timeEatSubRes = R.string.if_plan_6_1_time_eat_sub,
                timeEndRes = R.string.if_plan_6_1_time_end,
                timeEndSubRes = R.string.if_plan_6_1_time_end_sub,

                aboutDescRes = R.string.if_plan_6_1_about_desc,

                benefit1TitleRes = R.string.if_plan_6_1_benefit_1_title,
                benefit1DescRes = R.string.if_plan_6_1_benefit_1_desc,
                benefit2TitleRes = R.string.if_plan_6_1_benefit_2_title,
                benefit2DescRes = R.string.if_plan_6_1_benefit_2_desc,
                benefit3TitleRes = R.string.if_plan_6_1_benefit_3_title,
                benefit3DescRes = R.string.if_plan_6_1_benefit_3_desc,

                forWhomDescRes = R.string.if_plan_6_1_for_whom_desc,
                weeklyFlowDescRes = R.string.if_plan_6_1_weekly_flow_desc,

                faq1Res = R.string.if_plan_6_1_faq_1,
                faq1AnswerRes = R.string.if_plan_6_1_faq_1_answer,
                faq2Res = R.string.if_plan_6_1_faq_2,
                faq2AnswerRes = R.string.if_plan_6_1_faq_2_answer,
                faq3Res = R.string.if_plan_6_1_faq_3,
                faq3AnswerRes = R.string.if_plan_6_1_faq_3_answer
            ),

            FastingPlanType.PLAN_5_2 to FastingPlanUiModel(
                type = FastingPlanType.PLAN_5_2,
                selectedPlanLabelRes = R.string.if_plan_5_2_selected_label,
                selectedPlanRatioRes = R.string.if_plan_5_2_selected_ratio,

                timeStartRes = R.string.if_plan_5_2_time_start,
                timeStartSubRes = R.string.if_plan_5_2_time_start_sub,
                timeEatRes = R.string.if_plan_5_2_time_eat,
                timeEatSubRes = R.string.if_plan_5_2_time_eat_sub,
                timeEndRes = R.string.if_plan_5_2_time_end,
                timeEndSubRes = R.string.if_plan_5_2_time_end_sub,

                aboutDescRes = R.string.if_plan_5_2_about_desc,

                benefit1TitleRes = R.string.if_plan_5_2_benefit_1_title,
                benefit1DescRes = R.string.if_plan_5_2_benefit_1_desc,
                benefit2TitleRes = R.string.if_plan_5_2_benefit_2_title,
                benefit2DescRes = R.string.if_plan_5_2_benefit_2_desc,
                benefit3TitleRes = R.string.if_plan_5_2_benefit_3_title,
                benefit3DescRes = R.string.if_plan_5_2_benefit_3_desc,

                forWhomDescRes = R.string.if_plan_5_2_for_whom_desc,
                weeklyFlowDescRes = R.string.if_plan_5_2_weekly_flow_desc,

                faq1Res = R.string.if_plan_5_2_faq_1,
                faq1AnswerRes = R.string.if_plan_5_2_faq_1_answer,
                faq2Res = R.string.if_plan_5_2_faq_2,
                faq2AnswerRes = R.string.if_plan_5_2_faq_2_answer,
                faq3Res = R.string.if_plan_5_2_faq_3,
                faq3AnswerRes = R.string.if_plan_5_2_faq_3_answer
            ),
            FastingPlanType.PLAN_16_8 to FastingPlanUiModel(
                type = FastingPlanType.PLAN_16_8,
                selectedPlanLabelRes = R.string.if_detail_selected_plan,
                selectedPlanRatioRes = R.string.if_detail_selected_plan_ratio,

                timeStartRes = R.string.if_detail_time_start,
                timeStartSubRes = R.string.if_detail_time_start_sub,
                timeEatRes = R.string.if_detail_time_eat,
                timeEatSubRes = R.string.if_detail_time_eat_sub,
                timeEndRes = R.string.if_detail_time_end,
                timeEndSubRes = R.string.if_detail_time_end_sub,

                aboutDescRes = R.string.if_detail_about_desc,

                benefit1TitleRes = R.string.if_detail_benefit_1_title,
                benefit1DescRes = R.string.if_detail_benefit_1_desc,
                benefit2TitleRes = R.string.if_detail_benefit_2_title,
                benefit2DescRes = R.string.if_detail_benefit_2_desc,
                benefit3TitleRes = R.string.if_detail_benefit_3_title,
                benefit3DescRes = R.string.if_detail_benefit_3_desc,

                forWhomDescRes = R.string.if_detail_for_whom_desc,
                weeklyFlowDescRes = R.string.if_detail_weekly_flow_desc,

                faq1Res = R.string.if_detail_faq_1,
                faq1AnswerRes = R.string.if_detail_faq_1_answer,
                faq2Res = R.string.if_detail_faq_2,
                faq2AnswerRes = R.string.if_detail_faq_2_answer,
                faq3Res = R.string.if_detail_faq_3,
                faq3AnswerRes = R.string.if_detail_faq_3_answer
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentIntermittentFastingDetailBinding.bind(view)
        preferenceStore = DietPlanPreferenceStore(requireContext())

        setupClicks()
        setupFaqAccordion()
        setupPlanSelection()
        renderSelectedPlan(FastingPlanType.PLAN_16_8)
        renderSaveState()
        setupCustomPlanResult()
    }
    private fun toggleSaveFastingPlan() {
        val isSavedNow = preferenceStore.toggleFavorite(DietPlanCatalog.PLAN_INTERMITTENT_FASTING)

        renderSaveState()

        Toast.makeText(
            requireContext(),
            getString(
                if (isSavedNow) {
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
                    preferenceStore.getActivePlanId()
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
                16
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
                .newInstance(16)
                .show(childFragmentManager, CustomFastingPlanBottomSheet.TAG)
        }
    }

    private fun renderSelectedPlan(planType: FastingPlanType) {
        selectedPlanType = planType
        customFastingHours = null

        val plan = fastingPlans[planType] ?: return

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
        val safeFastingHours = fastingHours.coerceIn(12, 20)
        val eatingHours = 24 - safeFastingHours

        selectedPlanType = FastingPlanType.PLAN_CUSTOM
        customFastingHours = safeFastingHours

        resetPlanCardStyles()
        highlightPlanCard(fastingPlanCustomCard)

        val fastStartHour = 20
        val eatingStartHour = (fastStartHour + safeFastingHours) % 24

        selectedPlanLabelText.text = getString(R.string.if_custom_selected_label)
        selectedPlanRatioText.text = getString(
            R.string.if_custom_selected_ratio,
            safeFastingHours,
            eatingHours
        )

        timeStartText.text = formatHour(fastStartHour)
        timeStartSubText.text = getString(R.string.if_custom_time_start_sub)

        timeEatText.text = formatHour(eatingStartHour)
        timeEatSubText.text = getString(R.string.if_custom_time_eat_sub)

        timeEndText.text = formatHour(fastStartHour)
        timeEndSubText.text = getString(R.string.if_custom_time_end_sub)

        aboutDescText.text = getString(
            R.string.if_custom_about_desc,
            safeFastingHours,
            eatingHours
        )

        benefit1TitleText.text = getString(R.string.if_custom_benefit_1_title)
        benefit1DescText.text = getString(R.string.if_custom_benefit_1_desc)

        benefit2TitleText.text = getString(R.string.if_custom_benefit_2_title)
        benefit2DescText.text = getString(R.string.if_custom_benefit_2_desc)

        benefit3TitleText.text = getString(R.string.if_custom_benefit_3_title)
        benefit3DescText.text = getString(R.string.if_custom_benefit_3_desc)

        forWhomDescText.text = getString(
            R.string.if_custom_for_whom_desc,
            safeFastingHours,
            eatingHours
        )

        weeklyFlowDescText.text = getString(
            R.string.if_custom_weekly_flow_desc,
            safeFastingHours,
            eatingHours
        )

        faq1QuestionText.text = getString(
            R.string.if_custom_faq_1,
            safeFastingHours,
            eatingHours
        )
        faq1Answer.text = getString(R.string.if_custom_faq_1_answer)

        faq2QuestionText.text = getString(R.string.if_custom_faq_2)
        faq2Answer.text = getString(R.string.if_custom_faq_2_answer)

        faq3QuestionText.text = getString(R.string.if_custom_faq_3)
        faq3Answer.text = getString(R.string.if_custom_faq_3_answer)

        collapseFaqs()
    }

    private fun formatHour(hour: Int): String {
        val normalizedHour = ((hour % 24) + 24) % 24
        return String.format(Locale.getDefault(), "%02d:00", normalizedHour)
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
        preferenceStore.saveActivePlan(
            planId = DietPlanCatalog.PLAN_INTERMITTENT_FASTING,
            fastingType = selectedPlanType,
            customFastingHours = customFastingHours
        )
        preferenceStore.applyActivePlanToday()

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(
                    DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID,
                    DietPlanCatalog.PLAN_INTERMITTENT_FASTING
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