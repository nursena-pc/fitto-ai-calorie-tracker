package com.example.fitto.feature_meal

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.data.local.entity.FoodEntity
import com.example.fitto.data.local.repository.FoodRepository
import com.example.fitto.data.local.repository.MealRepository
import com.example.fitto.data.seed.SeedDataImporter
import com.example.fitto.databinding.FragmentAddMealBinding
import com.example.fitto.feature_meal.model.FoodUiModel
import com.example.fitto.feature_meal.model.MealType
import com.example.fitto.feature_meal.model.SelectedFoodUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import android.view.inputmethod.EditorInfo
import android.util.Log
import android.view.KeyEvent

class AddMealFragment : Fragment(R.layout.fragment_add_meal) {

    private var _binding: FragmentAddMealBinding? = null
    private val binding get() = _binding!!

    private val targetEntryDate: String by lazy {
        arguments?.getString(ARG_ENTRY_DATE) ?: LocalDate.now().toString()
    }

    private val targetMealType: MealType by lazy {
        MealType.fromArgument(arguments?.getString(ARG_MEAL_TYPE))
    }

    private lateinit var foodRepository: FoodRepository
    private lateinit var mealRepository: MealRepository
    private lateinit var suggestedFoodAdapter: AddMealFoodAdapter
    private lateinit var selectedFoodAdapter: SelectedFoodAdapter
    private var currentFoodResults: List<FoodEntity> = emptyList()
    private var lastOnlineQuery: String = ""
    private var nextUsdaPageNumber: Int = 2
    private var canLoadMoreUsda: Boolean = false
    private var isLoadingMoreUsda: Boolean = false
    private var searchJob: Job? = null
    private var seedReady: Boolean = false
    private var selectedFoods: List<SelectedFoodUiModel> = emptyList()
    private val foodPresentationResolver = AddMealFoodPresentationResolver()
    private val selectionStateResolver = AddMealSelectionStateResolver()
    private val searchStateResolver = AddMealSearchStateResolver()
    private val favoriteStateResolver = AddMealFavoriteStateResolver()
    private lateinit var saveAction: AddMealSaveAction
    private lateinit var customFoodAction: AddMealCustomFoodAction

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddMealBinding.bind(view)

        val appContext = requireContext().applicationContext
        foodRepository = FoodRepository(appContext)
        mealRepository = MealRepository(appContext)
        saveAction = AddMealSaveAction { mealType, selectedFoods, entryDate ->
            mealRepository.addManualMealEntry(
                mealType = mealType,
                selectedFoods = selectedFoods,
                entryDate = entryDate
            )
        }
        customFoodAction = AddMealCustomFoodAction { request ->
            foodRepository.createCustomFood(
                name = request.name,
                defaultServingAmount = request.servingAmount,
                defaultUnit = request.unit,
                caloriesPer100g = request.caloriesPer100g,
                proteinPer100g = request.proteinPer100g,
                carbsPer100g = request.carbsPer100g,
                fatPer100g = request.fatPer100g,
                mealType = request.mealType
            )
        }

        setupRecyclerViews()
        setupUi()
        setupPortionResultListener()
        setupCameraFoodResultListener()
        setupClicks()
        updateSelectedFoods()
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
            selectedFoods = selectionStateResolver.addSelectedFood(
                currentSelection = selectedFoods,
                selectedFood = FoodPortionBottomSheet.run {
                    bundle.toSelectedFoodUiModel()
                }
            )
            updateSelectedFoods()
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

