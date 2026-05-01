package com.kumud.wellnessflow.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.kumud.wellnessflow.R

object NotificationHelper {

    const val HYDRATION_CHANNEL_ID = "hydration_reminders"
    const val HYDRATION_NOTIFICATION_ID = 1001

    fun ensureHydrationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HYDRATION_CHANNEL_ID,
                context.getString(R.string.hydration_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.hydration_notification_channel_description)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
