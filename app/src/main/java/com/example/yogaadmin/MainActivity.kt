package com.example.yogaadmin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FilterBottomSheetDialog.FilterListener {

    private lateinit var yogaDao: YogaDao
    private lateinit var classListView: ListView
    private lateinit var drawerLayout: DrawerLayout
//    private lateinit var allYogaClasses: List<YogaClassWithTeacher>
//    private lateinit var filteredYogaClasses: List<YogaClassWithTeacher>
    private lateinit var classActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseReference: DatabaseReference
    private lateinit var yogaClassAdapter: YogaClassAdapter
    private var allYogaClasses: List<YogaClassWithTeacher> = emptyList()
    private var filteredYogaClasses: List<YogaClassWithTeacher> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        yogaDao = YogaDao(this)
        classListView = findViewById(R.id.classListView)

        firebaseReference = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")

        classActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshClassList()
            }
            val navigationView: NavigationView = findViewById(R.id.nav_view)
            navigationView.setCheckedItem(R.id.nav_home)
        }

        val searchView: SearchView = findViewById(R.id.search_view)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (classListView.adapter as YogaClassAdapter).filter(newText ?: "")
                return true
            }
        })

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_home)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        yogaClassAdapter = YogaClassAdapter(this, filteredYogaClasses)
        classListView.adapter = yogaClassAdapter

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

//        allYogaClasses = yogaDao.getAllClassesWithTeachers()
//        filteredYogaClasses = allYogaClasses
//        updateClassListView()
        allYogaClasses = emptyList() // or an empty list to start
        filteredYogaClasses = emptyList() // Initialize here to avoid uninitialized access

        updateClassListView()

        val filterButton: Button = findViewById(R.id.filter_button)
        filterButton.setOnClickListener {
            openFilterDialog()
        }

        classListView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ClassDetailActivity::class.java)
            startActivity(intent)
        }
        fetchClassesFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        refreshClassList()
    }

    private fun fetchClassesFromFirebase() {
        val yogaClassesRef: DatabaseReference = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")
        val yogaInstancesRef: DatabaseReference = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("YogaInstances")

        yogaClassesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val yogaClasses = mutableListOf<YogaClassWithTeacher>()
                for (classSnapshot in snapshot.children) {
                    val yogaClass = classSnapshot.getValue(YogaClass::class.java)
                    if (yogaClass != null) {

                        yogaClasses.add(YogaClassWithTeacher(yogaClass, teacher = "N/A", date = "No Schedule"))
                    }
                }

                yogaInstancesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(instanceSnapshot: DataSnapshot) {
                        for (yogaClassWithTeacher in yogaClasses) {
                            val classId = yogaClassWithTeacher.yogaClass.id.toString()

                            val matchingInstance = instanceSnapshot.children.find {
                                val instanceClassId = it.child("classId").getValue(Long::class.java)
                                instanceClassId?.toString() == classId
                            }


                            if (matchingInstance != null) {
                                yogaClassWithTeacher.teacher = matchingInstance.child("teacher").getValue(String::class.java) ?: "N/A"
                                yogaClassWithTeacher.date = matchingInstance.child("date").getValue(String::class.java) ?: "No Schedule"
                            }
                        }
                        allYogaClasses = yogaClasses
                        filteredYogaClasses = yogaClasses
                        updateClassListView()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@MainActivity, "Failed to load YogaInstances data.", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Error fetching YogaInstances data", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load yoga classes.", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching yoga classes", error.toException())
            }
        })
    }

    // Implement the function to filter classes from Firebase
    private fun getFilteredClassesFromFirebase(filters: Filters) {
        val yogaClassesRef: DatabaseReference = FirebaseDatabase.getInstance("https://yogadb-92737-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("yogaclasses")

        yogaClassesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val yogaClasses = mutableListOf<YogaClassWithTeacher>()

                for (classSnapshot in snapshot.children) {
                    val yogaClass = classSnapshot.getValue(YogaClass::class.java)
                    if (yogaClass != null) {
                        // Apply your filtering logic here based on the `filters`
                        if (yogaClass.matchesFilters(filters)) {
                            yogaClasses.add(YogaClassWithTeacher(yogaClass, teacher = "N/A", date = "No Schedule"))
                        }
                    }
                }
                // Assign the filtered data to the class list
                filteredYogaClasses = yogaClasses
                updateClassListView()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load filtered yoga classes.", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching filtered yoga classes", error.toException())
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
                startActivity(Intent(this, AddInstanceActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun refreshClassList() {
        fetchClassesFromFirebase()
    }

    private fun updateClassListView() {
        Log.d("MainActivity", "Yoga classes count: ${filteredYogaClasses.size}")
        yogaClassAdapter.updateData(filteredYogaClasses)
        yogaClassAdapter.notifyDataSetChanged()
    }

    private fun openFilterDialog() {
        val filterBottomSheetDialog = FilterBottomSheetDialog()
        filterBottomSheetDialog.show(supportFragmentManager, filterBottomSheetDialog.tag)
    }

    override fun onFiltersApplied(filters: Filters) {
        getFilteredClassesFromFirebase(filters)
        filteredYogaClasses = yogaDao.getFilteredClasses(filters)
        updateClassListView()
    }
}
