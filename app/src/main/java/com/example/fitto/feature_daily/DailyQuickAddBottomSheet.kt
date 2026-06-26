package com.example.fitto.feature_daily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            runDailyAction { openAddMeal() }
        }

        quickAddCameraFood.setOnClickListener {
            runDailyAction { openCameraFoodEntry() }
        }

        quickAddCreateFood.setOnClickListener {
            runDailyAction { openAddMeal() }
        }

        quickAddWater.setOnClickListener {
            runDailyAction { addQuickWater() }
        }

        quickAddActivity.setOnClickListener {
            runDailyAction { openAddExercise() }
        }

        quickAddDietPlan.setOnClickListener {
            runDailyAction { openDietPlans() }
        }
    }

    private fun runDailyAction(action: DailyFragment.() -> Unit) {
        dismiss()
        (parentFragment as? DailyFragment)?.action()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DailyQuickAddBottomSheet"
    }
}
