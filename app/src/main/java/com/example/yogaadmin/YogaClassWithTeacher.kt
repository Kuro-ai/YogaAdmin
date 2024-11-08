package com.example.yogaadmin

data class YogaClassWithTeacher(
    val yogaClass: YogaClass = YogaClass(),
    var teacher: String = "",
    var date: String = ""
)

