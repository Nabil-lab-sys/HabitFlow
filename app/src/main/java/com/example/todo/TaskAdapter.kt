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

class TaskAdapter(
    context: Context,
    private val tasks: MutableList<Task>,
    private val onChanged: () -> Unit
) : ArrayAdapter<Task>(context, 0, tasks) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task, parent, false)

        val task = getItem(position)!!

        val checkbox = view.findViewById<CheckBox>(R.id.taskCheckbox)
        val taskText = view.findViewById<TextView>(R.id.taskText)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

        taskText.text = task.text

        checkbox.setOnCheckedChangeListener(null)
        checkbox.isChecked = task.isCompleted
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            task.isCompleted = isChecked
            updateTextStyle(taskText, isChecked)
            onChanged()
        }

        updateTextStyle(taskText, task.isCompleted)

        deleteButton.setOnClickListener {
            val index = tasks.indexOf(task)
            if (index != -1) {
                tasks.removeAt(index)
                notifyDataSetChanged()
                onChanged()
            }
        }

        return view
    }

    private fun updateTextStyle(textView: TextView, isCompleted: Boolean) {
        if (isCompleted) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            textView.alpha = 0.5f
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            textView.alpha = 1.0f
        }
    }
}
