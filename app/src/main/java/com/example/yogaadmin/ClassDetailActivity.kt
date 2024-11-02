package com.example.yogaadmin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.android.material.button.MaterialButton
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.FirebaseDatabase

class ClassDetailActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var classDayOfWeek: TextView
    private lateinit var classTimeOfCourse: TextView
    private lateinit var classCapacity: TextView
    private lateinit var classDuration: TextView
    private lateinit var classPrice: TextView
    private lateinit var classSkillLevel: TextView
    private lateinit var classTypeOfClass: TextView
    private lateinit var classFocusArea: TextView
    private lateinit var classBodyArea: TextView
    private lateinit var classDescription: TextView
    private lateinit var classInstancesTextView: TextView
    private lateinit var classImage: ImageView
    private lateinit var deleteClassButton: Button
    private lateinit var editClassButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private var classId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)

            classDayOfWeek = findViewById(R.id.class_day_of_week)
            classTimeOfCourse = findViewById(R.id.class_time_of_course)
            classCapacity = findViewById(R.id.class_capacity)
            classDuration = findViewById(R.id.class_duration)
            classPrice = findViewById(R.id.class_price)
            classSkillLevel = findViewById(R.id.class_skill_level)
            classTypeOfClass = findViewById(R.id.class_type_of_class)
            classFocusArea = findViewById(R.id.class_focus_area)
            classBodyArea = findViewById(R.id.class_body_area)
            classDescription = findViewById(R.id.class_description)
            classImage = findViewById(R.id.class_image)
            deleteClassButton = findViewById(R.id.delete_class_button)
            editClassButton = findViewById(R.id.edit_class_button)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        classId = intent.getLongExtra("CLASS_ID", -1L)
        if (classId == -1L) {
            Toast.makeText(this, "Invalid class ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        deleteClassButton.setOnClickListener {
            deleteClass()
        }

        editClassButton.setOnClickListener {
            openEditClassActivity()
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

    override fun onResume() {
        super.onResume()
        refreshClassDetails()
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

    @SuppressLint("SetTextI18n")
    private fun refreshClassDetails() {
        val yogaDao = YogaDao(this)
        val yogaClass = yogaDao.getClassById(classId)
        val yogaInstances = yogaDao.getInstancesByClassId(classId)

        yogaClass?.let {
            displayClassDetails(it)
        } ?: run {
            classInstancesTextView.text = "Class not found."
        }

        displayClassInstances(yogaInstances)
        yogaClass?.imageUri?.let { imageUriString ->
            val imageUri = Uri.parse(imageUriString)
            loadImageFromUri(imageUri, classImage)
        } ?: run {
            classImage.setImageResource(R.drawable.image_placeholder)
        }
    }

//    private fun syncWithFirebase(yogaClass: YogaClass) {
//        val database = FirebaseDatabase.getInstance().getReference("yogaClasses").child(classId.toString())
//        database.get().addOnSuccessListener {
//            if (it.exists()) {
//                val firebaseClass = it.getValue(YogaClass::class.java)
//                firebaseClass?.let { displayClassDetails(it) }
//            }
//        }.addOnFailureListener {
//            Toast.makeText(this, "Failed to sync with Firebase", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun displayClassDetails(yogaClass: YogaClass) {
        classDayOfWeek.text = yogaClass.dayOfWeek
        classTimeOfCourse.text = SpannableString("Date \n ${yogaClass.timeOfCourse}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classCapacity.text = SpannableString("Capacity \n ${yogaClass.capacity}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classDuration.text = SpannableString("Duration \n ${yogaClass.duration} mins").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classPrice.text = SpannableString("Price \n £${yogaClass.pricePerClass ?: "N/A"}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classSkillLevel.text = SpannableString("Skill Level \n ${yogaClass.skillLevel}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classTypeOfClass.text = SpannableString("Type of Class \n ${yogaClass.typeOfClass}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 13, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classFocusArea.text = SpannableString("Focus Area \n ${yogaClass.focusArea}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classBodyArea.text = SpannableString("Body Area \n ${yogaClass.bodyArea}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        classDescription.text = SpannableString("Description \n ${yogaClass.description ?: "No description available."}").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val imageUriString = yogaClass.imageUri
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            loadImageFromUri(imageUri, classImage)
        } else {
            classImage.setImageResource(R.drawable.image_placeholder)
        }
    }

    private fun loadImageFromUri(uri: Uri, imageView: ImageView) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayClassInstances(instances: List<YogaInstance>) {
        val instancesLayout: LinearLayout = findViewById(R.id.class_instances_list)
        instancesLayout.removeAllViews()

        if (instances.isEmpty()) {
            val noInstancesTextView = TextView(this).apply {
                text = "No instances available."
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(70, 80, 150, 80)
                }
            }
            instancesLayout.addView(noInstancesTextView)
            return
        }

        for (instance in instances) {
            val cardView = CardView(this).apply {
            }


            val instanceLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val titleTextView = TextView(this).apply {
                text = "Session ${instances.indexOf(instance) + 1}"
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 20, 0, 0)
                }
            }

            val dateTextView = TextView(this).apply {
                text = "Date: ${instance.date}"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
            }

            val teacherTextView = TextView(this).apply {
                text = "Instructor: ${instance.teacher}"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
            }

            val commentTextView = TextView(this).apply {
                text = "Comment: ${instance.comments ?: "No comments available."}"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10
                }
            }

            val buttonLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val editButton = MaterialButton(this).apply {
                text = "Edit"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(80, 80, 50, 80)
                }
                setOnClickListener {
                    openEditInstanceActivity(instance.id.toLong())
                }
            }

            val deleteButton = MaterialButton(this).apply {
                text = "Delete"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(70, 80, 150, 80)
                }
                setOnClickListener {
                    deleteInstance(instance.id.toLong())
                }
                setBackgroundColor(Color.RED)
                setOnClickListener {
                    deleteInstance(instance.id.toLong())
                }
            }


            buttonLayout.addView(editButton)
            buttonLayout.addView(deleteButton)

            instanceLayout.addView(titleTextView)
            instanceLayout.addView(dateTextView)
            instanceLayout.addView(teacherTextView)
            instanceLayout.addView(commentTextView)
            instanceLayout.addView(buttonLayout)

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    topMargin = 10
                }
                setBackgroundColor(Color.LTGRAY)
            }

            instanceLayout.addView(divider)

            cardView.addView(instanceLayout)

            instancesLayout.addView(cardView)
        }

    }

    private fun deleteInstance(instanceId: Long) {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm Delete")
            setMessage("Are you sure you want to delete this instance?")
            setPositiveButton("Yes") { _, _ ->
                val yogaDao = YogaDao(this@ClassDetailActivity)

                yogaDao.deleteInstanceById(instanceId)

                val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("YogaInstances")
                    .child(instanceId.toString())

                database.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this@ClassDetailActivity, "Instance deleted.", Toast.LENGTH_SHORT).show()
                        refreshClassDetails()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@ClassDetailActivity, "Failed to delete instance from Firebase.", Toast.LENGTH_SHORT).show()
                    }
            }
            setNegativeButton("No", null)
            show()
        }
    }

    private fun deleteClass() {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm Delete")
            setMessage("Are you sure you want to delete this class and all its instances?")
            setPositiveButton("Yes") { _, _ ->
                deleteClassFromFirebase(classId.toString())
            }
            setNegativeButton("No", null)
            show()
        }
    }

    private fun deleteClassFromFirebase(classId: String) {
        val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
        Log.d("DeleteClass", "Deleting class with ID: $classId")

        database.getReference("yogaclasses").child(classId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this@ClassDetailActivity, "Class deleted successfully.", Toast.LENGTH_SHORT).show()
                deleteInstancesFromFirebase(classId)
            }
            .addOnFailureListener {
                Toast.makeText(this@ClassDetailActivity, "Failed to delete class from Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteInstancesFromFirebase(classId: String) {
        val database = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
        Log.d("DeleteClass", "Fetching all instances to test connectivity")

        database.getReference("YogaInstances")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Log.d("DeleteClass", "Instances found. Checking for matching classId")

                    snapshot.children.forEach { instance ->
                        val instanceClassId = instance.child("classId").value.toString()
                        Log.d("DeleteClass", "Instance ID: ${instance.key}, classId: $instanceClassId")

                        if (instanceClassId == classId) {
                            instance.ref.removeValue()
                        }
                    }
                    Toast.makeText(this@ClassDetailActivity, "Instances deleted from Firebase.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("DeleteClass", "No instances found in Firebase.")
                    Toast.makeText(this@ClassDetailActivity, "No instances to delete.", Toast.LENGTH_SHORT).show()
                }

                deleteFromLocalDatabase(classId)
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteClass", "Error fetching instances without filter: ${exception.message}")
                Toast.makeText(this@ClassDetailActivity, "Failed to retrieve instances in Firebase.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun deleteFromLocalDatabase(classId: String) {
        val yogaDao = YogaDao(this@ClassDetailActivity)
        yogaDao.deleteClassWithInstances(classId.toLong())
        finish()
    }

    private fun openEditClassActivity() {
        val intent = Intent(this, EditClassActivity::class.java).apply {
            putExtra("CLASS_ID", classId)
        }
        startActivity(intent)
    }


    private fun openEditInstanceActivity(instanceId: Long) {
        val intent = Intent(this, EditInstanceActivity::class.java)
        intent.putExtra("instanceId", instanceId)
        intent.putExtra("classId", classId)
        startActivity(intent)
    }

}
