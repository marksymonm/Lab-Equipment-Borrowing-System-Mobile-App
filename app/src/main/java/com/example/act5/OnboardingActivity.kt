package com.example.act5

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val getStartedButton = findViewById<Button>(R.id.btn_get_started)

        getStartedButton.setOnClickListener {
            // Save that onboarding has been completed
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("onboarding_completed", true)
            editor.apply()

            // Navigate to login screen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
