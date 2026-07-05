package com.example.fitto.feature_home.presentation.social

import com.example.fitto.core.text.AppLocales
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R
import com.example.fitto.databinding.ItemGroupMemberBinding
import java.util.Locale

class GroupMembersAdapter(
    private val onMemberClick: (GroupMemberUiModel) -> Unit
) : RecyclerView.Adapter<GroupMembersAdapter.GroupMemberViewHolder>() {

    private val items = mutableListOf<GroupMemberUiModel>()

    fun submitList(newItems: List<GroupMemberUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupMemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupMemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupMemberViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class GroupMemberViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GroupMemberUiModel) = with(binding) {
            memberNameText.text = item.userName
            memberSubtitleText.text = if (item.isMe) {
                root.context.getString(R.string.group_you)
            } else {
                root.context.getString(R.string.group_username_format, item.userName)
            }
            val hasPhoto = !item.photoUrl.isNullOrBlank()
            memberAvatarImage.isVisible = hasPhoto
            memberInitialsText.isVisible = !hasPhoto

            if (hasPhoto) {
                ProfilePhotoLoader.load(
                    imageView = memberAvatarImage,
                    photoUrl = item.photoUrl,
                    placeholderResId = R.drawable.ic_profile_person,
                    errorResId = R.drawable.ic_profile_person
                )
            } else {
                memberAvatarImage.setImageDrawable(null)
                memberInitialsText.text = createInitials(item.userName)
            }

            memberRoleBadgeText.isVisible = item.isCreator
            memberRoleBadgeText.text = root.context.getString(R.string.group_creator)
            root.setOnClickListener {
                onMemberClick(item)
            }
        }

        private fun createInitials(value: String): String {
            return value
                .replace("@", "")
                .split(" ", ".", "_", "-")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase(AppLocales.TURKISH) }
                .joinToString("")
                .ifBlank { value.firstOrNull()?.uppercase(AppLocales.TURKISH) ?: "F" }
        }
    }
}
