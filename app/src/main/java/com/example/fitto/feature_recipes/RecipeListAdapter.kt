package com.example.fitto.feature_recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemRecipeCardBinding
import com.example.fitto.databinding.ItemRecipeListBinding
import com.example.fitto.feature_recipes.data.RecipeFilterCatalog
import com.example.fitto.feature_recipes.model.RecipeListMode
import com.example.fitto.feature_recipes.model.RecipeUiModel
import com.example.fitto.feature_recipes.util.RecipeImageLoader
import kotlin.math.absoluteValue

class RecipeListAdapter(
    private val filterCatalog: RecipeFilterCatalog,
    private val onRecipeClick: (RecipeUiModel) -> Unit,
    private val onFavoriteClick: (RecipeUiModel) -> Unit
) : ListAdapter<RecipeUiModel, RecyclerView.ViewHolder>(RecipeDiffCallback) {

    var viewMode: RecipeListMode = RecipeListMode.GRID
        private set

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.stableLongId()
    }

    override fun getItemViewType(position: Int): Int {
        return when (viewMode) {
            RecipeListMode.GRID -> VIEW_TYPE_GRID
            RecipeListMode.LIST -> VIEW_TYPE_LIST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LIST -> ListRecipeViewHolder(
                ItemRecipeListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> GridRecipeViewHolder(
                ItemRecipeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recipe = getItem(position)
        when (holder) {
            is GridRecipeViewHolder -> holder.bind(recipe)
            is ListRecipeViewHolder -> holder.bind(recipe)
        }
    }

    fun updateViewMode(mode: RecipeListMode) {
        if (viewMode == mode) return
        viewMode = mode
        notifyDataSetChanged()
    }

    inner class GridRecipeViewHolder(
        private val binding: ItemRecipeCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeUiModel): Unit = with(binding) {
            RecipeImageLoader.load(recipeImage, recipe)
            titleText.text = recipe.title
            categoryChip.text = filterCatalog.mainCategoryLabel(recipe.mainCategoryKey)
            calorieText.text = if (recipe.calories > 0) {
                root.context.getString(R.string.recipes_kcal_value, recipe.calories)
            } else {
                root.context.getString(R.string.recipes_kcal_unknown)
            }
            timeText.text = recipe.durationLabel
            recipeCard.setOnClickListener { onRecipeClick(recipe) }
        }
    }

    inner class ListRecipeViewHolder(
        private val binding: ItemRecipeListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recipe: RecipeUiModel): Unit = with(binding) {
            RecipeImageLoader.load(recipeImage, recipe)
            titleText.text = recipe.title
            descriptionText.text = recipe.description
            categoryChip.text = filterCatalog.mainCategoryLabel(recipe.mainCategoryKey)
            metaText.text = listOf(
                recipe.durationLabel,
                recipe.servingDisplay.ifBlank { root.context.getString(R.string.recipes_serving_default) }
            ).filter { value -> value.isNotBlank() }.joinToString(" • ")
            recipeCard.setOnClickListener { onRecipeClick(recipe) }
        }
    }

    private object RecipeDiffCallback : DiffUtil.ItemCallback<RecipeUiModel>() {
        override fun areItemsTheSame(oldItem: RecipeUiModel, newItem: RecipeUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: RecipeUiModel, newItem: RecipeUiModel): Boolean =
            oldItem == newItem
    }

    private fun String.stableLongId(): Long {
        var result = 1125899906842597L
        forEach { char -> result = 31 * result + char.code }
        return result.absoluteValue
    }

    companion object {
        private const val VIEW_TYPE_GRID = 1
        private const val VIEW_TYPE_LIST = 2
    }
}
