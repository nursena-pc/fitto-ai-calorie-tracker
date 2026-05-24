package com.example.fitto.feature_dietplans

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemDietPlanBinding
import com.example.fitto.feature_dietplans.model.DietPlanUiModel
import androidx.core.view.isVisible
import com.example.fitto.feature_dietplans.data.CustomDietPlanStore

class DietPlansAdapter(
    private val onDetailClick: (DietPlanUiModel) -> Unit,
    private val onApplyClick: (DietPlanUiModel) -> Unit,
    private val onFavoriteClick: (DietPlanUiModel) -> Unit,
    private val onDeleteClick: (DietPlanUiModel) -> Unit
) : ListAdapter<DietPlanUiModel, DietPlansAdapter.DietPlanViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietPlanViewHolder {
        val binding = ItemDietPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DietPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DietPlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DietPlanViewHolder(
        private val binding: ItemDietPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DietPlanUiModel) = with(binding) {
            val context = root.context

            planTitle.text = item.titleText
                ?: item.titleRes?.let { context.getString(it) }
                        ?: context.getString(R.string.app_name)

            planDescription.text = item.descriptionText
                ?: item.descriptionRes?.let { context.getString(it) }
                        ?: context.getString(R.string.diet_plan_custom_default_desc)
            kcalText.text = context.getString(R.string.diet_plans_kcal_per_day, item.kcalPerDay)
            durationText.text = context.resources.getQuantityString(
                R.plurals.diet_plans_day_count,
                item.durationDays,
                item.durationDays
            )

            if (item.tagRes != null) {
                planTag.visibility = View.VISIBLE
                planTag.setText(item.tagRes)
            } else {
                planTag.visibility = View.GONE
            }
            val isCustomPlan = item.id.startsWith(CustomDietPlanStore.CUSTOM_PLAN_ID_PREFIX)
            deleteButton.isVisible = isCustomPlan
            favoriteButton.alpha = if (item.isFavorite) 1f else 0.55f
            favoriteButton.setColorFilter(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    if (item.isFavorite) R.color.fitto_green else R.color.fitto_text_secondary
                )
            )

            detailButton.text = context.getString(
                if (item.isDraft) {
                    R.string.diet_plans_edit_draft
                } else {
                    R.string.diet_plans_detail
                }
            )

            applyButton.text = context.getString(
                when {
                    item.isDraft -> R.string.diet_plans_complete_draft
                    item.isApplied -> R.string.diet_plans_applied
                    else -> R.string.diet_plans_apply
                }
            )

            applyButton.isEnabled = item.isDraft || !item.isApplied
            applyButton.alpha = if (item.isApplied && !item.isDraft) 0.72f else 1f

            if (item.isApplied && !item.isDraft) {
                applyButton.setIconResource(R.drawable.ic_check_white)
            } else {
                applyButton.icon = null
            }

            favoriteButton.isVisible = !item.isDraft

            val imageResId = item.imageResId ?: R.drawable.my_plan

            planImage.visibility = View.VISIBLE
            planImage.setImageResource(imageResId)
            planImagePlaceholder.visibility = View.GONE
            detailButton.setOnClickListener { onDetailClick(item) }
            applyButton.setOnClickListener { onApplyClick(item) }
            favoriteButton.setOnClickListener { onFavoriteClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DietPlanUiModel>() {
            override fun areItemsTheSame(
                oldItem: DietPlanUiModel,
                newItem: DietPlanUiModel
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: DietPlanUiModel,
                newItem: DietPlanUiModel
            ): Boolean = oldItem == newItem
        }
    }
}