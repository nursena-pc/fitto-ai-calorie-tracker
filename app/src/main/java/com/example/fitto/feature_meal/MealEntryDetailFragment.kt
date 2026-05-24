package com.example.fitto.feature_meal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.data.local.entity.MealEntryEntity
import com.example.fitto.databinding.ItemMealEntryDetailBinding
import com.example.fitto.feature_meal.model.MealEntryDetailUiModel

class MealEntryDetailAdapter(
    private val onEditClick: (MealEntryDetailUiModel) -> Unit,
    private val onDeleteClick: (MealEntryDetailUiModel) -> Unit
) : ListAdapter<MealEntryDetailUiModel, MealEntryDetailAdapter.MealEntryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealEntryViewHolder {
        val binding = ItemMealEntryDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealEntryViewHolder(
        private val binding: ItemMealEntryDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MealEntryDetailUiModel) = with(binding) {
            val context = root.context
            mealEntryTitleText.text = item.title
            mealEntrySourceText.text = when (item.sourceType) {
                MealEntryEntity.SOURCE_DIET_PLAN -> context.getString(R.string.meal_detail_source_diet_plan)
                else -> context.getString(R.string.meal_detail_source_manual)
            }
            mealEntryCaloriesText.text = context.getString(
                R.string.meal_detail_entry_calories,
                item.totalCalories
            )
            mealEntryFoodsText.text = item.foodSummary
            mealEntryMacrosText.text = context.getString(
                R.string.meal_detail_entry_macros,
                item.totalProtein,
                item.totalCarbs,
                item.totalFat
            )
            mealEntryFoodCountText.text = context.resources.getQuantityString(
                R.plurals.meal_detail_food_count,
                item.foodCount,
                item.foodCount
            )
            root.setOnClickListener { onEditClick(item) }
            editEntryButton.setOnClickListener { onEditClick(item) }
            deleteEntryButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MealEntryDetailUiModel>() {
        override fun areItemsTheSame(
            oldItem: MealEntryDetailUiModel,
            newItem: MealEntryDetailUiModel
        ): Boolean = oldItem.entryId == newItem.entryId

        override fun areContentsTheSame(
            oldItem: MealEntryDetailUiModel,
            newItem: MealEntryDetailUiModel
        ): Boolean = oldItem == newItem
    }
}
