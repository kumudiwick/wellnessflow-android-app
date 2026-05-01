package com.kumud.wellnessflow.ui.mood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.utils.DateUtils

class MoodsAdapter : ListAdapter<MoodEntry, MoodsAdapter.MoodViewHolder>(DiffCallback) {

    private val expandedEntries = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
        return MoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        val entry = getItem(position)
        val expanded = expandedEntries.contains(entry.id)
        holder.bind(entry, expanded)
    }

    inner class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emoji: TextView = itemView.findViewById(R.id.mood_emoji)
        private val label: TextView = itemView.findViewById(R.id.mood_label)
        private val timestamp: TextView = itemView.findViewById(R.id.mood_timestamp)
        private val note: TextView = itemView.findViewById(R.id.mood_note)

        fun bind(entry: MoodEntry, expanded: Boolean) {
            emoji.text = entry.emoji
            label.text = entry.moodLabel
            timestamp.text = buildTimestamp(entry)

            if (entry.note.isNotBlank()) {
                note.visibility = View.VISIBLE
                note.text = entry.note
                note.maxLines = if (expanded) Int.MAX_VALUE else 2
            } else {
                note.visibility = View.GONE
            }

            itemView.setOnClickListener {
                if (entry.note.isNotBlank()) {
                    if (expandedEntries.contains(entry.id)) {
                        expandedEntries.remove(entry.id)
                    } else {
                        expandedEntries.add(entry.id)
                    }
                    adapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { position ->
                        notifyItemChanged(position)
                    }
                }
            }
        }

        private fun buildTimestamp(entry: MoodEntry): String {
            val base = DateUtils.formatRelativeTimestamp(entry.timestamp)
            return if (entry.energyLevel != null) {
                itemView.context.getString(R.string.mood_timestamp_with_energy, base, entry.energyLevel)
            } else {
                base
            }
        }
    }

    fun getEntry(position: Int): MoodEntry? =
        if (position in 0 until itemCount) getItem(position) else null

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MoodEntry>() {
            override fun areItemsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean = oldItem == newItem
        }
    }
}
