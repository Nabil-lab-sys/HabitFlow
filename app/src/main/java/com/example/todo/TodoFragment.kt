package com.example.todo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.fragment.app.Fragment

class TodoFragment : Fragment() {

    private lateinit var taskInput: EditText
    private lateinit var addButton: Button
    private lateinit var taskList: ListView

    private lateinit var storage: TaskStorage
    private val tasks = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskInput = view.findViewById(R.id.taskInput)
        addButton = view.findViewById(R.id.addButton)
        taskList = view.findViewById(R.id.taskList)

        storage = TaskStorage(requireContext())
        tasks.clear()
        tasks.addAll(storage.load())

        adapter = TaskAdapter(requireContext(), tasks) { storage.save(tasks) }
        taskList.adapter = adapter

        addButton.setOnClickListener { addTask() }
        taskInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTask()
                true
            } else {
                false
            }
        }
    }

    private fun addTask() {
        val text = taskInput.text.toString().trim()
        if (text.isEmpty()) return

        tasks.add(Task(text))
        adapter.notifyDataSetChanged()
        storage.save(tasks)
        taskInput.text.clear()
    }
}
