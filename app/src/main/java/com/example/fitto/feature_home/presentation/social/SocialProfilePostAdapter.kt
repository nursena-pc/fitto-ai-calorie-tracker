package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.databinding.ItemProfilePostGridBinding

class SocialProfilePostAdapter(
    private val onPostClick: (SocialPost) -> Unit
) : ListAdapter<SocialPost, SocialProfilePostAdapter.ProfilePostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
        val binding = ItemProfilePostGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ProfilePostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfilePostViewHolder(
        private val binding: ItemProfilePostGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: SocialPost) = with(binding) {
            val hasImage = !post.imageUrl.isNullOrBlank()

            profilePostImage.isVisible = hasImage
            profilePostTextContainer.isVisible = !hasImage
            profilePostDraftBadge.isVisible = post.isDraft

            if (hasImage) {
                profilePostImage.load(post.imageUrl) {
                    crossfade(true)
                }
            } else {
                profilePostTextPreview.text = post.content
                    .ifBlank { post.mealInfo.orEmpty() }
                    .ifBlank { "..." }
            }

            root.setOnClickListener {
                onPostClick(post)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SocialPost>() {
        override fun areItemsTheSame(oldItem: SocialPost, newItem: SocialPost): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SocialPost, newItem: SocialPost): Boolean {
            return oldItem == newItem
        }
    }
}
