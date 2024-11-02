package com.example.yogaadmin

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddClassActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var yogaDao: YogaDao
    private lateinit var timeOfCourseInput: TextView
    private lateinit var selectedImageView: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var firebaseReference: DatabaseReference
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_class_form)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_class_layout)
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_add_class)

        FirebaseApp.initializeApp(this)
        Log.d("Firebase", "Firebase initialized")

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        yogaDao = YogaDao(this)

        val addClassButton: Button = findViewById(R.id.addClassButton)
        timeOfCourseInput = findViewById(R.id.timeOfCourseInput)
        selectedImageView = findViewById(R.id.imageView)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri?.let { uri ->
                    openImage(uri)
                } ?: run {
                    Toast.makeText(this, "Image capture failed", Toast.LENGTH_LONG).show()
                }
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedImageUri = it

                    val takeFlags: Int = result.data?.flags?.and(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    ) ?: 0
                    contentResolver.takePersistableUriPermission(it, takeFlags)

                    openImage(it)
                } ?: run {
                    Toast.makeText(this, "Image selection failed", Toast.LENGTH_LONG).show()
                }
            }
        }

        timeOfCourseInput.setOnClickListener {
            showTimePickerDialog()
        }

        selectedImageView.setOnClickListener {
            showImageSourceDialog()
        }

        setupSpinners()

        addClassButton.setOnClickListener {
            Log.d("AddClassButton", "Button clicked")
            addYogaClass()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openCamera()
                1 -> pickImageFromGallery()
            }
        }
        builder.show()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.yogaadmin.fileprovider",
                    it
                )
                selectedImageUri = photoURI
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(cameraIntent)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            selectedImageUri = Uri.fromFile(this)
        }
    }

    private fun pickImageFromGallery() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        pickImageLauncher.launch(intent)
    }


    private fun openImage(uri: Uri) {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            selectedImageView.setImageBitmap(bitmap)
            inputStream?.close()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                timeOfCourseInput.text = formatTime(selectedHour, selectedMinute)
            },
            hour,
            minute,
            false
        )

        timePickerDialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val hourIn12Format = if (hour % 12 == 0) 12 else hour % 12
        return String.format(
            Locale.getDefault(),
            "%02d:%02d %s",
            hourIn12Format,
            minute,
            if (hour < 12) "AM" else "PM"
        )
    }

    private fun setupSpinners() {
        val daysOfWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val typesOfClass = arrayOf("Hatha Yoga", "Vinyasa Yoga", "Ashtanga Yoga", "Iyengar Yoga", "Bikram Yoga", "Kundalini Yoga",
            "Restorative Yoga", "Yin Yoga", "Power Yoga", "Anusara Yoga", "Jivamukti Yoga", "Sivananda Yoga", "Aerial Yoga", "AcroYoga", "Chair Yoga",
            "Yoga Nidra", "Family Yoga", "Flow Yoga")
        val skillLevels = arrayOf("Beginner", "Intermediate", "Advanced", "All Levels")
        val focusAreas = arrayOf("Flexibility", "Strength", "Balance", "Muscles", "Relaxation")
        val bodyAreas = arrayOf("Full Body", "Upper Body", "Lower Body", "Back", "Abs")

        val dayOfWeekSpinner: Spinner = findViewById(R.id.dayOfWeekSpinner)
        val typeOfClassSpinner: Spinner = findViewById(R.id.typeOfClassSpinner)
        val skillLevelSpinner: Spinner = findViewById(R.id.skillLevelSpinner)
        val focusAreaSpinner: Spinner = findViewById(R.id.focusAreaSpinner)
        val bodyAreaSpinner: Spinner = findViewById(R.id.bodyAreaSpinner)

        dayOfWeekSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        typeOfClassSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typesOfClass).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        skillLevelSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, skillLevels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        focusAreaSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, focusAreas).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        bodyAreaSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bodyAreas).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun addYogaClass() {
        Log.d("AddYogaClass", "addYogaClass() called")

        val timeSelected = timeOfCourseInput.text.toString().isNotEmpty()
        if (!timeSelected) {
            Toast.makeText(this, "Please select a time for the class.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image for the class.", Toast.LENGTH_SHORT).show()
            return
        }

        val capacityInput = findViewById<EditText>(R.id.capacityInput).text.toString().toIntOrNull()
        val durationInput = findViewById<EditText>(R.id.durationInput).text.toString().toIntOrNull()
        val priceInput = findViewById<EditText>(R.id.priceInput).text.toString().toDoubleOrNull()

        if (capacityInput == null || durationInput == null || priceInput == null) {
            Toast.makeText(this, "Please enter valid values for capacity, duration, and price.", Toast.LENGTH_SHORT).show()
            return
        }

        val yogaClass = YogaClass(
            dayOfWeek = findViewById<Spinner>(R.id.dayOfWeekSpinner).selectedItem.toString(),
            timeOfCourse = timeOfCourseInput.text.toString(),
            capacity = capacityInput,
            duration = durationInput,
            pricePerClass = priceInput,
            typeOfClass = findViewById<Spinner>(R.id.typeOfClassSpinner).selectedItem.toString(),
            skillLevel = findViewById<Spinner>(R.id.skillLevelSpinner).selectedItem.toString(),
            focusArea = findViewById<Spinner>(R.id.focusAreaSpinner).selectedItem.toString(),
            bodyArea = findViewById<Spinner>(R.id.bodyAreaSpinner).selectedItem.toString(),
            description = findViewById<EditText>(R.id.descriptionInput).text.toString(),
            imageUri = selectedImageUri.toString()
        )

        val result = yogaDao.insertClass(yogaClass)
        if (result != -1L) {
            val sqliteId = result
            Log.d("SQLite", "Class added successfully to SQLite with ID: $sqliteId")
            yogaClass.id = sqliteId.toInt()

            firebaseReference = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("yogaclasses")
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                Log.e("Firebase", "Timeout while trying to add class to Firebase")
                Toast.makeText(this, "Timeout while trying to add class to Firebase.", Toast.LENGTH_SHORT).show()
            }

            handler.postDelayed(timeoutRunnable, 10000)

            firebaseReference.child(sqliteId.toString()).setValue(yogaClass)
                .addOnSuccessListener {
                    handler.removeCallbacks(timeoutRunnable)
                    Toast.makeText(this, "Class added successfully to SQLite and Firebase!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { exception ->
                    handler.removeCallbacks(timeoutRunnable)
                    Log.e("Firebase", "Failed to add class to Firebase", exception)
                    Toast.makeText(this, "Failed to add class to Firebase: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Failed to add class to SQLite", Toast.LENGTH_SHORT).show()
        }
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
                startActivity(Intent(this, AddInstanceActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
