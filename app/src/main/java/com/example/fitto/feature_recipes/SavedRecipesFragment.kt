package com.example.fitto.feature_recipes

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitto.R
import com.example.fitto.databinding.FragmentSavedRecipesBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.data.RecipeRepository
import com.example.fitto.feature_recipes.model.RecipeUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedRecipesFragment : Fragment(R.layout.fragment_saved_recipes) {

    private var _binding: FragmentSavedRecipesBinding? = null
    private val binding: FragmentSavedRecipesBinding
        get() = _binding!!

    private lateinit var repository: RecipeRepository
    private lateinit var adapter: MyRecipeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSavedRecipesBinding.bind(view)
        repository = RecipeRepository(requireContext())

        adapter = MyRecipeAdapter(
            filterCatalog = RecipeFilterCatalog(resources),
            onRecipeClick = ::openRecipeDetail
        )

        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.savedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.savedRecyclerView.adapter = adapter
        binding.savedRecyclerView.itemAnimator = null
        loadRecipes()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::repository.isInitialized) loadRecipes()
    }

    private fun loadRecipes() {
        viewLifecycleOwner.lifecycleScope.launch {
            val recipes: List<RecipeUiModel> = withContext(Dispatchers.IO) {
                repository.favoriteOriginalRecipes()
            }
            if (_binding == null) return@launch
            adapter.submitList(recipes)
            binding.emptyState.isVisible = recipes.isEmpty()
            binding.savedRecyclerView.isVisible = recipes.isNotEmpty()
        }
    }

    private fun openRecipeDetail(recipe: RecipeUiModel) {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .hide(this@SavedRecipesFragment)
            .add(R.id.homeTabContainer, RecipeDetailFragment.newInstance(recipe.id), TAG_RECIPE_DETAIL)
            .addToBackStack(TAG_RECIPE_DETAIL)
            .commit()
    }

    override fun onDestroyView() {
        binding.savedRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG_RECIPE_DETAIL = "recipe_detail"
    }
}
