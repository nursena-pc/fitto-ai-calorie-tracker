package com.example.fitto.feature_exercise

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.databinding.ItemExerciseActivityBinding
import com.example.fitto.feature_exercise.model.ExerciseUiModel

class ExerciseActivityAdapter(
    private val onActivityClick: (ExerciseUiModel) -> Unit,
    private val titleTextProvider: (ExerciseUiModel) -> String,
    private val descriptionTextProvider: (ExerciseUiModel) -> String,
    private val durationTextProvider: (Int) -> String,
    private val calorieTextProvider: (ExerciseUiModel) -> String
) : ListAdapter<ExerciseUiModel, ExerciseActivityAdapter.ExerciseViewHolder>(DiffCallback) {

    private var selectedActivityId: String? = null

    fun submitActivities(
        activities: List<ExerciseUiModel>,
        selectedId: String?
    ) {
        selectedActivityId = selectedId
        submitList(activities)
    }

    fun updateSelectedActivity(newSelectedId: String?) {
        val oldSelectedId = selectedActivityId
        if (oldSelectedId == newSelectedId) return

        selectedActivityId = newSelectedId

        val oldIndex = currentList.indexOfFirst { it.id == oldSelectedId }
        val newIndex = currentList.indexOfFirst { it.id == newSelectedId }

        if (oldIndex != -1) notifyItemChanged(oldIndex, PAYLOAD_SELECTION)
        if (newIndex != -1) notifyItemChanged(newIndex, PAYLOAD_SELECTION)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExerciseViewHolder {
        val binding = ItemExerciseActivityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ExerciseViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: ExerciseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position))
        } else {
            holder.bind(getItem(position))
        }
    }

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseActivityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(activity: ExerciseUiModel) = with(binding) {
            activityIconText.text = activity.iconEmoji
            activityTitleText.text = titleTextProvider(activity)
            activityDescriptionText.text = descriptionTextProvider(activity)
            activityMetaText.text = "${durationTextProvider(activity.defaultDurationMinutes)} · ${calorieTextProvider(activity)}"

            activityTitleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD)

            root.setOnClickListener {
                onActivityClick(activity)
            }

            activitySelectButton.setOnClickListener {
                onActivityClick(activity)
            }

            bindSelection(activity)
        }

        fun bindSelection(activity: ExerciseUiModel) = with(binding) {
            val context = root.context
            val isSelected = activity.id == selectedActivityId

            val green = ContextCompat.getColor(context, R.color.fitto_green)
            val white = ContextCompat.getColor(context, R.color.white)
            val divider = ContextCompat.getColor(context, R.color.fitto_divider_soft)
            val softGreen = ContextCompat.getColor(context, R.color.fitto_primary_soft)

            activityCard.strokeWidth = if (isSelected) dp(2) else dp(1)
            activityCard.setStrokeColor(if (isSelected) green else divider)

            activitySelectButton.text = if (isSelected) "✓" else "+"
            activitySelectButton.setTextColor(if (isSelected) white else green)
            activitySelectButton.backgroundTintList = ColorStateList.valueOf(
                if (isSelected) green else softGreen
            )
        }

        private fun dp(value: Int): Int {
            return (value * itemView.resources.displayMetrics.density).toInt()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ExerciseUiModel>() {
        override fun areItemsTheSame(
            oldItem: ExerciseUiModel,
            newItem: ExerciseUiModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ExerciseUiModel,
            newItem: ExerciseUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "payload_selection"
    }
}