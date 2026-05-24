package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemSearchPostBinding

class SearchPostAdapter(
    private val onPostClick: (SocialPost) -> Unit
) : ListAdapter<SocialPost, SearchPostAdapter.SearchPostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchPostViewHolder {
        val binding = ItemSearchPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SearchPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchPostViewHolder(
        private val binding: ItemSearchPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: SocialPost) = with(binding) {
            val context = root.context

            searchPostUserNameText.text = post.userName.ifBlank {
                context.getString(R.string.social_default_user)
            }

            searchPostContentText.text = post.content.ifBlank {
                post.mealInfo ?: context.getString(R.string.search_posts_tab)
            }

            val metaText = when {
                post.calories != null -> context.getString(R.string.social_kcal_value, post.calories)
                !post.mealInfo.isNullOrBlank() -> post.mealInfo
                else -> null
            }

            searchPostMetaText.isVisible = !metaText.isNullOrBlank()
            searchPostMetaText.text = metaText.orEmpty()

            val hasImage = !post.imageUrl.isNullOrBlank()

            if (hasImage) {
                searchPostImage.load(post.imageUrl)
            } else {
                searchPostImage.setImageResource(R.drawable.ic_nav_recipes)
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