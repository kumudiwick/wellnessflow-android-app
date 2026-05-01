package com.kumud.wellnessflow.ui.habits

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.Habit
import com.kumud.wellnessflow.data.models.HabitFrequency
import com.kumud.wellnessflow.databinding.DialogAddHabitBinding
import com.kumud.wellnessflow.databinding.FragmentHabitsBinding
import com.kumud.wellnessflow.utils.DateUtils
import com.kumud.wellnessflow.widget.HabitProgressWidgetProvider

/**
 * Manages the habit tracker including list rendering, dialogs, and completion progress.
 */
class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HabitsViewModel by viewModels { HabitsViewModelFactory(requireContext()) }
    private lateinit var habitsAdapter: HabitsAdapter

    private var completionState: Map<String, Boolean> = emptyMap()

    private val colorOptions = mapOf(
        R.id.color_chip_blue to Color.parseColor("#4F8EF7"),
        R.id.color_chip_green to Color.parseColor("#4CC9B0"),
        R.id.color_chip_orange to Color.parseColor("#FFB74D"),
        R.id.color_chip_purple to Color.parseColor("#A855F7"),
        R.id.color_chip_teal to Color.parseColor("#26A69A")
    )

    private val iconOptions = mapOf(
        R.id.icon_chip_run to R.drawable.ic_run,
        R.id.icon_chip_food to R.drawable.ic_food,
        R.id.icon_chip_mind to R.drawable.ic_meditate,
        R.id.icon_chip_sleep to R.drawable.ic_sleep,
        R.id.icon_chip_water to R.drawable.ic_hydration
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupObservers()
        binding.addHabitFab.setOnClickListener { showHabitDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupRecycler() {
        habitsAdapter = HabitsAdapter(
            onHabitChecked = { habit, isChecked -> viewModel.toggleCompletion(habit.id, isChecked) },
            onEditHabit = { habit -> showHabitDialog(habit) },
            onDeleteHabit = { habit -> confirmDeleteHabit(habit) }
        )
        binding.habitsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitsAdapter
        }
    }

    private fun setupObservers() {
        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            habitsAdapter.submitList(habits)
            binding.habitsEmptyView.isVisible = habits.isEmpty()
            updateProgress(habits, completionState)
        }

        viewModel.completionState.observe(viewLifecycleOwner) { completions ->
            completionState = completions
            habitsAdapter.updateCompletionState(completions)
            val habits = viewModel.habits.value.orEmpty()
            updateProgress(habits, completions)
        }

        viewModel.todayKey.observe(viewLifecycleOwner) { dateKey ->
            val formattedDate = DateUtils.formatDateKey(dateKey)
            val baseHint = getString(R.string.habits_progress_hint)
            binding.habitsProgressCaption.text =
                getString(R.string.habits_progress_caption_with_date, formattedDate, baseHint)
        }
    }

    // Recalculate progress UI and surface updates to the home screen widget.
    private fun updateProgress(habits: List<Habit>, completions: Map<String, Boolean>) {
        val total = habits.size
        val completedCount = habits.count { completions[it.id] == true }
        val percentage = if (total == 0) 0 else ((completedCount.toFloat() / total.toFloat()) * 100).toInt()
        binding.habitsProgressIndicator.progress = percentage
        binding.habitsProgressText.text = getString(R.string.home_habit_summary, completedCount, total)
        HabitProgressWidgetProvider.requestRefresh(requireContext().applicationContext)
    }

    private fun showHabitDialog(existingHabit: Habit?) {
        val dialogBinding = DialogAddHabitBinding.inflate(layoutInflater)

        dialogBinding.inputName.filters = arrayOf(InputFilter.LengthFilter(50))
        (dialogBinding.inputCategory as? AutoCompleteTextView)?.apply {
            val categories = resources.getStringArray(R.array.habit_categories)
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))
        }

        populateWeekdayChips(dialogBinding)

        existingHabit?.let { habit ->
            dialogBinding.inputName.setText(habit.name)
            dialogBinding.inputDescription.setText(habit.description)
            (dialogBinding.inputCategory as? AutoCompleteTextView)?.setText(habit.category, false)

            colorOptions.entries.firstOrNull { it.value == habit.color }?.key?.let(dialogBinding.colorGroup::check)
            iconOptions.entries.firstOrNull { it.value == habit.iconResId }?.key?.let(dialogBinding.iconGroup::check)

            val isWeekly = habit.frequency == HabitFrequency.WEEKLY
            dialogBinding.frequencySwitch.isChecked = isWeekly
            dialogBinding.targetDaysLabel.isVisible = isWeekly
            dialogBinding.weeklyDaysGroup.isVisible = isWeekly
            if (isWeekly) {
                habit.targetDays.forEach { dayIndex ->
                    dialogBinding.weeklyDaysGroup.findViewWithTag<Chip>(dayIndex)?.isChecked = true
                }
            }
        }

        dialogBinding.frequencySwitch.setOnCheckedChangeListener { _, isChecked ->
            dialogBinding.targetDaysLabel.isVisible = isChecked
            dialogBinding.weeklyDaysGroup.isVisible = isChecked
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingHabit == null) R.string.add_habit else R.string.edit_habit)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {

                val name = dialogBinding.inputName.text?.toString().orEmpty().trim()
                if (name.isEmpty()) {
                    dialogBinding.inputLayoutName.error = getString(R.string.habit_name_hint)
                    return@setOnClickListener
                }
                dialogBinding.inputLayoutName.error = null

                val description = dialogBinding.inputDescription.text?.toString().orEmpty().trim()
                val category = dialogBinding.inputCategory.text?.toString().orEmpty()
                val colorRes = colorOptions[dialogBinding.colorGroup.checkedChipId] ?: colorOptions.values.first()
                val iconResId = iconOptions[dialogBinding.iconGroup.checkedChipId] ?: R.drawable.ic_run
                val frequency = if (dialogBinding.frequencySwitch.isChecked) HabitFrequency.WEEKLY else HabitFrequency.DAILY
                val targetDays = dialogBinding.weeklyDaysGroup.checkedChipIds.mapNotNull { id ->
                    dialogBinding.weeklyDaysGroup.findViewById<Chip>(id)?.tag as? Int
                }

                if (frequency == HabitFrequency.WEEKLY && targetDays.isEmpty()) {
                    Snackbar.make(binding.root, R.string.habit_select_days_warning, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (existingHabit == null) {
                    viewModel.addHabit(name, description, category, colorRes, iconResId, frequency, targetDays)
                } else {
                    val updated = existingHabit.copy(
                        name = name,
                        description = description,
                        category = category,
                        color = colorRes,
                        iconResId = iconResId,
                        frequency = frequency,
                        targetDays = if (frequency == HabitFrequency.WEEKLY) targetDays else emptyList()
                    )
                    viewModel.updateHabit(updated)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun populateWeekdayChips(dialogBinding: DialogAddHabitBinding) {
        val labels = resources.getStringArray(R.array.weekdays_short)
        dialogBinding.weeklyDaysGroup.removeAllViews()
        labels.forEachIndexed { index, label ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.view_day_chip, dialogBinding.weeklyDaysGroup, false) as Chip
            chip.text = label
            chip.tag = index + 1 // 1-7
            dialogBinding.weeklyDaysGroup.addView(chip)
        }
    }

    private fun confirmDeleteHabit(habit: Habit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_habit_confirm)
            .setMessage(R.string.delete_habit_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                viewModel.deleteHabit(habit.id)
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


