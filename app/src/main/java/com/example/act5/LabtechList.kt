// Updated LabtechList.kt with search and category filter functionality
package com.example.act5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LabtechList : Fragment() {

    private lateinit var equipmentTable: TableLayout
    private lateinit var database: DatabaseReference
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var filterButton: Button

    private val equipmentList = mutableListOf<Triple<String, Int, String>>()
    private var currentFilter = "All"
    private var currentSearch = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_labtech_list, container, false)

        val backButton: ImageButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechList_to_labtechDashboard)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        equipmentTable = view.findViewById(R.id.equipmentTable)
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        filterButton = view.findViewById(R.id.filterButton)
        database = FirebaseDatabase.getInstance().getReference("equipment")

        searchButton.setOnClickListener {
            currentSearch = searchInput.text.toString().trim()
            updateTable()
        }

        filterButton.setOnClickListener {
            showCategoryDropdown()
        }

        fetchEquipmentList()

        return view
    }

    private fun fetchEquipmentList() {
        database.get().addOnSuccessListener { snapshot ->
            equipmentList.clear()

            for (categorySnap in snapshot.children) {
                for (equipSnap in categorySnap.children) {
                    val name = equipSnap.child("name").getValue(String::class.java) ?: ""
                    val quantity = equipSnap.child("quantity").getValue(Int::class.java) ?: 0
                    val category = equipSnap.child("category").getValue(String::class.java) ?: ""

                    equipmentList.add(Triple(name, quantity, category))
                }
            }

            updateTable()
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    private fun updateTable() {
        equipmentTable.removeViews(1, equipmentTable.childCount - 1) // keep header row

        val filteredList = equipmentList.filter {
            (currentFilter == "All" || it.third == currentFilter) &&
                    (currentSearch.isEmpty() || it.first.contains(currentSearch, ignoreCase = true))
        }

        for ((name, quantity, category) in filteredList) {
            val row = TableRow(requireContext())

            val nameTextView = TextView(requireContext()).apply {
                text = name
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(12, 30, 12, 30)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            val quantityTextView = TextView(requireContext()).apply {
                text = quantity.toString()
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(12, 30, 12, 30)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            val categoryTextView = TextView(requireContext()).apply {
                text = category
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(12, 30, 12, 30)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            row.addView(nameTextView)
            row.addView(quantityTextView)
            row.addView(categoryTextView)

            equipmentTable.addView(row)

            // ✅ Add a horizontal separator line after each row
            val separator = View(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    2 // thickness of line in pixels
                )
                setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            }
            equipmentTable.addView(separator)
        }
    }


    private fun showCategoryDropdown() {
        val popupMenu = PopupMenu(requireContext(), filterButton)
        popupMenu.menu.add("All")
        popupMenu.menu.add("Glassware")
        popupMenu.menu.add("Tools")
        popupMenu.menu.add("Heavy Equipment")

        popupMenu.setOnMenuItemClickListener { item ->
            currentFilter = item.title.toString()
            filterButton.text = "SORT ▼\n$currentFilter"
            updateTable()
            true
        }

        popupMenu.show()
    }
}