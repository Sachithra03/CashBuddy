package com.example.cashbuddy

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.cashbuddy.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide status bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        // Initialize UserManager
        userManager = UserManager(this)

        // Start animations
        startAnimations()

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000) // 2 seconds delay
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val scaleUp = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        binding.appLogo.startAnimation(fadeIn)
        binding.appName.startAnimation(scaleUp)
        binding.progressBar.startAnimation(fadeIn)
    }

    private fun navigateToNextScreen() {
        val currentUser = userManager.getCurrentUser()
        val intent = if (currentUser != null) {
            // User is logged in, go to MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // User is not logged in, go to LoginActivity
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
} 