package com.kumud.wellnessflow.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kumud.wellnessflow.data.repository.PreferencesManager

class TimeZoneChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val settings = PreferencesManager(context).getHydrationSettings()
            if (settings.enabled) {
                HydrationReminderScheduler.schedulePeriodic(context, settings)
            }
        }
    }
}
