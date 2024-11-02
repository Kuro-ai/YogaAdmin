package com.example.yogaadmin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ListView
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, FilterBottomSheetDialog.FilterListener {

    private lateinit var yogaDao: YogaDao
    private lateinit var classListView: ListView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var allYogaClasses: List<YogaClassWithTeacher>
    private lateinit var filteredYogaClasses: List<YogaClassWithTeacher>
    private lateinit var classActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseReference: DatabaseReference

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

        allYogaClasses = yogaDao.getAllClassesWithTeachers()
        filteredYogaClasses = allYogaClasses
        updateClassListView()

        val filterButton: Button = findViewById(R.id.filter_button)
        filterButton.setOnClickListener {
            openFilterDialog()
        }

        classListView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ClassDetailActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshClassList()
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
        val sqliteClasses = yogaDao.getAllClassesWithTeachers()
        firebaseReference.get().addOnSuccessListener { snapshot ->
            val firebaseClasses = snapshot.children.mapNotNull { it.getValue(YogaClassWithTeacher::class.java) }
            allYogaClasses = sqliteClasses + firebaseClasses
            filteredYogaClasses = allYogaClasses
            updateClassListView()
        }.addOnFailureListener {
            Log.e("MainActivity", "Error fetching data from Firebase", it)
        }
    }

    private fun updateClassListView() {
        Log.d("MainActivity", "Yoga classes count: ${filteredYogaClasses.size}")
        val adapter = YogaClassAdapter(this, filteredYogaClasses)
        classListView.adapter = adapter
    }

    private fun openFilterDialog() {
        val filterBottomSheetDialog = FilterBottomSheetDialog()
        filterBottomSheetDialog.show(supportFragmentManager, filterBottomSheetDialog.tag)
    }

    override fun onFiltersApplied(filters: Filters) {
        filteredYogaClasses = yogaDao.getFilteredClasses(filters)
        updateClassListView()
    }
}
