package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.databinding.BottomSheetMealEntryFoodsBinding
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MealEntryFoodsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMealEntryFoodsBinding? = null
    private val binding get() = _binding!!

    private lateinit var mealRepository: MealRepository
    private lateinit var adapter: MealEntryFoodAdapter

    private val entryId: String by lazy {
        requireArguments().getString(ARG_ENTRY_ID).orEmpty()
    }

    private val entryTitle: String by lazy {
        requireArguments().getString(ARG_ENTRY_TITLE).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMealEntryFoodsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mealRepository = MealRepository(requireContext().applicationContext)

        setupUi()
        setupRecyclerView()
        setupResultListener()
        setupClicks()
        loadFoods()
    }

    private fun setupUi() = with(binding) {
        titleText.text = entryTitle.ifBlank {
            getString(R.string.meal_entry_foods_title)
        }
        subtitleText.text = getString(R.string.meal_entry_foods_subtitle)
    }

    private fun setupRecyclerView() = with(binding) {
        adapter = MealEntryFoodAdapter(
            onEditClick = ::openAmountEditor,
            onDeleteClick = ::confirmDeleteFood
        )

        foodsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MealEntryFoodsBottomSheet.adapter
            itemAnimator = null
        }
    }

    private fun setupResultListener() {
        childFragmentManager.setFragmentResultListener(
            MealEntryFoodAmountBottomSheet.REQUEST_AMOUNT_CHANGED,
            viewLifecycleOwner
        ) { _, bundle ->
            val foodRowId = bundle.getString(
                MealEntryFoodAmountBottomSheet.BUNDLE_FOOD_ROW_ID
            ).orEmpty()

            val newAmount = bundle.getDouble(
                MealEntryFoodAmountBottomSheet.BUNDLE_NEW_AMOUNT,
                0.0
            )

            updateFoodAmount(foodRowId, newAmount)
        }
    }

    private fun setupClicks() = with(binding) {
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun loadFoods() {
        if (entryId.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val foods = withContext(Dispatchers.IO) {
                mealRepository.getFoodsForMealEntry(entryId)
            }

            if (_binding == null) return@launch

            adapter.submitList(foods)
            binding.emptyText.isVisible = foods.isEmpty()
            binding.foodsRecyclerView.isVisible = foods.isNotEmpty()
        }
    }

    private fun openAmountEditor(food: MealEntryFoodUiModel) {
        MealEntryFoodAmountBottomSheet.newInstance(food).show(
            childFragmentManager,
            MealEntryFoodAmountBottomSheet.TAG
        )
    }

    private fun confirmDeleteFood(food: MealEntryFoodUiModel) {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Fitto_WhiteAlertDialog
        )
            .setBackground(
                androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_dialog_white
                )
            )
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.meal_entry_food_delete_title)
            .setMessage(getString(R.string.meal_entry_food_delete_message, food.foodName))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.meal_detail_delete_confirm) { _, _ ->
                deleteFood(food.id)
            }
            .show()
    }

    private fun updateFoodAmount(foodRowId: String, newAmount: Double) {
        if (foodRowId.isBlank() || newAmount <= 0.0) return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.updateMealEntryFoodAmount(
                        foodRowId = foodRowId,
                        newAmount = newAmount
                    )
                }
            }.onSuccess {
                parentFragmentManager.setFragmentResult(
                    REQUEST_ENTRY_FOODS_CHANGED,
                    Bundle.EMPTY
                )
                loadFoods()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_entry_food_update_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteFood(foodRowId: String) {
        if (foodRowId.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.deleteMealEntryFood(foodRowId)
                }
            }.onSuccess {
                parentFragmentManager.setFragmentResult(
                    REQUEST_ENTRY_FOODS_CHANGED,
                    Bundle.EMPTY
                )
                loadFoods()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_entry_food_delete_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "meal_entry_foods_bottom_sheet"
        const val REQUEST_ENTRY_FOODS_CHANGED = "request_entry_foods_changed"

        private const val ARG_ENTRY_ID = "arg_entry_id"
        private const val ARG_ENTRY_TITLE = "arg_entry_title"

        fun newInstance(
            entryId: String,
            entryTitle: String
        ): MealEntryFoodsBottomSheet {
            return MealEntryFoodsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ENTRY_ID, entryId)
                    putString(ARG_ENTRY_TITLE, entryTitle)
                }
            }
        }
    }
}