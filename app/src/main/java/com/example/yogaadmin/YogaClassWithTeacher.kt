package com.example.yogaadmin

data class YogaClassWithTeacher(
    val yogaClass: YogaClass = YogaClass(),
    val teacher: String = "",
    val date: String = ""
)

