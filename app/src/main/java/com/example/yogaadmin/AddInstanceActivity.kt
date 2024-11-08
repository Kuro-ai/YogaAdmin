package com.example.yogaadmin

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddInstanceActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var yogaDao: YogaDao
    private lateinit var classSpinner: Spinner
    private lateinit var dateInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_instance_form)

        dateInput = findViewById(R.id.dateInput)
        dateInput.setOnClickListener {
            showDatePickerDialog()
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_instance_layout)
        val navigationView: NavigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_add_instance)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        yogaDao = YogaDao(this)

        classSpinner = findViewById(R.id.classSpinner)
        setupClassSpinner()

        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "Firebase initialized")

        val addInstanceButton: Button = findViewById(R.id.addInstanceButton)
        addInstanceButton.setOnClickListener {
            handleAddInstance()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun handleAddInstance() {
        val selectedClassPosition = classSpinner.selectedItemPosition
        if (selectedClassPosition == -1) {
            Toast.makeText(this, "Please select a class", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val yogaClasses = mutableListOf<YogaClass>()

                for (classSnapshot in snapshot.children) {
                    val yogaClass = classSnapshot.getValue(YogaClass::class.java)
                    if (yogaClass != null) {
                        yogaClasses.add(yogaClass)
                    }
                }

                if (selectedClassPosition >= yogaClasses.size) {
                    Toast.makeText(this@AddInstanceActivity, "Invalid class selection", Toast.LENGTH_SHORT).show()
                    return
                }

                val yogaClass = yogaClasses[selectedClassPosition]

                val date = dateInput.text.toString()
                val teacher = findViewById<EditText>(R.id.teacherInput).text.toString()

                if (date.isBlank()) {
                    Toast.makeText(this@AddInstanceActivity, "Please select a valid date", Toast.LENGTH_SHORT).show()
                    return
                }

                if (teacher.isBlank()) {
                    Toast.makeText(this@AddInstanceActivity, "Please enter a teacher's name", Toast.LENGTH_SHORT).show()
                    return
                }

                val yogaInstance = YogaInstance(
                    id = 0,
                    classId = yogaClass.id,
                    date = date,
                    teacher = teacher,
                    comments = findViewById<EditText>(R.id.commentsInput).text.toString()
                )

                val result = yogaDao.insertInstance(yogaInstance)
                if (result != -1L) {
                    addInstanceToFirebase(yogaInstance, result)
                } else {
                    Toast.makeText(this@AddInstanceActivity, "Failed to add instance", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddInstanceActivity, "Failed to load class data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addInstanceToFirebase(yogaInstance: YogaInstance, sqliteId: Long) {
        val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("YogaInstances")

        val instanceId = sqliteId.toString()

        val instanceData = mapOf(
            "id" to sqliteId,
            "classId" to yogaInstance.classId,
            "date" to yogaInstance.date,
            "teacher" to yogaInstance.teacher,
            "comments" to yogaInstance.comments
        )

        database.child(instanceId).setValue(instanceData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Instance added successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to add instance to Firebase", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setupClassSpinner() {
        val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")

        val classDescriptions = mutableListOf<String>()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                classDescriptions.clear()


                for (classSnapshot in snapshot.children) {
                    val yogaClass = classSnapshot.getValue(YogaClass::class.java)
                    if (yogaClass != null) {
                        val description = "${yogaClass.dayOfWeek} at ${yogaClass.timeOfCourse}, ${yogaClass.typeOfClass} (${yogaClass.skillLevel})"
                        classDescriptions.add(description)
                    }
                }

                val adapter = ArrayAdapter(this@AddInstanceActivity, android.R.layout.simple_spinner_item, classDescriptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                classSpinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddInstanceActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                item.isChecked = true
                startActivity(Intent(this, MainActivity::class.java))
            }
            R.id.nav_add_class -> {
                item.isChecked = true
                startActivity(Intent(this, AddClassActivity::class.java))
            }
            R.id.nav_add_instance -> {
                item.isChecked = true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInput.setText(dateFormat.format(selectedDate.time))

            checkDateWithDayOfWeek(selectedDate)

        }, year, month, day)

        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        datePickerDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun checkDateWithDayOfWeek(selectedDate: Calendar) {
        val selectedClassPosition = classSpinner.selectedItemPosition

        val yogaClasses = yogaDao.getAllClasses()
        if (selectedClassPosition == -1 || yogaClasses.isEmpty() || selectedClassPosition >= yogaClasses.size) {
            Toast.makeText(this, "No class selected or classes list is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val yogaClass = yogaClasses[selectedClassPosition]
        val classDayOfWeek = yogaClass.dayOfWeek

        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val selectedDayOfWeek = dayOfWeekFormat.format(selectedDate.time)

        if (!selectedDayOfWeek.equals(classDayOfWeek, ignoreCase = true)) {
            dateInput.setText("")
            Toast.makeText(this, "Selected date does not match class day of the week", Toast.LENGTH_LONG).show()
            return
        }

        val existingInstances = yogaDao.getInstancesByClassId(yogaClass.id.toLong())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateString = dateFormat.format(selectedDate.time)

        if (existingInstances.any { it.date == selectedDateString }) {
            dateInput.setText("")
            Toast.makeText(this, "The selected date is already booked for this class", Toast.LENGTH_LONG).show()
        }
    }

}
