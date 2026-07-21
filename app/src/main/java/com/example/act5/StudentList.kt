package com.example.act5

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentList : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var tableLayout: TableLayout
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var filterButton: Button

    private val auth = FirebaseAuth.getInstance()
    private var fullEquipmentList: MutableList<EquipmentItem> = mutableListOf()
    private var currentCategory: String = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_student_list, container, false)

        tableLayout = view.findViewById(R.id.equipmentTable)
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        filterButton = view.findViewById(R.id.filterButton)

        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            findNavController().navigate(R.id.action_studentList_to_studentDashboard)
        }

        database = FirebaseDatabase.getInstance().reference

        searchButton.setOnClickListener {
            displayFilteredEquipment()
        }

        filterButton.setOnClickListener {
            showCategoryDropdown()
        }

        loadEquipments()
        return view
    }

    private fun loadEquipments() {
        database.child("equipment").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullEquipmentList.clear()

                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: continue
                    for (equipmentSnapshot in categorySnapshot.children) {
                        val equipmentId = equipmentSnapshot.key ?: continue
                        val name = equipmentSnapshot.child("name").getValue(String::class.java) ?: continue
                        val category = equipmentSnapshot.child("category").getValue(String::class.java) ?: categoryName
                        val quantity = equipmentSnapshot.child("quantity").getValue(Int::class.java) ?: 0

                        fullEquipmentList.add(EquipmentItem(equipmentId, name, category, quantity))
                    }
                }

                displayFilteredEquipment()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayFilteredEquipment() {
        val searchQuery = searchInput.text.toString().trim().lowercase()
        val filteredList = fullEquipmentList.filter {
            (currentCategory == "All" || it.category == currentCategory) &&
                    (searchQuery.isEmpty() || it.name.lowercase().contains(searchQuery))
        }

        displayEquipmentTable(filteredList)
    }

    private fun displayEquipmentTable(equipmentList: List<EquipmentItem>) {
        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }

        for (equipment in equipmentList) {
            val row = TableRow(requireContext())

            val nameTextView = TextView(requireContext()).apply {
                text = equipment.name
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(8, 8, 8, 8)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            val categoryTextView = TextView(requireContext()).apply {
                text = equipment.category
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(8, 8, 8, 8)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            val quantityTextView = TextView(requireContext()).apply {
                text = equipment.quantity.toString()
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(8, 8, 8, 8)
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }

            val addButton = Button(requireContext()).apply {
                text = "Add to\nCart"
                setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                setTextColor(resources.getColor(android.R.color.white))
                setPadding(10, 10, 10, 10)
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
                isEnabled = equipment.quantity > 0
                setOnClickListener {
                    addToCart(equipment.id, equipment.name)
                }
            }

            row.addView(nameTextView)
            row.addView(categoryTextView)
            row.addView(quantityTextView)
            row.addView(addButton)

            tableLayout.addView(row)

            // ✅ Add a horizontal separator line after each row
            val separator = View(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    2 // Thickness of the line
                )
                setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            }
            tableLayout.addView(separator)
        }
    }

    private fun showCategoryDropdown() {
        val categories = arrayOf("All", "Glassware", "Tools", "Heavy Equipment")

        val popupMenu = PopupMenu(requireContext(), filterButton)
        categories.forEach { popupMenu.menu.add(it) }

        popupMenu.setOnMenuItemClickListener { item ->
            currentCategory = item.title.toString()
            filterButton.text = "$currentCategory ▼"
            displayFilteredEquipment()
            true
        }

        popupMenu.show()
    }

    private fun addToCart(equipmentId: String, equipmentName: String) {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = database.child("cart").child(userId).child(equipmentId)

        cartRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val cartItem = currentData.getValue(CartItem::class.java)
                if (cartItem == null) {
                    currentData.value = CartItem(equipmentName, 1)
                } else {
                    cartItem.quantity += 1
                    currentData.value = cartItem
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                } else if (committed) {
                    Toast.makeText(requireContext(), "$equipmentName added to cart", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    data class EquipmentItem(
        val id: String,
        val name: String,
        val category: String,
        val quantity: Int
    )

    data class CartItem(
        var name: String = "",
        var quantity: Int = 0
    )
}
