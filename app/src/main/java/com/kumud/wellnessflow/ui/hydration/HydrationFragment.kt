package com.kumud.wellnessflow.ui.hydration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.HydrationSettings
import com.kumud.wellnessflow.databinding.DialogHydrationSettingsBinding
import com.kumud.wellnessflow.databinding.FragmentHydrationBinding
import com.kumud.wellnessflow.utils.NotificationHelper
import com.kumud.wellnessflow.workers.HydrationReminderScheduler

/**
 * Drives hydration logging UI, reminder tuning, and WorkManager scheduling.
 */
class HydrationFragment : Fragment() {

    private var _binding: FragmentHydrationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HydrationViewModel by viewModels { HydrationViewModelFactory(requireContext()) }
    private lateinit var historyAdapter: HydrationHistoryAdapter
    private var latestSettings: HydrationSettings? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            latestSettings?.let { HydrationReminderScheduler.schedulePeriodic(requireContext(), it) }
        } else {
            Snackbar.make(binding.root, R.string.permission_notifications_rationale, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHydrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadState()
    }

    private fun setupRecycler() {
        historyAdapter = HydrationHistoryAdapter()
        binding.hydrationHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.log250Button.setOnClickListener { viewModel.logIntake(250) }
        binding.log500Button.setOnClickListener { viewModel.logIntake(500) }
        binding.log750Button.setOnClickListener { viewModel.logIntake(750) }
        binding.logCustomButton.setOnClickListener { showCustomAmountDialog() }
        binding.hydrationSettingsButton.setOnClickListener {
            val settings = latestSettings ?: viewModel.state.value?.settings
            if (settings != null) {
                showSettingsDialog(settings)
            } else {
                viewModel.loadState()
            }
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            latestSettings = state.settings
            binding.hydrationProgress.progress = state.progressPercent
            binding.hydrationProgressText.text = getString(
                R.string.hydration_progress_label,
                state.todayTotal,
                state.dailyGoal
            )
            binding.hydrationEmptyView.isVisible = state.history.isEmpty()
            historyAdapter.submitList(state.history)
        }
    }

    private fun showCustomAmountDialog() {
        val inputLayout = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            hint = getString(R.string.hydration_custom_amount_hint)
        }
        val editText = com.google.android.material.textfield.TextInputEditText(inputLayout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
        }
        inputLayout.addView(editText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.home_hydration_prompt)
            .setView(inputLayout)
            .setPositiveButton(R.string.action_save) { dialog, _ ->
                val amount = editText.text?.toString().orEmpty().toIntOrNull()
                if (amount != null && amount > 0) {
                    viewModel.logIntake(amount)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun handleToolbarMenu(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_hydration_settings -> {
                latestSettings?.let { showSettingsDialog(it) }
                true
            }
            else -> false
        }
    }

    private fun showSettingsDialog(current: HydrationSettings) {
        val dialogBinding = DialogHydrationSettingsBinding.inflate(layoutInflater)
        dialogBinding.reminderToggle.isChecked = current.enabled
        dialogBinding.intervalInput.setText(current.intervalMinutes.toString(), false)
        dialogBinding.dailyGoalInput.setText(current.dailyGoalMl.toString())
        dialogBinding.soundToggle.isChecked = current.notificationSound
        dialogBinding.vibrateToggle.isChecked = current.vibrate

        setupIntervalDropdown(dialogBinding)
        populateDayChips(dialogBinding, current.activeDays)
        updateTimeButton(dialogBinding.startTimeButton, current.startHour, current.startMinute)
        updateTimeButton(dialogBinding.endTimeButton, current.endHour, current.endMinute)

        dialogBinding.startTimeButton.setOnClickListener {
            showTimePicker(current.startHour, current.startMinute) { hour, minute ->
                updateTimeButton(dialogBinding.startTimeButton, hour, minute)
                dialogBinding.startTimeButton.tag = Pair(hour, minute)
            }
        }
        dialogBinding.endTimeButton.setOnClickListener {
            showTimePicker(current.endHour, current.endMinute) { hour, minute ->
                updateTimeButton(dialogBinding.endTimeButton, hour, minute)
                dialogBinding.endTimeButton.tag = Pair(hour, minute)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hydration_settings)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val interval = dialogBinding.intervalInput.text?.toString()?.toIntOrNull() ?: current.intervalMinutes
                val goal = dialogBinding.dailyGoalInput.text?.toString()?.toIntOrNull() ?: current.dailyGoalMl
                val startPair = (dialogBinding.startTimeButton.tag as? Pair<Int, Int>) ?: Pair(current.startHour, current.startMinute)
                val endPair = (dialogBinding.endTimeButton.tag as? Pair<Int, Int>) ?: Pair(current.endHour, current.endMinute)
                val activeDays = dialogBinding.activeDaysGroup.checkedChipIds.mapNotNull { id ->
                    dialogBinding.activeDaysGroup.findViewById<Chip>(id)?.tag as? Int
                }

                if (dialogBinding.reminderToggle.isChecked && activeDays.isEmpty()) {
                    Snackbar.make(binding.root, R.string.hydration_select_days_warning, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val updatedSettings = current.copy(
                    enabled = dialogBinding.reminderToggle.isChecked,
                    intervalMinutes = interval,
                    startHour = startPair.first,
                    startMinute = startPair.second,
                    endHour = endPair.first,
                    endMinute = endPair.second,
                    activeDays = if (activeDays.isEmpty()) current.activeDays else activeDays,
                    dailyGoalMl = goal,
                    notificationSound = dialogBinding.soundToggle.isChecked,
                    vibrate = dialogBinding.vibrateToggle.isChecked
                )

                viewModel.updateSettings(updatedSettings)
                handleReminderScheduling(updatedSettings)
                Snackbar.make(binding.root, R.string.hydration_saved, Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupIntervalDropdown(binding: DialogHydrationSettingsBinding) {
        val options = resources.getStringArray(R.array.hydration_interval_options)
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        binding.intervalInput.setAdapter(adapter)
    }

    private fun populateDayChips(binding: DialogHydrationSettingsBinding, selectedDays: List<Int>) {
        val labels = resources.getStringArray(R.array.weekdays_short)
        binding.activeDaysGroup.removeAllViews()
        labels.forEachIndexed { index, label ->
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.view_day_chip, binding.activeDaysGroup, false) as Chip
            chip.text = label
            chip.tag = index + 1
            chip.isChecked = selectedDays.contains(index + 1)
            binding.activeDaysGroup.addView(chip)
        }
    }

    private fun updateTimeButton(button: com.google.android.material.button.MaterialButton, hour: Int, minute: Int) {
        button.text = String.format("%02d:%02d", hour, minute)
        button.tag = Pair(hour, minute)
    }

    private fun showTimePicker(hour: Int, minute: Int, onTimeSelected: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .build()
        picker.addOnPositiveButtonClickListener { onTimeSelected(picker.hour, picker.minute) }
        picker.show(parentFragmentManager, "hydration_time_picker")
    }

    // Ensure notification channels exist and schedule or cancel WorkManager jobs.
    private fun handleReminderScheduling(settings: HydrationSettings) {
        NotificationHelper.ensureHydrationChannel(requireContext())
        if (settings.enabled) {
            if (requiresNotificationPermission() && !hasNotificationPermission()) {
                latestSettings = settings
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                HydrationReminderScheduler.schedulePeriodic(requireContext(), settings)
            }
        } else {
            HydrationReminderScheduler.cancel(requireContext())
        }
    }

    private fun requiresNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun hasNotificationPermission(): Boolean {
        return if (!requiresNotificationPermission()) true else ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}





