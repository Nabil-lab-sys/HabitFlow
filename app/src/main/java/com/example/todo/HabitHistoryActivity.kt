package com.example.todo

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitHistoryActivity : AppCompatActivity() {

    private lateinit var historyInfoText: TextView
    private lateinit var historySummaryText: TextView
    private var habit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_history)

        val root = findViewById<View>(R.id.historyRoot)
        val initialPadding = intArrayOf(
            root.paddingLeft,
            root.paddingTop,
            root.paddingRight,
            root.paddingBottom
        )
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                initialPadding[0] + bars.left,
                initialPadding[1] + bars.top,
                initialPadding[2] + bars.right,
                initialPadding[3] + bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }

        val passedHabit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("habit", Habit::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("habit") as? Habit
        }
        if (passedHabit == null) {
            finish()
            return
        }
        habit = passedHabit
        val historyTitle = findViewById<TextView>(R.id.historyTitle)
        historySummaryText = findViewById(R.id.historySummaryText)
        val historyCalendarView = findViewById<CalendarView>(R.id.historyCalendarView)
        historyInfoText = findViewById(R.id.historyInfoText)

        historyTitle.text = getString(R.string.history_title, passedHabit.text)
        showSummary(passedHabit)

        historyCalendarView.date = System.currentTimeMillis()
        showStatusFor(historyCalendarView.date)

        historyCalendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            showStatusFor(calendar.timeInMillis)
        }
    }

    private fun showStatusFor(timeInMillis: Long) {
        val dateStr = formatDate(timeInMillis)
        val completed = habit?.completedDates?.contains(dateStr) == true
        historyInfoText.text = if (completed) {
            getString(R.string.history_day_completed, dateStr)
        } else {
            getString(R.string.history_day_not_completed, dateStr)
        }
    }

    private fun showSummary(habit: Habit) {
        val total = habit.completedDates.size
        val current = HabitStats.currentStreak(habit.completedDates)
        val best = HabitStats.bestStreak(habit.completedDates)
        historySummaryText.text = getString(
            R.string.history_summary,
            resources.getQuantityString(R.plurals.days_completed, total, total),
            resources.getQuantityString(R.plurals.streak_days, current, current),
            resources.getQuantityString(R.plurals.streak_days, best, best)
        )
    }

    private fun formatDate(timeInMillis: Long): String =
        SimpleDateFormat(HabitStats.DATE_PATTERN, Locale.US).format(Date(timeInMillis))
}
