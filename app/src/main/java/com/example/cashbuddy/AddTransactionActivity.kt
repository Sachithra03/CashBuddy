package com.example.cashbuddy

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var transactionTypeRadioGroup: RadioGroup
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var saveButton: Button
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var userManager: UserManager
    private lateinit var viewSummaryButton: Button

    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    private val incomeCategories = listOf(
        "Salary", "Investment Profit", "Rental Income", "Other Income"
    )
    private val expenseCategories = listOf(
        "Food & Dining", "Transportation", "Shopping", "Entertainment",
        "Bills & Utilities", "Health & Medical", "Travel", "Education",
        "Personal Care", "Gifts & Donations", "Other"
    )


    private var isEditMode = false
    private var transactionKey: String? = null
    private var oldAmount: Double = 0.0
    private var oldType: String = ""
    private var oldCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        userManager = UserManager(this)
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        notificationHelper = NotificationHelper(this)
        initializeViews()
        setupBottomNavigation()
        setupCategoryDropdown()
        setupClickListeners()
        loadTransactionData()

        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Set up category adapter
        val categories = resources.getStringArray(R.array.categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryAutoComplete.setAdapter(adapter)

        // Set up date picker
        findViewById<EditText>(R.id.dateEditText).setOnClickListener {
            showDatePicker()
        }

        // Set up save button
        saveButton.setOnClickListener {
            saveTransaction()
        }
    }

    private fun initializeViews() {
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        dateEditText = findViewById(R.id.dateEditText)
        transactionTypeRadioGroup = findViewById(R.id.transactionTypeRadioGroup)
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete)
        saveButton = findViewById(R.id.saveButton)
        viewSummaryButton = findViewById(R.id.viewSummaryButton)
        
        // Set toolbar title
        findViewById<TextView>(R.id.toolbarTitle).text = "Add Transaction"

        // Set current date by default
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        dateEditText.setText(currentDate)

        // Set up date picker
        dateEditText.setOnClickListener {
            showDatePicker()
        }

        // Set up category adapter
        val incomeCategories = arrayOf(
            "Salary",
            "Bonus",
            "Investment",
            "Freelance",
            "Business",
            "Rental",
            "Other Income"
        )
        val expenseCategories = arrayOf(
            "Food",
            "Transport",
            "Shopping",
            "Entertainment",
            "Bills",
            "Health",
            "Education",
            "Other"
        )

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, incomeCategories)
        categoryAutoComplete.setAdapter(categoryAdapter)

        // Update categories when transaction type changes
        transactionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val categories = if (checkedId == R.id.incomeRadioButton) incomeCategories else expenseCategories
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            categoryAutoComplete.setAdapter(adapter)
            categoryAutoComplete.setText("", false)
        }

        // Set up save button
        saveButton.setOnClickListener {
            saveTransaction()
        }

        // Set up view summary button
        viewSummaryButton.setOnClickListener {
            startActivity(Intent(this, ViewTransactionsActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
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
        bottomNavigationView.menu.findItem(R.id.navigation_view).isChecked = true
    }

    private fun setupCategoryDropdown() {
        // Get categories from resources
        val incomeCategories = resources.getStringArray(R.array.income_categories)
        val expenseCategories = resources.getStringArray(R.array.expense_categories)

        // Set initial categories based on selected radio button
        val isIncome = findViewById<RadioButton>(R.id.incomeRadioButton).isChecked
        val initialCategories = if (isIncome) incomeCategories else expenseCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, initialCategories)
        categoryAutoComplete.setAdapter(adapter)

        // Update categories when transaction type changes
        transactionTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val categories = if (checkedId == R.id.incomeRadioButton) incomeCategories else expenseCategories
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            categoryAutoComplete.setAdapter(adapter)
            categoryAutoComplete.setText("") // Clear the text when type changes
        }
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveTransaction()
        }
    }

    private fun loadTransactionData() {
        isEditMode = intent.getBooleanExtra("is_edit", false)
        if (isEditMode) {
            transactionKey = intent.getStringExtra("transaction_key")
            val amount = intent.getDoubleExtra("amount", 0.0)
            val description = intent.getStringExtra("description") ?: ""
            val type = intent.getStringExtra("type") ?: ""
            val category = intent.getStringExtra("category") ?: ""
            val date = intent.getStringExtra("date") ?: ""

            oldAmount = amount
            oldType = type
            oldCategory = category

            amountEditText.setText(amount.toString())
            descriptionEditText.setText(description)
            if (type == "income") {
                findViewById<RadioButton>(R.id.incomeRadioButton).isChecked = true
            } else {
                findViewById<RadioButton>(R.id.expenseRadioButton).isChecked = true
            }
            categoryAutoComplete.setText(category)
            
            // Update toolbar title
            findViewById<TextView>(R.id.toolbarTitle).text = "Edit Transaction"
        }
    }

    private fun saveTransaction() {
        val amount = amountEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val isIncome = findViewById<RadioButton>(R.id.incomeRadioButton).isChecked
        val selectedCategory = categoryAutoComplete.text.toString()

        if (amount.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory.isBlank()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val amountValue = amount.toDouble()
            val currentUser = userManager.getCurrentUser() ?: return
            val prefs = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val date = if (isEditMode) intent.getStringExtra("date") ?: "" 
                      else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val type = if (isIncome) "income" else "expense"
            val key = if (isEditMode) transactionKey!! else "transaction_${System.currentTimeMillis()}"

            val transactionData = JSONObject().apply {
                put("amount", amountValue)
                put("description", description)
                put("type", type)
                put("category", selectedCategory)
                put("date", date)
            }

            editor.putString(key, transactionData.toString()).apply()

            if (isEditMode) {
                // Update monthly summary by removing old amount and adding new amount
                val monthKey = date.substring(0, 7)
                val monthlyPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
                val currentIncome = monthlyPrefs.getFloat("${monthKey}_income", 0f)
                val currentExpense = monthlyPrefs.getFloat("${monthKey}_expense", 0f)
                
                with(monthlyPrefs.edit()) {
                    if (oldType == "income") {
                        putFloat("${monthKey}_income", currentIncome - oldAmount.toFloat())
                    } else {
                        putFloat("${monthKey}_expense", currentExpense - oldAmount.toFloat())
                    }
                    if (type == "income") {
                        putFloat("${monthKey}_income", currentIncome - oldAmount.toFloat() + amountValue.toFloat())
                    } else {
                        putFloat("${monthKey}_expense", currentExpense - oldAmount.toFloat() + amountValue.toFloat())
                    }
                    apply()
                }

                // Update category summary
                val categoryPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
                val oldCategoryKey = "${oldCategory}_${oldType}"
                val newCategoryKey = "${selectedCategory}_${type}"
                val currentOldCategoryAmount = categoryPrefs.getFloat(oldCategoryKey, 0f)
                val currentNewCategoryAmount = categoryPrefs.getFloat(newCategoryKey, 0f)
                
                with(categoryPrefs.edit()) {
                    putFloat(oldCategoryKey, currentOldCategoryAmount - oldAmount.toFloat())
                    putFloat(newCategoryKey, currentNewCategoryAmount + amountValue.toFloat())
                    apply()
                }
            } else {
                // Update monthly summary for new transaction
                updateMonthlySummary(date, amountValue, type)
                // Update category summary for new transaction
                updateCategorySummary(selectedCategory, amountValue, type)
            }

            // Show notification for expense transactions
            if (!isIncome) {
                notificationHelper.showDailyReminder()
            }

            Toast.makeText(this, if (isEditMode) "Transaction updated successfully" else "Transaction added successfully", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to ${if (isEditMode) "update" else "add"} transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMonthlySummary(date: String, amount: Double, type: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthKey = dateFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!)
            
            val currentUser = userManager.getCurrentUser() ?: return
            val prefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            if (type == "income") {
                val currentIncome = prefs.getFloat("${monthKey}_income", 0f)
                editor.putFloat("${monthKey}_income", (currentIncome + amount).toFloat())
            } else {
                val currentExpense = prefs.getFloat("${monthKey}_expense", 0f)
                editor.putFloat("${monthKey}_expense", (currentExpense + amount).toFloat())
            }
            
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error updating monthly summary: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCategorySummary(category: String, amount: Double, type: String) {
        try {
            val currentUser = userManager.getCurrentUser() ?: return
            val prefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            val categoryKey = "${category}_${type}"
            val currentAmount = prefs.getFloat(categoryKey, 0f)
            editor.putFloat(categoryKey, (currentAmount + amount).toFloat())
            
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error updating category summary: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                findViewById<EditText>(R.id.dateEditText).setText(dateFormat.format(selectedDate.time))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}