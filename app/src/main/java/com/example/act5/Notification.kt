package com.example.act5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.*

class Notification : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var borrowedRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var equipmentRef: DatabaseReference
    private lateinit var transactionRef: DatabaseReference
    private lateinit var tableLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_notification, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            findNavController().navigate(R.id.action_notification_to_labtechDashboard)
        }

        tableLayout = view.findViewById(R.id.tableLayout)

        database = FirebaseDatabase.getInstance()
        borrowedRef = database.getReference("borrowedEquipments")
        usersRef = database.getReference("users")
        equipmentRef = database.getReference("equipment")
        transactionRef = database.getReference("transactionHistory")

        loadRequests()

        return view
    }

    private fun loadRequests() {
        borrowedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: continue
                    val equipmentMap = mutableMapOf<String, Map<String, Any>>()

                    for (equip in userSnapshot.children) {
                        val item = equip.value as? Map<String, Any> ?: continue
                        equipmentMap[equip.key!!] = item
                    }

                    usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnap: DataSnapshot) {
                            val borrowerName = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            val itemsText = equipmentMap.entries.joinToString("\n") {
                                val name = it.value["name"]
                                val qty = it.value["quantity"]
                                "$qty $name"
                            }

                            addRequestRow(uid, uid, borrowerName, itemsText, equipmentMap)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addRequestRow(
        requestId: String,
        uid: String,
        borrowerName: String,
        items: String,
        equipmentMap: Map<String, Map<String, Any>>
    ) {
        val row = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(resources.getColor(android.R.color.white))
        }

        fun createCell(text: String): TextView {
            return TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(resources.getColor(android.R.color.black))
                this.text = text
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(8, 8, 8, 8)
            }
        }

        val actionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val acceptBtn = Button(context).apply {
            text = "Accept"
            setBackgroundColor(0xFF038707.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                processRequest(requestId, uid, equipmentMap, "Accepted")
            }
        }

        val rejectBtn = Button(context).apply {
            text = "Reject"
            setBackgroundColor(0xFF8C0A02.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                processRequest(requestId, uid, equipmentMap, "Rejected")
            }
        }

        actionLayout.addView(acceptBtn)
        actionLayout.addView(rejectBtn)

        row.addView(createCell(borrowerName))
        row.addView(createCell(items))
        row.addView(createCell("Pending for Approval"))
        row.addView(actionLayout)

        tableLayout.addView(row)
    }

    private fun processRequest(
        requestId: String,
        uid: String,
        equipmentMap: Map<String, Map<String, Any>>,
        status: String
    ) {
        // Convert equipmentMap to mutableMap to avoid serialization issues
        val equipmentData = mutableMapOf<String, Any>()
        for ((pushId, item) in equipmentMap) {
            equipmentData[pushId] = item
        }

        val transactionData = mapOf(
            "uid" to uid,
            "equipments" to equipmentData,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )

        transactionRef.push().setValue(transactionData)

        // If rejected, return items to inventory
        if (status == "Rejected") {
            for ((_, item) in equipmentMap) {
                val name = item["name"] as? String ?: continue
                val category = item["category"] as? String ?: continue
                val quantity = (item["quantity"] as? Long)?.toInt() ?: continue

                val equipItemRef = equipmentRef.child(category).child(name)
                equipItemRef.child("quantity").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentQty = (snapshot.getValue(Long::class.java) ?: 0L).toInt()
                        equipItemRef.child("quantity").setValue(currentQty + quantity)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to update stock: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        // Remove the request from borrowedEquipments
        borrowedRef.child(requestId).removeValue()

        Toast.makeText(context, "Request $status", Toast.LENGTH_SHORT).show()
    }
}
