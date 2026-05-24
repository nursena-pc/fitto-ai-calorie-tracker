package com.example.fitto.feature_recipes

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentMyRecipesBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeFilterState
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {

    private var _binding: FragmentMyRecipesBinding? = null
    private val binding: FragmentMyRecipesBinding
        get() = _binding!!

    private lateinit var repository: RecipeRepository
    private lateinit var catalog: RecipeFilterCatalog
    private lateinit var adapter: MyRecipeAdapter
    private var savedRecipes: List<RecipeUiModel> = emptyList()
    private var userRecipes: List<RecipeUiModel> = emptyList()
    private var communityRecipes: List<RecipeUiModel> = emptyList()
    private var activeTab: RecipeBookTab = RecipeBookTab.SAVED
    private var query: String = ""
    private var filterState: RecipeFilterState = RecipeFilterState()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMyRecipesBinding.bind(view)
        repository = RecipeRepository(requireContext())
        catalog = RecipeFilterCatalog(resources)

        adapter = MyRecipeAdapter(
            filterCatalog = catalog,
            onRecipeClick = ::openRecipeDetail,
            onRemoveClick = ::confirmRemoveRecipe
        )

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.addRecipeButton.setOnClickListener { openAddRecipe() }
        binding.savedTabButton.setOnClickListener { selectTab(RecipeBookTab.SAVED) }
        binding.mineTabButton.setOnClickListener { selectTab(RecipeBookTab.MINE) }
        binding.sharedTabButton.setOnClickListener { selectTab(RecipeBookTab.SHARED) }
        binding.filterButton.setOnClickListener {
            RecipeFilterBottomSheetFragment
                .newInstance(filterState)
                .show(childFragmentManager, TAG_FILTER_SHEET)
        }
        binding.searchEditText.doAfterTextChanged { editable ->
            query = editable?.toString().orEmpty().trim()
            renderCurrentTab()
        }

        childFragmentManager.setFragmentResultListener(
            RecipeFilterState.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            filterState = RecipeFilterState.fromBundle(
                bundle.getBundle(RecipeFilterState.RESULT_BUNDLE_KEY)
            )
            renderCurrentTab()
        }

        parentFragmentManager.setFragmentResultListener(
            REQUEST_RECIPE_BOOK_CHANGED,
            viewLifecycleOwner
        ) { _, _ ->
            activeTab = RecipeBookTab.MINE
            styleTabs()
            loadRecipes()
        }

        binding.myRecipesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myRecipesRecyclerView.adapter = adapter
        binding.myRecipesRecyclerView.itemAnimator = null

        styleTabs()
        loadRecipes()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::repository.isInitialized) loadRecipes()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null && ::repository.isInitialized) {
            loadRecipes()
        }
    }

    private fun loadRecipes() {
        viewLifecycleOwner.lifecycleScope.launch {
            savedRecipes = withContext(Dispatchers.IO) { repository.favoriteOriginalRecipes() }
            userRecipes = withContext(Dispatchers.IO) { repository.userRecipes() }
            communityRecipes = withContext(Dispatchers.IO) { repository.communityRecipes() }
            if (_binding == null) return@launch
            renderCurrentTab()
        }
    }

    private fun selectTab(tab: RecipeBookTab) {
        if (activeTab == tab) return
        activeTab = tab
        styleTabs()
        renderCurrentTab()
    }

    private fun styleTabs(): Unit = with(binding) {
        styleTabButton(savedTabButton, activeTab == RecipeBookTab.SAVED)
        styleTabButton(mineTabButton, activeTab == RecipeBookTab.MINE)
        styleTabButton(sharedTabButton, activeTab == RecipeBookTab.SHARED)
    }

    private fun styleTabButton(button: com.google.android.material.button.MaterialButton, selected: Boolean) {
        button.background = ContextCompat.getDrawable(
            requireContext(),
            if (selected) R.drawable.bg_recipe_tab_selected else R.drawable.bg_recipe_tab_unselected
        )
        button.backgroundTintList = null
        button.strokeWidth = 0
        button.setTextColor(ContextCompat.getColor(requireContext(), if (selected) R.color.fitto_green else R.color.fitto_text_dark))
        button.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), if (selected) R.color.fitto_green else R.color.fitto_text_secondary)
        )
        button.setTypeface(button.typeface, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun renderCurrentTab() {
        val normalizedQuery = query.lowercase(TURKISH_LOCALE)
        val base = when (activeTab) {
            RecipeBookTab.SAVED -> savedRecipes
            RecipeBookTab.MINE -> userRecipes.filter { recipe ->
                recipe.id.startsWith("copied_") || recipe.id.startsWith("user_")
            }
            RecipeBookTab.SHARED -> communityRecipes
        }

        val tabRecipes = base.filter { recipe ->
            normalizedQuery.isBlank() ||
                recipe.title.lowercase(TURKISH_LOCALE).contains(normalizedQuery) ||
                recipe.description.lowercase(TURKISH_LOCALE).contains(normalizedQuery)
        }.filter { recipe -> recipe.matchesFilter(filterState) }

        adapter.submitList(tabRecipes)
        binding.emptyState.isVisible = tabRecipes.isEmpty()
        binding.myRecipesRecyclerView.isVisible = tabRecipes.isNotEmpty()
        binding.emptySubtitleText.text = getString(
            when (activeTab) {
                RecipeBookTab.SAVED -> R.string.recipes_book_empty_saved
                RecipeBookTab.MINE -> R.string.recipes_book_empty_mine
                RecipeBookTab.SHARED -> R.string.recipes_book_empty_shared
            }
        )
    }

    private fun RecipeUiModel.matchesFilter(state: RecipeFilterState): Boolean {
        val categoryMatches = state.selectedMainCategoryKey?.let { mainCategoryKey == it } ?: true
        val subcategoryMatches = state.selectedSubcategoryKey?.let { it in subcategoryKeys } ?: true
        val ingredientMatches = state.selectedMainIngredientKeys.isEmpty() ||
            mainIngredientKeys.any { it in state.selectedMainIngredientKeys }
        val dietMatches = state.selectedDietKeys.isEmpty() || state.selectedDietKeys.all { it in dietKeys }
        val methodMatches = state.selectedMethodKeys.isEmpty() || methodKeys.any { it in state.selectedMethodKeys }
        val durationMatches = state.selectedDurationKey?.let { durationLabel == it } ?: true
        val difficultyMatches = state.selectedDifficultyKey?.let { difficultyKey == it } ?: true
        val ingredientPool = ingredientNames.map { it.lowercase(TURKISH_LOCALE) }
        val includeMatches = state.selectedIncludedIngredientKeys.isEmpty() ||
            state.selectedIncludedIngredientKeys.all { key -> ingredientPool.any { it.contains(key) } }
        val excludeMatches = state.selectedExcludedIngredientKeys.isEmpty() ||
            state.selectedExcludedIngredientKeys.none { key -> ingredientPool.any { it.contains(key) } }
        return categoryMatches && subcategoryMatches && ingredientMatches && dietMatches &&
            methodMatches && durationMatches && difficultyMatches && includeMatches && excludeMatches
    }

    private fun confirmRemoveRecipe(recipe: RecipeUiModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.recipes_book_remove_title)
            .setMessage(getString(R.string.recipes_book_remove_message, recipe.title))
            .setNegativeButton(R.string.recipes_cancel, null)
            .setPositiveButton(R.string.recipes_book_remove_confirm) { _, _ ->
                removeRecipeFromBook(recipe)
            }
            .show()
    }

    private fun removeRecipeFromBook(recipe: RecipeUiModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            val removed = withContext(Dispatchers.IO) { repository.removeFromBook(recipe.id) }
            if (_binding == null) return@launch

            if (removed) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.recipes_book_removed, recipe.title),
                    Toast.LENGTH_SHORT
                ).show()
                loadRecipes()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.recipes_book_remove_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openRecipeDetail(recipe: RecipeUiModel) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this@MyRecipesFragment)
            .add(R.id.homeTabContainer, RecipeDetailFragment.newInstance(recipe.id), TAG_RECIPE_DETAIL)
            .addToBackStack(TAG_RECIPE_DETAIL)
            .commit()
    }

    private fun openAddRecipe() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this@MyRecipesFragment)
            .add(R.id.homeTabContainer, RecipeAddFragment(), TAG_RECIPE_ADD)
            .addToBackStack(TAG_RECIPE_ADD)
            .commit()
    }

    override fun onDestroyView() {
        binding.myRecipesRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private enum class RecipeBookTab { SAVED, MINE, SHARED }

    companion object {
        const val REQUEST_RECIPE_BOOK_CHANGED = "request_recipe_book_changed"
        const val RESULT_RECIPE_ID = "result_recipe_id"

        private const val TAG_RECIPE_DETAIL = "recipe_detail"
        private const val TAG_RECIPE_ADD = "recipe_add"
        private const val TAG_FILTER_SHEET = "recipe_book_filter_sheet"
        private val TURKISH_LOCALE = Locale("tr", "TR")
    }
}
