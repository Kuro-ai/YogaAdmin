package com.example.yogaadmin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.yogaadmin.databinding.ActivityEditInstanceBinding
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class EditInstanceActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityEditInstanceBinding
    private lateinit var yogaDao: YogaDao
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private var instanceId: Long = -1L
    private var classId: Long = -1L
    private lateinit var firebaseDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditInstanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        yogaDao = YogaDao(this)
        firebaseDatabase = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("YogaInstances")

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_instance_layout)
        val navView: NavigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        instanceId = intent.getLongExtra("instanceId", -1L)
        classId = intent.getLongExtra("classId", -1L)

        if (instanceId != -1L) {
            loadInstanceDetails()
        } else {
            Toast.makeText(this, "Error loading instance details", Toast.LENGTH_SHORT).show()
            finish()
        }



        binding.dateInput.setOnClickListener {
            showDatePickerDialog()
        }

        binding.saveButton.setOnClickListener {
            updateInstance()
        }
    }

    private fun loadInstanceDetails() {
        val yogaInstance = yogaDao.getInstanceById(instanceId)
        if (yogaInstance != null) {
            binding.dateInput.setText(yogaInstance.date)
            binding.teacherInput.setText(yogaInstance.teacher)
            binding.commentsInput.setText(yogaInstance.comments)
        } else {
            Toast.makeText(this, "No instance details found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateInstance() {
        val updatedInstance = YogaInstance(
            id = instanceId.toInt(),
            classId = classId.toInt(),
            date = binding.dateInput.text.toString(),
            teacher = binding.teacherInput.text.toString(),
            comments = binding.commentsInput.text.toString()
        )

        firebaseDatabase.child(instanceId.toString()).setValue(updatedInstance)
            .addOnSuccessListener {
                val success = yogaDao.updateInstance(updatedInstance)
                if (success > 0) {
                    Toast.makeText(this, "Instance updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update instance in SQLite", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update instance in Firebase", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val selectedDayOfWeek = dayOfWeekFormat.format(selectedDate.time)

            val yogaClass = yogaDao.getClassById(classId)
            val expectedDayOfWeek = yogaClass?.dayOfWeek

            if (!selectedDayOfWeek.equals(expectedDayOfWeek, ignoreCase = true)) {
                binding.dateInput.setText("")
                Toast.makeText(this, "Selected date does not match class day of the week", Toast.LENGTH_LONG).show()
                return@DatePickerDialog
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            binding.dateInput.setText(dateFormat.format(selectedDate.time))

        }, year, month, day)

        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            R.id.nav_add_class -> {
                startActivity(Intent(this, AddClassActivity::class.java))
            }
            R.id.nav_add_instance -> {
                startActivity(Intent(this, AddInstanceActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}

