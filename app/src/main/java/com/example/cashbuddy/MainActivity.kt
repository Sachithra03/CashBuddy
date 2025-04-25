package com.example.cashbuddy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cashbuddy.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userManager: UserManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var sharedPreferences: SharedPreferences

    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val transactionDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val NOTIFICATION_PERMISSION_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dark mode
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)
        notificationHelper = NotificationHelper(this)

        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }

        // Hide status bar and action bar using modern approach
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        supportActionBar?.hide()

        initializeViews()
        setupBottomNavigation()
        setupClickListeners()
        updateDashboard()
        showDailyReminder()
    }

    private fun initializeViews() {
        // Set current date
        binding.currentDateTextView.text = dateFormat.format(Date())

        // Set username
        val currentUser = userManager.getCurrentUser()
        binding.usernameTextView.text = currentUser?.name ?: "User"

        // Setup RecyclerView
        binding.recentTransactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentTransactionsRecyclerView.adapter = TransactionAdapter(emptyList())
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_view -> {
                    startActivity(Intent(this, ViewTransactionsActivity::class.java))
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, UpdateBudgetActivity::class.java))
                    true
                }
                R.id.navigation_summary -> {
                    startActivity(Intent(this, CategorySummaryActivity::class.java))
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.menu.findItem(R.id.navigation_home).isChecked = true
    }

    private fun setupClickListeners() {
        binding.addTransactionButton.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun updateDashboard() {
        val currentUser = userManager.getCurrentUser() ?: return
        val currentDate = transactionDateFormat.format(Date())
        val monthKey = currentDate.substring(0, 7) // yyyy-MM

        // Get budget
        val budgetPrefs = getSharedPreferences("budget_${currentUser.email}", Context.MODE_PRIVATE)
        val budget = budgetPrefs.getFloat("monthly_budget", 0f)

        // Get monthly summary
        val monthlyPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
        val income = monthlyPrefs.getFloat("${monthKey}_income", 0f)
        val expense = monthlyPrefs.getFloat("${monthKey}_expense", 0f)

        // Update budget progress
        if (budget > 0) {
            val percentage = (expense / budget * 100).toInt()
            binding.budgetProgressBar.progress = percentage
            binding.budgetStatusTextView.text = "${formatter.format(expense)} / ${formatter.format(budget)}"
            binding.percentageTextView.text = "$percentage% spent"
            
            // Update progress bar color based on percentage
            val progressColor = when {
                percentage >= 90 -> getColor(R.color.error)
                percentage >= 75 -> getColor(R.color.warning)
                else -> getColor(R.color.success)
            }
            binding.budgetProgressBar.progressTintList = ColorStateList.valueOf(progressColor)

            // Check and send budget notifications
            when {
                percentage >= 100 -> {
                    notificationHelper.showBudgetExceeded(expense.toDouble(), budget.toDouble())
                }
                percentage >= 75 -> {
                    notificationHelper.showBudgetWarning(percentage, expense.toDouble(), budget.toDouble())
                }
            }
        } else {
            binding.budgetStatusTextView.text = "No budget set"
            binding.percentageTextView.text = "0% spent"
            binding.budgetProgressBar.progress = 0
            binding.budgetProgressBar.progressTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
        }

        // Update income and expense
        binding.incomeStatusTextView.text = formatter.format(income)
        binding.expenseStatusTextView.text = formatter.format(expense)

        // Update recent transactions
        val transactionPrefs = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val transactions = mutableListOf<Transaction>()

        transactionPrefs.all.forEach { (key, value) ->
            if (key.startsWith("transaction_")) {
                try {
                    val json = JSONObject(value as String)
                    val date = json.getString("date")
                    if (date.startsWith(monthKey)) {
                        transactions.add(Transaction(
                            amount = json.getDouble("amount"),
                            description = json.getString("description"),
                            type = json.getString("type"),
                            category = json.getString("category"),
                            date = date
                        ))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Sort by date (newest first) and take last 5
        val recentTransactions = transactions.sortedByDescending { it.date }.take(5)
        (binding.recentTransactionsRecyclerView.adapter as TransactionAdapter).updateTransactions(recentTransactions)
    }

    private fun showDailyReminder() {
        notificationHelper.showDailyReminder()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    data class Transaction(
        val amount: Double,
        val description: String,
        val type: String,
        val category: String,
        val date: String
    )

    class TransactionAdapter(private var transactions: List<Transaction>) : 
        RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val amountTextView: TextView = view.findViewById(R.id.amountTextView)
            val descriptionTextView: TextView = view.findViewById(R.id.descriptionTextView)
            val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = transactions[position]
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
            
            holder.amountTextView.text = formatter.format(transaction.amount)
            holder.amountTextView.setTextColor(
                if (transaction.type == "income") 
                    holder.itemView.context.getColor(R.color.success)
                else 
                    holder.itemView.context.getColor(R.color.error)
            )
            
            holder.descriptionTextView.text = transaction.description
            holder.dateTextView.text = transaction.date
        }

        override fun getItemCount() = transactions.size

        fun updateTransactions(newTransactions: List<Transaction>) {
            transactions = newTransactions
            notifyDataSetChanged()
        }
    }
}