package com.kumud.wellnessflow.ui.mood

import android.text.InputFilter
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.databinding.DialogAddMoodBinding

data class MoodEntryInput(
    val emoji: String,
    val label: String,
    val note: String,
    val energyLevel: Int
)

private val moodOptions = mapOf(
    R.id.mood_option_great to Pair("\uD83D\uDE04", "Great"),
    R.id.mood_option_good to Pair("\uD83D\uDE0A", "Good"),
    R.id.mood_option_okay to Pair("\uD83D\uDE10", "Okay"),
    R.id.mood_option_low to Pair("\uD83D\uDE14", "Low"),
    R.id.mood_option_bad to Pair("\uD83D\uDE22", "Bad")
)

fun Fragment.showMoodEntryDialog(onSubmit: (MoodEntryInput) -> Unit) {
    val dialogBinding = DialogAddMoodBinding.inflate(layoutInflater)
    dialogBinding.moodNoteInput.filters = arrayOf(InputFilter.LengthFilter(200))
    dialogBinding.moodToggleGroup.check(R.id.mood_option_great)
    dialogBinding.moodToggleGroup.setOnCheckedStateChangeListener { _, checkedIds ->
        if (checkedIds.isNotEmpty()) {
            dialogBinding.moodPrompt.error = null
        }
    }

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.add_mood_entry)
        .setView(dialogBinding.root)
        .setPositiveButton(R.string.action_save, null)
        .setNegativeButton(R.string.action_cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedId = dialogBinding.moodToggleGroup.checkedChipId
            val moodPair = moodOptions[selectedId]
            if (moodPair == null) {
                dialogBinding.moodPrompt.error = getString(R.string.mood_picker_prompt)
                return@setOnClickListener
            }

            val note = dialogBinding.moodNoteInput.text?.toString().orEmpty().trim()
            val energy = dialogBinding.moodEnergySlider.value.toInt()
            onSubmit(MoodEntryInput(moodPair.first, moodPair.second, note, energy))
            dialog.dismiss()
        }
    }

    dialog.show()
}
