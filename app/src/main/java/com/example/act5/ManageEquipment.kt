package com.example.act5

import android.os.Bundle
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

class ManageEquipment : Fragment() {

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_manage_equipment, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance().reference.child("equipment")

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_manageEquipment_to_labtechDashboard)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val categories = listOf("Glassware", "Tools", "Heavy Equipment")

        val spinnerAdd = view.findViewById<Spinner>(R.id.spinner_add)
        val adapterAdd = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.textSize = 16f
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold)
                return textView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getDropDownView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.textSize = 16f
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold)
                textView.setBackgroundColor(resources.getColor(android.R.color.white))
                return textView
            }
        }
        adapterAdd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAdd.adapter = adapterAdd


        val spinnerRemove = view.findViewById<Spinner>(R.id.spinner_remove)
        val adapterRemove = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, categories) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold)
                textView.textSize = 16f
                return textView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getDropDownView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.textSize = 16f
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_semibold)
                textView.setBackgroundColor(resources.getColor(android.R.color.white))
                return textView
            }
        }
        adapterRemove.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRemove.adapter = adapterRemove


        val etAddName = view.findViewById<EditText>(R.id.et_equipment_name)
        val etAddQuantity = view.findViewById<EditText>(R.id.et_add_quantity)
        val btnAdd = view.findViewById<Button>(R.id.btn_add)

        val etRemoveName = view.findViewById<EditText>(R.id.et_remove_equipment_name)
        val etRemoveQuantity = view.findViewById<EditText>(R.id.et_remove_quantity)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)

        btnAdd.setOnClickListener {
            val name = etAddName.text.toString().trim()
            val category = spinnerAdd.selectedItem.toString()
            val quantityStr = etAddQuantity.text.toString().trim()

            if (name.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter equipment name and quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(requireContext(), "Enter a valid positive quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addOrUpdateEquipment(name, category, quantity)
        }

        btnRemove.setOnClickListener {
            val name = etRemoveName.text.toString().trim()
            val category = spinnerRemove.selectedItem.toString()
            val quantityStr = etRemoveQuantity.text.toString().trim()

            if (name.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter equipment name and quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(requireContext(), "Enter a valid positive quantity", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            removeOrUpdateEquipment(name, category, quantity)
        }

        return view
    }

    private fun addOrUpdateEquipment(name: String, category: String, quantityToAdd: Int) {
        val equipmentRef = database.child(category).child(name)

        equipmentRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Update existing quantity
                val currentQuantity = snapshot.child("quantity").getValue(Int::class.java) ?: 0
                val newQuantity = currentQuantity + quantityToAdd

                equipmentRef.child("quantity").setValue(newQuantity)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Equipment quantity updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update equipment", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Create new equipment entry
                val equipmentData = mapOf(
                    "name" to name,
                    "category" to category,
                    "quantity" to quantityToAdd
                )

                equipmentRef.setValue(equipmentData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Equipment added successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to add equipment", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeOrUpdateEquipment(name: String, category: String, quantityToRemove: Int) {
        val equipmentRef = database.child(category).child(name)

        equipmentRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentQuantity = snapshot.child("quantity").getValue(Int::class.java) ?: 0

                if (quantityToRemove >= currentQuantity) {
                    // Remove equipment entirely
                    equipmentRef.removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Equipment removed completely", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to remove equipment", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Deduct quantity
                    val newQuantity = currentQuantity - quantityToRemove
                    equipmentRef.child("quantity").setValue(newQuantity)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Equipment quantity updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update equipment", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(requireContext(), "Equipment not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
