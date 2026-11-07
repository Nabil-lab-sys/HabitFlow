package com.example.todo

import java.io.Serializable

data class Habit(
    val text: String,
    var completedDates: MutableSet<String> = mutableSetOf()
) : Serializable