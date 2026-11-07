package com.example.todo

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class TaskStorage(context: Context) {

    private val prefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)

    fun load(): MutableList<Task> {
        val json = prefs.getString("tasks", null) ?: return mutableListOf()
        return try {
            val list = mutableListOf<Task>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Task(
                        text = obj.optString("text"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (e: JSONException) {
            mutableListOf()
        }
    }

    fun save(tasks: List<Task>) {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("text", task.text)
            obj.put("isCompleted", task.isCompleted)
            array.put(obj)
        }
        prefs.edit().putString("tasks", array.toString()).apply()
    }
}
