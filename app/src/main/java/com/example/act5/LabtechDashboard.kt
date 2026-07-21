package com.example.act5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class LabtechDashboard : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_labtech_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigate to List of Equipments
        val listOfEquipmentsButton =
            view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.listOfEquipmentsButton)
        listOfEquipmentsButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_labtechList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Navigate to Manage Equipment
        val manageEquipmentButton =
            view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.manageEquipmentButton)
        manageEquipmentButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_manageEquipment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Navigate to Manage Programs
        val manageProgramsButton =
            view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.manageProgramsButton)
        manageProgramsButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_managePrograms)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Navigate to Borrowing History
        val borrowingRecordsButton =
            view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.borrowingRecordsButton)
        borrowingRecordsButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_borrowingHistory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Navigate to Login (Logout Button)
        val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_loginFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Navigate to Notification (ImageButton)
        val notificationButton =
            view.findViewById<ImageButton>(R.id.notificationButton) // Corrected to ImageButton
        notificationButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_labtechDashboard_to_notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
