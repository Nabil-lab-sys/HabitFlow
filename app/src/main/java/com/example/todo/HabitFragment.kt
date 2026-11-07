package com.example.todo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitFragment : Fragment() {

    private lateinit var habitInput: EditText
    private lateinit var addHabitButton: Button
    private lateinit var habitList: ListView

    private lateinit var storage: HabitStorage
    private val habits = mutableListOf<Habit>()
    private lateinit var adapter: HabitAdapter
    private var todayDate: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitInput = view.findViewById(R.id.habitInput)
        addHabitButton = view.findViewById(R.id.addHabitButton)
        habitList = view.findViewById(R.id.habitList)

        storage = HabitStorage(requireContext())
        habits.clear()
        habits.addAll(storage.load())

        todayDate = currentDate()
        resetDailyHabits()

        adapter = HabitAdapter(requireContext(), habits, { storage.save(habits) }, todayDate) { habit ->
            val intent = Intent(requireContext(), HabitHistoryActivity::class.java)
            intent.putExtra("habit", habit)
            startActivity(intent)
        }
        habitList.adapter = adapter

        addHabitButton.setOnClickListener { addHabit() }
        habitInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addHabit()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::adapter.isInitialized) return

        val current = currentDate()
        if (current != todayDate) {
            todayDate = current
            resetDailyHabits()
            adapter.todayDate = current
            adapter.notifyDataSetChanged()
        }
    }

    private fun addHabit() {
        val text = habitInput.text.toString().trim()
        if (text.isEmpty()) return

        habits.add(Habit(text))
        adapter.notifyDataSetChanged()
        storage.save(habits)
        habitInput.text.clear()
    }

    private fun currentDate(): String =
        SimpleDateFormat(HabitStats.DATE_PATTERN, Locale.US).format(Date())

    private fun resetDailyHabits() {
        val prefs = requireContext()
            .getSharedPreferences("habit_daily_prefs", Context.MODE_PRIVATE)
        val lastResetDate = prefs.getString("last_reset_date", "")
        if (lastResetDate != todayDate) {
            prefs.edit().putString("last_reset_date", todayDate).apply()
        }
    }
}
