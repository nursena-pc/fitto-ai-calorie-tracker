package com.example.fitto.feature_recipes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.FragmentRecipesBinding
import com.example.fitto.feature_home.presentation.notifications.FittoNotificationStore
import com.example.fitto.feature_home.presentation.social.NotificationsFragment
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeFilterOption
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeHeaderState
import com.example.fitto.feature_recipes.model.RecipeListMode
import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private var _binding: FragmentRecipesBinding? = null
    private val binding: FragmentRecipesBinding
        get() = _binding!!

    private lateinit var catalog: RecipeFilterCatalog
    private lateinit var repository: RecipeRepository
    private lateinit var headerAdapter: RecipeHeaderAdapter
    private lateinit var listAdapter: RecipeListAdapter
    private lateinit var concatAdapter: ConcatAdapter

    private var filterState: RecipeFilterState = RecipeFilterState()
    private var searchQuery: String = ""
    private var listMode: RecipeListMode = RecipeListMode.GRID
    private var resultCount: Int = 0
    private var renderJob: Job? = null
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRecipesBinding.bind(view)
        catalog = RecipeFilterCatalog(resources)
        repository = RecipeRepository(requireContext())

        setupRecyclerView()
        setupFilterResultListener()
        observeNotificationBadge()
        updateHeader()
        renderRecipes(preserveScroll = false)
    }

    private fun observeNotificationBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FittoNotificationStore.changeToken.collect {
                    if (::headerAdapter.isInitialized) {
                        updateHeader(
                            notificationBadgeCount = FittoNotificationStore.getUnreadCount(requireContext())
                        )
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        headerAdapter = RecipeHeaderAdapter(
            filterCatalog = catalog,
            onSearchChanged = { query -> onSearchChanged(query) },
            onFilterClick = {
                RecipeFilterBottomSheetFragment
                    .newInstance(filterState)
                    .show(childFragmentManager, TAG_FILTER_SHEET)
            },
            onNotificationClick = {
                NotificationsFragment().show(childFragmentManager, TAG_NOTIFICATIONS)
            },
            onViewModeClick = { toggleListMode() },
            onCategoryClick = ::onCategoryChipClicked,
            onClearFiltersClick = ::clearFiltersKeepingPosition,
            onPantrySearchClick = ::openPantrySearch,
            onShoppingListClick = ::openShoppingList,
            onMyRecipesClick = ::openMyRecipes,
            onVoiceSearchClick = ::startVoiceSearch
        )

        listAdapter = RecipeListAdapter(
            filterCatalog = catalog,
            onRecipeClick = ::openRecipeDetail,
            onFavoriteClick = ::toggleFavorite
        )

        concatAdapter = ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .build(),
            headerAdapter,
            listAdapter
        )

        binding.recipesRecyclerView.apply {
            adapter = concatAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            itemAnimator = null
        }

        applyListMode(RecipeListMode.GRID, preserveScroll = false)
    }

    private fun onSearchChanged(query: String) {
        if (searchQuery == query) return
        searchQuery = query
        updateHeader()
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            renderRecipes(preserveScroll = true)
        }
    }

    private fun toggleListMode() {
        val nextMode = when (listMode) {
            RecipeListMode.GRID -> RecipeListMode.LIST
            RecipeListMode.LIST -> RecipeListMode.GRID
        }
        applyListMode(nextMode, preserveScroll = true)
    }

    private fun applyListMode(mode: RecipeListMode, preserveScroll: Boolean) {
        if (!::listAdapter.isInitialized) return

        val recyclerView = binding.recipesRecyclerView
        recyclerView.stopScroll()
        val layoutState: Parcelable? = if (preserveScroll) {
            recyclerView.layoutManager?.onSaveInstanceState()
        } else {
            null
        }

        listMode = mode
        recyclerView.recycledViewPool.clear()
        listAdapter.updateViewMode(mode)

        recyclerView.layoutManager = when (mode) {
            RecipeListMode.GRID -> GridLayoutManager(requireContext(), GRID_SPAN_COUNT).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (position == HEADER_ADAPTER_POSITION) GRID_SPAN_COUNT else 1
                    }
                }
            }

            RecipeListMode.LIST -> LinearLayoutManager(requireContext())
        }

        updateHeader()

        if (layoutState != null) {
            recyclerView.post {
                recyclerView.layoutManager?.onRestoreInstanceState(layoutState)
            }
        }
    }

    private fun setupFilterResultListener() {
        childFragmentManager.setFragmentResultListener(
            RecipeFilterState.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            filterState = RecipeFilterState.fromBundle(
                bundle.getBundle(RecipeFilterState.RESULT_BUNDLE_KEY)
            )
            updateHeader()
            renderRecipes(preserveScroll = true)
        }
    }

    private fun onCategoryChipClicked(option: RecipeFilterOption) {
        val hadAdvancedFilters = filterState.hasAdvancedFilters()
        val newState = when {
            option.key == RecipeHeaderAdapter.KEY_ALL_CATEGORIES -> RecipeFilterState()

            filterState.selectedMainCategoryKey == option.key && !filterState.hasAdvancedFilters() -> {
                RecipeFilterState()
            }

            else -> RecipeFilterState(selectedMainCategoryKey = option.key)
        }

        val shouldWarnReset = hadAdvancedFilters && newState != filterState
        filterState = newState

        updateHeader()
        renderRecipes(preserveScroll = true)

        if (shouldWarnReset) {
            Toast.makeText(
                requireContext(),
                getString(R.string.recipes_filter_reset_notice),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun clearFiltersKeepingPosition() {
        if (!filterState.hasActiveFilters()) return
        filterState = RecipeFilterState()
        updateHeader()
        renderRecipes(preserveScroll = true)
        Toast.makeText(requireContext(), R.string.recipes_filters_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun renderRecipes(preserveScroll: Boolean = true) {
        val querySnapshot = searchQuery
        val filterSnapshot = filterState
        val layoutState = if (preserveScroll) {
            binding.recipesRecyclerView.layoutManager?.onSaveInstanceState()
        } else {
            null
        }

        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            val filteredRecipes: List<RecipeUiModel> = withContext(Dispatchers.IO) {
                repository.filterRecipes(
                    query = querySnapshot,
                    filterState = filterSnapshot
                )
            }

            if (_binding == null) return@launch

            resultCount = filteredRecipes.size
            listAdapter.submitList(filteredRecipes) {
                if (layoutState != null) {
                    binding.recipesRecyclerView.layoutManager?.onRestoreInstanceState(layoutState)
                }
            }
            updateHeader()

            binding.emptyState.isVisible = filteredRecipes.isEmpty()
            binding.recipesRecyclerView.isVisible = true
        }
    }

    private fun updateHeader(
        notificationBadgeCount: Int = headerAdapter.currentState().notificationBadgeCount
    ) {
        if (!::headerAdapter.isInitialized) return
        headerAdapter.submitState(
            RecipeHeaderState(
                filterState = filterState,
                searchQuery = searchQuery,
                resultCount = resultCount,
                listMode = listMode,
                notificationBadgeCount = notificationBadgeCount
            )
        )
    }

    private fun toggleFavorite(recipe: RecipeUiModel) {
        repository.toggleFavorite(recipe.id)

        val messageRes = if (recipe.isFavorite) {
            R.string.recipes_favorite_removed
        } else {
            R.string.recipes_favorite_added
        }

        Toast.makeText(
            requireContext(),
            getString(messageRes, recipe.title),
            Toast.LENGTH_LONG
        ).show()

        renderRecipes(preserveScroll = true)
    }

    private fun openRecipeDetail(recipe: RecipeUiModel) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this)
            .add(
                R.id.homeTabContainer,
                RecipeDetailFragment.newInstance(recipe.id),
                TAG_RECIPE_DETAIL
            )
            .addToBackStack(TAG_RECIPE_DETAIL)
            .commit()
    }


    private fun openPantrySearch() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this)
            .add(R.id.homeTabContainer, FridgeInventoryFragment(), TAG_FRIDGE_INVENTORY)
            .addToBackStack(TAG_FRIDGE_INVENTORY)
            .commit()
    }

    private fun openShoppingList() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this)
            .add(R.id.homeTabContainer, ShoppingListFragment(), TAG_SHOPPING_LIST)
            .addToBackStack(TAG_SHOPPING_LIST)
            .commit()
    }

    private fun startVoiceSearch() {
        Toast.makeText(
            requireContext(),
            R.string.recipes_voice_search_coming_soon,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openMyRecipes() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this)
            .add(R.id.homeTabContainer, MyRecipesFragment(), TAG_MY_RECIPES)
            .addToBackStack(TAG_MY_RECIPES)
            .commit()
    }

    private fun openAddRecipe() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this)
            .add(R.id.homeTabContainer, RecipeAddFragment(), TAG_RECIPE_ADD)
            .addToBackStack(TAG_RECIPE_ADD)
            .commit()
    }

    override fun onDestroyView() {
        renderJob?.cancel()
        searchJob?.cancel()
        renderJob = null
        searchJob = null
        binding.recipesRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG_FILTER_SHEET = "recipe_filter_sheet"
        private const val TAG_NOTIFICATIONS = "NotificationsFragment"
        private const val TAG_RECIPE_DETAIL = "recipe_detail"
        private const val TAG_MY_RECIPES = "my_recipes"
        private const val TAG_SAVED_RECIPES = "saved_recipes"
        private const val TAG_FRIDGE_INVENTORY = "fridge_inventory"
        private const val TAG_SHOPPING_LIST = "shopping_list"
        private const val TAG_RECIPE_ADD = "recipe_add"
        private const val GRID_SPAN_COUNT = 2
        private const val SEARCH_DEBOUNCE_MS = 180L
        private const val HEADER_ADAPTER_POSITION = 0
    }
}
