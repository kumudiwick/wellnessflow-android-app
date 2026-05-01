package com.kumud.wellnessflow.ui.habits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kumud.wellnessflow.data.repository.PreferencesManager

class HabitsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
            return HabitsViewModel(PreferencesManager(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
