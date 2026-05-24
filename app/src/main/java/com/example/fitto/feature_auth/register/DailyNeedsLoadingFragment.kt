package com.example.fitto.feature_auth.register

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentDailyNeedsLoadingBinding

class DailyNeedsLoadingFragment : Fragment(R.layout.fragment_daily_needs_loading) {

    private var _binding: FragmentDailyNeedsLoadingBinding? = null
    private val binding get() = _binding!!

    private var buttonFillAnimator: ValueAnimator? = null
    private var isPlanReady = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentDailyNeedsLoadingBinding.bind(view)
        binding.root.setOnboardingProgress(11)

        setupInitialState()
        setupClicks()
        startButtonLoadingAnimation()
    }

    private fun setupInitialState() = with(binding) {
        isPlanReady = false

        showPlanButton.isEnabled = false
        showPlanButton.alpha = 1f
        showPlanButtonText.text = getString(R.string.daily_needs_preparing)

        showPlanButtonFill.layoutParams = showPlanButtonFill.layoutParams.apply {
            width = 0
        }
    }

    private fun setupClicks() = with(binding) {
        dailyNeedsLoadingBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        showPlanButton.setOnClickListener {
            if (!isPlanReady) return@setOnClickListener

            // TODO: Result ekranını bağlayınca burayı navigation yapacağız.
            findNavController().navigate(
                R.id.action_dailyNeedsLoadingFragment_to_dailyNeedsResultFragment
            )

            // Örnek:
            // findNavController().navigate(
            //     R.id.action_dailyNeedsLoadingFragment_to_dailyNeedsResultFragment
            // )
        }
    }

    private fun startButtonLoadingAnimation() = with(binding) {
        buttonFillAnimator?.cancel()

        showPlanButton.doOnLayout {
            val targetWidth = showPlanButton.width

            buttonFillAnimator = ValueAnimator.ofInt(0, targetWidth).apply {
                duration = CALCULATION_DELAY_MILLIS
                interpolator = LinearInterpolator()

                addUpdateListener { animator ->
                    val animatedWidth = animator.animatedValue as Int

                    showPlanButtonFill.layoutParams =
                        showPlanButtonFill.layoutParams.apply {
                            width = animatedWidth
                        }
                }

                doOnEnd {
                    if (_binding == null) return@doOnEnd

                    isPlanReady = true
                    showPlanButton.isEnabled = true
                    showPlanButtonText.text = getString(R.string.show_my_plan)

                    showPlanButtonFill.layoutParams =
                        showPlanButtonFill.layoutParams.apply {
                            width = targetWidth
                        }
                }

                start()
            }
        }
    }

    override fun onDestroyView() {
        buttonFillAnimator?.cancel()
        buttonFillAnimator = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val CALCULATION_DELAY_MILLIS = 4000L
    }
}