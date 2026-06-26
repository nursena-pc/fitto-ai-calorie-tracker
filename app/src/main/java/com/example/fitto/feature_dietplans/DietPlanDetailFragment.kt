package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDietPlanDetailBinding
import com.example.fitto.feature_dietplans.data.DietPlanPreferenceStore

class DietPlanDetailFragment : Fragment(R.layout.fragment_diet_plan_detail) {

    private var _binding: FragmentDietPlanDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceStore: DietPlanPreferenceStore
    private lateinit var dietPlanPreferenceAction: DietPlanPreferenceAction
    private var currentPlanId: String = PLAN_MEDITERRANEAN

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDietPlanDetailBinding.bind(view)
        preferenceStore = DietPlanPreferenceStore(requireContext())
        dietPlanPreferenceAction = DietPlanPreferenceAction.from(preferenceStore)

        currentPlanId = arguments?.getString(ARG_PLAN_ID) ?: PLAN_MEDITERRANEAN
        setupClicks()
        bindPlanDetail(currentPlanId)
        renderSaveState()
    }
    private fun toggleSaveCurrentPlan() {
        val detail = DietPlanDetailCatalog.get(currentPlanId) ?: return
        val favoriteResult = dietPlanPreferenceAction.toggleFavorite(currentPlanId)

        renderSaveState()

        Toast.makeText(
            requireContext(),
            getString(
                if (favoriteResult.isFavorite) {
                    R.string.diet_plans_favorite_added_message
                } else {
                    R.string.diet_plans_favorite_removed_message
                },
                getString(detail.titleRes)
            ),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, favoriteResult.activePlanId)
            }
        )
    }

    private fun renderSaveState() = with(binding) {
        val isSaved = preferenceStore.isFavorite(currentPlanId)

        savePlanButton.text = getString(
            if (isSaved) R.string.diet_plan_detail_saved_plan
            else R.string.diet_plan_detail_save_plan
        )

        savePlanButton.alpha = if (isSaved) 0.86f else 1f
    }
    private fun bindPlanDetail(planId: String) = with(binding) {
        val detail = DietPlanDetailCatalog.get(planId) ?: return@with

        heroCoverImage.setImageResource(detail.coverImageRes)
        heroTitle.setText(detail.titleRes)

        badge1Text.setText(detail.badge1Res)
        badge2Text.setText(detail.badge2Res)
        badge3Text.setText(detail.badge3Res)
        badge4Text.setText(detail.badge4Res)

        aboutTitleIconText.text = detail.aboutIcon
        aboutDescText.setText(detail.aboutDescRes)

        benefit1IconText.text = detail.benefit1Icon
        benefit1TitleText.setText(detail.benefit1TitleRes)
        benefit1DescText.setText(detail.benefit1DescRes)

        benefit2IconText.text = detail.benefit2Icon
        benefit2TitleText.setText(detail.benefit2TitleRes)
        benefit2DescText.setText(detail.benefit2DescRes)

        benefit3IconText.text = detail.benefit3Icon
        benefit3TitleText.setText(detail.benefit3TitleRes)
        benefit3DescText.setText(detail.benefit3DescRes)

        sampleMeal1TitleText.setText(detail.sampleMeal1TitleRes)
        sampleMeal1DescText.setText(detail.sampleMeal1DescRes)
        sampleMeal1KcalText.setText(detail.sampleMeal1KcalRes)

        sampleMeal2TitleText.setText(detail.sampleMeal2TitleRes)
        sampleMeal2DescText.setText(detail.sampleMeal2DescRes)
        sampleMeal2KcalText.setText(detail.sampleMeal2KcalRes)

        sampleMeal3TitleText.setText(detail.sampleMeal3TitleRes)
        sampleMeal3DescText.setText(detail.sampleMeal3DescRes)
        sampleMeal3KcalText.setText(detail.sampleMeal3KcalRes)

        sampleMeal4TitleText.setText(detail.sampleMeal4TitleRes)
        sampleMeal4DescText.setText(detail.sampleMeal4DescRes)
        sampleMeal4KcalText.setText(detail.sampleMeal4KcalRes)

        featuredFood1IconText.text = detail.featuredFood1Icon
        featuredFood1Text.setText(detail.featuredFood1Res)

        featuredFood2IconText.text = detail.featuredFood2Icon
        featuredFood2Text.setText(detail.featuredFood2Res)

        featuredFood3IconText.text = detail.featuredFood3Icon
        featuredFood3Text.setText(detail.featuredFood3Res)

        featuredFood4IconText.text = detail.featuredFood4Icon
        featuredFood4Text.setText(detail.featuredFood4Res)

        featuredFood5IconText.text = detail.featuredFood5Icon
        featuredFood5Text.setText(detail.featuredFood5Res)

        featuredFood6IconText.text = detail.featuredFood6Icon
        featuredFood6Text.setText(detail.featuredFood6Res)

        applyFeaturedFoodCount(detail.featuredFoodCount)

        forWhom1Text.setText(detail.forWhom1Res)
        forWhom2Text.setText(detail.forWhom2Res)
        forWhom3Text.setText(detail.forWhom3Res)

        macroTitleText.setText(detail.macroTitleRes)
        macroCarbText.setText(detail.macroCarbRes)
        macroProteinText.setText(detail.macroProteinRes)
        macroFatText.setText(detail.macroFatRes)

        attentionTitleIconText.text = detail.attentionIcon
        attention1Text.setText(detail.attention1Res)
        attention2Text.setText(detail.attention2Res)
        attention3Text.setText(detail.attention3Res)
    }
    private fun applyFeaturedFoodCount(count: Int) = with(binding) {
        featuredFood5Card.visibility = if (count >= 5) View.VISIBLE else View.GONE
        featuredFood6Card.visibility = if (count >= 6) View.VISIBLE else View.GONE
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        applyPlanButton.setOnClickListener {
            applyCurrentPlan()
        }
        bottomApplyPlanButton.setOnClickListener {
            applyCurrentPlan()
        }
        savePlanButton.setOnClickListener {
            toggleSaveCurrentPlan()
        }
    }


    private fun applyCurrentPlan() {
        val detail = DietPlanDetailCatalog.get(currentPlanId) ?: return
        val applyResult = dietPlanPreferenceAction.applyPlan(currentPlanId)

        parentFragmentManager.setFragmentResult(
            DietPlansFragment.REQUEST_DIET_PLAN_CHANGED,
            Bundle().apply {
                putString(DietPlansFragment.BUNDLE_ACTIVE_PLAN_ID, applyResult.activePlanId)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.diet_plans_apply_success_message, getString(detail.titleRes)),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
    companion object {
        private const val ARG_PLAN_ID = "arg_plan_id"
        const val PLAN_DASH = "dash"
        const val PLAN_MEDITERRANEAN = "mediterranean"
        const val PLAN_KETOGENIC = "ketogenic"
        const val PLAN_MIND = "mind"
        const val PLAN_ELIMINATION = "elimination"
        const val PLAN_PLANT_BASED = "plant_based"
        const val PLAN_WEIGHT_GAIN = "weight_gain"

        fun newInstance(planId: String): DietPlanDetailFragment {
            return DietPlanDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLAN_ID, planId)
                }
            }
        }
    }
}
