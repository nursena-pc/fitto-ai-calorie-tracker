package com.example.fitto.feature_meal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemMealEntryFoodBinding
import com.example.fitto.feature_meal.model.MealEntryFoodUiModel

class MealEntryFoodAdapter(
    private val onEditClick: (MealEntryFoodUiModel) -> Unit,
    private val onDeleteClick: (MealEntryFoodUiModel) -> Unit
) : ListAdapter<MealEntryFoodUiModel, MealEntryFoodAdapter.FoodViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemMealEntryFoodBinding.inflate(
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
        private val binding: ItemMealEntryFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MealEntryFoodUiModel) = with(binding) {
            foodNameText.text = item.foodName
            foodAmountText.text = item.amountLabel
            foodCaloriesText.text = root.context.getString(
                R.string.meal_entry_food_kcal_value,
                item.calories
            )
            foodMacrosText.text = root.context.getString(
                R.string.meal_entry_food_macro_value,
                item.protein,
                item.carbs,
                item.fat
            )

            editFoodButton.setOnClickListener {
                onEditClick(item)
            }

            deleteFoodButton.setOnClickListener {
                onDeleteClick(item)
            }

            root.setOnClickListener {
                onEditClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MealEntryFoodUiModel>() {
        override fun areItemsTheSame(
            oldItem: MealEntryFoodUiModel,
            newItem: MealEntryFoodUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: MealEntryFoodUiModel,
            newItem: MealEntryFoodUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}