package com.example.fitto.feature_exercise

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetExerciseFilterBinding
import com.example.fitto.feature_exercise.model.ExerciseDurationRange
import com.example.fitto.feature_exercise.model.ExerciseFilterState
import com.example.fitto.feature_exercise.model.ExerciseIntensity
import com.example.fitto.feature_exercise.model.ExerciseSort
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ExerciseFilterBottomSheetFragment : BottomSheetDialogFragment(
    R.layout.bottom_sheet_exercise_filter
) {

    private var _binding: BottomSheetExerciseFilterBinding? = null
    private val binding get() = _binding!!

    private var currentFilterState = ExerciseFilterState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentFilterState = ExerciseFilterState(
            durationRange = arguments?.getString(ARG_DURATION_RANGE)
                ?.let { runCatching { ExerciseDurationRange.valueOf(it) }.getOrNull() }
                ?: ExerciseDurationRange.ALL,
            intensity = arguments?.getString(ARG_INTENSITY)
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { ExerciseIntensity.valueOf(it) }.getOrNull() },
            sort = arguments?.getString(ARG_SORT)
                ?.let { runCatching { ExerciseSort.valueOf(it) }.getOrNull() }
                ?: ExerciseSort.RECOMMENDED
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetExerciseFilterBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        renderCurrentState()
        setupClicks()
    }

    private fun renderCurrentState() = with(binding) {
        durationChipGroup.check(
            when (currentFilterState.durationRange) {
                ExerciseDurationRange.ALL -> R.id.durationAllChip
                ExerciseDurationRange.SHORT -> R.id.durationShortChip
                ExerciseDurationRange.MEDIUM -> R.id.durationMediumChip
                ExerciseDurationRange.LONG -> R.id.durationLongChip
            }
        )

        intensityChipGroup.check(
            when (currentFilterState.intensity) {
                null -> R.id.intensityAllChip
                ExerciseIntensity.LIGHT -> R.id.intensityLightChip
                ExerciseIntensity.MODERATE -> R.id.intensityModerateChip
                ExerciseIntensity.VIGOROUS -> R.id.intensityVigorousChip
            }
        )

        sortChipGroup.check(
            when (currentFilterState.sort) {
                ExerciseSort.RECOMMENDED -> R.id.sortRecommendedChip
                ExerciseSort.KCAL_LOW_HIGH -> R.id.sortKcalLowHighChip
                ExerciseSort.KCAL_HIGH_LOW -> R.id.sortKcalHighLowChip
                ExerciseSort.DURATION_LOW_HIGH -> R.id.sortDurationLowHighChip
            }
        )
    }

    private fun setupClicks() = with(binding) {
        resetFilterButton.setOnClickListener {
            currentFilterState = ExerciseFilterState()
            renderCurrentState()
        }

        applyFilterButton.setOnClickListener {
            val selectedState = ExerciseFilterState(
                durationRange = selectedDurationRange(),
                intensity = selectedIntensity(),
                sort = selectedSort()
            )

            parentFragmentManager.setFragmentResult(
                REQUEST_EXERCISE_FILTER,
                bundleOf(
                    BUNDLE_DURATION_RANGE to selectedState.durationRange.name,
                    BUNDLE_INTENSITY to selectedState.intensity?.name.orEmpty(),
                    BUNDLE_SORT to selectedState.sort.name
                )
            )

            dismiss()
        }
    }

    private fun selectedDurationRange(): ExerciseDurationRange {
        return when (binding.durationChipGroup.checkedChipId) {
            R.id.durationShortChip -> ExerciseDurationRange.SHORT
            R.id.durationMediumChip -> ExerciseDurationRange.MEDIUM
            R.id.durationLongChip -> ExerciseDurationRange.LONG
            else -> ExerciseDurationRange.ALL
        }
    }

    private fun selectedIntensity(): ExerciseIntensity? {
        return when (binding.intensityChipGroup.checkedChipId) {
            R.id.intensityLightChip -> ExerciseIntensity.LIGHT
            R.id.intensityModerateChip -> ExerciseIntensity.MODERATE
            R.id.intensityVigorousChip -> ExerciseIntensity.VIGOROUS
            else -> null
        }
    }

    private fun selectedSort(): ExerciseSort {
        return when (binding.sortChipGroup.checkedChipId) {
            R.id.sortKcalLowHighChip -> ExerciseSort.KCAL_LOW_HIGH
            R.id.sortKcalHighLowChip -> ExerciseSort.KCAL_HIGH_LOW
            R.id.sortDurationLowHighChip -> ExerciseSort.DURATION_LOW_HIGH
            else -> ExerciseSort.RECOMMENDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_EXERCISE_FILTER = "request_exercise_filter"
        const val BUNDLE_DURATION_RANGE = "bundle_duration_range"
        const val BUNDLE_INTENSITY = "bundle_intensity"
        const val BUNDLE_SORT = "bundle_sort"

        private const val ARG_DURATION_RANGE = "arg_duration_range"
        private const val ARG_INTENSITY = "arg_intensity"
        private const val ARG_SORT = "arg_sort"

        fun newInstance(filterState: ExerciseFilterState): ExerciseFilterBottomSheetFragment {
            return ExerciseFilterBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_DURATION_RANGE to filterState.durationRange.name,
                    ARG_INTENSITY to filterState.intensity?.name.orEmpty(),
                    ARG_SORT to filterState.sort.name
                )
            }
        }
    }
}