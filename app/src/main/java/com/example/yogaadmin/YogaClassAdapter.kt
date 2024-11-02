package com.example.yogaadmin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore

class YogaClassAdapter(
    private val context: Context,
    private var yogaClassesWithTeacher: List<YogaClassWithTeacher>
) : BaseAdapter() {

    private var filteredYogaClasses: List<YogaClassWithTeacher> = yogaClassesWithTeacher

    companion object {
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1001
    }

    init {
        fetchClassesFromFirebase()
    }

    override fun getCount(): Int {
        return filteredYogaClasses.size
    }

    override fun getItem(position: Int): YogaClassWithTeacher {
        return filteredYogaClasses[position]
    }

    override fun getItemId(position: Int): Long {
        return filteredYogaClasses[position].yogaClass.id.toLong()
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.yoga_class_item, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val yogaClassWithTeacher = getItem(position)

        if (yogaClassWithTeacher.yogaClass.dayOfWeek.isNotEmpty()) {
            viewHolder.classDayOfWeek.text = yogaClassWithTeacher.yogaClass.dayOfWeek
            viewHolder.classDayOfWeek.visibility = View.VISIBLE
        } else {
            viewHolder.classDayOfWeek.visibility = View.GONE
        }

        if (yogaClassWithTeacher.yogaClass.timeOfCourse.isNotEmpty()) {
            viewHolder.classTimeOfCourse.text = yogaClassWithTeacher.yogaClass.timeOfCourse
            viewHolder.classTimeOfCourse.visibility = View.VISIBLE
        } else {
            viewHolder.classTimeOfCourse.visibility = View.GONE
        }

        if (yogaClassWithTeacher.yogaClass.capacity > 0) {
            viewHolder.classCapacity.text = "Capacity: ${yogaClassWithTeacher.yogaClass.capacity}"
            viewHolder.classCapacity.visibility = View.VISIBLE
        } else {
            viewHolder.classCapacity.visibility = View.GONE
        }

        if (yogaClassWithTeacher.yogaClass.duration > 0) {
            viewHolder.classDuration.text = "Duration: ${yogaClassWithTeacher.yogaClass.duration} mins"
            viewHolder.classDuration.visibility = View.VISIBLE
        } else {
            viewHolder.classDuration.visibility = View.GONE
        }

        if (yogaClassWithTeacher.yogaClass.pricePerClass != null) {
            viewHolder.classPrice.text = "Price: £${yogaClassWithTeacher.yogaClass.pricePerClass}"
            viewHolder.classPrice.visibility = View.VISIBLE
        } else {
            viewHolder.classPrice.visibility = View.GONE
        }

        if (yogaClassWithTeacher.teacher.isNotEmpty()) {
            viewHolder.classTeacher.text = "Teacher: ${yogaClassWithTeacher.teacher}"
            viewHolder.classTeacher.visibility = View.VISIBLE
        } else {
            viewHolder.classTeacher.visibility = View.GONE
        }

        if (yogaClassWithTeacher.date.isNotEmpty()) {
            viewHolder.date.text = "(${yogaClassWithTeacher.date})"
            viewHolder.date.visibility = View.VISIBLE
        } else {
            viewHolder.date.visibility = View.GONE
        }

        val imageUriString = yogaClassWithTeacher.yogaClass.imageUri
        if (!imageUriString.isNullOrEmpty()) {
            val imageUri = Uri.parse(imageUriString)
            loadImageFromUri(imageUri, viewHolder.classImage)
            viewHolder.classImage.visibility = View.VISIBLE
        } else {
            viewHolder.classImage.setImageResource(R.drawable.image_placeholder)
            viewHolder.classImage.visibility = View.VISIBLE
        }

        view.setOnClickListener {
            val intent = Intent(context, ClassDetailActivity::class.java).apply {
                putExtra("CLASS_ID", yogaClassWithTeacher.yogaClass.id.toLong())
            }
            context.startActivity(intent)
        }

        return view
    }

    private fun loadImageFromUri(uri: Uri, imageView: ImageView) {
        if (checkStoragePermission()) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestStoragePermission()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    (context as MainActivity),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                Toast.makeText(context, "Storage access is needed to load images", Toast.LENGTH_SHORT).show()
            }
            ActivityCompat.requestPermissions(
                context,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
    }

    private fun fetchClassesFromFirebase() {
        val firebaseDb = FirebaseFirestore.getInstance()
        firebaseDb.collection("YogaClasses")
            .get()
            .addOnSuccessListener { documents ->
                val firebaseClasses = mutableListOf<YogaClassWithTeacher>()
                for (document in documents) {
                    val yogaClass = YogaClass(
                        id = document.getLong("id")?.toInt() ?: 0,
                        dayOfWeek = document.getString("day_of_week") ?: "",
                        timeOfCourse = document.getString("time_of_course") ?: "",
                        capacity = document.getLong("capacity")?.toInt() ?: 0,
                        duration = document.getLong("duration")?.toInt() ?: 0,
                        pricePerClass = document.getDouble("price_per_class"),
                        typeOfClass = document.getString("type_of_class") ?: "",
                        skillLevel = document.getString("skill_level") ?: "",
                        focusArea = document.getString("focus_area") ?: "",
                        bodyArea = document.getString("body_area") ?: "",
                        description = document.getString("description") ?: "",
                        imageUri = document.getString("imageUri")
                    )
                    val teacher = document.getString("teacher") ?: "N/A"
                    val date = document.getString("date") ?: "No Schedule"

                    firebaseClasses.add(YogaClassWithTeacher(yogaClass, teacher, date))
                }
                updateDataList(firebaseClasses)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load Firebase data", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun updateDataList(firebaseClasses: List<YogaClassWithTeacher>) {
        yogaClassesWithTeacher = yogaClassesWithTeacher + firebaseClasses
        filteredYogaClasses = yogaClassesWithTeacher
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredYogaClasses = if (query.isEmpty()) {
            yogaClassesWithTeacher
        } else {
            yogaClassesWithTeacher.filter { it.teacher.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    private class ViewHolder(view: View) {
        val classDayOfWeek: TextView = view.findViewById(R.id.class_day_of_week)
        val classTimeOfCourse: TextView = view.findViewById(R.id.class_time_of_course)
        val classCapacity: TextView = view.findViewById(R.id.class_capacity)
        val classDuration: TextView = view.findViewById(R.id.class_duration)
        val classPrice: TextView = view.findViewById(R.id.class_price)
        val classTeacher: TextView = view.findViewById(R.id.class_teacher)
        val date: TextView = view.findViewById(R.id.date)
        val classImage: ImageView = view.findViewById(R.id.class_image)
    }
}
