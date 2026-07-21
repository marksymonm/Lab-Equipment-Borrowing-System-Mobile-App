package com.example.act5

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.*

class BorrowingHistory : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var tableLayout: TableLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_borrowing_history, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_borrowingHistory_to_labtechDashboard)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tableLayout = view.findViewById(R.id.table_borrowing_history)
        database = FirebaseDatabase.getInstance().reference

        loadBorrowedEquipments()
        loadTransactionHistory()

        return view
    }

    private fun loadBorrowedEquipments() {
        val usersRef = database.child("users")
        val borrowedRef = database.child("borrowedEquipments")

        borrowedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: continue

                    usersRef.child(uid).child("name")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnap: DataSnapshot) {
                                val borrowerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                val items = mutableListOf<String>()

                                for (itemSnapshot in userSnapshot.children) {
                                    val name = itemSnapshot.child("name").getValue(String::class.java) ?: continue
                                    val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                                    items.add("$quantity $name")
                                }

                                addRow(borrowerName, items.joinToString("\n"), "Pending for Approval", null)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading borrowed items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadTransactionHistory() {
        val usersRef = database.child("users")
        val historyRef = database.child("transactionHistory")

        historyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (transactionSnapshot in snapshot.children) {
                    val transactionKey = transactionSnapshot.key ?: continue
                    val uid = transactionSnapshot.child("uid").getValue(String::class.java) ?: continue
                    val status = transactionSnapshot.child("status").getValue(String::class.java) ?: "Unknown"

                    usersRef.child(uid).child("name")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnap: DataSnapshot) {
                                val borrowerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                val items = mutableListOf<String>()

                                val equipmentSnap = transactionSnapshot.child("equipments")
                                for (equip in equipmentSnap.children) {
                                    val name = equip.child("name").getValue(String::class.java) ?: continue
                                    val quantity = equip.child("quantity").getValue(Int::class.java) ?: 0
                                    items.add("$quantity $name")
                                }

                                val updatePath = "transactionHistory/$transactionKey/status"

                                addRow(borrowerName, items.joinToString("\n"), status, updatePath)
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading history: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addRow(borrower: String, items: String, status: String, updatePath: String?) {
        val row = TableRow(requireContext()).apply {
            setPadding(8, 8, 8, 8)
        }

        val borrowerView = TextView(requireContext()).apply {
            text = borrower
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(30, 10, 10, 10)
            setTextColor(resources.getColor(android.R.color.black))
            textSize = 16f
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            gravity = Gravity.CENTER
        }

        val itemsView = TextView(requireContext()).apply {
            text = items
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(10, 10, 10, 10)
            setTextColor(resources.getColor(android.R.color.black))
            textSize = 15f
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            gravity = Gravity.CENTER
        }

        val statusView = TextView(requireContext()).apply {
            text = status
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(-15, 10, 30, 10)
            setTextColor(resources.getColor(android.R.color.black))
            textSize = 16f
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            gravity = Gravity.CENTER
        }

        val actionButton = Button(requireContext()).apply {
            text = "Mark as Returned"
            setTextColor(resources.getColor(android.R.color.white))
            setPadding(10, 30, 10, 30)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
            setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            isAllCaps = false
            setPadding(8, 16, 8, 16)
            gravity = Gravity.CENTER
            isEnabled = status.equals("accepted", ignoreCase = true)

            if (!isEnabled) alpha = 0.5f

            setOnClickListener {
                if (updatePath != null) {
                    database.child(updatePath).setValue("returned")
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Status updated to returned", Toast.LENGTH_SHORT).show()

                            // Restock items
                            val transactionKey = updatePath.split("/")[1]
                            database.child("transactionHistory").child(transactionKey).child("equipments")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (equipmentSnapshot in snapshot.children) {
                                            val name = equipmentSnapshot.child("name").getValue(String::class.java)
                                            val category = equipmentSnapshot.child("category").getValue(String::class.java)
                                            val quantity = equipmentSnapshot.child("quantity").getValue(Int::class.java) ?: 0

                                            if (!name.isNullOrEmpty() && !category.isNullOrEmpty()) {
                                                val equipmentRef = database.child("equipment").child(category)
                                                equipmentRef.orderByChild("name").equalTo(name)
                                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                                        override fun onDataChange(equipSnap: DataSnapshot) {
                                                            for (item in equipSnap.children) {
                                                                val currentQty = item.child("quantity").getValue(Int::class.java) ?: 0
                                                                item.ref.child("quantity").setValue(currentQty + quantity)
                                                            }
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {}
                                                    })
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })

                            // Update UI
                            statusView.text = "Returned"
                            isEnabled = false
                            alpha = 0.5f
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        row.addView(borrowerView, TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(itemsView, TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f))
        row.addView(statusView, TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(actionButton, TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f))

        tableLayout.addView(row)

        val separator = View(requireContext()).apply {
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2)
            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
        }
        tableLayout.addView(separator)
    }
}
