package com.kumud.wellnessflow.ui.hydration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.utils.DateUtils

class HydrationHistoryAdapter : ListAdapter<HydrationHistoryItem, HydrationHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hydration_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.history_date)
        private val amountText: TextView = itemView.findViewById(R.id.history_amount)

        fun bind(item: HydrationHistoryItem) {
            dateText.text = DateUtils.formatDayLabel(item.dateKey)
            amountText.text = itemView.context.getString(R.string.hydration_history_amount, item.totalMl)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HydrationHistoryItem>() {
            override fun areItemsTheSame(oldItem: HydrationHistoryItem, newItem: HydrationHistoryItem): Boolean = oldItem.dateKey == newItem.dateKey
            override fun areContentsTheSame(oldItem: HydrationHistoryItem, newItem: HydrationHistoryItem): Boolean = oldItem == newItem
        }
    }
}
