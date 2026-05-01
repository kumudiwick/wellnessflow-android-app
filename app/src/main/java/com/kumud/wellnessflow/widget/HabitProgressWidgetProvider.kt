package com.kumud.wellnessflow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kumud.wellnessflow.R
import com.kumud.wellnessflow.data.repository.PreferencesManager
import com.kumud.wellnessflow.ui.main.MainActivity
import com.kumud.wellnessflow.utils.DateUtils

class HabitProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HabitProgressWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            onUpdate(context, manager, ids)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.appwidget_habit_progress)
        val prefs = PreferencesManager(context)
        val habits = prefs.loadHabits()
        val completions = prefs.loadCompletions()
        val todayKey = DateUtils.todayKey()
        val completed = completions[todayKey]?.count { it.value } ?: 0
        val total = habits.size

        if (total > 0) {
            val summary = context.getString(R.string.home_habit_summary, completed, total)
            val percentage = (completed.toFloat() / total.toFloat() * 100).toInt()
            views.setTextViewText(R.id.widget_progress, "$summary ($percentage%)")
        } else {
            views.setTextViewText(R.id.widget_progress, context.getString(R.string.habits_empty_state))
        }

        val formattedDate = DateUtils.formatDateKey(todayKey)
        views.setTextViewText(R.id.widget_date, formattedDate)

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        private const val ACTION_REFRESH = "com.kumud.wellnessflow.widget.ACTION_REFRESH"

        fun requestRefresh(context: Context) {
            val intent = Intent(context, HabitProgressWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}

