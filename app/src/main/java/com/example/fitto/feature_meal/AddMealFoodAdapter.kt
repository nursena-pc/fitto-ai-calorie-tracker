package com.example.fitto.feature_meal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemFoodSuggestionBinding
import com.example.fitto.feature_meal.model.FoodUiModel

class AddMealFoodAdapter(
    private val onAddClick: (FoodUiModel) -> Unit,
    private val onFavoriteClick: (FoodUiModel) -> Unit
) : ListAdapter<FoodUiModel, AddMealFoodAdapter.FoodViewHolder>(FoodDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(
        private val binding: ItemFoodSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodUiModel) = with(binding) {
            foodEmojiText.text = item.emoji
            foodNameText.text = item.name
            foodServingText.text = item.serving
            foodCaloriesText.text = root.context.getString(
                R.string.add_meal_food_calories,
                item.calories
            )

            val favoriteTint = if (item.isFavorite) {
                ContextCompat.getColor(root.context, R.color.fitto_yellow)
            } else {
                ContextCompat.getColor(root.context, R.color.fitto_text_secondary)
            }

            favoriteFoodButton.setImageResource(R.drawable.ic_star_outline)
            favoriteFoodButton.setColorFilter(favoriteTint)

            favoriteFoodButton.setOnClickListener {
                onFavoriteClick(item)
            }

            addFoodButton.setOnClickListener {
                onAddClick(item)
            }

            root.setOnClickListener {
                onAddClick(item)
            }
        }
    }

    private object FoodDiffCallback : DiffUtil.ItemCallback<FoodUiModel>() {
        override fun areItemsTheSame(oldItem: FoodUiModel, newItem: FoodUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodUiModel, newItem: FoodUiModel): Boolean {
            return oldItem == newItem
        }
    }
}