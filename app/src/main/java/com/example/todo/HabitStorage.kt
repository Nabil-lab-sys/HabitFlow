package com.example.todo

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class HabitStorage(context: Context) {
    private val prefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)

    fun load(): MutableList<Habit> {
        val json = prefs.getString("habits", null) ?: return mutableListOf()
        return try {
            val list = mutableListOf<Habit>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val habit = Habit(text = obj.optString("text"))
                if (obj.has("completedDates")) {
                    val datesArray = obj.optJSONArray("completedDates")
                    if (datesArray != null) {
                        for (j in 0 until datesArray.length()) {
                            habit.completedDates.add(datesArray.getString(j))
                        }
                    }
                }
                list.add(habit)
            }
            list
        } catch (e: JSONException) {
            mutableListOf()
        }
    }

    fun save(habits: List<Habit>) {
        val array = JSONArray()
        for (habit in habits) {
            val obj = JSONObject()
            obj.put("text", habit.text)
            val datesArray = JSONArray()
            for (date in habit.completedDates) {
                datesArray.put(date)
            }
            obj.put("completedDates", datesArray)
            array.put(obj)
        }
        prefs.edit().putString("habits", array.toString()).apply()
    }
}
