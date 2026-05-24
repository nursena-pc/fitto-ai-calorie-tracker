package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.fitto.R
import com.example.fitto.data.remote.logmeal.LogMealFoodCandidate
import com.example.fitto.databinding.BottomSheetLogmealCandidatePortionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class LogMealCandidatePortionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLogmealCandidatePortionBinding? = null
    private val binding get() = _binding!!

    private val candidate: LogMealFoodCandidate by lazy {
        @Suppress("DEPRECATION")
        requireArguments().getSerializable(ARG_CANDIDATE) as LogMealFoodCandidate
    }

    private var amountGrams: Double = 100.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLogmealCandidatePortionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        amountGrams = candidate.servingGrams?.takeIf { it > 0.0 } ?: 100.0

        binding.titleText.text = candidate.name.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }

        binding.amountEditText.setText(formatAmount(amountGrams))
        updateNutritionPreview()

        binding.amountEditText.post {
            binding.amountEditText.setSelection(
                binding.amountEditText.text?.length ?: 0
            )
        }

        binding.minusButton.setOnClickListener {
            amountGrams = (amountGrams - 10.0).coerceAtLeast(1.0)
            binding.amountEditText.setText(formatAmount(amountGrams))
            updateNutritionPreview()
        }

        binding.plusButton.setOnClickListener {
            amountGrams = (amountGrams + 10.0).coerceAtMost(2000.0)
            binding.amountEditText.setText(formatAmount(amountGrams))
            updateNutritionPreview()
        }

        binding.amountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                syncAmountFromInput()
            }
        }

        binding.applyButton.setOnClickListener {
            syncAmountFromInput()

            val updatedCandidate = candidate.withUpdatedServing(amountGrams)

            parentFragmentManager.setFragmentResult(
                REQUEST_PORTION_UPDATED,
                bundleOf(BUNDLE_UPDATED_CANDIDATE to updatedCandidate)
            )

            dismiss()
        }
    }

    private fun syncAmountFromInput() {
        val value = binding.amountEditText.text
            ?.toString()
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?: amountGrams

        amountGrams = value.coerceIn(1.0, 2000.0)
        binding.amountEditText.setText(formatAmount(amountGrams))
        updateNutritionPreview()
    }

    private fun updateNutritionPreview() {
        val preview = candidate.withUpdatedServing(amountGrams)

        binding.calorieText.text = getString(
            R.string.logmeal_candidate_kcal,
            preview.calories?.roundToInt() ?: 0
        )

        binding.detailText.text = getString(
            R.string.logmeal_candidate_detail,
            formatAmount(amountGrams),
            formatAmount(preview.protein ?: 0.0),
            formatAmount(preview.carbs ?: 0.0),
            formatAmount(preview.fat ?: 0.0)
        )
    }

    private fun LogMealFoodCandidate.withUpdatedServing(
        newServingGrams: Double
    ): LogMealFoodCandidate {
        val oldServingGrams = servingGrams?.takeIf { it > 0.0 } ?: 100.0
        val factor = newServingGrams / oldServingGrams

        return copy(
            servingGrams = newServingGrams,
            calories = calories?.let { it * factor },
            protein = protein?.let { it * factor },
            carbs = carbs?.let { it * factor },
            fat = fat?.let { it * factor },
            fiber = fiber?.let { it * factor }
        )
    }

    private fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "logmeal_candidate_portion"

        const val REQUEST_PORTION_UPDATED = "request_logmeal_candidate_portion_updated"
        const val BUNDLE_UPDATED_CANDIDATE = "bundle_updated_candidate"

        private const val ARG_CANDIDATE = "arg_candidate"

        fun newInstance(
            candidate: LogMealFoodCandidate
        ): LogMealCandidatePortionBottomSheet {
            return LogMealCandidatePortionBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CANDIDATE, candidate)
                }
            }
        }
    }
}