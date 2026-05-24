package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.fitto.R
import com.example.fitto.databinding.BottomSheetMealEntryPortionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

class MealEntryPortionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMealEntryPortionBinding? = null
    private val binding get() = _binding!!

    private lateinit var entryId: String
    private lateinit var title: String
    private var currentCalories: Int = 0
    private var currentProtein: Double = 0.0
    private var currentCarbs: Double = 0.0
    private var currentFat: Double = 0.0
    private var multiplier: Double = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        entryId = args.getString(ARG_ENTRY_ID).orEmpty()
        title = args.getString(ARG_TITLE).orEmpty()
        currentCalories = args.getInt(ARG_CALORIES, 0)
        currentProtein = args.getDouble(ARG_PROTEIN, 0.0)
        currentCarbs = args.getDouble(ARG_CARBS, 0.0)
        currentFat = args.getDouble(ARG_FAT, 0.0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMealEntryPortionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupClicks()
        renderPreview()
    }

    private fun setupUi() = with(binding) {
        mealEntryEditTitleText.text = title.ifBlank { getString(R.string.meal_entry_edit_title_fallback) }
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener { dismiss() }
        decreasePortionButton.setOnClickListener {
            multiplier = (multiplier - STEP).coerceAtLeast(MIN_MULTIPLIER).roundOneDecimal()
            renderPreview()
        }
        increasePortionButton.setOnClickListener {
            multiplier = (multiplier + STEP).coerceAtMost(MAX_MULTIPLIER).roundOneDecimal()
            renderPreview()
        }
        resetPortionButton.setOnClickListener {
            multiplier = 1.0
            renderPreview()
        }
        applyPortionButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_MEAL_ENTRY_PORTION_CHANGED,
                Bundle().apply {
                    putString(BUNDLE_ENTRY_ID, entryId)
                    putDouble(BUNDLE_MULTIPLIER, multiplier)
                }
            )
            dismiss()
        }
    }

    private fun renderPreview() = with(binding) {
        val percent = (multiplier * 100).roundToInt()
        portionMultiplierText.text = getString(R.string.meal_entry_edit_multiplier_value, percent)
        oldCaloriesText.text = getString(R.string.meal_entry_edit_old_calories, currentCalories)
        newCaloriesText.text = getString(
            R.string.meal_entry_edit_new_calories,
            (currentCalories * multiplier).roundToInt().coerceAtLeast(0)
        )
        newMacrosText.text = getString(
            R.string.meal_entry_edit_new_macros,
            (currentProtein * multiplier).roundOneDecimal(),
            (currentCarbs * multiplier).roundOneDecimal(),
            (currentFat * multiplier).roundOneDecimal()
        )
        applyPortionButton.isEnabled = multiplier != 1.0
        applyPortionButton.alpha = if (multiplier == 1.0) 0.55f else 1f
    }

    private fun Double.roundOneDecimal(): Double = (this * 10.0).roundToInt() / 10.0

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "meal_entry_portion_bottom_sheet"
        const val REQUEST_MEAL_ENTRY_PORTION_CHANGED = "request_meal_entry_portion_changed"
        const val BUNDLE_ENTRY_ID = "bundle_entry_id"
        const val BUNDLE_MULTIPLIER = "bundle_multiplier"

        private const val ARG_ENTRY_ID = "arg_entry_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_CALORIES = "arg_calories"
        private const val ARG_PROTEIN = "arg_protein"
        private const val ARG_CARBS = "arg_carbs"
        private const val ARG_FAT = "arg_fat"

        private const val STEP = 0.1
        private const val MIN_MULTIPLIER = 0.5
        private const val MAX_MULTIPLIER = 2.0

        fun newInstance(entry: com.example.fitto.feature_meal.model.MealEntryDetailUiModel): MealEntryPortionBottomSheet {
            return MealEntryPortionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ENTRY_ID, entry.entryId)
                    putString(ARG_TITLE, entry.title)
                    putInt(ARG_CALORIES, entry.totalCalories)
                    putDouble(ARG_PROTEIN, entry.totalProtein)
                    putDouble(ARG_CARBS, entry.totalCarbs)
                    putDouble(ARG_FAT, entry.totalFat)
                }
            }
        }
    }
}
