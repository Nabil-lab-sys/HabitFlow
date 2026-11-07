package com.example.todo

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Stores how much time the user actually spent focusing, per day, in
 * SharedPreferences. Break time is never recorded.
 */
class FocusStorage(context: Context) {

    private val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)

    fun addFocusedMillis(millis: Long) {
        if (millis < 1000L) return

        val key = dayKey(Calendar.getInstance())
        val todayTotal = prefs.getLong(key, 0L) + millis
        prefs.edit()
            .putLong(key, todayTotal)
            .putLong(KEY_TOTAL, prefs.getLong(KEY_TOTAL, 0L) + millis)
            .apply()
    }

    fun addCompletedSession() {
        prefs.edit().putInt(KEY_SESSIONS, completedSessions() + 1).apply()
    }

    fun completedSessions(): Int = prefs.getInt(KEY_SESSIONS, 0)

    fun todayMillis(): Long = prefs.getLong(dayKey(Calendar.getInstance()), 0L)

    /** Total for today plus the six previous days. */
    fun weekMillis(): Long {
        val calendar = Calendar.getInstance()
        var total = 0L
        repeat(7) {
            total += prefs.getLong(dayKey(calendar), 0L)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }

    fun totalMillis(): Long = prefs.getLong(KEY_TOTAL, 0L)

    /** Oldest first, only days with recorded focus time. */
    fun recentDays(limit: Int = 7): List<DayTotal> {
        val calendar = Calendar.getInstance()
        val days = mutableListOf<DayTotal>()
        repeat(limit) {
            val date = dayKey(calendar)
            val millis = prefs.getLong(date, 0L)
            if (millis > 0L) days.add(DayTotal(date, millis))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return days.reversed()
    }

    data class DayTotal(val date: String, val millis: Long)

    companion object {
        private const val KEY_TOTAL = "total_millis"
        private const val KEY_SESSIONS = "completed_sessions"
        private const val KEY_DAY_PREFIX = "day_"
        const val DAY_PATTERN = "yyyy-MM-dd"

        private fun dayKey(calendar: Calendar): String =
            KEY_DAY_PREFIX + SimpleDateFormat(DAY_PATTERN, Locale.US).format(calendar.time)

        /** Compact human-readable duration, e.g. "0 min", "35 min", "1h 20m". */
        fun formatDuration(millis: Long): String {
            val minutes = millis / 60_000L
            return when {
                minutes < 1L -> "0 min"
                minutes < 60L -> "$minutes min"
                else -> {
                    val hours = minutes / 60L
                    val remaining = minutes % 60L
                    if (remaining == 0L) "${hours}h" else "${hours}h ${remaining}m"
                }
            }
        }
    }
}
