package com.example.yogaadmin

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.yogaadmin.databinding.FilterBottomSheetBinding

class FilterBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: FilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var listener: FilterListener

    interface FilterListener {
        fun onFiltersApplied(filters: Filters)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FilterListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement FilterListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.applyButton.setOnClickListener {
            applyFilters()
        }
    }

    private fun applyFilters() {
        val selectedDaysOfWeek = mutableListOf<String>()
        if (binding.checkSunday.isChecked) selectedDaysOfWeek.add("Sunday")
        if (binding.checkMonday.isChecked) selectedDaysOfWeek.add("Monday")
        if (binding.checkTuesday.isChecked) selectedDaysOfWeek.add("Tuesday")
        if (binding.checkWednesday.isChecked) selectedDaysOfWeek.add("Wednesday")
        if (binding.checkThursday.isChecked) selectedDaysOfWeek.add("Thursday")
        if (binding.checkFriday.isChecked) selectedDaysOfWeek.add("Friday")
        if (binding.checkSaturday.isChecked) selectedDaysOfWeek.add("Saturday")

        val selectedSkillLevel = when {
            binding.checkBeginner.isChecked -> "Beginner"
            binding.checkIntermediate.isChecked -> "Intermediate"
            binding.checkAdvanced.isChecked -> "Advanced"
            binding.checkAllLevels.isChecked -> "All Levels"
            else -> ""
        }

        val selectedDuration = when {
            binding.checkShort.isChecked -> 1
            binding.checkMedium.isChecked -> 2
            binding.checkLong.isChecked -> 3
            else -> 0
        }

        val selectedFocusArea = mutableListOf<String>()
        if (binding.checkFlexibility.isChecked) selectedFocusArea.add("Flexibility")
        if (binding.checkStrength.isChecked) selectedFocusArea.add("Strength")
        if (binding.checkBalance.isChecked) selectedFocusArea.add("Balance")
        if (binding.checkCore.isChecked) selectedFocusArea.add("Core")
        if (binding.checkRelaxation.isChecked) selectedFocusArea.add("Relaxation")

        val selectedBodyArea = mutableListOf<String>()
        if (binding.checkFullBody.isChecked) selectedBodyArea.add("Full Body")
        if (binding.checkUpperBody.isChecked) selectedBodyArea.add("Upper Body")
        if (binding.checkLowerBody.isChecked) selectedBodyArea.add("Lower Body")
        if (binding.checkBackSpine.isChecked) selectedBodyArea.add("Back")
        if (binding.checkCoreAbs.isChecked) selectedBodyArea.add("Abs")

        val capacity = when {
            binding.checkSmallGroup.isChecked -> 9
            binding.checkMediumGroup.isChecked -> 25
            binding.checkLargeGroup.isChecked -> 50
            else -> 0
        }

        val pricePerClass = when {
            binding.checkLowPrice.isChecked -> 20.0
            binding.checkMediumPrice.isChecked -> 50.0
            binding.checkHighPrice.isChecked -> 51.0
            else -> -1.0
        }

        val filters = Filters(
            dayOfWeek = selectedDaysOfWeek,
            skillLevel = selectedSkillLevel,
            duration = selectedDuration,
            focusArea = selectedFocusArea,
            bodyArea = selectedBodyArea,
            capacity = capacity,
            pricePerClass = pricePerClass
        )

        Log.d("AppliedFilters", "Filters: $filters")

        listener.onFiltersApplied(filters)

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

