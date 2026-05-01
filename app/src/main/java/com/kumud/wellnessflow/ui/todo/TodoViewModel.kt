package com.kumud.wellnessflow.ui.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kumud.wellnessflow.data.models.TodoItem
import com.kumud.wellnessflow.data.repository.PreferencesManager

class TodoViewModel(private val preferences: PreferencesManager) : ViewModel() {

    private val _todos = MutableLiveData<List<TodoItem>>()
    val todos: LiveData<List<TodoItem>> = _todos

    fun refresh() {
        val stored = preferences.loadTodoItems()
        _todos.value = stored
    }

    fun addTodo(title: String, notes: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val items = preferences.loadTodoItems()
        val updated = TodoItem(title = trimmedTitle, notes = notes.trim())
        items.add(0, updated)
        persist(items)
    }

    fun updateTodo(item: TodoItem, title: String, notes: String) {
        val items = preferences.loadTodoItems()
        val index = items.indexOfFirst { it.id == item.id }
        if (index == -1) return
        items[index] = item.copy(title = title.trim(), notes = notes.trim())
        persist(items)
    }

    fun toggleCompleted(item: TodoItem, completed: Boolean) {
        val items = preferences.loadTodoItems()
        val index = items.indexOfFirst { it.id == item.id }
        if (index == -1) return
        items[index] = item.copy(isCompleted = completed)
        persist(items)
    }

    fun deleteTodo(item: TodoItem) {
        val items = preferences.loadTodoItems()
        val updated = items.filterNot { it.id == item.id }
        persist(updated.toMutableList())
    }

    private fun persist(mutable: MutableList<TodoItem>) {
        val ordered = mutable.sortedWith(compareBy<TodoItem> { it.isCompleted }.thenByDescending { it.createdAt })
        preferences.saveTodoItems(ordered)
        _todos.value = ordered
    }
}


