package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentGoalSelectionBinding
import android.widget.TextView
class GoalSelectionFragment : Fragment(R.layout.fragment_goal_selection) {

    private var _binding: FragmentGoalSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    private var selectedGoal: GoalType? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentGoalSelectionBinding.bind(view)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        binding.root.setOnboardingProgress(5)

        setupInitialState()
        setupClicks()
        updateGoalSelection()
    }

    private fun setupInitialState() = with(binding) {
        goalNextButton.isEnabled = false
        goalNextButton.alpha = 0.55f
    }

    private fun setupClicks() = with(binding) {
        goalBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        goalLoseCard.setOnClickListener {
            selectedGoal = GoalType.LOSE_WEIGHT
            updateGoalSelection()
        }

        goalMaintainCard.setOnClickListener {
            selectedGoal = GoalType.MAINTAIN_WEIGHT
            updateGoalSelection()
        }

        goalGainCard.setOnClickListener {
            selectedGoal = GoalType.GAIN_WEIGHT
            updateGoalSelection()
        }

        goalLearnCard.setOnClickListener {
            selectedGoal = GoalType.LEARN_NUTRITION
            updateGoalSelection()
        }

        goalHealthyCard.setOnClickListener {
            selectedGoal = GoalType.EAT_HEALTHY
            updateGoalSelection()
        }

        goalNextButton.setOnClickListener {
            submitGoal()
        }
    }

    private fun updateGoalSelection() = with(binding) {
        updateGoalCard(
            card = goalLoseCard,
            checkView = goalLoseCheck,
            descView = goalLoseDescText,
            isSelected = selectedGoal == GoalType.LOSE_WEIGHT
        )

        updateGoalCard(
            card = goalMaintainCard,
            checkView = goalMaintainCheck,
            descView = goalMaintainDescText,
            isSelected = selectedGoal == GoalType.MAINTAIN_WEIGHT
        )

        updateGoalCard(
            card = goalGainCard,
            checkView = goalGainCheck,
            descView = goalGainDescText,
            isSelected = selectedGoal == GoalType.GAIN_WEIGHT
        )

        updateGoalCard(
            card = goalLearnCard,
            checkView = goalLearnCheck,
            descView = goalLearnDescText,
            isSelected = selectedGoal == GoalType.LEARN_NUTRITION
        )

        updateGoalCard(
            card = goalHealthyCard,
            checkView = goalHealthyCheck,
            descView = goalHealthyDescText,
            isSelected = selectedGoal == GoalType.EAT_HEALTHY
        )

        val hasSelection = selectedGoal != null
        goalNextButton.isEnabled = hasSelection
        goalNextButton.alpha = if (hasSelection) 1f else 0.55f
    }
    private fun updateGoalCard(
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

    private fun submitGoal() {
        val goal = selectedGoal ?: return

        onboardingViewModel.saveGoal(goal)

        findNavController().navigate(
            R.id.action_goalSelectionFragment_to_barrierFragment
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}