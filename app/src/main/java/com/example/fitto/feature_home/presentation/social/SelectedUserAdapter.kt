package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemSelectedUserBinding

class SelectedUserAdapter(
    private val onRemoveClick: (SearchUserUiModel) -> Unit
) : ListAdapter<SearchUserUiModel, SelectedUserAdapter.SelectedUserViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedUserViewHolder {
        val binding = ItemSelectedUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SelectedUserViewHolder(
        private val binding: ItemSelectedUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SearchUserUiModel) = with(binding) {
            selectedUserNameText.text = user.userName
            selectedUserInitialText.text = user.userName.firstOrNull()?.uppercase() ?: "F"

            val hasPhoto = !user.photoUrl.isNullOrBlank()
            selectedUserAvatarImage.isVisible = hasPhoto
            selectedUserInitialText.isVisible = !hasPhoto

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = selectedUserAvatarImage,
                    photoUrl = user.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                selectedUserAvatarImage.setImageDrawable(null)
            }

            selectedUserRemoveButton.setOnClickListener {
                onRemoveClick(user)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SearchUserUiModel>() {
        override fun areItemsTheSame(oldItem: SearchUserUiModel, newItem: SearchUserUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SearchUserUiModel, newItem: SearchUserUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
