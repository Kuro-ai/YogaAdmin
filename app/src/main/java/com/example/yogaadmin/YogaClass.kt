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
    var imageUri: String? = null
){
    fun matchesFilters(filters: Filters): Boolean {

        if (filters.dayOfWeek.isNotEmpty() && !filters.dayOfWeek.contains(dayOfWeek)) {
            return false
        }

        if (filters.skillLevel.isNotEmpty() && filters.skillLevel != skillLevel) {
            return false
        }

        if (filters.focusArea.isNotEmpty() && !filters.focusArea.contains(focusArea)) {
            return false
        }

        if (filters.bodyArea.isNotEmpty() && !filters.bodyArea.contains(bodyArea)) {
            return false
        }

        if (filters.capacity > 0) {
            when {
                filters.capacity < 10 && capacity >= 10 -> return false
                filters.capacity in 10..25 && (capacity < 10 || capacity > 25) -> return false
                filters.capacity > 25 && capacity <= 25 -> return false
            }
        }

        if (filters.pricePerClass >= 0) {
            when {
                filters.pricePerClass <= 20.0 && pricePerClass!! > 20.0 -> return false
                filters.pricePerClass in 21.0..50.0 && (pricePerClass!! < 20 || pricePerClass > 50) -> return false
                filters.pricePerClass > 50.0 && pricePerClass!! <= 50.0 -> return false
            }
        }

        if (filters.duration > 0 && filters.duration != duration) {
            return false
        }

        return true
    }
}

