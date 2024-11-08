package com.example.yogaadmin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class YogaClassAdapter(
    private val context: Context,
    private var yogaClassesWithTeacher: List<YogaClassWithTeacher>
) : BaseAdapter() {

    private var filteredYogaClasses: List<YogaClassWithTeacher> = yogaClassesWithTeacher


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

        val imageBase64 = yogaClassWithTeacher.yogaClass.imageUri
        if (!imageBase64.isNullOrEmpty()) {
            loadImageFromBase64(imageBase64, viewHolder.classImage)
        } else {
            viewHolder.classImage.setImageResource(R.drawable.image_placeholder)
        }


        view.setOnClickListener {
            val intent = Intent(context, ClassDetailActivity::class.java).apply {
                putExtra("CLASS_ID", yogaClassWithTeacher.yogaClass.id.toLong())
            }
            context.startActivity(intent)
        }

        return view
    }

    private fun loadImageFromBase64(base64String: String, imageView: ImageView) {
        try {
            val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchClassesFromFirebase() {
        val yogaClassesRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("yogaclasses")
        val yogaInstancesRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("YogaInstances")

        yogaClassesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firebaseClasses = mutableListOf<YogaClassWithTeacher>()

                for (document in snapshot.children) {
                    val yogaClass = YogaClass(
                        id = document.child("id").getValue(Int::class.java) ?: 0,
                        dayOfWeek = document.child("day_of_week").getValue(String::class.java) ?: "",
                        timeOfCourse = document.child("time_of_course").getValue(String::class.java) ?: "",
                        capacity = document.child("capacity").getValue(Int::class.java) ?: 0,
                        duration = document.child("duration").getValue(Int::class.java) ?: 0,
                        pricePerClass = document.child("price_per_class").getValue(Double::class.java),
                        typeOfClass = document.child("type_of_class").getValue(String::class.java) ?: "",
                        skillLevel = document.child("skill_level").getValue(String::class.java) ?: "",
                        focusArea = document.child("focus_area").getValue(String::class.java) ?: "",
                        bodyArea = document.child("body_area").getValue(String::class.java) ?: "",
                        description = document.child("description").getValue(String::class.java) ?: "",
                        imageUri = document.child("imageUri").getValue(String::class.java)
                    )

                    val yogaClassWithTeacher = YogaClassWithTeacher(yogaClass, teacher = "N/A", date = "No Schedule")
                    firebaseClasses.add(yogaClassWithTeacher)
                }

                yogaInstancesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(instanceSnapshot: DataSnapshot) {
                        for (yogaClassWithTeacher in firebaseClasses) {
                            val classId = yogaClassWithTeacher.yogaClass.id.toString()

                            val matchingInstance = instanceSnapshot.children.find {
                                it.child("classId").getValue(String::class.java) == classId
                            }

                            if (matchingInstance != null) {
                                yogaClassWithTeacher.teacher = matchingInstance.child("teacher").getValue(String::class.java) ?: "N/A"
                                yogaClassWithTeacher.date = matchingInstance.child("date").getValue(String::class.java) ?: "No Schedule"
                            }
                        }

                        Log.d("YogaClassAdapter", "Fetched ${firebaseClasses.size} classes with teacher and date from YogaInstances.")
                        updateDataList(firebaseClasses)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load YogaInstances data", Toast.LENGTH_SHORT).show()
                        Log.e("YogaClassAdapter", "Error fetching YogaInstances data", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load YogaClasses data", Toast.LENGTH_SHORT).show()
                Log.e("YogaClassAdapter", "Error fetching YogaClasses data", error.toException())
            }
        })
    }


    private fun updateDataList(firebaseClasses: List<YogaClassWithTeacher>) {
        yogaClassesWithTeacher = yogaClassesWithTeacher + firebaseClasses
        filteredYogaClasses = yogaClassesWithTeacher
        notifyDataSetChanged()
    }

    fun updateData(newYogaClasses: List<YogaClassWithTeacher>) {
        yogaClassesWithTeacher = newYogaClasses
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
