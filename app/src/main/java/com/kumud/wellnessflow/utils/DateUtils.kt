package com.kumud.wellnessflow.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val storageFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val friendlyDateFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    fun todayKey(): String = storageFormatter.format(Date())

    fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return storageFormatter.format(cal.time)
    }

    fun dateKeyDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return storageFormatter.format(cal.time)
    }


    fun formatDateKey(dateKey: String): String =
        runCatching { friendlyDateFormatter.format(storageFormatter.parse(dateKey)!!) }
            .getOrDefault(dateKey)

    fun formatDayLabel(dateKey: String): String =
        runCatching { dayOfWeekFormatter.format(storageFormatter.parse(dateKey)!!) }
            .getOrDefault(dateKey)

    fun formatTime(timestamp: Long): String = timeFormatter.format(Date(timestamp))

    fun formatTimestamp(timestamp: Long): String = friendlyDateFormatter.format(Date(timestamp))

    fun formatRelativeTimestamp(timestamp: Long): String {
        val dateKey = storageFormatter.format(Date(timestamp))
        val time = formatTime(timestamp)
        return when (dateKey) {
            todayKey() -> "Today · $time"
            yesterdayKey() -> "Yesterday · $time"
            else -> "${formatDateKey(dateKey)} · $time"
        }
    }

    fun isSameDay(timestamp: Long, dateKey: String): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return storageFormatter.format(calendar.time) == dateKey
    }

    fun currentWeekdayIndex(): Int {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    fun startOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
