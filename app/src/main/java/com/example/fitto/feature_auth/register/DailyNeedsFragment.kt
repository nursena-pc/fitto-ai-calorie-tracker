package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyNeedsBinding

class DailyNeedsFragment : Fragment(R.layout.fragment_daily_needs) {

    private var _binding: FragmentDailyNeedsBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentDailyNeedsBinding.bind(view)
        onboardingViewModel = obtainOnboardingViewModel()

        binding.root.setOnboardingProgress(7)

        binding.dailyNeedsBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.dailyNeedsNextButton.setOnClickListener {
            val needsTargetWeight = onboardingViewModel.state.goalType == GoalType.LOSE_WEIGHT ||
                    onboardingViewModel.state.goalType == GoalType.GAIN_WEIGHT

            val destination = if (needsTargetWeight) {
                R.id.action_dailyNeedsFragment_to_weightLossReasonFragment
            } else {
                R.id.action_dailyNeedsFragment_to_activityLevelFragment
            }

            findNavController().navigate(destination)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}