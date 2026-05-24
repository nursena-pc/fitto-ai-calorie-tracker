package com.example.fitto.feature_home.presentation.social

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.fitto.R
import kotlin.math.roundToInt

class SocialCreateHeaderAdapter(
    private val onCreatePostClick: () -> Unit,
    private val onProfileClick: () -> Unit
) : RecyclerView.Adapter<SocialCreateHeaderAdapter.HeaderViewHolder>() {

    private var profilePhotoUrl: String? = null

    fun updateProfilePhoto(photoUrl: String?) {
        if (profilePhotoUrl == photoUrl) return
        profilePhotoUrl = photoUrl
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_social_create_post_header, parent, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(profilePhotoUrl)
    }

    override fun getItemCount(): Int = 1

    inner class HeaderViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(photoUrl: String?) {
            val avatarContainer = itemView.findViewById<FrameLayout>(R.id.headerAvatarContainer)
            val avatarImage = itemView.findViewById<ImageView>(R.id.headerUserPhotoImage)
            val hasPhoto = !photoUrl.isNullOrBlank()

            if (avatarImage != null) {
                if (hasPhoto) {
                    avatarContainer.setBackgroundResource(0)
                    avatarImage.layoutParams = FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER)
                    avatarImage.scaleType = ImageView.ScaleType.CENTER_CROP
                    avatarImage.imageTintList = null
                    ProfilePhotoLoader.load(
                        imageView = avatarImage,
                        photoUrl = photoUrl,
                        placeholderResId = R.drawable.ic_person_outline,
                        errorResId = R.drawable.ic_person_outline
                    )
                } else {
                    avatarContainer.setBackgroundResource(R.drawable.bg_circle_primary_soft)
                    avatarImage.layoutParams = FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER)
                    avatarImage.scaleType = ImageView.ScaleType.CENTER
                    avatarImage.setImageResource(R.drawable.ic_person_outline)
                    avatarImage.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.fitto_green)
                    )
                }
            }

            avatarContainer?.apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { onProfileClick() }
            }
            avatarImage?.apply {
                isClickable = true
                isFocusable = true
                setOnClickListener { onProfileClick() }
            }

            itemView.setOnClickListener { onCreatePostClick() }
            itemView.findViewById<View>(R.id.headerCreatePostButton)
                ?.setOnClickListener { onCreatePostClick() }
        }

        private fun dp(value: Int): Int {
            return (value * itemView.resources.displayMetrics.density).roundToInt()
        }
    }
}