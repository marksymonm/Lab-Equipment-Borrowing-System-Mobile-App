package com.example.act5

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentProfile : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var transactionHistoryRef: DatabaseReference

    private lateinit var nameText: TextView
    private lateinit var programText: TextView
    private lateinit var studentIdText: TextView
    private lateinit var yearSectionText: TextView
    private lateinit var tableRowsLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_studprofile, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        transactionHistoryRef = FirebaseDatabase.getInstance().reference.child("transactionHistory")

        // Initialize UI elements
        nameText = view.findViewById(R.id.nameText)
        studentIdText = view.findViewById(R.id.studentIdText)
        yearSectionText = view.findViewById(R.id.yearSectionText)
        tableRowsLayout = view.findViewById(R.id.tableRows)

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_studentProfile_to_studentDashboard)
        }

        loadUserData()

        return view
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.d("StudentProfile", "No user logged in, redirecting...")
            findNavController().navigate(R.id.action_studentDashboard_to_loginFragment)
            return
        }

        val userId = currentUser.uid
        Log.d("StudentProfile", "Loading data for user ID: $userId")

        database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "N/A"
                    val course = snapshot.child("course").getValue(String::class.java) ?: "N/A"
                    val program = snapshot.child("program").getValue(String::class.java) ?: "N/A"
                    val studentNo = snapshot.child("studentNo").getValue(String::class.java) ?: "N/A"
                    val yearSection = snapshot.child("yearSection").getValue(String::class.java) ?: "N/A"
                    val combinedText = "$course - $program - $yearSection"

                    nameText.text = name.uppercase()
                    studentIdText.text = studentNo
                    yearSectionText.text = combinedText  // Shows Course - Program, Year and Section
                    loadTransactionHistory(userId)
                } else {
                    nameText.text = "N/A"
                    studentIdText.text = "N/A"
                    yearSectionText.text = "N/A"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StudentProfile", "Database error: ${error.message}")
            }
        })
    }

    private fun loadTransactionHistory(userId: String) {
        // Clear previous transaction rows
        tableRowsLayout.removeAllViews()

        transactionHistoryRef.orderByChild("uid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Show message if no transaction history found
                        val noDataView = TextView(context).apply {
                            text = "No transaction history found."
                            setTextColor(Color.DKGRAY)
                            gravity = Gravity.CENTER
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                        }
                        tableRowsLayout.addView(noDataView)
                        return
                    }

                    for (transactionSnapshot in snapshot.children) {
                        // Get equipments child (a map of equipment items)
                        val equipmentsSnapshot = transactionSnapshot.child("equipments")
                        val equipmentList = mutableListOf<String>()
                        for (equipSnap in equipmentsSnapshot.children) {
                            val name =
                                equipSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            val qty = equipSnap.child("quantity").getValue(Int::class.java) ?: 0
                            equipmentList.add("$qty $name")
                        }
                        val equipmentText = equipmentList.joinToString(separator = "\n")

                        val status =
                            transactionSnapshot.child("status").getValue(String::class.java)
                                ?: "Unknown"

                        // Create horizontal LinearLayout row for each transaction
                        val rowLayout = LinearLayout(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).also { it.bottomMargin = 8 }
                            orientation = LinearLayout.HORIZONTAL
                            setBackgroundColor(Color.WHITE)
                            setPadding(8, 8, 8, 8)
                        }

                        val equipmentTextView = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                            text = equipmentText
                            gravity = Gravity.CENTER
                            setPadding(8, 8, 8, 8)
                            setTextColor(resources.getColor(android.R.color.black))
                            textSize = 16f
                            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
                        }

                        val statusTextView = TextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                            text = status
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            setPadding(8, 8, 8, 25)
                            setTextColor(resources.getColor(android.R.color.black))
                            textSize = 16f
                            typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
                        }

                        rowLayout.addView(equipmentTextView)
                        rowLayout.addView(statusTextView)

                        tableRowsLayout.addView(rowLayout)

                        // Add a horizontal separator line after each row
                        val separator = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2 // Thickness of the line in pixels
                            )
                            setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                        }

                        tableRowsLayout.addView(separator)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("StudentProfile", "Failed to load transaction history: ${error.message}")
                }
            })
    }
}
