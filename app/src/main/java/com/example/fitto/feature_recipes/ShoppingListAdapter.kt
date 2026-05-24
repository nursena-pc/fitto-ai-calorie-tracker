package com.example.fitto.feature_recipes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R

class ShoppingListAdapter(
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemViewHolder>() {

    private val items = mutableListOf<Item>()

    fun submitItems(newItems: List<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Item? = items.getOrNull(position)

    fun removeAt(position: Int): Item? {
        val removed = items.getOrNull(position) ?: return null
        items.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list_row, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.shoppingItemNameText)
        private val amountText: TextView = itemView.findViewById(R.id.shoppingItemAmountText)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let(onItemClick)
                }
            }
        }

        fun bind(item: Item) {
            nameText.text = item.name
            amountText.text = item.amountUnit.ifBlank { "—" }
        }
    }

    data class Item(
        val sourceIndex: Int,
        val raw: String,
        val name: String,
        val amountUnit: String
    )
}
