package com.example.fitto.feature_home.presentation.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemGroupAddMemberBinding
import java.util.Locale

class GroupAddMembersAdapter(
    private val onUserClick: (SearchUserUiModel) -> Unit
) : RecyclerView.Adapter<GroupAddMembersAdapter.AddMemberViewHolder>() {

    private val items = mutableListOf<SearchUserUiModel>()
    private val selectedIds = mutableSetOf<String>()

    fun submitList(newItems: List<SearchUserUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun toggleSelection(user: SearchUserUiModel) {
        if (selectedIds.contains(user.id)) {
            selectedIds.remove(user.id)
        } else {
            selectedIds.add(user.id)
        }
        notifyDataSetChanged()
    }

    fun getSelectedUsers(): List<SearchUserUiModel> {
        return items.filter { user -> selectedIds.contains(user.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddMemberViewHolder {
        val binding = ItemGroupAddMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddMemberViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AddMemberViewHolder(
        private val binding: ItemGroupAddMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: SearchUserUiModel) = with(binding) {
            addMemberNameText.text = user.userName
            addMemberBioText.text = user.bio?.takeIf { it.isNotBlank() } ?: "@${user.userName}"

            val hasPhoto = !user.photoUrl.isNullOrBlank()
            addMemberAvatarImage.isVisible = hasPhoto
            addMemberInitialsText.isVisible = !hasPhoto

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = addMemberAvatarImage,
                    photoUrl = user.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                addMemberAvatarImage.setImageDrawable(null)
                addMemberInitialsText.text = createInitials(user.userName)
            }

            addMemberCheckContainer.isVisible = selectedIds.contains(user.id)

            root.setOnClickListener {
                onUserClick(user)
            }
        }

        private fun createInitials(value: String): String {
            return value
                .replace("@", "")
                .split(" ", ".", "_", "-")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase(Locale("tr", "TR")) }
                .joinToString("")
                .ifBlank { value.firstOrNull()?.uppercase(Locale("tr", "TR")) ?: "F" }
        }
    }
}