package com.example.cashbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.cashbuddy.databinding.ActivitySettingsBinding
import android.content.SharedPreferences
import android.widget.ImageButton

class SettingsActivity : AppCompatActivity() {
    private lateinit var logoutButton: MaterialButton
    private lateinit var deleteAccountButton: MaterialButton
    private lateinit var notificationSwitch: SwitchMaterial
    private lateinit var darkModeSwitch: SwitchMaterial
    private lateinit var privacyPolicyTextView: TextView
    private lateinit var termsOfServiceTextView: TextView
    private lateinit var userManager: UserManager
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var backupButton: MaterialButton
    private lateinit var restoreButton: MaterialButton
    private lateinit var resetAppButton: MaterialButton
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var dataManager: DataManager
    private lateinit var dailyReminderService: DailyReminderService
    private lateinit var dailyReminderSwitch: SwitchMaterial
    private lateinit var reminderTimeButton: MaterialButton
    private lateinit var reminderTimeText: TextView
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize helpers
        notificationHelper = NotificationHelper(this)
        dataManager = DataManager(this)
        dailyReminderService = DailyReminderService(this)

        // Check if user is logged in
        userManager = UserManager(this)
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Hide status bar and action bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()

        setupBottomNavigation()
        initializeViews()
        setupClickListeners()
        loadSettings()
        updateProfileInfo()

        // Set up daily reminder toggle and time picker
        dailyReminderSwitch = findViewById(R.id.dailyReminderSwitch)
        reminderTimeButton = findViewById(R.id.reminderTimeButton)
        reminderTimeText = findViewById(R.id.reminderTimeText)
        
        // Load saved reminder time
        val savedHour = dailyReminderService.getReminderHour()
        val savedMinute = dailyReminderService.getReminderMinute()
        updateReminderTimeText(savedHour, savedMinute)
        
        dailyReminderSwitch.isChecked = dailyReminderService.isReminderEnabled()
        
        dailyReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            dailyReminderService.setReminderEnabled(isChecked)
            if (isChecked) {
                Toast.makeText(this, "Daily reminder enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Daily reminder disabled", Toast.LENGTH_SHORT).show()
            }
        }

        reminderTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        setupDarkModeSwitch()
        setupBackButton()
    }

    private fun initializeViews() {
        logoutButton = findViewById(R.id.logoutButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)
        notificationSwitch = findViewById(R.id.notificationSwitch)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        privacyPolicyTextView = findViewById(R.id.privacyPolicyTextView)
        termsOfServiceTextView = findViewById(R.id.termsOfServiceTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        backupButton = findViewById(R.id.backupButton)
        restoreButton = findViewById(R.id.restoreButton)
        resetAppButton = findViewById(R.id.resetAppButton)
        
        // Set toolbar title
        findViewById<TextView>(R.id.toolbarTitle).text = "Settings"
    }

    private fun updateProfileInfo() {
        val currentUser = userManager.getCurrentUser() ?: return
        usernameTextView.text = "Username: ${currentUser.name}"
        emailTextView.text = "Email: ${currentUser.email}"
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    if (this::class.java != MainActivity::class.java) {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    true
                }
                R.id.navigation_view -> {
                    if (this::class.java != ViewTransactionsActivity::class.java) {
                        startActivity(Intent(this, ViewTransactionsActivity::class.java))
                    }
                    true
                }
                R.id.navigation_budget -> {
                    if (this::class.java != UpdateBudgetActivity::class.java) {
                        startActivity(Intent(this, UpdateBudgetActivity::class.java))
                    }
                    true
                }
                R.id.navigation_summary -> {
                    if (this::class.java != CategorySummaryActivity::class.java) {
                        startActivity(Intent(this, CategorySummaryActivity::class.java))
                    }
                    true
                }
                R.id.navigation_settings -> {
                    if (this::class.java != SettingsActivity::class.java) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.menu.findItem(R.id.navigation_settings).isChecked = true
    }

    private fun setupClickListeners() {
        logoutButton.setOnClickListener {
            // Logout user
            userManager.logoutUser()

            // Show logout message
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationPreference(isChecked)
            if (isChecked) {
                notificationHelper.showDailyReminder()
            }
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveDarkModePreference(isChecked)
        }

        privacyPolicyTextView.setOnClickListener {
            // TODO: Implement privacy policy screen
            Toast.makeText(this, "Privacy Policy coming soon!", Toast.LENGTH_SHORT).show()
        }

        termsOfServiceTextView.setOnClickListener {
            // TODO: Implement terms of service screen
            Toast.makeText(this, "Terms of Service coming soon!", Toast.LENGTH_SHORT).show()
        }

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        backupButton.setOnClickListener {
            if (dataManager.backupData()) {
                notificationHelper.showBackupSuccess()
                Toast.makeText(this, "Backup completed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }

        restoreButton.setOnClickListener {
            val exportDir = File(getExternalFilesDir(null), "CashBuddy")
            if (!exportDir.exists() || exportDir.listFiles()?.isEmpty() == true) {
                Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the most recent backup file
            val backupFiles = exportDir.listFiles { file -> 
                file.name.startsWith("cashbuddy_backup_") && file.name.endsWith(".json") 
            }
            val mostRecentFile = backupFiles?.maxByOrNull { it.lastModified() }

            if (mostRecentFile != null) {
                if (dataManager.restoreData(mostRecentFile.absolutePath)) {
                    notificationHelper.showRestoreSuccess()
                    Toast.makeText(this, "Data restored successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Restore failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            }
        }

        resetAppButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun loadSettings() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("settings_${currentUser.email}", MODE_PRIVATE)
        notificationSwitch.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        darkModeSwitch.isChecked = sharedPreferences.getBoolean("dark_mode", false)
    }

    private fun saveNotificationPreference(enabled: Boolean) {
        val currentUser = userManager.getCurrentUser() ?: return
        getSharedPreferences("settings_${currentUser.email}", MODE_PRIVATE)
            .edit()
            .putBoolean("notifications_enabled", enabled)
            .apply()
    }

    private fun saveDarkModePreference(enabled: Boolean) {
        val currentUser = userManager.getCurrentUser() ?: return
        getSharedPreferences("settings_${currentUser.email}", MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", enabled)
            .apply()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<TextInputLayout>(R.id.currentPasswordInput)
        val newPasswordInput = dialogView.findViewById<TextInputLayout>(R.id.newPasswordInput)
        val confirmPasswordInput = dialogView.findViewById<TextInputLayout>(R.id.confirmPasswordInput)
        val changePasswordButton = dialogView.findViewById<MaterialButton>(R.id.changePasswordButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        changePasswordButton.setOnClickListener {
            val currentPassword = currentPasswordInput.editText?.text.toString()
            val newPassword = newPasswordInput.editText?.text.toString()
            val confirmPassword = confirmPasswordInput.editText?.text.toString()

            // Reset error states
            currentPasswordInput.error = null
            newPasswordInput.error = null
            confirmPasswordInput.error = null

            var hasError = false

            // Validate current password
            if (currentPassword.isEmpty()) {
                currentPasswordInput.error = "Current password is required"
                hasError = true
            }

            // Validate new password
            if (newPassword.isEmpty()) {
                newPasswordInput.error = "New password is required"
                hasError = true
            } else if (newPassword.length < 6) {
                newPasswordInput.error = "Password must be at least 6 characters"
                hasError = true
            }

            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                confirmPasswordInput.error = "Please confirm your new password"
                hasError = true
            } else if (newPassword != confirmPassword) {
                confirmPasswordInput.error = "Passwords do not match"
                hasError = true
            }

            if (!hasError) {
                val currentUser = userManager.getCurrentUser() ?: return@setOnClickListener
                if (userManager.verifyPassword(currentUser.email, currentPassword)) {
                    userManager.updatePassword(currentUser.email, newPassword)
                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    currentPasswordInput.error = "Current password is incorrect"
                }
            }
        }

        dialog.show()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset App")
            .setMessage("Are you sure you want to reset the app? ")
            .setPositiveButton("Reset") { _, _ ->
                resetApp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetApp() {
        val currentUser = userManager.getCurrentUser() ?: return
        val prefs = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val budgetPrefs = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE)
        val categoriesPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
        val monthlySummaryPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)

        // Clear all data
        prefs.edit().clear().apply()
        budgetPrefs.edit().clear().apply()
        categoriesPrefs.edit().clear().apply()
        monthlySummaryPrefs.edit().clear().apply()

        // Show success message
        Toast.makeText(this, "App data has been reset successfully", Toast.LENGTH_SHORT).show()
        
        // Refresh the current activity to reflect changes
        recreate()
    }

    private fun showTimePickerDialog() {
        val savedHour = dailyReminderService.getReminderHour()
        val savedMinute = dailyReminderService.getReminderMinute()
        
        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                dailyReminderService.setReminderTime(hourOfDay, minute)
                updateReminderTimeText(hourOfDay, minute)
                if (dailyReminderService.isReminderEnabled()) {
                    dailyReminderService.scheduleDailyReminder(hourOfDay, minute)
                    Toast.makeText(this, "Reminder time updated", Toast.LENGTH_SHORT).show()
                }
            },
            savedHour,
            savedMinute,
            false
        )
        timePickerDialog.show()
    }

    private fun updateReminderTimeText(hour: Int, minute: Int) {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val formattedMinute = String.format("%02d", minute)
        reminderTimeText.text = "Reminder time: $displayHour:$formattedMinute $amPm"
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = userManager.getCurrentUser() ?: return

        // Delete all user data
        val prefs = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val budgetPrefs = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE)
        val categoriesPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
        val monthlySummaryPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
        val settingsPrefs = getSharedPreferences("settings_${currentUser.email}", Context.MODE_PRIVATE)
        val appSettingsPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Clear all preferences
        prefs.edit().clear().apply()
        budgetPrefs.edit().clear().apply()
        categoriesPrefs.edit().clear().apply()
        monthlySummaryPrefs.edit().clear().apply()
        settingsPrefs.edit().clear().apply()
        appSettingsPrefs.edit().clear().apply()

        // Delete user account
        userManager.deleteUser(currentUser.email)

        // Show success message
        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()

        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupDarkModeSwitch() {
        val darkModeSwitch = binding.darkModeSwitch
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        darkModeSwitch.isChecked = isDarkMode

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun setupBackButton() {
        binding.navToolbar.backButton.setOnClickListener {
            finish()
        }
    }
} 