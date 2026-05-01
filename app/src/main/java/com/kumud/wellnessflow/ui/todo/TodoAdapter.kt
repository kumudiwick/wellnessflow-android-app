package com.kumud.wellnessflow.ui.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.TodoItem
import com.kumud.wellnessflow.databinding.ItemTodoBinding

class TodoAdapter(
    private val onToggle: (TodoItem, Boolean) -> Unit,
    private val onEdit: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTodoBinding.inflate(inflater, parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodoItem) {
            binding.todoCheckbox.apply {
                setOnCheckedChangeListener(null)
                isChecked = item.isCompleted
                setOnCheckedChangeListener { _, isChecked -> onToggle(item, isChecked) }
            }

            binding.todoTitle.text = item.title
            binding.todoTitle.paintFlags = if (item.isCompleted) {
                binding.todoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.todoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.todoTitle.alpha = if (item.isCompleted) 0.6f else 1f

            val notes = item.notes
            binding.todoNotes.text = notes
            binding.todoNotes.visibility = if (notes.isNotBlank()) View.VISIBLE else View.GONE

            binding.todoMore.setOnClickListener { view ->
                showPopup(view, item)
            }

            binding.root.setOnClickListener { onEdit(item) }
        }

        private fun showPopup(anchor: View, item: TodoItem) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.inflate(R.menu.menu_todo_item)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEdit(item)
                        true
                    }
                    R.id.action_delete -> {
                        onDelete(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean =
            oldItem == newItem
    }
}
