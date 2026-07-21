package com.example.act5

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.text.InputType
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.GoogleAuthProvider

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var isFromGoogle = false
    private val studentNoPattern = Regex("""TUPC-\d{2}-\d{4}""")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val etName = view.findViewById<EditText>(R.id.etRegisterName)
        val spinnerProgram = view.findViewById<Spinner>(R.id.spinnerRegisterProgram)
        val etYearSection = view.findViewById<EditText>(R.id.etRegisterYearSection)
        val etStudentNo = view.findViewById<EditText>(R.id.etRegisterStudentNo)
        val etEmail = view.findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<EditText>(R.id.etConfirmPassword)
        val ivTogglePassword = view.findViewById<ImageView>(R.id.ivToggleRPassword)
        val ivToggleConfirmPassword = view.findViewById<ImageView>(R.id.ivToggleCPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)

        val programMap = mutableMapOf<String, Pair<String, String>>() // "Course - Program" -> (Course, Program)
        val programList = mutableListOf<String>()
        val programRef = database.child("programs")

        val spinnerAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            programList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.textSize = 16f
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
                return textView
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView = super.getDropDownView(position, convertView, parent) as TextView
                textView.setTextColor(resources.getColor(android.R.color.black))
                textView.textSize = 16f
                textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_medium)
                textView.setBackgroundColor(resources.getColor(android.R.color.white))
                return textView
            }
        }

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProgram.adapter = spinnerAdapter


        programRef.get().addOnSuccessListener { snapshot ->
            programList.clear()
            snapshot.children.forEach {
                val course = it.child("course").getValue(String::class.java)
                val program = it.child("program").getValue(String::class.java)
                if (!course.isNullOrEmpty() && !program.isNullOrEmpty()) {
                    val displayText = "$course - $program"
                    programList.add(displayText)
                    programMap[displayText] = Pair(course, program)
                }
            }
            spinnerAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to load programs: ${it.message}", Toast.LENGTH_SHORT).show()
        }

        val nameFromArgs = arguments?.getString("name")
        val emailFromArgs = arguments?.getString("email")
        if (!nameFromArgs.isNullOrEmpty() && !emailFromArgs.isNullOrEmpty()) {
            etName.setText(nameFromArgs)
            etEmail.setText(emailFromArgs)
            etEmail.isEnabled = false
            etPassword.visibility = View.GONE
            etConfirmPassword.visibility = View.GONE
            ivTogglePassword.visibility = View.GONE
            ivToggleConfirmPassword.visibility = View.GONE
            isFromGoogle = true
        }

        if (etStudentNo.text.toString().isEmpty()) {
            etStudentNo.setText("TUPC-")
            etStudentNo.setSelection(etStudentNo.text!!.length)
        }

        etStudentNo.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            private val prefix = "TUPC-"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                val raw = s.toString()
                var input = if (raw.startsWith(prefix)) raw.removePrefix(prefix) else raw
                input = input.filter { it.isDigit() }

                val formatted = StringBuilder(prefix)
                if (input.length > 2) {
                    formatted.append(input.substring(0, 2)).append("-")
                    formatted.append(input.substring(2, input.length.coerceAtMost(6)))
                } else {
                    formatted.append(input)
                }

                etStudentNo.setText(formatted.toString())
                etStudentNo.setSelection(formatted.length)
                isEditing = false
            }
        })

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val selectedDisplay = spinnerProgram.selectedItem?.toString()?.trim()
            val course = programMap[selectedDisplay]?.first ?: ""
            val program = programMap[selectedDisplay]?.second ?: ""
            val yearSection = etYearSection.text.toString().trim()
            val studentNo = etStudentNo.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            when {
                name.isEmpty() -> {
                    etName.error = "Name is required"
                    etName.requestFocus()
                    return@setOnClickListener
                }
                selectedDisplay.isNullOrEmpty() -> {
                    Toast.makeText(context, "Please select a program", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                yearSection.isEmpty() -> {
                    etYearSection.error = "Year and Section is required"
                    etYearSection.requestFocus()
                    return@setOnClickListener
                }
                !studentNoPattern.matches(studentNo) -> {
                    etStudentNo.error = "Invalid Student No format. Expected: TUPC-12-3456"
                    etStudentNo.requestFocus()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    etEmail.error = "Email is required"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Please enter a valid email"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !isFromGoogle && password.isEmpty() -> {
                    etPassword.error = "Password is required"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
                !isFromGoogle && password.length < 6 -> {
                    etPassword.error = "Password should be at least 6 characters"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
                !isFromGoogle && confirmPassword.isEmpty() -> {
                    etConfirmPassword.error = "Please confirm your password"
                    etConfirmPassword.requestFocus()
                    return@setOnClickListener
                }
                !isFromGoogle && password != confirmPassword -> {
                    etConfirmPassword.error = "Passwords do not match"
                    etConfirmPassword.requestFocus()
                    return@setOnClickListener
                }
            }

            btnRegister.isEnabled = false
            btnRegister.text = "REGISTERING..."

            val registerUser = registerUser@{
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    Toast.makeText(requireContext(), "User ID is null. Please try again.", Toast.LENGTH_SHORT).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "REGISTER"
                    return@registerUser
                }

                val userInfo = mapOf(
                    "name" to name,
                    "course" to course,
                    "program" to program,
                    "yearSection" to yearSection,
                    "studentNo" to studentNo,
                    "email" to email
                )

                database.child("users").child(uid).setValue(userInfo)
                    .addOnCompleteListener { dbTask ->
                        btnRegister.isEnabled = true
                        btnRegister.text = "REGISTER"
                        if (dbTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        } else {
                            Toast.makeText(requireContext(), "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            if (isFromGoogle) {
                val idToken = arguments?.getString("idToken")
                if (idToken == null) {
                    Toast.makeText(requireContext(), "Google ID token missing", Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "REGISTER"
                    return@setOnClickListener
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            registerUser()
                        } else {
                            Toast.makeText(requireContext(), "Google Sign-in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            btnRegister.text = "REGISTER"
                        }
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        btnRegister.isEnabled = true
                        btnRegister.text = "REGISTER"

                        if (task.isSuccessful) {
                            registerUser()
                        } else {
                            Toast.makeText(requireContext(), "Registration failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        var isPasswordVisible = false
        ivTogglePassword.setOnClickListener {
            if (etPassword.visibility == View.VISIBLE) {
                isPasswordVisible = !isPasswordVisible
                etPassword.inputType = if (isPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                etPassword.setSelection(etPassword.text.length)
                ivTogglePassword.setImageResource(if (isPasswordVisible) R.drawable.hide else R.drawable.show)
            }
        }

        var isConfirmPasswordVisible = false
        ivToggleConfirmPassword.setOnClickListener {
            if (etConfirmPassword.visibility == View.VISIBLE) {
                isConfirmPasswordVisible = !isConfirmPasswordVisible
                etConfirmPassword.inputType = if (isConfirmPasswordVisible)
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                etConfirmPassword.setSelection(etConfirmPassword.text.length)
                ivToggleConfirmPassword.setImageResource(if (isConfirmPasswordVisible) R.drawable.hide else R.drawable.show)
            }
        }

        return view
    }
}
