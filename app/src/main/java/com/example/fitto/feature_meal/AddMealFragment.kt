package com.example.fitto.feature_meal

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.databinding.FragmentAddMealBinding
import com.example.fitto.feature_meal.model.FoodUiModel
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import com.example.fitto.fittoAppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddMealFragment : Fragment(R.layout.fragment_add_meal) {

    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!

    private val targetEntryDate: String by lazy {
        arguments?.getString(ARG_ENTRY_DATE) ?: LocalDate.now().toString()
    }

    private val targetMealType: MealType by lazy {
        MealType.fromArgument(arguments?.getString(ARG_MEAL_TYPE))
    }

    private lateinit var suggestedFoodAdapter: AddMealFoodAdapter
    private lateinit var selectedFoodAdapter: SelectedFoodAdapter
    private var searchJob: Job? = null
    private val foodPresentationResolver = AddMealFoodPresentationResolver()
    private val searchStateResolver = AddMealSearchStateResolver()
    private val screenStateStore = AddMealScreenStateStore()
    private lateinit var coordinator: AddMealCoordinator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddMealBinding.bind(view)

        coordinator = requireContext().fittoAppContainer.createAddMealCoordinator()

        setupRecyclerViews()
        setupUi()
        setupPortionResultListener()
        setupCameraFoodResultListener()
        setupClicks()
        renderSelectedFoods()
        setupCustomFoodResultListener()

        searchFoods(
            query = binding.searchEditText.text?.toString().orEmpty(),
            debounce = false,
            allowOnlineSearch = false
        )
    }

    private fun setupRecyclerViews() = with(binding) {
        suggestedFoodAdapter = AddMealFoodAdapter(
            onAddClick = ::openPortionBottomSheet,
            onFavoriteClick = ::toggleFavoriteFood
        )
        selectedFoodAdapter = SelectedFoodAdapter(::removeFoodFromSelection)

        suggestedFoodsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = suggestedFoodAdapter
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        selectedFoodsRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = selectedFoodAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupUi() = with(binding) {
        targetMealChipText.text = getString(
            R.string.add_meal_target_meal_chip,
            getString(targetMealType.titleRes)
        )
    }

    private fun setupPortionResultListener() {
        childFragmentManager.setFragmentResultListener(
            FoodPortionBottomSheet.REQUEST_PORTION_SELECTED,
            viewLifecycleOwner
        ) { _, bundle ->
            screenStateStore.addSelectedFood(
                FoodPortionBottomSheet.run {
                    bundle.toSelectedFoodUiModel()
                }
            )
            renderSelectedFoods()
        }
    }

    private fun setupClicks() = with(binding) {
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadMoreFoodsButton.setOnClickListener {
            loadMoreUsdaFoods()
        }
        cameraButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.homeTabContainer,
                    CameraFoodEntryFragment.newInstance(
                        mealType = targetMealType
                    )
                )
                .addToBackStack(CameraFoodEntryFragment.BACK_STACK_NAME)
                .commit()
        }

        favoriteButton.setOnClickListener {
            showFavoriteFoods()
        }

        seeAllFoodsText.setOnClickListener {
            CreateCustomFoodBottomSheet.newInstance(
                mealType = targetMealType.argumentValue
            ).show(
                childFragmentManager,
                CreateCustomFoodBottomSheet.TAG
            )
        }

        addMealButton.setOnClickListener {
            saveSelectedFoodsToMeal()
        }

        searchEditText.doAfterTextChanged { editable ->
            searchFoods(
                query = editable?.toString().orEmpty(),
                debounce = true,
                allowOnlineSearch = false
            )
        }

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val isDoneAction = actionId == EditorInfo.IME_ACTION_DONE
            val isEnterUp = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_UP

            if (isSearchAction || isDoneAction || isEnterUp) {
                val query = searchEditText.text?.toString().orEmpty()

                Log.d(
                    "AddMealFragment",
                    "Search submitted. query=$query allowOnlineSearch=true actionId=$actionId"
                )

                searchFoods(
                    query = query,
                    debounce = false,
                    allowOnlineSearch = true
                )

                searchEditText.clearFocus()
                true
            } else {
                false
            }
        }
    }

    private fun setupCameraFoodResultListener() {
        parentFragmentManager.setFragmentResultListener(
            CameraFoodEntryFragment.REQUEST_CAMERA_FOODS_RECOGNIZED,
            viewLifecycleOwner
        ) { _, bundle ->
            val foodIds = bundle
                .getStringArrayList(CameraFoodEntryFragment.BUNDLE_FOOD_IDS)
                .orEmpty()

            if (foodIds.isEmpty()) return@setFragmentResultListener

            val request = AddMealDetectedFoodsRequest(foodIds = foodIds)

            viewLifecycleOwner.lifecycleScope.launch {
                val result = coordinator.loadDetectedFoods(request)

                if (_binding == null) return@launch

                when (result) {
                    AddMealDetectedFoodsResult.NoFoods -> Unit
                    is AddMealDetectedFoodsResult.Imported -> {
                        screenStateStore.addDetectedFoods(result.detectedFoods)
                        renderSelectedFoods()
                        saveSelectedFoodsToMeal()
                    }
                }
            }
        }
    }

    private fun setupCustomFoodResultListener() {
        childFragmentManager.setFragmentResultListener(
            CreateCustomFoodBottomSheet.REQUEST_CUSTOM_FOOD_CREATED,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString(CreateCustomFoodBottomSheet.BUNDLE_NAME).orEmpty()
            val servingAmount = bundle.getDouble(
                CreateCustomFoodBottomSheet.BUNDLE_SERVING_AMOUNT,
                100.0
            )
            val unit = bundle.getString(CreateCustomFoodBottomSheet.BUNDLE_UNIT).orEmpty()
            val calories = bundle.getDouble(
                CreateCustomFoodBottomSheet.BUNDLE_CALORIES_PER_100G,
                0.0
            )
            val protein = bundle.getDouble(
                CreateCustomFoodBottomSheet.BUNDLE_PROTEIN_PER_100G,
                0.0
            )
            val carbs = bundle.getDouble(
                CreateCustomFoodBottomSheet.BUNDLE_CARBS_PER_100G,
                0.0
            )
            val fat = bundle.getDouble(
                CreateCustomFoodBottomSheet.BUNDLE_FAT_PER_100G,
                0.0
            )
            val mealType = bundle.getString(
                CreateCustomFoodBottomSheet.BUNDLE_MEAL_TYPE
            ).orEmpty()

            createCustomFoodAndOpenPortionSheet(
                name = name,
                servingAmount = servingAmount,
                unit = unit,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                mealType = mealType
            )
        }
    }

    private fun createCustomFoodAndOpenPortionSheet(
        name: String,
        servingAmount: Double,
        unit: String,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        mealType: String
    ) {
        val request = AddMealCustomFoodRequest(
            name = name,
            servingAmount = servingAmount,
            unit = unit,
            caloriesPer100g = caloriesPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            mealType = mealType
        )

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = coordinator.createCustomFood(request)) {
                is AddMealCustomFoodResult.Created -> showCreatedCustomFood(result.food)
                is AddMealCustomFoodResult.Failed -> showCustomFoodSaveError()
            }
        }
    }

    private fun showCreatedCustomFood(food: FoodEntity) {
        val foodUiModel = food.toFoodUiModel()
        openPortionBottomSheet(foodUiModel)

        searchFoods(
            query = binding.searchEditText.text?.toString().orEmpty(),
            debounce = false,
            allowOnlineSearch = false
        )
    }

    private fun showCustomFoodSaveError() {
        Toast.makeText(
            requireContext(),
            getString(R.string.custom_food_save_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun searchFoods(
        query: String,
        debounce: Boolean,
        allowOnlineSearch: Boolean
    ) {
        searchJob?.cancel()
        val contentToken = screenStateStore.beginFoodContentLoad()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            if (debounce) delay(300)

            val normalizedQuery = searchStateResolver.normalizeQuery(query)
            if (context == null) return@launch

            Log.d(
                "AddMealFragment",
                "searchFoods query=$normalizedQuery allowOnlineSearch=$allowOnlineSearch"
            )

            if (_binding != null && searchStateResolver.shouldShowSearchingTitle(normalizedQuery)) {
                binding.foodsSectionTitleText.text = getString(R.string.add_meal_searching_foods)
            }

            if (searchStateResolver.shouldSearchOnline(normalizedQuery, allowOnlineSearch)) {
                Log.d(
                    "AddMealFragment",
                    "USDA search will run for query=$normalizedQuery"
                )
            }

            val searchResult = coordinator.search(
                AddMealFoodSearchRequest(
                    normalizedQuery = normalizedQuery,
                    mealType = targetMealType.argumentValue,
                    allowOnlineSearch = allowOnlineSearch,
                    onlinePageSize = USDA_PAGE_SIZE
                )
            )

            if (_binding == null) return@launch

            val favoriteIds = coordinator.loadFavoriteIds()

            if (_binding == null) return@launch
            if (!screenStateStore.applySearchResult(contentToken, searchResult)) return@launch

            binding.foodsSectionTitleText.text = getString(searchResult.section.titleRes())
            showEmptySearchFeedback(searchResult.emptyFeedback)
            renderLoadMoreButton()
            suggestedFoodAdapter.submitList(
                foodPresentationResolver.toFoodUiModels(
                    foods = screenStateStore.current().currentFoodResults,
                    languageCode = currentLanguageCode(),
                    favoriteIds = favoriteIds
                )
            )
        }
    }

    private fun loadMoreUsdaFoods() {
        val loadToken = screenStateStore.beginLoadMore() ?: return
        renderLoadMoreButton()

        val request = AddMealLoadMoreRequest(
            query = loadToken.query,
            mealType = targetMealType.argumentValue,
            currentFoods = loadToken.currentFoods,
            currentPage = loadToken.currentPage,
            pageSize = USDA_PAGE_SIZE
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = coordinator.loadMore(request)
            val favoriteIds = if (result is AddMealLoadMoreActionResult.Loaded) {
                coordinator.loadFavoriteIds()
            } else {
                emptySet()
            }

            if (!screenStateStore.applyLoadMoreResult(loadToken, result)) return@launch
            if (_binding == null) return@launch

            when (result) {
                is AddMealLoadMoreActionResult.NoMoreFoods -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.add_meal_no_more_foods),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AddMealLoadMoreActionResult.Loaded -> {
                    suggestedFoodAdapter.submitList(
                        foodPresentationResolver.toFoodUiModels(
                            foods = screenStateStore.current().currentFoodResults,
                            languageCode = currentLanguageCode(),
                            favoriteIds = favoriteIds
                        )
                    )
                }
            }

            renderLoadMoreButton()
        }
    }

    private fun AddMealFoodSection.titleRes(): Int {
        return when (this) {
            AddMealFoodSection.RECENT -> R.string.add_meal_recent_foods
            AddMealFoodSection.SUGGESTED -> R.string.add_meal_suggested_foods
            AddMealFoodSection.RESULTS -> R.string.add_meal_food_results
        }
    }

    private fun showEmptySearchFeedback(feedback: AddMealEmptySearchFeedback) {
        val messageRes = when (feedback) {
            AddMealEmptySearchFeedback.NONE -> return
            AddMealEmptySearchFeedback.NO_ONLINE_FOOD -> R.string.add_meal_no_online_food_found
            AddMealEmptySearchFeedback.PRESS_SEARCH_FOR_MORE_RESULTS -> {
                R.string.add_meal_press_search_for_more_results
            }
        }

        Toast.makeText(
            requireContext(),
            getString(messageRes),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderLoadMoreButton() {
        if (_binding == null) return

        val screenState = screenStateStore.current()
        val uiState = screenStateStore.loadMoreUiState()

        binding.loadMoreFoodsButton.visibility = if (uiState.visible) View.VISIBLE else View.GONE
        binding.loadMoreFoodsButton.isEnabled = uiState.enabled
        binding.loadMoreFoodsButton.text = getString(
            if (screenState.onlinePaging.isLoadingMore) {
                R.string.add_meal_loading_more_foods
            } else {
                R.string.add_meal_load_more_foods
            }
        )
    }

    private fun showFavoriteFoods() {
        searchJob?.cancel()
        val contentToken = screenStateStore.beginFoodContentLoad()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = coordinator.loadFavorites()

            if (!screenStateStore.applyFavoriteFoods(contentToken, result.foods)) return@launch
            if (_binding == null) return@launch

            binding.foodsSectionTitleText.text = getString(R.string.add_meal_favorite_foods)

            if (result.shouldShowEmptyFeedback) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_meal_no_favorite_foods),
                    Toast.LENGTH_SHORT
                ).show()
            }

            renderLoadMoreButton()
            suggestedFoodAdapter.submitList(
                foodPresentationResolver.toFoodUiModels(
                    foods = screenStateStore.current().currentFoodResults,
                    languageCode = currentLanguageCode(),
                    favoriteIds = result.foods.mapTo(mutableSetOf()) { food -> food.id }
                )
            )
        }
    }

    private fun toggleFavoriteFood(food: FoodUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = coordinator.toggleFavorite(food.id)

            val message = when (result.feedback) {
                AddMealFavoriteToggleFeedback.ADDED -> {
                    getString(R.string.add_meal_added_to_favorites, food.name)
                }
                AddMealFavoriteToggleFeedback.REMOVED -> {
                    getString(R.string.add_meal_removed_from_favorites, food.name)
                }
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            if (result.shouldRefreshSearch) {
                val currentQuery = binding.searchEditText.text?.toString().orEmpty()
                searchFoods(
                    query = currentQuery,
                    debounce = false,
                    allowOnlineSearch = false
                )
            }
        }
    }

    private fun saveSelectedFoodsToMeal() {
        val saveSnapshot = screenStateStore.beginSave() ?: return
        renderSelectedFoods()

        val request = AddMealSaveRequest(
            mealType = targetMealType,
            selectedFoods = saveSnapshot.selectedFoods,
            entryDate = targetEntryDate
        )

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = coordinator.save(request)) {
                AddMealSaveResult.EmptySelection -> restoreSaveState()
                is AddMealSaveResult.Failed -> showSaveError()
                is AddMealSaveResult.Saved -> showSaveSuccess(result)
            }
        }
    }

    private fun showSaveSuccess(result: AddMealSaveResult.Saved) {
        val mealName = getString(result.mealType.titleRes)

        parentFragmentManager.setFragmentResult(
            REQUEST_MEAL_ENTRY_ADDED,
            Bundle().apply {
                putString(BUNDLE_MEAL_TYPE, result.mealType.argumentValue)
                putString(BUNDLE_ENTRY_DATE, result.entryDate)
            }
        )

        Toast.makeText(
            requireContext(),
            getString(
                R.string.add_meal_mock_added_message,
                result.selectedCount,
                mealName
            ),
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun showSaveError() {
        restoreSaveState()

        Toast.makeText(
            requireContext(),
            getString(R.string.add_meal_save_error_message),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun restoreSaveState() {
        screenStateStore.finishSave()
        if (_binding != null) renderSelectedFoods()
    }

    private fun openPortionBottomSheet(food: FoodUiModel) {
        FoodPortionBottomSheet.newInstance(food).show(
            childFragmentManager,
            FoodPortionBottomSheet.TAG
        )
    }

    private fun removeFoodFromSelection(food: SelectedFoodUiModel) {
        screenStateStore.removeSelectedFood(food)
        renderSelectedFoods()
    }

    private fun renderSelectedFoods() = with(binding) {
        selectedFoodAdapter.submitList(screenStateStore.current().selectedFoods)

        val selectionState = screenStateStore.selectionUiState()
        selectedSummaryText.text = getString(
            R.string.add_meal_selected_summary,
            selectionState.selectedCount,
            selectionState.totalCalories
        )

        selectedFoodsRecyclerView.visibility =
            if (selectionState.selectedListVisible) View.VISIBLE else View.GONE

        emptySelectedText.visibility =
            if (selectionState.emptyStateVisible) View.VISIBLE else View.GONE

        addMealButton.isEnabled = selectionState.saveButtonEnabled
        addMealButton.alpha = selectionState.saveButtonAlpha
    }

    private fun FoodEntity.toFoodUiModel(
        isFavorite: Boolean = false
    ): FoodUiModel {
        return foodPresentationResolver.toFoodUiModel(
            food = this,
            languageCode = currentLanguageCode(),
            isFavorite = isFavorite
        )
    }

    private fun currentLanguageCode(): String {
        return resources.configuration.locales[0].language
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_MEAL_TYPE = "meal_type"
        private const val ARG_ENTRY_DATE = "entry_date"
        private const val USDA_PAGE_SIZE = 8

        const val BACK_STACK_NAME = "add_meal"
        const val REQUEST_MEAL_ENTRY_ADDED = "request_meal_entry_added"
        const val BUNDLE_MEAL_TYPE = "bundle_meal_type"
        const val BUNDLE_ENTRY_DATE = "bundle_entry_date"

        fun newInstance(
            mealType: MealType = MealType.BREAKFAST,
            entryDate: String
        ): AddMealFragment {
            return AddMealFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEAL_TYPE, mealType.argumentValue)
                    putString(ARG_ENTRY_DATE, entryDate)
                }
            }
        }
    }
}