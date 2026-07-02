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
            dispatch(DailyQuickAction.SEARCH_FOOD)
        }

        quickAddCameraFood.setOnClickListener {
            dispatch(DailyQuickAction.CAMERA_FOOD)
        }

        quickAddCreateFood.setOnClickListener {
            dispatch(DailyQuickAction.CREATE_FOOD)
        }

        quickAddWater.setOnClickListener {
            dispatch(DailyQuickAction.WATER)
        }

        quickAddActivity.setOnClickListener {
            dispatch(DailyQuickAction.ACTIVITY)
        }

        quickAddDietPlan.setOnClickListener {
            dispatch(DailyQuickAction.DIET_PLAN)
        }
    }

    private fun dispatch(action: DailyQuickAction) {
        dismiss()
        parentFragmentManager.setFragmentResult(
            REQUEST_QUICK_ACTION,
            Bundle().apply {
                putString(BUNDLE_QUICK_ACTION, action.name)
            }
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DailyQuickAddBottomSheet"
        const val REQUEST_QUICK_ACTION = "daily_quick_action"
        const val BUNDLE_QUICK_ACTION = "daily_quick_action_value"
    }
}
