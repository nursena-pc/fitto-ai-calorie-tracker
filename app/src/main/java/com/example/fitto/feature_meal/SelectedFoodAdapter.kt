package com.example.fitto.feature_meal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.databinding.ItemSelectedFoodBinding
import com.example.fitto.feature_meal.model.SelectedFoodUiModel

class SelectedFoodAdapter(
    private val onRemoveClick: (SelectedFoodUiModel) -> Unit
) : ListAdapter<SelectedFoodUiModel, SelectedFoodAdapter.SelectedFoodViewHolder>(SelectedFoodDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedFoodViewHolder {
        val binding = ItemSelectedFoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectedFoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedFoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SelectedFoodViewHolder(
        private val binding: ItemSelectedFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectedFoodUiModel) = with(binding) {
            selectedFoodEmojiText.text = item.emoji
            selectedFoodNameText.text = item.name
            selectedFoodAmountText.text = root.context.getString(
                com.example.fitto.R.string.add_meal_selected_food_detail,
                item.amountLabel,
                item.calories
            )
            removeSelectedFoodButton.setOnClickListener { onRemoveClick(item) }
        }
    }

    private object SelectedFoodDiffCallback : DiffUtil.ItemCallback<SelectedFoodUiModel>() {
        override fun areItemsTheSame(
            oldItem: SelectedFoodUiModel,
            newItem: SelectedFoodUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SelectedFoodUiModel,
            newItem: SelectedFoodUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
