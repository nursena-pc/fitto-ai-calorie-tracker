package com.example.fitto.feature_meal

import android.content.Context
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
    private var selectedFoods: List<SelectedFoodUiModel> = emptyList()
    private val foodPresentationResolver = AddMealFoodPresentationResolver()
    private val selectionStateResolver = AddMealSelectionStateResolver()
    private val searchStateResolver = AddMealSearchStateResolver()
    private lateinit var foodSearchAction: AddMealFoodSearchAction
    private lateinit var loadMoreAction: AddMealLoadMoreAction
    private lateinit var saveAction: AddMealSaveAction
    private lateinit var customFoodAction: AddMealCustomFoodAction
    private lateinit var detectedFoodsAction: AddMealDetectedFoodsAction
    private lateinit var favoritesAction: AddMealFavoritesAction
    private lateinit var foodCatalogInitializer: AddMealFoodCatalogInitializer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddMealBinding.bind(view)

        val appContext = requireContext().applicationContext
        initializeDependencies(appContext)

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

    private fun initializeDependencies(appContext: Context) {
        foodRepository = FoodRepository(appContext)
        mealRepository = MealRepository(appContext)
        foodCatalogInitializer = AddMealFoodCatalogInitializer {
            SeedDataImporter.importFoodSeedIfNeeded(appContext)
        }
        foodSearchAction = AddMealFoodSearchAction(
            getRecentFoods = foodRepository::getRecentFoods,
            searchLocalFoods = { query, mealType, limit ->
                foodRepository.searchFoods(
                    query = query,
                    mealType = mealType,
                    limit = limit
                )
            },
            searchOnlineFoods = { query, mealType, limit, pageNumber ->
                foodRepository.searchUsdaFoods(
                    query = query,
                    mealType = mealType,
                    limit = limit,
                    pageNumber = pageNumber
                )
            }
        )
        loadMoreAction = AddMealLoadMoreAction { query, mealType, limit, pageNumber ->
            foodRepository.searchUsdaFoods(
                query = query,
                mealType = mealType,
                limit = limit,
                pageNumber = pageNumber
            )
        }
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
        detectedFoodsAction = AddMealDetectedFoodsAction(
            getFoodById = foodRepository::getFoodById,
            presentationResolver = foodPresentationResolver
        )
        favoritesAction = AddMealFavoritesAction(
            getFavoriteFoods = foodRepository::getFavoriteFoods,
            getFavoriteFoodIds = foodRepository::getFavoriteFoodIds,
            toggleFavoriteFood = foodRepository::toggleFavoriteFood
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
                val result = withContext(Dispatchers.IO) {
                    detectedFoodsAction.load(request)
                }

                if (_binding == null) return@launch

                when (result) {
                    AddMealDetectedFoodsResult.NoFoods -> Unit
                    is AddMealDetectedFoodsResult.Imported -> {
                        selectedFoods = selectionStateResolver.addDetectedFoods(
                            currentSelection = selectedFoods,
                            detectedFoods = result.detectedFoods
                        )
                        updateSelectedFoods()
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

            val searchResult = withContext(Dispatchers.IO) {
                foodCatalogInitializer.ensureInitialized()
                foodSearchAction.search(
                    AddMealFoodSearchRequest(
                        normalizedQuery = normalizedQuery,
                        mealType = targetMealType.argumentValue,
                        allowOnlineSearch = allowOnlineSearch,
                        onlinePageSize = USDA_PAGE_SIZE
                    )
                )
            }

            if (_binding == null) return@launch

            val favoriteIds = withContext(Dispatchers.IO) {
                favoritesAction.loadFavoriteIds()
            }

            binding.foodsSectionTitleText.text = getString(searchResult.section.titleRes())
            showEmptySearchFeedback(searchResult.emptyFeedback)
            applyOnlinePagingState(searchResult.pagingState)
            currentFoodResults = searchResult.foods
            updateLoadMoreButton()
            suggestedFoodAdapter.submitList(
                foodPresentationResolver.toFoodUiModels(
                    foods = searchResult.foods,
                    languageCode = currentLanguageCode(),
                    favoriteIds = favoriteIds
                )
            )
        }
    }

    private fun loadMoreUsdaFoods() {
        val query = lastOnlineQuery.trim()

        if (!searchStateResolver.canStartLoadMore(query, canLoadMoreUsda, isLoadingMoreUsda)) return

        isLoadingMoreUsda = true
        binding.loadMoreFoodsButton.isEnabled = false
        binding.loadMoreFoodsButton.text = getString(R.string.add_meal_loading_more_foods)

        val request = AddMealLoadMoreRequest(
            query = query,
            mealType = targetMealType.argumentValue,
            currentFoods = currentFoodResults,
            currentPage = nextUsdaPageNumber,
            pageSize = USDA_PAGE_SIZE
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { loadMoreAction.load(request) }

            if (_binding == null) return@launch

            isLoadingMoreUsda = false

            when (result) {
                is AddMealLoadMoreActionResult.NoMoreFoods -> {
                    canLoadMoreUsda = result.canLoadMore
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.add_meal_no_more_foods),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is AddMealLoadMoreActionResult.Loaded -> {
                    nextUsdaPageNumber = result.nextPageNumber
                    canLoadMoreUsda = result.canLoadMore
                    currentFoodResults = result.foods

                    val favoriteIds = withContext(Dispatchers.IO) {
                        favoritesAction.loadFavoriteIds()
                    }

                    if (_binding == null) return@launch

                    suggestedFoodAdapter.submitList(
                        foodPresentationResolver.toFoodUiModels(
                            foods = currentFoodResults,
                            languageCode = currentLanguageCode(),
                            favoriteIds = favoriteIds
                        )
                    )
                }
            }

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

    private fun applyOnlinePagingState(state: AddMealOnlinePagingState) {
        when (state) {
            AddMealOnlinePagingState.Keep -> Unit
            AddMealOnlinePagingState.Reset -> {
                lastOnlineQuery = ""
                nextUsdaPageNumber = 2
                canLoadMoreUsda = false
            }
            is AddMealOnlinePagingState.Active -> {
                lastOnlineQuery = state.query
                nextUsdaPageNumber = state.nextPageNumber
                canLoadMoreUsda = state.canLoadMore
            }
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

        val uiState = searchStateResolver.loadMoreUiState(
            canLoadMore = canLoadMoreUsda,
            hasCurrentResults = currentFoodResults.isNotEmpty(),
            isLoadingMore = isLoadingMoreUsda
        )

        binding.loadMoreFoodsButton.visibility = if (uiState.visible) View.VISIBLE else View.GONE
        binding.loadMoreFoodsButton.isEnabled = uiState.enabled
        binding.loadMoreFoodsButton.text = getString(R.string.add_meal_load_more_foods)
    }

    private fun showFavoriteFoods() {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                favoritesAction.loadFavorites()
            }

            if (_binding == null) return@launch

            binding.foodsSectionTitleText.text = getString(R.string.add_meal_favorite_foods)

            if (result.shouldShowEmptyFeedback) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_meal_no_favorite_foods),
                    Toast.LENGTH_SHORT
                ).show()
            }

            suggestedFoodAdapter.submitList(
                foodPresentationResolver.toFoodUiModels(
                    foods = result.foods,
                    languageCode = currentLanguageCode(),
                    favoriteIds = result.foods.mapTo(mutableSetOf()) { food -> food.id }
                )
            )
        }
    }

    private fun toggleFavoriteFood(food: FoodUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                favoritesAction.toggle(food.id)
            }

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