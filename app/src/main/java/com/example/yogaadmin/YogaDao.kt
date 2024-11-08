package com.example.yogaadmin

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class YogaDao(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun insertClass(yogaClass: YogaClass): Long {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues().apply {
            put("day_of_week", yogaClass.dayOfWeek)
            put("time_of_course", yogaClass.timeOfCourse)
            put("capacity", yogaClass.capacity)
            put("duration", yogaClass.duration)
            put("price_per_class", yogaClass.pricePerClass)
            put("skill_level", yogaClass.skillLevel)
            put("type_of_class", yogaClass.typeOfClass)
            put("focus_area", yogaClass.focusArea)
            put("body_area", yogaClass.bodyArea)
            put("description", yogaClass.description)
            put("imageUri", yogaClass.imageUri)
        }

        return db.insert(DatabaseHelper.TABLE_CLASS, null, contentValues)
    }

    fun insertInstance(yogaInstance: YogaInstance): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("class_id", yogaInstance.classId)
            put("date", yogaInstance.date)
            put("teacher", yogaInstance.teacher)
            put("comments", yogaInstance.comments)
        }
        return db.insert(DatabaseHelper.TABLE_INSTANCE, null, values)
    }

    fun updateClass(yogaClass: YogaClass): Int {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues().apply {
            put("day_of_week", yogaClass.dayOfWeek)
            put("time_of_course", yogaClass.timeOfCourse)
            put("capacity", yogaClass.capacity)
            put("duration", yogaClass.duration)
            put("price_per_class", yogaClass.pricePerClass)
            put("skill_level", yogaClass.skillLevel)
            put("type_of_class", yogaClass.typeOfClass)
            put("focus_area", yogaClass.focusArea)
            put("body_area", yogaClass.bodyArea)
            put("description", yogaClass.description)
            put("imageUri", yogaClass.imageUri)
        }

        return db.update(
            DatabaseHelper.TABLE_CLASS,
            contentValues,
            "id = ?",
            arrayOf(yogaClass.id.toString())
        )
    }

    fun updateInstance(yogaInstance: YogaInstance): Int {
        val db = dbHelper.writableDatabase
        val contentValues = ContentValues().apply {
            put("class_id", yogaInstance.classId)
            put("date", yogaInstance.date)
            put("teacher", yogaInstance.teacher)
            put("comments", yogaInstance.comments)
        }

        return db.update(
            DatabaseHelper.TABLE_INSTANCE,
            contentValues,
            "id = ?",
            arrayOf(yogaInstance.id.toString())
        )
    }

    fun deleteClassWithInstances(classId: Long) {
        deleteInstancesByClassId(classId)
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_CLASS, "id = ?", arrayOf(classId.toString()))
        db.close()
    }

    fun deleteInstancesByClassId(classId: Long) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_INSTANCE, "class_id = ?", arrayOf(classId.toString()))
        db.close()
    }

    fun deleteInstanceById(instanceId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_INSTANCE, "id = ?", arrayOf(instanceId.toString()))
    }

    fun getAllClasses(): List<YogaClass> {
        val classList = mutableListOf<YogaClass>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_CLASS,
            null,
            null,
            null,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val yogaClass = YogaClass(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("day_of_week")),
                    timeOfCourse = cursor.getString(cursor.getColumnIndexOrThrow("time_of_course")),
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                    pricePerClass = cursor.getDouble(cursor.getColumnIndexOrThrow("price_per_class")),
                    typeOfClass = cursor.getString(cursor.getColumnIndexOrThrow("type_of_class")),
                    skillLevel = cursor.getString(cursor.getColumnIndexOrThrow("skill_level")),
                    focusArea = cursor.getString(cursor.getColumnIndexOrThrow("focus_area")),
                    bodyArea = cursor.getString(cursor.getColumnIndexOrThrow("body_area")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
                )
                classList.add(yogaClass)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return classList
    }

    fun getClassById(classId: Long): YogaClass? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_CLASS,
            null,
            "id = ?",
            arrayOf(classId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            YogaClass(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("day_of_week")),
                timeOfCourse = cursor.getString(cursor.getColumnIndexOrThrow("time_of_course")),
                capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                pricePerClass = cursor.getDouble(cursor.getColumnIndexOrThrow("price_per_class")),
                typeOfClass = cursor.getString(cursor.getColumnIndexOrThrow("type_of_class")),
                skillLevel = cursor.getString(cursor.getColumnIndexOrThrow("skill_level")),
                focusArea = cursor.getString(cursor.getColumnIndexOrThrow("focus_area")),
                bodyArea = cursor.getString(cursor.getColumnIndexOrThrow("body_area")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun getInstancesByClassId(classId: Long): List<YogaInstance> {
        val instances = mutableListOf<YogaInstance>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_INSTANCE,
            null,
            "class_id = ?",
            arrayOf(classId.toString()),
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val yogaInstance = YogaInstance(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    classId = cursor.getInt(cursor.getColumnIndexOrThrow("class_id")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                    comments = cursor.getString(cursor.getColumnIndexOrThrow("comments"))
                )
                instances.add(yogaInstance)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return instances
    }

    fun getInstanceById(instanceId: Long): YogaInstance? {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_INSTANCE,
            null,
            "id = ?",
            arrayOf(instanceId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            YogaInstance(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                classId = cursor.getInt(cursor.getColumnIndexOrThrow("class_id")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")),
                comments = cursor.getString(cursor.getColumnIndexOrThrow("comments"))
            )
        } else {
            null
        }.also { cursor.close() }
    }

    fun getAllClassesWithTeachers(): List<YogaClassWithTeacher> {
        val classesWithTeachers = mutableListOf<YogaClassWithTeacher>()
        val db = dbHelper.readableDatabase

        val query = """
        SELECT c.id, c.day_of_week, c.time_of_course, c.type_of_class, c.skill_level, c.capacity, c.duration, c.price_per_class,
                c.focus_area, c.body_area, c.description, c.imageUri, i.teacher, i.date
        FROM ${DatabaseHelper.TABLE_CLASS} c
        LEFT JOIN ${DatabaseHelper.TABLE_INSTANCE} i ON c.id = i.class_id
    """
        val cursor: Cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val yogaClass = YogaClass(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("day_of_week")),
                    timeOfCourse = cursor.getString(cursor.getColumnIndexOrThrow("time_of_course")),
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                    pricePerClass = cursor.getDouble(cursor.getColumnIndexOrThrow("price_per_class")),
                    typeOfClass = cursor.getString(cursor.getColumnIndexOrThrow("type_of_class")),
                    skillLevel = cursor.getString(cursor.getColumnIndexOrThrow("skill_level")),
                    focusArea = cursor.getString(cursor.getColumnIndexOrThrow("focus_area")),
                    bodyArea = cursor.getString(cursor.getColumnIndexOrThrow("body_area")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
                )

                val teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")) ?: "N/A"
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date")) ?: "No Schedule"

                classesWithTeachers.add(YogaClassWithTeacher(yogaClass, teacher, date))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return classesWithTeachers
    }

    fun getFilteredClasses(filters: Filters): List<YogaClassWithTeacher> {
        val classesWithTeachers = mutableListOf<YogaClassWithTeacher>()
        val db = dbHelper.readableDatabase

        // Base query
        var query = """
        SELECT c.id, c.day_of_week, c.time_of_course, c.type_of_class, c.skill_level, c.capacity, 
               c.duration, c.price_per_class, c.focus_area, c.body_area, c.description, c.imageUri,
               i.teacher, i.date
        FROM ${DatabaseHelper.TABLE_CLASS} c
        LEFT JOIN ${DatabaseHelper.TABLE_INSTANCE} i ON c.id = i.class_id
        WHERE 1=1
    """

        val args = mutableListOf<String>()

        if (filters.dayOfWeek.isNotEmpty()) {
            query += " AND c.day_of_week IN (${filters.dayOfWeek.joinToString(", ") { "?" }})"
            args.addAll(filters.dayOfWeek)
        }

        if (filters.skillLevel.isNotEmpty()) {
            query += " AND c.skill_level = ?"
            args.add(filters.skillLevel)
        }

        if (filters.focusArea.isNotEmpty()) {
            query += " AND c.focus_area IN (${filters.focusArea.joinToString(", ") { "?" }})"
            args.addAll(filters.focusArea)
        }

        if (filters.bodyArea.isNotEmpty()) {
            query += " AND c.body_area IN (${filters.bodyArea.joinToString(", ") { "?" }})"
            args.addAll(filters.bodyArea)
        }

        filters.capacity.let { capacity ->
            if (capacity > 0) {
                when {
                    capacity < 10 -> {
                        query += " AND c.capacity < ?"
                        args.add("10")
                    }

                    capacity in 10..25 -> {
                        query += " AND c.capacity BETWEEN ? AND ?"
                        args.add("10")
                        args.add("25")
                    }

                    capacity > 25 -> {
                        query += " AND c.capacity > ?"
                        args.add("25")
                    }
                }
            }
        }

        filters.pricePerClass.let { price ->
            if (price >= 0) {
                when {
                    price <= 20.0 -> {
                        query += " AND c.price_per_class <= ?"
                        args.add(price.toString())
                    }
                    price in 21.0..50.0 -> {
                        query += " AND c.price_per_class BETWEEN ? AND ?"
                        args.add("20")
                        args.add("50")
                    }
                    price > 50.0 -> {
                        query += " AND c.price_per_class > ?"
                        args.add("50")
                    }
                }
            }
        }

        val cursor: Cursor = db.rawQuery(query, args.toTypedArray())
        if (cursor.moveToFirst()) {
            do {
                val yogaClass = YogaClass(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow("day_of_week")),
                    timeOfCourse = cursor.getString(cursor.getColumnIndexOrThrow("time_of_course")),
                    capacity = cursor.getInt(cursor.getColumnIndexOrThrow("capacity")),
                    duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration")),
                    pricePerClass = cursor.getDouble(cursor.getColumnIndexOrThrow("price_per_class")),
                    typeOfClass = cursor.getString(cursor.getColumnIndexOrThrow("type_of_class")),
                    skillLevel = cursor.getString(cursor.getColumnIndexOrThrow("skill_level")),
                    focusArea = cursor.getString(cursor.getColumnIndexOrThrow("focus_area")),
                    bodyArea = cursor.getString(cursor.getColumnIndexOrThrow("body_area")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
                )

                val teacher = cursor.getString(cursor.getColumnIndexOrThrow("teacher")) ?: "N/A"
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date")) ?: "No Schedule"

                classesWithTeachers.add(YogaClassWithTeacher(yogaClass, teacher, date))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return classesWithTeachers
    }
}