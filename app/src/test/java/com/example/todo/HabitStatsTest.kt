package com.example.todo

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitStatsTest {

    private fun date(offsetDays: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat(HabitStats.DATE_PATTERN, Locale.US).format(calendar.time)
    }

    @Test
    fun `empty dates have no streak`() {
        assertEquals(0, HabitStats.currentStreak(emptySet()))
        assertEquals(0, HabitStats.bestStreak(emptySet()))
    }

    @Test
    fun `current streak counts consecutive days ending today`() {
        val dates = setOf(date(0), date(-1), date(-2))
        assertEquals(3, HabitStats.currentStreak(dates))
    }

    @Test
    fun `current streak measured from yesterday when today not done`() {
        val dates = setOf(date(-1), date(-2))
        assertEquals(2, HabitStats.currentStreak(dates))
    }

    @Test
    fun `gap breaks the current streak`() {
        val dates = setOf(date(0), date(-2))
        assertEquals(1, HabitStats.currentStreak(dates))
    }

    @Test
    fun `current streak is zero when only old days completed`() {
        val dates = setOf(date(-3), date(-4))
        assertEquals(0, HabitStats.currentStreak(dates))
    }

    @Test
    fun `best streak finds the longest run`() {
        val dates = setOf(date(0), date(-1), date(-5), date(-6), date(-7))
        assertEquals(3, HabitStats.bestStreak(dates))
    }

    @Test
    fun `best streak of a single day is one`() {
        assertEquals(1, HabitStats.bestStreak(setOf(date(0))))
    }

    @Test
    fun `non-consecutive days do not extend best streak`() {
        val dates = setOf(date(0), date(-2), date(-4))
        assertEquals(1, HabitStats.bestStreak(dates))
    }
}
