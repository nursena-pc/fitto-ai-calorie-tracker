package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.databinding.FragmentMealDetailBinding
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel
import com.example.fitto.feature_meal.model.MealType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealDetailFragment : Fragment(R.layout.fragment_meal_detail) {

    private var _binding: FragmentMealDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var mealRepository: MealRepository
    private lateinit var mealEntryAdapter: MealEntryDetailAdapter

    private val mealType: MealType by lazy {
        MealType.fromArgument(arguments?.getString(ARG_MEAL_TYPE))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMealDetailBinding.bind(view)
        mealRepository = MealRepository(requireContext().applicationContext)

        setupRecyclerView()
        setupUi()
        setupResultListener()
        setupClicks()
        loadMealDetails()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::mealRepository.isInitialized) {
            loadMealDetails()
        }
    }

    private fun setupRecyclerView() = with(binding) {
        mealEntryAdapter = MealEntryDetailAdapter(::showEditEntrySheet, ::confirmDeleteEntry)
        mealEntriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealEntryAdapter
            itemAnimator = null
        }
    }

    private fun setupUi() = with(binding) {
        mealDetailTitleText.text = getString(
            R.string.meal_detail_title_today,
            getString(mealType.titleRes)
        )
        mealDetailSubtitleText.text = getString(R.string.meal_detail_subtitle)
    }

    private fun setupResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddMealFragment.REQUEST_MEAL_ENTRY_ADDED,
            viewLifecycleOwner
        ) { _, _ ->
            loadMealDetails()
            parentFragmentManager.setFragmentResult(REQUEST_MEAL_DETAIL_CHANGED, Bundle.EMPTY)
        }
        childFragmentManager.setFragmentResultListener(
            MealEntryFoodsBottomSheet.REQUEST_ENTRY_FOODS_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            loadMealDetails()
            parentFragmentManager.setFragmentResult(REQUEST_MEAL_DETAIL_CHANGED, Bundle.EMPTY)
        }
        childFragmentManager.setFragmentResultListener(
            MealEntryPortionBottomSheet.REQUEST_MEAL_ENTRY_PORTION_CHANGED,
            viewLifecycleOwner
        ) { _, bundle ->
            val entryId = bundle.getString(MealEntryPortionBottomSheet.BUNDLE_ENTRY_ID).orEmpty()
            val multiplier = bundle.getDouble(MealEntryPortionBottomSheet.BUNDLE_MULTIPLIER, 1.0)
            updateEntryPortion(entryId, multiplier)
        }
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        addFoodButton.setOnClickListener {
            openAddMeal()
        }
    }

    private fun loadMealDetails() {
        if (_binding == null || !::mealRepository.isInitialized) return

        viewLifecycleOwner.lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                mealRepository.getMealSummaryForTodayAndType(mealType)
            }
            val entries = withContext(Dispatchers.IO) {
                mealRepository.getMealEntryDetailsForToday(mealType)
            }

            if (_binding == null) return@launch

            binding.mealDetailCaloriesText.text = getString(
                R.string.meal_detail_summary_calories,
                summary.totalCalories
            )
            binding.mealDetailMacrosText.text = getString(
                R.string.meal_detail_summary_macros,
                summary.totalProtein,
                summary.totalCarbs,
                summary.totalFat
            )

            mealEntryAdapter.submitList(entries)
            binding.emptyMealText.isVisible = entries.isEmpty()
            binding.mealEntriesRecyclerView.isVisible = entries.isNotEmpty()
        }
    }

    private fun showEditEntrySheet(entry: MealEntryDetailUiModel) {
        MealEntryFoodsBottomSheet.newInstance(
            entryId = entry.entryId,
            entryTitle = entry.title
        ).show(
            childFragmentManager,
            MealEntryFoodsBottomSheet.TAG
        )
    }

    private fun updateEntryPortion(entryId: String, multiplier: Double) {
        if (entryId.isBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.scaleMealEntry(entryId, multiplier)
                }
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_entry_edit_updated_message),
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.setFragmentResult(REQUEST_MEAL_DETAIL_CHANGED, Bundle.EMPTY)
                loadMealDetails()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_entry_edit_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteEntry(entry: MealEntryDetailUiModel) {
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
            .setTitle(R.string.meal_detail_delete_title)
            .setMessage(getString(R.string.meal_detail_delete_message, entry.title))
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.meal_detail_delete_confirm) { _, _ ->
                deleteEntry(entry)
            }
            .show()
    }

    private fun deleteEntry(entry: MealEntryDetailUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    mealRepository.deleteMealEntry(entry.entryId)
                }
            }.onSuccess {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_detail_deleted_message),
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.setFragmentResult(REQUEST_MEAL_DETAIL_CHANGED, Bundle.EMPTY)
                loadMealDetails()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.meal_detail_delete_error_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openAddMeal() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.homeTabContainer,
                AddMealFragment.newInstance(
                    mealType = mealType,
                    entryDate = todayIsoDate()
                )
            )
            .addToBackStack(AddMealFragment.BACK_STACK_NAME)
            .commit()
    }

    private fun todayIsoDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MEAL_TYPE = "meal_type"
        const val BACK_STACK_NAME = "meal_detail"
        const val REQUEST_MEAL_DETAIL_CHANGED = "request_meal_detail_changed"

        fun newInstance(mealType: MealType): MealDetailFragment {
            return MealDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType.argumentValue)
                }
            }
        }
    }
}
