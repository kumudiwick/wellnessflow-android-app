package com.kumud.wellnessflow.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kumud.wellnessflow.data.models.HydrationDayLog
import com.kumud.wellnessflow.data.models.HydrationLogEntry
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.utils.DateUtils
import com.kumud.wellnessflow.utils.NotificationHelper

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DONE -> {
                logIntake(context)
                NotificationManagerCompat.from(context).cancel(NotificationHelper.HYDRATION_NOTIFICATION_ID)
            }
            ACTION_SNOOZE -> {
                HydrationReminderScheduler.scheduleSnooze(context, 15)
            }
        }
    }

    private fun logIntake(context: Context, amount: Int = 250) {
        val prefs = PreferencesManager(context)
        val logs = prefs.loadHydrationLogs()
        val todayKey = DateUtils.todayKey()
        val existing = logs[todayKey]
        val newEntry = HydrationLogEntry(System.currentTimeMillis(), amount)
        val entries = (existing?.entries ?: emptyList()) + newEntry
        logs[todayKey] = HydrationDayLog(entries, entries.sumOf { it.amountMl })
        prefs.saveHydrationLogs(logs)
    }

    companion object {
        const val ACTION_DONE = "com.kumud.wellnessflow.ACTION_HYDRATION_DONE"
        const val ACTION_SNOOZE = "com.kumud.wellnessflow.ACTION_HYDRATION_SNOOZE"

        fun doneIntent(context: Context): Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DONE
        }

        fun snoozeIntent(context: Context): Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
        }
    }
}
