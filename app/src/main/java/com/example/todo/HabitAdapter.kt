package com.example.todo

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView

class HabitAdapter(
    context: Context,
    private val habits: MutableList<Habit>,
    private val onChanged: () -> Unit,
    var todayDate: String = "",
    private val onHistoryClick: (Habit) -> Unit = {}
) : ArrayAdapter<Habit>(context, 0, habits) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_habit, parent, false)

        val habit = getItem(position)!!

        val checkbox = view.findViewById<CheckBox>(R.id.habitCheckbox)
        val habitText = view.findViewById<TextView>(R.id.habitText)
        val habitStatsText = view.findViewById<TextView>(R.id.habitStatsText)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteHabitButton)
        val historyButton = view.findViewById<android.widget.Button>(R.id.habitHistoryButton)

        habitText.text = habit.text
        updateStats(habitStatsText, habit)

        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = habit.completedDates.contains(todayDate)
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (todayDate.isNotEmpty()) {
                if (isChecked) {
                    habit.completedDates.add(todayDate)
                } else {
                    habit.completedDates.remove(todayDate)
                }
            }
            updateTextStyle(habitText, isChecked)
            updateStats(habitStatsText, habit)
            onChanged()
        }

        updateTextStyle(habitText, checkbox.isChecked)

        historyButton.setOnClickListener {
            onHistoryClick(habit)
        }

        deleteButton.setOnClickListener {
            val index = habits.indexOf(habit)
            if (index != -1) {
                habits.removeAt(index)
                notifyDataSetChanged()
                onChanged()
            }
        }

        return view
    }

    private fun updateStats(textView: TextView, habit: Habit) {
        val total = habit.completedDates.size
        if (total == 0) {
            textView.text = context.getString(R.string.habit_no_completions)
            return
        }

        val days = context.resources.getQuantityString(R.plurals.days_count, total, total)
        val streak = HabitStats.currentStreak(habit.completedDates)
        textView.text = if (streak == 0) {
            context.getString(R.string.habit_row_stats_no_streak, days)
        } else {
            val streakDays = context.resources.getQuantityString(
                R.plurals.streak_days_compact,
                streak,
                streak
            )
            context.getString(R.string.habit_row_stats, days, streakDays)
        }
    }

    private fun updateTextStyle(textView: TextView, isDone: Boolean) {
        // Dim completed habits without drawing a line through the text.
        textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        textView.alpha = if (isDone) 0.5f else 1.0f
    }
}