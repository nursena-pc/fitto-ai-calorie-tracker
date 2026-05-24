package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemSearchUserBinding

class SearchUserAdapter(
    private val onUserClick: (SearchUserUiModel) -> Unit,
    private val selectedUserIdsProvider: () -> Set<String> = { emptySet() }
) : ListAdapter<SearchUserUiModel, SearchUserAdapter.SearchUserViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        val binding = ItemSearchUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SearchUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchUserViewHolder(
        private val binding: ItemSearchUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SearchUserUiModel) = with(binding) {
            val context = root.context
            val isSelected = selectedUserIdsProvider().contains(user.id)

            root.strokeWidth = if (isSelected) 3 else 0
            root.setStrokeColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.fitto_green else android.R.color.transparent
                )
            )

            searchUserNameText.text = user.userName
            searchUserBioText.text = if (isSelected) {
                context.getString(R.string.messages_selected_member)
            } else {
                user.bio
                    ?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.social_no_bio)
            }

            val initial = user.userName.firstOrNull()?.uppercase() ?: "F"
            searchUserInitialText.text = initial

            val hasPhoto = !user.photoUrl.isNullOrBlank()
            searchUserAvatarImage.isVisible = hasPhoto
            searchUserInitialText.isVisible = !hasPhoto

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = searchUserAvatarImage,
                    photoUrl = user.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                searchUserAvatarImage.setImageDrawable(null)
            }

            root.setOnClickListener {
                onUserClick(user)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SearchUserUiModel>() {
        override fun areItemsTheSame(
            oldItem: SearchUserUiModel,
            newItem: SearchUserUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: SearchUserUiModel,
            newItem: SearchUserUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
