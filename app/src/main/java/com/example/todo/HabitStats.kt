package com.example.todo

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Shared date/streak calculations for habit history. */
object HabitStats {

    const val DATE_PATTERN = "yyyy-MM-dd"

    /**
     * Counts consecutive completed days ending today.
     * If today isn't completed yet, the streak is measured from yesterday so it
     * doesn't drop to zero before the user has had a chance to check in.
     */
    fun currentStreak(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0

        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val cursor = Calendar.getInstance()
        if (!dates.contains(formatter.format(cursor.time))) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        while (dates.contains(formatter.format(cursor.time))) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun bestStreak(dates: Set<String>): Int {
        if (dates.isEmpty()) return 0

        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
        val parsedDates = dates
            .mapNotNull { runCatching { formatter.parse(it) }.getOrNull() }
            .sorted()
        if (parsedDates.isEmpty()) return 0

        var best = 1
        var run = 1
        for (i in 1 until parsedDates.size) {
            run = if (isNextDay(parsedDates[i - 1], parsedDates[i])) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    private fun isNextDay(previous: Date, current: Date): Boolean {
        val nextDay = Calendar.getInstance().apply {
            time = previous
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val currentDay = Calendar.getInstance().apply {
            time = current
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextDay.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR) &&
            nextDay.get(Calendar.DAY_OF_YEAR) == currentDay.get(Calendar.DAY_OF_YEAR)
    }
}
