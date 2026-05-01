package com.kumud.wellnessflow.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.models.ProfileNote
import com.kumud.wellnessflow.databinding.ActivityProfileBinding
import com.kumud.wellnessflow.databinding.DialogProfileTextInputBinding
import com.kumud.wellnessflow.databinding.ItemProfileNoteBinding
import com.kumud.wellnessflow.ui.main.MainActivity
import com.kumud.wellnessflow.utils.DateUtils

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(this) }
    private var latestState: ProfileUiState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNav()
        configureChart()
        setupInteractions()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.profileToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.profileToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.profileToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBottomNav() {
        binding.profileBottomNav.selectedItemId = R.id.menu_home
        binding.profileBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home,
                R.id.menu_habits,
                R.id.menu_mood,
                R.id.menu_todo,
                R.id.menu_hydration -> {
                    navigateToMain(item.itemId)
                    true
                }
                else -> false
            }
        }
        binding.profileBottomNav.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.menu_home,
                R.id.menu_habits,
                R.id.menu_mood,
                R.id.menu_todo,
                R.id.menu_hydration -> navigateToMain(item.itemId)
            }
        }
    }

    private fun setupInteractions() {
        binding.profileBioEditButton.setOnClickListener {
            showBioDialog(latestState?.bio.orEmpty())
        }
        binding.profileAddNoteButton.setOnClickListener {
            showNoteDialog()
        }
    }

    private fun navigateToMain(menuId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DESTINATION, menuId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun configureChart() = with(binding.profileChart) {
        description.isEnabled = false
        legend.isEnabled = false
        axisRight.isEnabled = false
        setTouchEnabled(false)
        setScaleEnabled(false)
        setNoDataText("")
        setViewPortOffsets(40f, 28f, 40f, 32f)

        val gridColor = ContextCompat.getColor(this@ProfileActivity, R.color.divider_light)
        val textColor = ContextCompat.getColor(this@ProfileActivity, R.color.text_secondary)

        axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 10f
            granularity = 1f
            setDrawAxisLine(false)
            setDrawGridLines(true)
            this.gridColor = gridColor
            this.textColor = textColor
            setLabelCount(3, true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = when {
                    value >= 8f -> getString(R.string.profile_mood_axis_high)
                    value >= 4f -> getString(R.string.profile_mood_axis_mid)
                    else -> getString(R.string.profile_mood_axis_low)
                }
            }
        }
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawAxisLine(false)
            setDrawGridLines(false)
            granularity = 1f
            this.textColor = textColor
        }
    }

    private fun observeState() {
        viewModel.state.observe(this) { state ->
            latestState = state
            binding.profileName.text = getString(R.string.profile_welcome, state.displayName)
            binding.profileMemberSince.text = getString(R.string.profile_member_since, state.memberSince)

            val hasBio = state.bio.isNotBlank()
            binding.profileBioText.isVisible = hasBio
            binding.profileBioPlaceholder.isVisible = !hasBio
            if (hasBio) {
                binding.profileBioText.text = state.bio
            }

            renderMoodTrend(state.moodTrend)
            renderNotes(state.notes)

            val habitsSummary = getString(R.string.profile_metric_habits, state.weeklyHabitsCompleted, state.weeklyHabitsTotal)
            binding.profileHabitsValue.text = habitsSummary

            val hydrationSummary = getString(R.string.profile_metric_hydration, state.weeklyHydrationTotal, state.weeklyHydrationGoal)
            binding.profileHydrationValue.text = hydrationSummary

            val moodsSummary = getString(R.string.profile_metric_moods, state.weeklyMoodsLogged)
            binding.profileMoodsValue.text = moodsSummary
        }
    }

    private fun renderMoodTrend(points: List<MoodTrendPoint>) {
        val entries = points.mapIndexed { index, point ->
            val yValue = point.averageEnergy ?: Float.NaN
            Entry(index.toFloat(), yValue)
        }
        val hasData = entries.any { !it.y.isNaN() }
        binding.profileChart.isVisible = hasData
        binding.profileChartEmpty.isVisible = !hasData
        if (!hasData) {
            binding.profileChart.clear()
            return
        }

        val accentColor = ContextCompat.getColor(this, R.color.accent_salmon)
        val dataSet = LineDataSet(entries, getString(R.string.profile_mood_trend_title)).apply {
            color = accentColor
            setDrawCircles(true)
            setCircleColor(accentColor)
            circleRadius = 4f
            circleHoleColor = ContextCompat.getColor(this@ProfileActivity, android.R.color.white)
            lineWidth = 2.5f
            valueTextSize = 0f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.bg_profile_mood_fill)
            highLightColor = ContextCompat.getColor(this@ProfileActivity, android.R.color.transparent)
        }

        binding.profileChart.xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.dayLabel })
        binding.profileChart.data = LineData(dataSet)
        binding.profileChart.animateY(600)
        binding.profileChart.invalidate()
    }

    private fun renderNotes(notes: List<ProfileNote>) {
        binding.profileNotesContainer.removeAllViews()
        binding.profileNotesEmpty.isVisible = notes.isEmpty()
        if (notes.isEmpty()) return

        notes.forEach { note ->
            val itemBinding = ItemProfileNoteBinding.inflate(layoutInflater, binding.profileNotesContainer, false)
            itemBinding.profileNoteContent.text = note.content
            itemBinding.profileNoteTimestamp.text = getString(
                R.string.profile_note_timestamp,
                DateUtils.formatTimestamp(note.createdAt),
                DateUtils.formatTime(note.createdAt)
            )
            itemBinding.profileNoteEditButton.setOnClickListener { showNoteDialog(note) }
            itemBinding.profileNoteDeleteButton.setOnClickListener {
                viewModel.deleteNote(note.id)
                Snackbar.make(binding.root, R.string.profile_note_deleted, Snackbar.LENGTH_SHORT).show()
            }
            binding.profileNotesContainer.addView(itemBinding.root)
        }
    }

    private fun showBioDialog(currentBio: String) {
        val dialogBinding = DialogProfileTextInputBinding.inflate(layoutInflater)
        dialogBinding.dialogInputLayout.hint = getString(R.string.profile_dialog_bio_hint)
        dialogBinding.dialogInput.setText(currentBio)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.profile_dialog_title_bio)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val text = dialogBinding.dialogInput.text?.toString()?.trim() ?: ""
                if (text == currentBio.trim()) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                viewModel.updateBio(text)
                Snackbar.make(binding.root, R.string.profile_bio_saved, Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showNoteDialog(existingNote: ProfileNote? = null) {
        val dialogBinding = DialogProfileTextInputBinding.inflate(layoutInflater)
        dialogBinding.dialogInputLayout.hint = getString(R.string.profile_dialog_note_hint)
        dialogBinding.dialogInput.setText(existingNote?.content.orEmpty())

        val dialogTitle = if (existingNote == null) {
            R.string.profile_dialog_title_note_add
        } else {
            R.string.profile_dialog_title_note_edit
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(dialogTitle)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save, null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val text = dialogBinding.dialogInput.text?.toString()?.trim().orEmpty()
                if (text.isBlank()) {
                    dialogBinding.dialogInputLayout.error = getString(R.string.profile_dialog_note_error)
                    return@setOnClickListener
                }
                if (existingNote != null && text == existingNote.content.trim()) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                dialogBinding.dialogInputLayout.error = null
                viewModel.saveNote(text, existingNote?.id)
                Snackbar.make(binding.root, R.string.profile_note_saved, Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