    private fun FoodEntity.toSelectedFoodFromDetectedServing(): SelectedFoodUiModel {
        return foodPresentationResolver.toSelectedFoodFromDetectedServing(this)
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

            viewLifecycleOwner.lifecycleScope.launch {
                val foods = withContext(Dispatchers.IO) {
                    foodIds.mapNotNull { foodId ->
                        foodRepository.getFoodById(foodId)
                    }
                }

                if (_binding == null || foods.isEmpty()) return@launch

                selectedFoods = selectionStateResolver.addDetectedFoods(
                    currentSelection = selectedFoods,
                    detectedFoods = foods.map { food ->
                        food.toSelectedFoodFromDetectedServing()
                    }
                )

                updateSelectedFoods()

                saveSelectedFoodsToMeal()
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
            when (val result = withContext(Dispatchers.IO) { customFoodAction.create(request) }) {
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
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            if (debounce) delay(300)

            val normalizedQuery = searchStateResolver.normalizeQuery(query)
            val appContext = context?.applicationContext ?: return@launch

            Log.d(
                "AddMealFragment",
                "searchFoods query=$normalizedQuery allowOnlineSearch=$allowOnlineSearch"
            )

            if (_binding != null && searchStateResolver.shouldShowSearchingTitle(normalizedQuery)) {
                binding.foodsSectionTitleText.text = getString(R.string.add_meal_searching_foods)
            }

            var showingRecentFoods = false
            var showingSuggestedFoods = false
            var showingSearchResults = false
            var onlineSearchAttempted = false

            val foods = withContext(Dispatchers.IO) {
                ensureSeedImported(appContext)

                when {
                    normalizedQuery.isBlank() -> {
                        val recentFoods = foodRepository.getRecentFoods(limit = 12)

                        if (recentFoods.isNotEmpty()) {
                            showingRecentFoods = true
                            recentFoods
                        } else {
                            showingSuggestedFoods = true
                            foodRepository.searchFoods(
                                query = "",
                                mealType = targetMealType.argumentValue,
                                limit = 40
                            )
                        }
                    }

                    else -> {
                        val localFoods = foodRepository.searchFoods(
                            query = normalizedQuery,
                            mealType = targetMealType.argumentValue,
                            limit = 40
                        )

                        if (searchStateResolver.shouldSearchOnline(normalizedQuery, allowOnlineSearch)) {
                            showingSearchResults = true
                            onlineSearchAttempted = true

                            Log.d(
                                "AddMealFragment",
                                "USDA search will run for query=$normalizedQuery"
                            )

                            val usdaFoods = foodRepository.searchUsdaFoods(
                                query = normalizedQuery,
                                mealType = targetMealType.argumentValue,
                                limit = USDA_PAGE_SIZE,
                                pageNumber = 1
                            )

                            lastOnlineQuery = normalizedQuery
                            nextUsdaPageNumber = 2
                            canLoadMoreUsda = searchStateResolver.canLoadMore(
                                fetchedCount = usdaFoods.size,
                                pageSize = USDA_PAGE_SIZE
                            )

                            searchStateResolver.mergeFoods(localFoods, usdaFoods)

                        } else {
                            if (localFoods.isNotEmpty()) {
                                showingSearchResults = true
                                localFoods
                            } else {
                                emptyList<FoodEntity>()
                            }
                        }
                    }
                }
            }

            if (_binding == null) return@launch

            val favoriteIds = withContext(Dispatchers.IO) {
                favoriteStateResolver.favoriteIdSet(
                    foodRepository.getFavoriteFoodIds(limit = 200)
                )
            }

            binding.foodsSectionTitleText.text = getString(
                searchStateResolver.sectionFor(
                    showingRecentFoods = showingRecentFoods,
                    showingSuggestedFoods = showingSuggestedFoods,
                    showingSearchResults = showingSearchResults
                ).titleRes()
            )

            showEmptySearchFeedback(
                searchStateResolver.emptyFeedback(
                    foods = foods,
                    normalizedQuery = normalizedQuery,
                    onlineSearchAttempted = onlineSearchAttempted,
                    allowOnlineSearch = allowOnlineSearch
                )
            )

            if (searchStateResolver.shouldResetOnlinePaging(allowOnlineSearch)) {
                lastOnlineQuery = ""
                nextUsdaPageNumber = 2
                canLoadMoreUsda = false
            }
            currentFoodResults = foods
            updateLoadMoreButton()
            suggestedFoodAdapter.submitList(
                foods.map { food ->
                    food.toFoodUiModel(isFavorite = food.id in favoriteIds)
                }
            )
        }
    }
    private fun loadMoreUsdaFoods() {
        val query = lastOnlineQuery.trim()

        if (!searchStateResolver.canStartLoadMore(query, canLoadMoreUsda, isLoadingMoreUsda)) return

        isLoadingMoreUsda = true
        binding.loadMoreFoodsButton.isEnabled = false
        binding.loadMoreFoodsButton.text = getString(R.string.add_meal_loading_more_foods)

        viewLifecycleOwner.lifecycleScope.launch {
            val newFoods = runCatching {
                withContext(Dispatchers.IO) {
                    foodRepository.searchUsdaFoods(
                        query = query,
                        mealType = targetMealType.argumentValue,
                        limit = USDA_PAGE_SIZE,
                        pageNumber = nextUsdaPageNumber
                    )
                }
            }.getOrElse {
                emptyList()
            }

            if (_binding == null) return@launch

            isLoadingMoreUsda = false

            if (newFoods.isEmpty()) {
                canLoadMoreUsda = false
                updateLoadMoreButton()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_meal_no_more_foods),
                    Toast.LENGTH_SHORT
                ).show()

                return@launch
            }

            nextUsdaPageNumber = searchStateResolver.nextPageAfterLoad(nextUsdaPageNumber)
            canLoadMoreUsda = searchStateResolver.canLoadMore(
                fetchedCount = newFoods.size,
                pageSize = USDA_PAGE_SIZE
            )

            currentFoodResults = searchStateResolver.mergeFoods(currentFoodResults, newFoods)

            val favoriteIds = withContext(Dispatchers.IO) {
                favoriteStateResolver.favoriteIdSet(
                    foodRepository.getFavoriteFoodIds(limit = 200)
                )
            }

            suggestedFoodAdapter.submitList(
                currentFoodResults.map { food ->
                    food.toFoodUiModel(isFavorite = food.id in favoriteIds)
                }
            )

            updateLoadMoreButton()
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

    private fun updateLoadMoreButton() {
        if (_binding == null) return

        binding.loadMoreFoodsButton.visibility =
            if (canLoadMoreUsda && currentFoodResults.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.loadMoreFoodsButton.isEnabled = !isLoadingMoreUsda
        binding.loadMoreFoodsButton.text = getString(R.string.add_meal_load_more_foods)
    }

    private fun showFavoriteFoods() {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val foods = withContext(Dispatchers.IO) {
                foodRepository.getFavoriteFoods(limit = 50)
            }

            if (_binding == null) return@launch

            binding.foodsSectionTitleText.text = getString(R.string.add_meal_favorite_foods)

            if (favoriteStateResolver.shouldShowEmptyFavoriteFeedback(foods)) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_meal_no_favorite_foods),
                    Toast.LENGTH_SHORT
                ).show()
            }

