package com.example.cashbuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UpdateBudgetActivity : AppCompatActivity() {
    private lateinit var budgetEditText: TextInputEditText
    private lateinit var saveBudgetButton: Button
    private lateinit var totalSpentTextView: TextView
    private lateinit var remainingBudgetTextView: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var percentageTextView: TextView
    private lateinit var userManager: UserManager
    private lateinit var notificationHelper: NotificationHelper
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_budget)

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

        notificationHelper = NotificationHelper(this)
        setupBottomNavigation()
        initializeViews()
        loadCurrentBudget()

        saveBudgetButton.setOnClickListener {
            saveBudget()
        }

        updateSpendingSummary()
        setupToolbar()

        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Set up add transaction button
        findViewById<FloatingActionButton>(R.id.addTransactionButton).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Check login state on resume
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        loadCurrentBudget()
        updateSpendingSummary()
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
        bottomNavigationView.menu.findItem(R.id.navigation_budget).isChecked = true
    }

    private fun initializeViews() {
        budgetEditText = findViewById(R.id.budgetEditText)
        saveBudgetButton = findViewById(R.id.saveBudgetButton)
        totalSpentTextView = findViewById(R.id.totalSpentTextView)
        remainingBudgetTextView = findViewById(R.id.remainingBudgetTextView)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        percentageTextView = findViewById(R.id.percentageTextView)
    }

    private fun loadCurrentBudget() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE)
        val currentBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (currentBudget > 0) {
            budgetEditText.setText(currentBudget.toString())
        }
    }

    private fun saveBudget() {
        val currentUser = userManager.getCurrentUser() ?: return
        val budgetText = budgetEditText.text.toString()
        if (budgetText.isEmpty()) {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val budget = budgetText.toFloat()
            val sharedPreferences = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE)
            sharedPreferences.edit().putFloat("monthly_budget", budget).apply()

            // Show notification for budget update
            notificationHelper.showBudgetWarning(0, 0.0, budget.toDouble())

            Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show()
            updateSpendingSummary()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSpendingSummary() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        var totalSpent = 0f

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        allEntries.forEach { (key, value) ->
            if (key.startsWith("transaction_")) {
                try {
                    val jsonObject = JSONObject(value.toString())
                    val amount = jsonObject.getDouble("amount")
                    val type = jsonObject.getString("type")
                    val date = jsonObject.getString("date")

                    val transactionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                    val transactionCalendar = Calendar.getInstance()
                    transactionCalendar.time = transactionDate!!

                    if (transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(Calendar.YEAR) == currentYear &&
                        type == "expense") {
                        totalSpent += amount.toFloat()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val monthlyBudget = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE).getFloat("monthly_budget", 0f)
        val remainingBudget = monthlyBudget - totalSpent
        val percentageSpent = (totalSpent / monthlyBudget * 100).toInt()

        totalSpentTextView.text = "Total Spent: ${formatter.format(totalSpent)}"
        remainingBudgetTextView.text = "Remaining: ${formatter.format(remainingBudget)}"
        budgetProgressBar.progress = percentageSpent.coerceIn(0, 100)
        percentageTextView.text = "$percentageSpent% of budget used"

        val progressColor = when {
            percentageSpent >= 90 -> android.graphics.Color.RED
            percentageSpent >= 75 -> android.graphics.Color.rgb(255, 165, 0) // Orange
            else -> android.graphics.Color.rgb(76, 175, 80) // Green
        }
        budgetProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

        // Check and notify budget status
        if (monthlyBudget > 0) {
            when {
                percentageSpent >= 100 -> {
                    notificationHelper.showBudgetExceeded(totalSpent.toDouble(), monthlyBudget.toDouble())
                }
                percentageSpent >= 75 -> {
                    notificationHelper.showBudgetWarning(percentageSpent, totalSpent.toDouble(), monthlyBudget.toDouble())
                }
            }
        }
    }

    private fun setupToolbar() {
        // Set toolbar title
        findViewById<TextView>(R.id.toolbarTitle).text = "Monthly Budget"
    }
}