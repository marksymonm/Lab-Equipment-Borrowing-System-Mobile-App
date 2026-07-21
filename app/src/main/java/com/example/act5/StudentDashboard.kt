package com.example.act5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class StudentDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_student_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets for padding
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Apply custom fonts programmatically
        val montserratBlack = ResourcesCompat.getFont(requireContext(), R.font.montserrat_black)
        val montserratBold2 = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold2)

        view.findViewById<TextView>(R.id.dashboardTitle)?.typeface = montserratBlack

        view.findViewById<ConstraintLayout>(R.id.listOfEquipmentsButton)?.let { layout ->
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextView) child.typeface = montserratBold2
            }
        }

        view.findViewById<ConstraintLayout>(R.id.borrowingGuidelinesButton)?.let { layout ->
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextView) child.typeface = montserratBold2
            }
        }

        // Navigation logic
        val listOfEquipmentsButton = view.findViewById<ConstraintLayout>(R.id.listOfEquipmentsButton)
        listOfEquipmentsButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_studentDashboard_to_studentList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val borrowingGuidelinesButton = view.findViewById<ConstraintLayout>(R.id.borrowingGuidelinesButton)
        borrowingGuidelinesButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_studentDashboard_to_guidelines)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }

        val notificationButton = view.findViewById<ImageButton>(R.id.notificationButton)
        val cartButton = view.findViewById<ImageButton>(R.id.cartButton)
        val profileButton = view.findViewById<ImageButton>(R.id.profileButton)

        notificationButton.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboard_to_studentNotification)
        }

        cartButton.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboard_to_cart)
        }

        profileButton.setOnClickListener {
            findNavController().navigate(R.id.action_studentDashboard_to_studentProfile)
        }
    }

    private fun logoutUser() {
        // Clear stored user session or login info
        val sharedPref = requireActivity().getSharedPreferences("USER_PREFS", 0)
        sharedPref.edit().clear().apply()

        // Navigate back to login screen
        findNavController().navigate(R.id.action_studentDashboard_to_loginFragment)
    }
}
