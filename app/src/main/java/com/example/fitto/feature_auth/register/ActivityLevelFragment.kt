package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentActivityLevelBinding
import android.widget.TextView
class ActivityLevelFragment : Fragment(R.layout.fragment_activity_level) {

    private var _binding: FragmentActivityLevelBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    private var selectedActivityLevel: ActivityLevel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentActivityLevelBinding.bind(view)
        onboardingViewModel = obtainOnboardingViewModel()

        binding.root.setOnboardingProgress(if (needsTargetWeight()) 9 else 10)

        setupInitialState()
        setupClicks()
        updateActivitySelection()
    }

    private fun setupInitialState() = with(binding) {
        activityLevelNextButton.isEnabled = false
        activityLevelNextButton.alpha = 0.55f
    }

    private fun setupClicks() = with(binding) {
        activityLevelBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        activityLowCard.setOnClickListener {
            selectedActivityLevel = ActivityLevel.LOW
            updateActivitySelection()
        }

        activityModerateCard.setOnClickListener {
            selectedActivityLevel = ActivityLevel.MODERATE
            updateActivitySelection()
        }

        activityHighCard.setOnClickListener {
            selectedActivityLevel = ActivityLevel.HIGH
            updateActivitySelection()
        }

        activityVeryHighCard.setOnClickListener {
            selectedActivityLevel = ActivityLevel.VERY_HIGH
            updateActivitySelection()
        }

        activityLevelNextButton.setOnClickListener {
            submitActivityLevel()
        }
    }

    private fun updateActivitySelection() = with(binding) {
        updateActivityCard(
            card = activityLowCard,
            checkView = activityLowCheck,
            descView = activityLowDescText,
            isSelected = selectedActivityLevel == ActivityLevel.LOW
        )

        updateActivityCard(
            card = activityModerateCard,
            checkView = activityModerateCheck,
            descView = activityModerateDescText,
            isSelected = selectedActivityLevel == ActivityLevel.MODERATE
        )

        updateActivityCard(
            card = activityHighCard,
            checkView = activityHighCheck,
            descView = activityHighDescText,
            isSelected = selectedActivityLevel == ActivityLevel.HIGH
        )

        updateActivityCard(
            card = activityVeryHighCard,
            checkView = activityVeryHighCheck,
            descView = activityVeryHighDescText,
            isSelected = selectedActivityLevel == ActivityLevel.VERY_HIGH
        )

        val hasSelection = selectedActivityLevel != null
        activityLevelNextButton.isEnabled = hasSelection
        activityLevelNextButton.alpha = if (hasSelection) 1f else 0.55f
    }

    private fun updateActivityCard(
        card: LinearLayout,
        checkView: FrameLayout,
        descView: TextView,
        isSelected: Boolean
    ) {
        card.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_fitto_card_selected
            } else {
                R.drawable.bg_fitto_card_normal
            }
        )

        checkView.isVisible = isSelected
        descView.isVisible = isSelected
    }

    private fun submitActivityLevel() {
        val activityLevel = selectedActivityLevel ?: return

        onboardingViewModel.saveActivityLevel(activityLevel)

        val destination = if (needsTargetWeight()) {
            R.id.action_activityLevelFragment_to_targetWeightFragment
        } else {
            R.id.action_activityLevelFragment_to_dailyNeedsLoadingFragment
        }

        findNavController().navigate(destination)
    }

    private fun needsTargetWeight(): Boolean {
        return onboardingViewModel.state.goalType == GoalType.LOSE_WEIGHT ||
                onboardingViewModel.state.goalType == GoalType.GAIN_WEIGHT
    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}