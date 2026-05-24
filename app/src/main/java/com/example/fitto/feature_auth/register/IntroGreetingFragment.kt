package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentIntroGreetingBinding

class IntroGreetingFragment : Fragment(R.layout.fragment_intro_greeting) {

    private var _binding: FragmentIntroGreetingBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentIntroGreetingBinding.bind(view)
        binding.root.setOnboardingProgress(4)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        val name = onboardingViewModel.state.name
            .trim()
            .ifBlank {
                getString(R.string.app_name)
            }

        binding.introGreetingTitleText.text =
            getString(R.string.intro_greeting_title, name)

        binding.introGreetingBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.introGreetingButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_introGreetingFragment_to_goalSelectionFragment
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}