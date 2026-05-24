package com.example.fitto.feature_home.presentation.profile

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R

class ProfileDetailAdapter(
    items: List<ProfileDetailItem>,
    private val onItemClick: (ProfileDetailItem) -> Unit = {}
) : RecyclerView.Adapter<ProfileDetailAdapter.ProfileDetailViewHolder>() {

    private var items: List<ProfileDetailItem> = items

    fun updateItems(newItems: List<ProfileDetailItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_detail_row, parent, false)

        return ProfileDetailViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProfileDetailViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ProfileDetailViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val sectionHeaderTextView: TextView =
            itemView.findViewById(R.id.textProfileDetailSectionHeader)

        private val rowLayout: View =
            itemView.findViewById(R.id.layoutProfileDetailRow)

        private val iconContainerView: View =
            itemView.findViewById(R.id.iconProfileDetailRowContainer)

        private val iconImageView: ImageView =
            itemView.findViewById(R.id.imageProfileDetailRowIcon)

        private val titleTextView: TextView =
            itemView.findViewById(R.id.textProfileDetailRowTitle)

        private val subtitleTextView: TextView =
            itemView.findViewById(R.id.textProfileDetailRowSubtitle)

        private val trailingTextView: TextView =
            itemView.findViewById(R.id.textProfileDetailRowTrailing)

        private val chevronTextView: TextView =
            itemView.findViewById(R.id.textProfileDetailRowChevron)

        private val rowSwitch: Switch =
            itemView.findViewById(R.id.switchProfileDetailRow)

        private val dividerView: View =
            itemView.findViewById(R.id.viewProfileDetailDivider)

        fun bind(item: ProfileDetailItem) {
            val context = itemView.context
            val normalTextColor = ContextCompat.getColor(context, R.color.profile_text)
            val subTextColor = ContextCompat.getColor(context, R.color.profile_subtext)
            val greenColor = ContextCompat.getColor(context, R.color.fitto_green)
            val dangerColor = ContextCompat.getColor(context, R.color.reminder_delete)
            val disabledAlpha = if (item.isEnabled) 1f else 0.42f

            sectionHeaderTextView.isVisible = item.sectionTitleResId != null
            item.sectionTitleResId?.let { sectionHeaderTextView.setText(it) }
            sectionHeaderTextView.alpha = disabledAlpha

            iconContainerView.isVisible = item.iconResId != null
            item.iconResId?.let { iconRes ->
                iconImageView.setImageResource(iconRes)
                iconImageView.setColorFilter(if (item.isDanger) dangerColor else greenColor)
            }
            iconContainerView.alpha = disabledAlpha

            titleTextView.text = item.titleText
                ?.takeIf { it.isNotBlank() }
                ?: context.getString(item.titleResId)
            titleTextView.setTextColor(
                when {
                    item.isDanger -> dangerColor
                    item.isPrimaryButton -> greenColor
                    else -> normalTextColor
                }
            )

            val subtitle = item.subtitleText
                ?: item.subtitleResId?.let { context.getString(it) }

            subtitleTextView.isVisible = !subtitle.isNullOrBlank()
            subtitleTextView.text = subtitle.orEmpty()
            subtitleTextView.setTextColor(subTextColor)

            trailingTextView.isVisible = !item.trailingText.isNullOrBlank() && !item.isSwitch
            trailingTextView.text = item.trailingText.orEmpty()
            trailingTextView.setTextColor(if (item.isDanger) dangerColor else subTextColor)

            rowSwitch.isVisible = item.isSwitch
            rowSwitch.isChecked = item.switchChecked
            rowSwitch.isEnabled = item.isEnabled
            rowSwitch.isClickable = false
            rowSwitch.isFocusable = false

            chevronTextView.isVisible = item.showChevron && !item.isSwitch && !item.isPrimaryButton && !item.isInfoCard
            chevronTextView.setTextColor(subTextColor)

            titleTextView.alpha = disabledAlpha
            subtitleTextView.alpha = disabledAlpha
            trailingTextView.alpha = disabledAlpha
            chevronTextView.alpha = disabledAlpha
            rowSwitch.alpha = disabledAlpha

            val backgroundTintColor = when {
                item.isInfoCard -> ContextCompat.getColor(context, R.color.fitto_badge_background)
                else -> ContextCompat.getColor(context, R.color.profile_menu_background)
            }
            rowLayout.backgroundTintList = ColorStateList.valueOf(backgroundTintColor)

            dividerView.isVisible = item.showDividerAfter

            rowLayout.isEnabled = item.isEnabled
            rowLayout.isClickable = item.isEnabled
            rowLayout.alpha = if (item.isEnabled) 1f else 0.72f
            rowLayout.setOnClickListener {
                if (item.isEnabled) {
                    onItemClick(item)
                }
            }
        }
    }
}
