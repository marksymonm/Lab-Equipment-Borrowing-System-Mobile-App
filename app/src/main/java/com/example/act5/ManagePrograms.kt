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

class ManagePrograms : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var etCourse: EditText
    private lateinit var etProgram: EditText
    private lateinit var btnAddProgram: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_manage_programs, container, false)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etCourse = view.findViewById(R.id.et_course)
        etProgram = view.findViewById(R.id.et_program)
        btnAddProgram = view.findViewById(R.id.btn_add_program)
        database = FirebaseDatabase.getInstance().getReference("programs")

        btnAddProgram.setOnClickListener {
            val courseInput = etCourse.text.toString().trim().uppercase()
            val programInput = etProgram.text.toString().trim().uppercase()

            if (courseInput.isEmpty()) {
                etCourse.error = "Please enter a course"
                etCourse.requestFocus()
                return@setOnClickListener
            }
            if (programInput.isEmpty()) {
                etProgram.error = "Please enter a program"
                etProgram.requestFocus()
                return@setOnClickListener
            }

            checkAndSaveProgram(courseInput, programInput)
        }

        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            try {
                findNavController().navigate(R.id.action_managePrograms_to_labtechDashboard)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }

    private fun checkAndSaveProgram(course: String, program: String) {
        database.orderByChild("program").equalTo(program).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val existingCourse = child.child("course").getValue(String::class.java)?.uppercase()
                    if (existingCourse == course) {
                        Toast.makeText(context, "Program already exists for this course", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                saveProgramToDatabase(course, program)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error checking program: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveProgramToDatabase(course: String, program: String) {
        val key = database.push().key ?: return
        val programData = Program(course, program)
        database.child(key).setValue(programData)
            .addOnSuccessListener {
                Toast.makeText(context, "Program added successfully", Toast.LENGTH_SHORT).show()
                etCourse.text.clear()
                etProgram.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add program: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    data class Program(
        val course: String = "",
        val program: String = ""
    )
}
