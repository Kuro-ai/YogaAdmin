package com.example.yogaadmin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "yoga.db"
        private const val DATABASE_VERSION = 12

        const val TABLE_CLASS = "class_table"
        const val TABLE_INSTANCE = "instance_table"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createClassTable = """
        CREATE TABLE $TABLE_CLASS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            day_of_week TEXT NOT NULL,
            time_of_course TEXT NOT NULL,
            capacity INTEGER NOT NULL,
            duration INTEGER NOT NULL,
            price_per_class REAL NOT NULL,
            type_of_class TEXT NOT NULL,
            skill_level TEXT NOT NULL,
            focus_area TEXT NOT NULL,
            body_area TEXT NOT NULL,
            description TEXT,
            imageUri TEXT
        )
    """


        db?.execSQL(createClassTable)
        val createInstanceTable = """
            CREATE TABLE $TABLE_INSTANCE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                teacher TEXT NOT NULL,
                comments TEXT NOT NULL,
                FOREIGN KEY(class_id) REFERENCES $TABLE_CLASS(id)
            )
        """
        db?.execSQL(createInstanceTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CLASS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_INSTANCE")
        onCreate(db)
    }
}
