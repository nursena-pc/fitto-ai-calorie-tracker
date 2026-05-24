package com.example.fitto.feature_home.presentation.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R

class ProfileMenuAdapter(
    private val items: List<ProfileMenuItem>,
    private val onItemClick: (ProfileMenuId) -> Unit
) : RecyclerView.Adapter<ProfileMenuAdapter.ProfileMenuViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileMenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_menu, parent, false)

        return ProfileMenuViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfileMenuViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProfileMenuViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView =
            itemView.findViewById(R.id.textProfileMenuTitle)

        private val iconImageView: ImageView =
            itemView.findViewById(R.id.imageProfileMenuIcon)

        fun bind(item: ProfileMenuItem) {
            titleTextView.setText(item.titleResId)

            iconImageView.setImageResource(item.iconResId)
            iconImageView.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    R.color.fitto_green
                )
            )

            itemView.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }
}