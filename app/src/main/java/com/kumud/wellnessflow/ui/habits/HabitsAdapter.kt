package com.kumud.wellnessflow.ui.habits

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.Habit

class HabitsAdapter(
    private val onHabitChecked: (Habit, Boolean) -> Unit,
    private val onEditHabit: (Habit) -> Unit,
    private val onDeleteHabit: (Habit) -> Unit
) : ListAdapter<Habit, HabitsAdapter.HabitViewHolder>(DiffCallback) {

    private var completionState: Map<String, Boolean> = emptyMap()

    fun updateCompletionState(state: Map<String, Boolean>) {
        completionState = state
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        val completed = completionState[habit.id] == true
        holder.bind(habit, completed)
    }

    inner class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val colorCard: MaterialCardView = itemView.findViewById(R.id.habit_color_card)
        private val name: TextView = itemView.findViewById(R.id.habit_name)
        private val description: TextView = itemView.findViewById(R.id.habit_description)
        private val colorStrip: View = itemView.findViewById(R.id.habit_color_strip)
        private val checkbox: CheckBox = itemView.findViewById(R.id.habit_checkbox)
        private val moreButton: ImageButton = itemView.findViewById(R.id.habit_more)

        fun bind(habit: Habit, completed: Boolean) {
            name.text = habit.name
            if (habit.description.isNotBlank()) {
                description.visibility = View.VISIBLE
                description.text = habit.description
            } else {
                description.visibility = View.GONE
            }

            colorCard.setCardBackgroundColor(habit.color)
            colorStrip.setBackgroundColor(habit.color)
            card.strokeColor = habit.color
            checkbox.buttonTintList = ColorStateList.valueOf(habit.color)

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = completed
            checkbox.setOnCheckedChangeListener { _, isChecked -> onHabitChecked(habit, isChecked) }

            moreButton.setOnClickListener {
                showPopupMenu(it, habit)
            }
            card.setOnLongClickListener {
                showPopupMenu(it, habit)
                true
            }
        }

        private fun showPopupMenu(anchor: View, habit: Habit) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.habit_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_habit -> {
                        onEditHabit(habit)
                        true
                    }
                    R.id.action_delete_habit -> {
                        onDeleteHabit(habit)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Habit>() {
            override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean = oldItem == newItem
        }
    }
}


