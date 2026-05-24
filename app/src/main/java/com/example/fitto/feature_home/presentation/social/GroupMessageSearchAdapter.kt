package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.databinding.ItemGroupMessageSearchResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupMessageSearchAdapter(
    private val onClick: (ChatMessageUiModel) -> Unit
) : ListAdapter<ChatMessageUiModel, GroupMessageSearchAdapter.SearchViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemGroupMessageSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchViewHolder(
        private val binding: ItemGroupMessageSearchResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessageUiModel) = with(binding) {
            resultSenderNameText.text = item.senderName
            resultMessageText.text = item.text
            resultDateText.text = formatDate(item.createdAt)

            root.setOnClickListener {
                onClick(item)
            }
        }

        private fun formatDate(value: Long): String {
            if (value <= 0L) return ""
            return SimpleDateFormat("dd MMM, HH:mm", Locale("tr", "TR"))
                .format(Date(value))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatMessageUiModel>() {
        override fun areItemsTheSame(
            oldItem: ChatMessageUiModel,
            newItem: ChatMessageUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ChatMessageUiModel,
            newItem: ChatMessageUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}