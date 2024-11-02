package com.example.yogaadmin

data class Filters(
    val dayOfWeek: List<String> = emptyList(),
    val skillLevel: String,
    val duration: Int,
    val focusArea: List<String> = emptyList(),
    val bodyArea: List<String> = emptyList(),
    val goal: List<String> = emptyList(),
    val capacity: Int,
    val pricePerClass: Double
)
