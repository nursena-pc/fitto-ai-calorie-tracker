package com.example.fitto.feature_auth.register

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.fitto.R
import com.example.fitto.databinding.FragmentBarrierBinding

class BarrierFragment : Fragment(R.layout.fragment_barrier) {

    private var _binding: FragmentBarrierBinding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingViewModel: OnboardingViewModel

    private val selectedBarriers = linkedSetOf<BarrierType>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentBarrierBinding.bind(view)
        onboardingViewModel = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]

        binding.root.setOnboardingProgress(6)

        setupInitialState()
        setupClicks()
        setupInputs()
        updateBarrierSelection()
    }

    private fun setupInitialState() = with(binding) {
        barrierNextButton.isEnabled = false
        barrierNextButton.alpha = 0.55f
        barrierOtherInputContainer.isVisible = false
        barrierOtherErrorText.isVisible = false
    }

    private fun setupClicks() = with(binding) {
        barrierBackButton.setOnClickListener {
            findNavController().popBackStack()
        }

        barrierSupportCard.setOnClickListener {
            toggleBarrier(BarrierType.SUPPORT)
        }

        barrierBodyConcernCard.setOnClickListener {
            toggleBarrier(BarrierType.BODY_CONCERN)
        }

        barrierFamilyDietCard.setOnClickListener {
            toggleBarrier(BarrierType.FAMILY_DIET)
        }

        barrierTimeCard.setOnClickListener {
            toggleBarrier(BarrierType.TIME)
        }

        barrierMotivationCard.setOnClickListener {
            toggleBarrier(BarrierType.MOTIVATION)
        }

        barrierEmotionalEatingCard.setOnClickListener {
            toggleBarrier(BarrierType.EMOTIONAL_EATING)
        }

        barrierMealPlanningCard.setOnClickListener {
            toggleBarrier(BarrierType.MEAL_PLANNING)
        }

        barrierHealthCard.setOnClickListener {
            toggleBarrier(BarrierType.HEALTH)
        }

        barrierKnowledgeCard.setOnClickListener {
            toggleBarrier(BarrierType.KNOWLEDGE)
        }

        barrierOtherCard.setOnClickListener {
            toggleBarrier(BarrierType.OTHER)
        }

        barrierNextButton.setOnClickListener {
            submitBarriers()
        }
    }

    private fun setupInputs() = with(binding) {
        barrierOtherEditText.doAfterTextChanged {
            barrierOtherErrorText.isVisible = false
            updateNextButtonState()
        }
    }

    private fun toggleBarrier(type: BarrierType) {
        if (selectedBarriers.contains(type)) {
            selectedBarriers.remove(type)
        } else {
            if (selectedBarriers.size >= MAX_SELECTION_COUNT) {
                return
            }

            selectedBarriers.add(type)
        }

        updateBarrierSelection()
    }

    private fun updateBarrierSelection() = with(binding) {
        val isMaxReached = selectedBarriers.size >= MAX_SELECTION_COUNT

        updateBarrierCard(
            card = barrierSupportCard,
            checkView = barrierSupportCheck,
            isSelected = selectedBarriers.contains(BarrierType.SUPPORT),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierBodyConcernCard,
            checkView = barrierBodyConcernCheck,
            isSelected = selectedBarriers.contains(BarrierType.BODY_CONCERN),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierFamilyDietCard,
            checkView = barrierFamilyDietCheck,
            isSelected = selectedBarriers.contains(BarrierType.FAMILY_DIET),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierTimeCard,
            checkView = barrierTimeCheck,
            isSelected = selectedBarriers.contains(BarrierType.TIME),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierMotivationCard,
            checkView = barrierMotivationCheck,
            isSelected = selectedBarriers.contains(BarrierType.MOTIVATION),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierEmotionalEatingCard,
            checkView = barrierEmotionalEatingCheck,
            isSelected = selectedBarriers.contains(BarrierType.EMOTIONAL_EATING),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierMealPlanningCard,
            checkView = barrierMealPlanningCheck,
            isSelected = selectedBarriers.contains(BarrierType.MEAL_PLANNING),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierHealthCard,
            checkView = barrierHealthCheck,
            isSelected = selectedBarriers.contains(BarrierType.HEALTH),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierKnowledgeCard,
            checkView = barrierKnowledgeCheck,
            isSelected = selectedBarriers.contains(BarrierType.KNOWLEDGE),
            isMaxReached = isMaxReached
        )

        updateBarrierCard(
            card = barrierOtherCard,
            checkView = barrierOtherCheck,
            isSelected = selectedBarriers.contains(BarrierType.OTHER),
            isMaxReached = isMaxReached
        )

        val isOtherSelected = selectedBarriers.contains(BarrierType.OTHER)
        barrierOtherInputContainer.isVisible = isOtherSelected

        if (!isOtherSelected) {
            barrierOtherEditText.setText("")
            barrierOtherErrorText.isVisible = false
        }

        updateNextButtonState()
    }
    private fun updateBarrierCard(
        card: LinearLayout,
        checkView: FrameLayout,
        isSelected: Boolean,
        isMaxReached: Boolean
    ) {
        card.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_fitto_card_selected
            } else {
                R.drawable.bg_fitto_card_normal
            }
        )

        checkView.isVisible = isSelected

        val shouldDim = isMaxReached && !isSelected

        card.alpha = if (shouldDim) 0.42f else 1f
        card.isEnabled = !shouldDim
        card.isClickable = !shouldDim
    }

    private fun updateNextButtonState() = with(binding) {
        val hasSelection = selectedBarriers.isNotEmpty()
        val isOtherSelected = selectedBarriers.contains(BarrierType.OTHER)
        val otherText = barrierOtherEditText.text?.toString()?.trim().orEmpty()

        val canContinue = hasSelection && (!isOtherSelected || otherText.isNotBlank())

        barrierNextButton.isEnabled = canContinue
        barrierNextButton.alpha = if (canContinue) 1f else 0.55f
    }

    private fun submitBarriers() = with(binding) {
        if (selectedBarriers.isEmpty()) {
            return@with
        }

        val isOtherSelected = selectedBarriers.contains(BarrierType.OTHER)
        val otherText = barrierOtherEditText.text?.toString()?.trim().orEmpty()

        if (isOtherSelected && otherText.isBlank()) {
            barrierOtherErrorText.isVisible = true
            return@with
        }

        onboardingViewModel.saveBarriers(
            barriers = selectedBarriers.toList(),
            otherText = otherText.takeIf { it.isNotBlank() }
        )

        findNavController().navigate(R.id.action_barrierFragment_to_dailyNeedsFragment)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MAX_SELECTION_COUNT = 3
    }
}