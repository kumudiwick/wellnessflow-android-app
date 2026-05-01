package com.kumud.wellnessflow.ui.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.TodoItem
import com.kumud.wellnessflow.databinding.DialogAddTodoBinding
import com.kumud.wellnessflow.databinding.FragmentTodoBinding

class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodoViewModel by viewModels { TodoViewModelFactory(requireContext()) }
    private lateinit var adapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeState()
        binding.addTodoFab.setOnClickListener { showTodoDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupRecycler() {
        adapter = TodoAdapter(
            onToggle = { item, completed -> viewModel.toggleCompleted(item, completed) },
            onEdit = { item -> showTodoDialog(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        binding.todoRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.todoRecycler.adapter = adapter
    }

    private fun observeState() {
        viewModel.todos.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.todoEmptyView.isVisible = items.isEmpty()
        }
    }

    private fun showTodoDialog(existing: TodoItem?) {
        val dialogBinding = DialogAddTodoBinding.inflate(layoutInflater)
        val dialogTitle = if (existing == null) R.string.todo_add_dialog_title else R.string.todo_edit_dialog_title

        existing?.let {
            dialogBinding.inputTitle.setText(it.title)
            dialogBinding.inputNotes.setText(it.notes)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            val positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                val title = dialogBinding.inputTitle.text?.toString().orEmpty()
                if (title.isBlank()) {
                    dialogBinding.inputLayoutTitle.error = getString(R.string.todo_name_hint)
                    return@setOnClickListener
                }
                dialogBinding.inputLayoutTitle.error = null

                val notes = dialogBinding.inputNotes.text?.toString().orEmpty()
                if (existing == null) {
                    viewModel.addTodo(title, notes)
                } else {
                    viewModel.updateTodo(existing, title, notes)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDelete(item: TodoItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.todo_delete_confirm)
            .setMessage(R.string.todo_delete_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteTodo(item)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
