package com.example.fitto.feature_daily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetDailyQuickAddBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DailyQuickAddBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDailyQuickAddBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDailyQuickAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClicks()
    }

    private fun setupClicks() = with(binding) {
        quickAddSearchFood.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.openAddMeal()
        }

        quickAddCameraFood.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.openCameraFoodEntry()
        }

        quickAddCreateFood.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.openAddMeal()
        }

        quickAddWater.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.addQuickWater()
        }

        quickAddActivity.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.openAddExercise()
        }

        quickAddDietPlan.setOnClickListener {
            dismiss()
            (parentFragment as? DailyFragment)?.openDietPlans()
        }
    }

    private fun openAddMealFromSheet() {
        dismiss()
        (parentFragment as? DailyFragment)?.openAddMeal()
    }

    private fun handleAction(label: String) {
        dismiss()
        Toast.makeText(
            requireContext(),
            getString(R.string.daily_mock_action_message, label),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DailyQuickAddBottomSheet"
    }
}
