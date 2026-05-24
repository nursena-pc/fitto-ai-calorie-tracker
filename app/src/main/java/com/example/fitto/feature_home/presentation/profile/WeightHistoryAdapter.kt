package com.example.fitto.feature_home.presentation.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitto.R
import com.example.fitto.feature_home.presentation.profile.health.WeightHistoryItem
import com.example.fitto.feature_home.presentation.profile.health.WeightTrend
import com.example.fitto.feature_home.presentation.profile.health.formatWeightKg

class WeightHistoryAdapter(
    private val items: List<WeightHistoryItem>
) : RecyclerView.Adapter<WeightHistoryAdapter.WeightHistoryViewHolder>() {

    inner class WeightHistoryViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val textHistoryDate =
            itemView.findViewById<TextView>(R.id.textHistoryDate)

        private val textHistoryWeight =
            itemView.findViewById<TextView>(R.id.textHistoryWeight)

        private val imageHistoryTrend =
            itemView.findViewById<ImageView>(R.id.imageHistoryTrend)

        fun bind(item: WeightHistoryItem) {
            textHistoryDate.text = item.dateText
            textHistoryWeight.text =
                itemView.context.getString(R.string.weight_value_format, formatWeightKg(item.weightKg))

            when (item.trend) {
                WeightTrend.UP -> {
                    imageHistoryTrend.setImageResource(R.drawable.ic_arrow_upward)
                    imageHistoryTrend.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.fitto_orange)
                    )
                }

                WeightTrend.DOWN -> {
                    imageHistoryTrend.setImageResource(R.drawable.ic_arrow_downward)
                    imageHistoryTrend.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.fitto_green)
                    )
                }

                WeightTrend.SAME -> {
                    imageHistoryTrend.setImageResource(R.drawable.ic_remove)
                    imageHistoryTrend.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.profile_subtext)
                    )
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WeightHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_history, parent, false)

        return WeightHistoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: WeightHistoryViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}