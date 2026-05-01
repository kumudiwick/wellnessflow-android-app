package com.kumud.wellnessflow.ui.mood

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.databinding.FragmentMoodBinding

/**
 * Hosts the mood journal, connecting list display, entry dialog, and sharing actions.
 */
class MoodFragment : Fragment() {

    private var _binding: FragmentMoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels { MoodViewModelFactory(requireContext()) }
    private lateinit var moodsAdapter: MoodsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecycler()
        attachSwipeToDelete()
        setupObservers()
        binding.addMoodFab.setOnClickListener { showMoodEntryDialog { input ->
            viewModel.addMoodEntry(input.emoji, input.label, input.note, input.energyLevel)
        } }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadEntries()
    }

    private fun setupToolbar() {
        binding.moodToolbar.inflateMenu(R.menu.mood_menu)
        binding.moodToolbar.setOnMenuItemClickListener(::handleToolbarMenu)
    }

    private fun setupRecycler() {
        moodsAdapter = MoodsAdapter()
        binding.moodRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            moodsAdapter.submitList(entries)
            binding.moodEmptyView.isVisible = entries.isEmpty()
        }
    }

    private fun attachSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return
                }
                val entry = moodsAdapter.getEntry(position)
                if (entry == null) {
                    moodsAdapter.notifyItemChanged(position)
                    return
                }
                viewModel.deleteMoodEntry(entry.id)
                showUndoSnackbar(entry)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.moodRecycler)
    }

    private fun showUndoSnackbar(deletedEntry: MoodEntry) {
        Snackbar.make(binding.root, R.string.mood_deleted_message, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewModel.restoreMoodEntry(deletedEntry)
            }
            .show()
    }


    private fun handleToolbarMenu(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_moods -> {
                shareMoodSummary(viewModel.entries.value.orEmpty())
                true
            }
            else -> false
        }
    }

    // Compose a short text summary so the user can share recent moods.
    private fun shareMoodSummary(entries: List<MoodEntry>) {
        if (entries.isEmpty()) return
        val topEntries = entries.take(5)
        val summary = buildString {
            appendLine(getString(R.string.mood_title))
            topEntries.forEach { entry ->
                val note = entry.note.ifBlank { getString(R.string.mood_note_hint) }
                appendLine("${entry.emoji} ${entry.moodLabel} • $note")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.menu_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
