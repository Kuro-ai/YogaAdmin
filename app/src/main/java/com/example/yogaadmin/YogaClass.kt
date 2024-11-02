package com.example.yogaadmin

data class YogaClass(
    var id: Int = 0,
    val dayOfWeek: String = "",
    val timeOfCourse: String = "",
    val capacity: Int = 0,
    val duration: Int = 0,
    val pricePerClass: Double? = null,
    val skillLevel: String = "",
    val typeOfClass: String = "",
    val focusArea: String = "",
    val bodyArea: String = "",
    val description: String? = null,
    val imageUri: String? = null
)

