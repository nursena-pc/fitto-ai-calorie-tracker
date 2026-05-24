package com.example.fitto.feature_home.presentation.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitto.R

class ProfilePhotoAlbumAdapter(
    private val onPhotoClick: (ProfilePhotoAlbumItem) -> Unit
) : RecyclerView.Adapter<ProfilePhotoAlbumAdapter.ProfilePhotoAlbumViewHolder>() {

    private var items: List<ProfilePhotoAlbumItem> = emptyList()

    fun submitList(newItems: List<ProfilePhotoAlbumItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfilePhotoAlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_photo_album, parent, false)
        return ProfilePhotoAlbumViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfilePhotoAlbumViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProfilePhotoAlbumViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.imageProfileAlbumPhoto)
        private val sourceTextView: TextView = itemView.findViewById(R.id.textProfileAlbumPhotoSource)
        private val dateTextView: TextView = itemView.findViewById(R.id.textProfileAlbumPhotoDate)

        fun bind(item: ProfilePhotoAlbumItem) {
            imageView.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_create_post_image_placeholder)
                error(R.drawable.bg_create_post_image_placeholder)
            }
            sourceTextView.text = item.sourceTitle
            dateTextView.text = item.dateText
            itemView.setOnClickListener { onPhotoClick(item) }
        }
    }
}