            suggestedFoodAdapter.submitList(
                foods.map { food ->
                    food.toFoodUiModel(isFavorite = true)
                }
            )
        }
    }

    private fun toggleFavoriteFood(food: FoodUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            val isFavorite = withContext(Dispatchers.IO) {
                foodRepository.toggleFavoriteFood(food.id)
            }

            val message = when (favoriteStateResolver.toggleFeedback(isFavorite)) {
                AddMealFavoriteToggleFeedback.ADDED -> {
                    getString(R.string.add_meal_added_to_favorites, food.name)
                }
                AddMealFavoriteToggleFeedback.REMOVED -> {
                    getString(R.string.add_meal_removed_from_favorites, food.name)
                }
            }

            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

            if (favoriteStateResolver.shouldRefreshSearchAfterToggle()) {
                val currentQuery = binding.searchEditText.text?.toString().orEmpty()
                searchFoods(
                    query = currentQuery,
                    debounce = false,
                    allowOnlineSearch = false
                )
            }
        }
    }
    private suspend fun ensureSeedImported(appContext: android.content.Context) {
        if (seedReady) return
        SeedDataImporter.importFoodSeedIfNeeded(appContext)
        seedReady = true
    }

    private fun saveSelectedFoodsToMeal() {
        val request = AddMealSaveRequest(
            mealType = targetMealType,
            selectedFoods = selectedFoods,
            entryDate = targetEntryDate
        )

        if (!saveAction.canSave(request.selectedFoods)) return

        binding.addMealButton.isEnabled = false
        binding.addMealButton.alpha = 0.65f

        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = withContext(Dispatchers.IO) { saveAction.save(request) }) {
                AddMealSaveResult.EmptySelection -> restoreSaveButton()
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
        restoreSaveButton()

        Toast.makeText(
            requireContext(),
            getString(R.string.add_meal_save_error_message),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun restoreSaveButton() {
        if (_binding != null) {
            binding.addMealButton.isEnabled = true
            binding.addMealButton.alpha = 1f
        }
    }

    private fun openPortionBottomSheet(food: FoodUiModel) {
        FoodPortionBottomSheet.newInstance(food).show(
            childFragmentManager,
            FoodPortionBottomSheet.TAG
        )
    }

    private fun removeFoodFromSelection(food: SelectedFoodUiModel) {
        selectedFoods = selectionStateResolver.removeSelectedFood(
            currentSelection = selectedFoods,
            food = food
        )
        updateSelectedFoods()
    }

    private fun updateSelectedFoods() = with(binding) {
        selectedFoodAdapter.submitList(selectedFoods)

        val selectionState = selectionStateResolver.resolveUiState(selectedFoods)
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
            languageCode = resources.configuration.locales[0].language,
            isFavorite = isFavorite
        )
    }

    private fun showMockMessage(label: String) {
        Toast.makeText(
            requireContext(),
            getString(R.string.daily_mock_action_message, label),
            Toast.LENGTH_SHORT
        ).show()
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