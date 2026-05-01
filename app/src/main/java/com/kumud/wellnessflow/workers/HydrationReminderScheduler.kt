package com.kumud.wellnessflow.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kumud.wellnessflow.data.models.HydrationSettings
import java.util.concurrent.TimeUnit

object HydrationReminderScheduler {

    private const val UNIQUE_WORK_NAME = "hydration_reminder_work"
    private const val UNIQUE_SNOOZE_NAME = "hydration_snooze_work"

    fun schedulePeriodic(context: Context, settings: HydrationSettings) {
        val intervalMinutes = settings.intervalMinutes.coerceAtLeast(15)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<HydrationWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleSnooze(context: Context, minutes: Long) {
        val request = OneTimeWorkRequestBuilder<HydrationWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .addTag(UNIQUE_SNOOZE_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SNOOZE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SNOOZE_NAME)
    }
}
