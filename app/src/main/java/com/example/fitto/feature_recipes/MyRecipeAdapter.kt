package com.example.fitto.feature_recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemRecipeListBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.RecipeImageLoader

class MyRecipeAdapter(
    private val filterCatalog: RecipeFilterCatalog,
    private val onRecipeClick: (RecipeUiModel) -> Unit,
    private val onRemoveClick: ((RecipeUiModel) -> Unit)? = null
) : ListAdapter<RecipeUiModel, MyRecipeAdapter.MyRecipeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecipeViewHolder {
        val binding = ItemRecipeListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyRecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyRecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MyRecipeViewHolder(
        private val binding: ItemRecipeListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeUiModel): Unit = with(binding) {
            RecipeImageLoader.load(recipeImage, recipe)
            titleText.text = recipe.title
            descriptionText.text = recipe.description
            categoryChip.text = when {
                recipe.id.startsWith("copied_") || recipe.id.startsWith("user_") -> root.context.getString(R.string.recipes_book_tab_mine)
                recipe.id.startsWith("social_") || recipe.id.startsWith("shared_") -> root.context.getString(R.string.recipes_book_tab_shared)
                else -> root.context.getString(R.string.recipes_book_tab_saved)
            }
            metaText.text = root.context.getString(
                R.string.recipes_list_meta_format,
                recipe.durationLabel,
                recipe.servingDisplay.ifBlank { root.context.getString(R.string.recipes_serving_default) }
            )
            removeBookButton.isVisible = onRemoveClick != null
            removeBookButton.setOnClickListener { onRemoveClick?.invoke(recipe) }
            recipeCard.setOnClickListener { onRecipeClick(recipe) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecipeUiModel>() {
        override fun areItemsTheSame(oldItem: RecipeUiModel, newItem: RecipeUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecipeUiModel, newItem: RecipeUiModel): Boolean =
            oldItem == newItem
    }
}
