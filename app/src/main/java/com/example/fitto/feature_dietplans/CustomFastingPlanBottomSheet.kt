package com.example.fitto.feature_dietplans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetCustomFastingPlanBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomFastingPlanBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCustomFastingPlanBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.ThemeOverlay_Fitto_WhiteBottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCustomFastingPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialHours = arguments?.getInt(
            ARG_INITIAL_FASTING_HOURS,
            CustomFastingPlanRules.DEFAULT_FASTING_HOURS
        ) ?: CustomFastingPlanRules.DEFAULT_FASTING_HOURS
        setupUi(initialHours)
        setupClicks()
    }

    private fun setupUi(initialHours: Int) = with(binding) {
        val safeInitialHours = CustomFastingPlanRules.sanitizeFastingHours(initialHours)

        fastingHoursSlider.value = safeInitialHours.toFloat()
        updatePreview(safeInitialHours)

        fastingHoursSlider.addOnChangeListener { _, value, _ ->
            updatePreview(value.toInt())
        }
    }

    private fun updatePreview(fastingHours: Int) = with(binding) {
        val schedule = CustomFastingPlanRules.resolveSchedule(fastingHours)

        selectedRatioText.text = getString(
            R.string.if_custom_dialog_preview,
            schedule.fastingHours,
            schedule.eatingHours
        )

        summaryText.text = getString(R.string.if_custom_dialog_summary)

        startInfoText.text = getString(R.string.if_custom_dialog_start_info)
        eatInfoText.text = getString(
            R.string.if_custom_dialog_eat_info,
            schedule.eatingHours
        )
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener { dismiss() }
        cancelButton.setOnClickListener { dismiss() }

        applyButton.setOnClickListener {
            val fastingHours = fastingHoursSlider.value.toInt()

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putInt(BUNDLE_FASTING_HOURS, fastingHours)
                }
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "CustomFastingPlanBottomSheet"
        const val REQUEST_KEY = "custom_fasting_plan_request"
        const val BUNDLE_FASTING_HOURS = "bundle_fasting_hours"
        private const val ARG_INITIAL_FASTING_HOURS = "arg_initial_fasting_hours"

        fun newInstance(
            initialHours: Int = CustomFastingPlanRules.DEFAULT_FASTING_HOURS
        ): CustomFastingPlanBottomSheet {
            return CustomFastingPlanBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_FASTING_HOURS, initialHours)
                }
            }
        }
    }
}
