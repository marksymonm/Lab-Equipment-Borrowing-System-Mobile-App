package com.example.act5

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoPath = "android.resource://$packageName/${R.raw.labsplash}"
        videoView.setVideoURI(Uri.parse(videoPath))

        videoView.setOnPreparedListener { mediaPlayer ->
            val videoWidth = mediaPlayer.videoWidth
            val videoHeight = mediaPlayer.videoHeight
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            val newWidth = (screenHeight * videoWidth) / videoHeight
            if (newWidth < screenWidth) {
                videoView.layoutParams.width = screenWidth
                videoView.layoutParams.height = (screenWidth * videoHeight) / videoWidth
            } else {
                videoView.layoutParams.width = newWidth
                videoView.layoutParams.height = screenHeight
            }
            videoView.requestLayout()
        }

        videoView.setOnCompletionListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }

        videoView.start()
    }
}
