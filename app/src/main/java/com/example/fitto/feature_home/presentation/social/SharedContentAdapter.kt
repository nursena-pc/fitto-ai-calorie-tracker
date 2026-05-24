package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemSharedContentBinding

class SharedContentAdapter(
    private val onItemClick: (SharedContentUiModel) -> Unit
) : RecyclerView.Adapter<SharedContentAdapter.SharedContentViewHolder>() {

    private val items = mutableListOf<SharedContentUiModel>()

    fun submitList(newItems: List<SharedContentUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedContentViewHolder {
        val binding = ItemSharedContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SharedContentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SharedContentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SharedContentViewHolder(
        private val binding: ItemSharedContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SharedContentUiModel) = with(binding) {
            sharedContentItemTitleText.text = item.title
            sharedContentItemSubtitleText.text = item.subtitle

            sharedContentIcon.setImageResource(
                when (item.type) {
                    SharedContentType.MEDIA -> R.drawable.ic_image
                    SharedContentType.LINK -> R.drawable.ic_public
                    SharedContentType.FILE -> R.drawable.ic_attach_file
                }
            )

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}