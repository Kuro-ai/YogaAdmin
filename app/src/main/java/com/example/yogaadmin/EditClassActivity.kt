package com.example.yogaadmin

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
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
import com.example.yogaadmin.databinding.ActivityEditClassBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class EditClassActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityEditClassBinding
    private lateinit var yogaDao: YogaDao
    private var classId: Long = -1L
    private var originalDayOfWeek: String? = null
    private var isDayOfWeekChanged: Boolean = false
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var selectedImageView: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseDatabase: DatabaseReference

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditClassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        yogaDao = YogaDao(this)


        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_class_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)


        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.drawer_open, R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        selectedImageView = findViewById(R.id.imageView)

        selectedImageView.setOnClickListener {
            showImageSourceDialog()
        }

        firebaseDatabase = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")

        setupSpinners()

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

        classId = intent.getLongExtra("CLASS_ID", -1L)

        if (classId != -1L) {
            loadClassDetails()
        } else {
            Toast.makeText(this, "Error loading class details", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.timeOfCourseInput.setOnClickListener {
            showTimePickerDialog()
        }

        binding.saveButton.setOnClickListener {
            updateClass()
        }

        binding.dayOfWeekSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDay = binding.dayOfWeekSpinner.selectedItem.toString()
                isDayOfWeekChanged = (originalDayOfWeek != null && selectedDay != originalDayOfWeek)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
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

    private fun loadClassDetails() {
        val yogaClassFromSql = yogaDao.getClassById(classId)

        if (yogaClassFromSql != null) {
            populateUiWithYogaClass(yogaClassFromSql)
        } else {
            firebaseDatabase.child(classId.toString()).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val dataSnapshot = task.result
                    if (dataSnapshot.exists()) {
                        val yogaClassFromFirebase = dataSnapshot.getValue(YogaClass::class.java)
                        yogaClassFromFirebase?.let {
                            populateUiWithYogaClass(it)
                        } ?: run {
                            Toast.makeText(this, "Class not found in Firebase", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Class not found in Firebase", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Failed to load class details from Firebase", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun populateUiWithYogaClass(yogaClass: YogaClass) {
        originalDayOfWeek = yogaClass.dayOfWeek

        binding.dayOfWeekSpinner.setSelection(getSpinnerPosition(yogaClass.dayOfWeek, binding.dayOfWeekSpinner))
        binding.timeOfCourseInput.text = yogaClass.timeOfCourse
        binding.capacityInput.setText(yogaClass.capacity.toString())
        binding.durationInput.setText(yogaClass.duration.toString())
        binding.pricePerClassInput.setText(yogaClass.pricePerClass.toString())
        binding.skillLevelSpinner.setSelection(getSpinnerPosition(yogaClass.skillLevel, binding.skillLevelSpinner))
        binding.typeOfClassSpinner.setSelection(getSpinnerPosition(yogaClass.typeOfClass, binding.typeOfClassSpinner))
        binding.focusAreaSpinner.setSelection(getSpinnerPosition(yogaClass.focusArea, binding.focusAreaSpinner))
        binding.bodyAreaSpinner.setSelection(getSpinnerPosition(yogaClass.bodyArea, binding.bodyAreaSpinner))
        binding.descriptionInput.setText(yogaClass.description)

        yogaClass.imageUri?.let { base64String ->
            loadImageFromBase64(base64String, selectedImageView)
        }
    }



    private fun loadImageFromBase64(base64String: String, imageView: ImageView) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateClass() {
        try {
            val imageBase64 = selectedImageUri?.let { encodeImageToBase64(it) }

            val updatedClass = YogaClass(
                id = classId.toInt(),
                dayOfWeek = binding.dayOfWeekSpinner.selectedItem.toString(),
                timeOfCourse = binding.timeOfCourseInput.text.toString(),
                capacity = binding.capacityInput.text.toString().toInt(),
                duration = binding.durationInput.text.toString().toInt(),
                pricePerClass = binding.pricePerClassInput.text.toString().toDouble(),
                skillLevel = binding.skillLevelSpinner.selectedItem.toString(),
                typeOfClass = binding.typeOfClassSpinner.selectedItem.toString(),
                focusArea = binding.focusAreaSpinner.selectedItem.toString(),
                bodyArea = binding.bodyAreaSpinner.selectedItem.toString(),
                description = binding.descriptionInput.text.toString(),
                imageUri = imageBase64
            )

            // Update in Firebase first
            firebaseDatabase.child(classId.toString()).setValue(updatedClass)
                .addOnSuccessListener {
                    Toast.makeText(this, "Class updated successfully in Firebase", Toast.LENGTH_SHORT).show()
                    if (isDayOfWeekChanged) {
                        Toast.makeText(this, "Warning: You have changed the day of the week!", Toast.LENGTH_SHORT).show()
                    }

                    // Redirect to MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update class in Firebase", Toast.LENGTH_SHORT).show()
                }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter valid data for all fields", Toast.LENGTH_SHORT).show()
        }
    }



    private fun getSpinnerPosition(value: String, spinner: Spinner): Int {
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(value)
        if (position == -1) {
            Log.e("EditClassActivity", "Value '$value' not found in spinner")
        }
        return position
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

    @SuppressLint("DefaultLocale")
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hourOfDay, minute ->
            binding.timeOfCourseInput.text = String.format("%02d:%02d", hourOfDay, minute)
        }, currentHour, currentMinute, true).show()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery()
        } else {
            Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show()
        }
    }
}