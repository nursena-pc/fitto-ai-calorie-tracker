package com.example.fitto.feature_recipes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.databinding.ItemRecipeIngredientPickerBinding
import com.example.fitto.feature_recipes.model.RecipeFilterOption

class RecipeIngredientPickerAdapter(
    private var selectedKeys: Set<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : ListAdapter<RecipeFilterOption, RecipeIngredientPickerAdapter.IngredientViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemRecipeIngredientPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IngredientViewHolder(
        private val binding: ItemRecipeIngredientPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(option: RecipeFilterOption): Unit = with(binding) {
            ingredientText.text = option.label
            ingredientCheckBox.setOnCheckedChangeListener(null)
            ingredientCheckBox.isChecked = option.key in selectedKeys

            root.setOnClickListener { toggle(option) }
            ingredientCheckBox.setOnClickListener { toggle(option) }
        }

        private fun toggle(option: RecipeFilterOption): Unit {
            val position: Int = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            selectedKeys = if (option.key in selectedKeys) {
                selectedKeys - option.key
            } else {
                selectedKeys + option.key
            }

            onSelectionChanged(selectedKeys)
            notifyItemChanged(position)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RecipeFilterOption>() {
        override fun areItemsTheSame(oldItem: RecipeFilterOption, newItem: RecipeFilterOption): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: RecipeFilterOption, newItem: RecipeFilterOption): Boolean {
            return oldItem == newItem
        }
    }
}
