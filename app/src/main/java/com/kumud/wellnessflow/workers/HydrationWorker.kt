package com.kumud.wellnessflow.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.ui.main.MainActivity
import com.kumud.wellnessflow.utils.DateUtils
import com.kumud.wellnessflow.utils.NotificationHelper

class HydrationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val preferences = PreferencesManager(applicationContext)
        val settings = preferences.getHydrationSettings()
        if (!settings.enabled) return Result.success()

        NotificationHelper.ensureHydrationChannel(applicationContext)

        val todayTotal = preferences.loadHydrationLogs()[DateUtils.todayKey()]?.totalMl ?: 0
        val goal = settings.dailyGoalMl

        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val doneIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            NotificationActionReceiver.doneIntent(applicationContext),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            applicationContext,
            2,
            NotificationActionReceiver.snoozeIntent(applicationContext),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.HYDRATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hydration)
            .setContentTitle(applicationContext.getString(R.string.hydration_notification_title))
            .setContentText(applicationContext.getString(R.string.hydration_notification_body, todayTotal, goal))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_hydration, applicationContext.getString(R.string.action_done), doneIntent)
            .addAction(R.drawable.ic_hydration, applicationContext.getString(R.string.action_snooze), snoozeIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NotificationHelper.HYDRATION_NOTIFICATION_ID, notification)

        return Result.success()
    }
}

