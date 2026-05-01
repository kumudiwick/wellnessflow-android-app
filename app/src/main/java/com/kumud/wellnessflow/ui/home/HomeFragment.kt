package com.kumud.wellnessflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.MoodEntry
import com.kumud.wellnessflow.databinding.FragmentHomeBinding
import com.kumud.wellnessflow.ui.mood.MoodEntryInput
import com.kumud.wellnessflow.ui.mood.showMoodEntryDialog
import com.kumud.wellnessflow.ui.profile.ProfileActivity
import com.kumud.wellnessflow.utils.DateUtils

/**
 * Aggregates wellness metrics for the dashboard and routes quick actions to feature tabs.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDaySelector()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadState()
    }

    private fun setupDaySelector() {
        val chipGroup = binding.homeDayChipGroup
        val labels = resources.getStringArray(R.array.weekdays_short)
        val storageFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startOfWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        chipGroup.removeAllViews()
        var initialSelectionId = View.NO_ID
        val todayKey = DateUtils.todayKey()

        labels.forEachIndexed { index, label ->
            val dayCalendar = (startOfWeek.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index)
            }
            val dateKey = storageFormatter.format(dayCalendar.time)
            val chip = layoutInflater.inflate(R.layout.view_day_chip, chipGroup, false) as Chip
            chip.id = ViewCompat.generateViewId()
            val dayNumber = dayCalendar.get(Calendar.DAY_OF_MONTH)
            chip.text = getString(R.string.home_day_chip_text, label, dayNumber)
            chip.tag = dateKey
            chip.isChecked = dateKey == todayKey
            chipGroup.addView(chip)
            if (chip.isChecked) {
                initialSelectionId = chip.id
            }
        }

        when {
            initialSelectionId != View.NO_ID -> {
                chipGroup.check(initialSelectionId)
                updateDaySubtitle(todayKey)
            }
            chipGroup.childCount > 0 -> {
                val firstChip = chipGroup.getChildAt(0) as Chip
                chipGroup.check(firstChip.id)
                updateDaySubtitle(firstChip.tag as String)
            }
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedId)
            val dateKey = chip?.tag as? String ?: return@setOnCheckedStateChangeListener
            updateDaySubtitle(dateKey)
        }
    }

    private fun updateDaySubtitle(dateKey: String) {
        val friendly = DateUtils.formatDateKey(dateKey)
        val subtitle = if (dateKey == DateUtils.todayKey()) {
            getString(R.string.home_day_selection_today, friendly)
        } else {
            getString(R.string.home_day_selection_generic, friendly)
        }
        binding.homeDaySubtitle.text = subtitle
    }

    private fun setupListeners() {
        binding.homeHabitCard.setOnClickListener { selectBottomTab(R.id.menu_habits) }
        binding.homeHydrationCard.setOnClickListener { selectBottomTab(R.id.menu_hydration) }
        binding.homeHydrationButton.setOnClickListener {
            viewModel.logHydration(250)
            Snackbar.make(binding.root, R.string.hydration_quick_add_confirmation, Snackbar.LENGTH_SHORT).show()
        }
        binding.homeMoodButton.setOnClickListener { showMoodEntryDialog(::handleMoodInput) }
        binding.homeProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.homeGreeting.text = state.greeting
            binding.homeDate.text = state.dateDisplay
            val headerMessage = if (state.userName.isNotBlank()) {
                getString(R.string.home_header_message_personalized, state.userName)
            } else {
                getString(R.string.home_header_message)
            }
            binding.homeHeaderMessage.text = headerMessage

            binding.homeHabitProgress.text = getString(R.string.home_habit_summary, state.habitCompleted, state.habitTotal)
            binding.homeHabitProgressIndicator.progress = state.habitProgressPercent.coerceIn(0, 100)

            binding.homeLastMood.text = buildMoodSummary(state.lastMood)

            binding.homeHydrationProgress.text = getString(R.string.hydration_progress_label, state.hydrationTotal, state.hydrationGoal)
            binding.homeHydrationIndicator.progress = state.hydrationProgressPercent
            binding.homeHydrationIndicator.isVisible = state.hydrationGoal > 0
        }
    }

    // Turn the most recent mood into a readable line for the dashboard header.
    private fun buildMoodSummary(mood: MoodEntry?): String {
        if (mood == null) return getString(R.string.mood_empty_state)
        val relativeTime = DateUtils.formatRelativeTimestamp(mood.timestamp)
        val energyText = mood.energyLevel?.let { getString(R.string.home_mood_energy, it) }
        val note = mood.note.takeIf { it.isNotBlank() }

        return buildString {
            append(mood.emoji)
            append(' ')
            append(mood.moodLabel)
            append("  ")
            append(relativeTime)
            if (!energyText.isNullOrBlank()) {
                append("  ")
                append(energyText)
            }
            if (!note.isNullOrBlank()) {
                append('\n')
                append(note)
            }
        }
    }

    private fun handleMoodInput(input: MoodEntryInput) {
        val moodEntry = MoodEntry(
            emoji = input.emoji,
            moodLabel = input.label,
            note = input.note,
            energyLevel = input.energyLevel,
            date = DateUtils.todayKey()
        )
        viewModel.addMoodEntry(moodEntry)
    }

    private fun selectBottomTab(menuId: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)?.selectedItemId = menuId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
