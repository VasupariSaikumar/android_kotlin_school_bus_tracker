package com.example.android_kotlin_school_bus_tracker.domain

data class Bus(
    val id: String = "",
    val name: String = "Unnamed Bus",
    val createdAt: Long = System.currentTimeMillis()
)