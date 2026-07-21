package com.example.act5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StudentNotification : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var transactionRef: DatabaseReference
    private lateinit var tableLayout: LinearLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_notification_student, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_studentNotification_to_studentDashboard)
        }

        tableLayout = view.findViewById(R.id.tableLayout)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        transactionRef = database.getReference("transactionHistory")

        loadStudentTransactions()

        return view
    }

    private fun loadStudentTransactions() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // No user logged in, handle appropriately
            return
        }

        val uid = currentUser.uid

        // Clear any existing rows except the header
        // Assuming the first child is header, remove all others
        val childCount = tableLayout.childCount
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1)
        }

        // Query all transactions, filter by uid
        transactionRef.orderByChild("uid").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (transactionSnap in snapshot.children) {
                        val status = transactionSnap.child("status").getValue(String::class.java)
                            ?: "Unknown"
                        val equipmentsSnap = transactionSnap.child("equipments")

                        val itemsList = mutableListOf<String>()
                        for (equipSnap in equipmentsSnap.children) {
                            val equipData = equipSnap.value as? Map<*, *>
                            val name = equipData?.get("name") as? String ?: continue
                            val quantity = (equipData["quantity"] as? Long)?.toInt() ?: 0
                            itemsList.add("$quantity $name")
                        }
                        val itemsText = itemsList.joinToString("\n")

                        addDataRow(itemsText, status)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }


    private fun addDataRow(items: String, status: String) {
        val row = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 0) // bottom margin only; separator handles space
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 12, 12, 12)

        }

        fun createCell(text: String): TextView {
            return TextView(requireContext()).apply {
                layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                this.text = text
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(12, 12, 12, 12)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                textSize = 16f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
            }
        }

        row.addView(createCell(items))
        row.addView(createCell(status))
        tableLayout.addView(row)

        // ✅ Add horizontal separator line
        val separator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(16, 0, 16, 8) // top spacing only
            }
            setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
            )
        }

        tableLayout.addView(separator)
    }
}
