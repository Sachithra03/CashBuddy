package com.example.cashbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var signupButton: MaterialButton
    private lateinit var loginTextView: TextView
    private lateinit var userManager: UserManager

    // Password pattern: at least 8 characters, 1 number, 1 symbol, 1 capital letter
    private val PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(?=.*[A-Z]).{8,}$"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Hide status bar and action bar
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        userManager = UserManager(this)
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        signupButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)
    }

    private fun setupClickListeners() {
        signupButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                val user = User(name, email, password)
                if (userManager.registerUser(user)) {
                    // Navigate to MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginTextView.setOnClickListener {
            finish() // Go back to login screen
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        // Validate name
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            return false
        }

        // Validate email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email address"
            return false
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            passwordEditText.error = "Password must be at least 8 characters and contain at least 1 number, 1 symbol, and 1 capital letter"
            return false
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return false
        }

        return true
    }
} 