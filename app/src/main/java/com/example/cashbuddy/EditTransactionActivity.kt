package com.example.cashbuddy

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class EditTransactionActivity : AppCompatActivity() {
    private lateinit var amountEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var categoryAutoCompleteTextView: AutoCompleteTextView
    private lateinit var dateEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var userManager: UserManager
    private var transactionKey: String = ""
    private var transactionType: String = ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        userManager = UserManager(this)
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        loadTransactionData()
        setupCategoryDropdown()
        setupDatePicker()
        setupSaveButton()
    }

    private fun initializeViews() {
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        categoryAutoCompleteTextView = findViewById(R.id.categoryAutoCompleteTextView)
        dateEditText = findViewById(R.id.dateEditText)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadTransactionData() {
        transactionKey = intent.getStringExtra("transaction_key") ?: return
        transactionType = intent.getStringExtra("type") ?: return

        amountEditText.setText(intent.getDoubleExtra("amount", 0.0).toString())
        descriptionEditText.setText(intent.getStringExtra("description"))
        categoryAutoCompleteTextView.setText(intent.getStringExtra("category"))
        dateEditText.setText(displayDateFormat.format(dateFormat.parse(intent.getStringExtra("date"))))
    }

    private fun setupCategoryDropdown() {
        val categories = when (transactionType) {
            "income" -> arrayOf("Salary", "Bonus", "Investment", "Other")
            else -> arrayOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Other")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categoryAutoCompleteTextView.setAdapter(adapter)
        categoryAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            categoryAutoCompleteTextView.setText(categories[position], false)
        }
    }

    private fun setupDatePicker() {
        dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(displayDateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (amountEditText.text.toString().isEmpty()) {
            amountEditText.error = "Amount is required"
            isValid = false
        }

        if (descriptionEditText.text.toString().isEmpty()) {
            descriptionEditText.error = "Description is required"
            isValid = false
        }

        if (categoryAutoCompleteTextView.text.toString().isEmpty()) {
            categoryAutoCompleteTextView.error = "Category is required"
            isValid = false
        }

        if (dateEditText.text.toString().isEmpty()) {
            dateEditText.error = "Date is required"
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        
        val amount = amountEditText.text.toString().toDouble()
        val description = descriptionEditText.text.toString()
        val category = categoryAutoCompleteTextView.text.toString()
        val date = dateFormat.format(displayDateFormat.parse(dateEditText.text.toString()))

        // Get the original transaction data
        val originalTransactionJson = sharedPreferences.getString(transactionKey, null)
        if (originalTransactionJson != null) {
            val originalJson = JSONObject(originalTransactionJson)
            val originalAmount = originalJson.getDouble("amount")
            val originalType = originalJson.getString("type")
            val originalCategory = originalJson.getString("category")
            val originalDate = originalJson.getString("date")

            // Update monthly summary by removing old amount
            val originalMonthKey = originalDate.substring(0, 7)
            val monthlyPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
            val currentIncome = monthlyPrefs.getFloat("${originalMonthKey}_income", 0f)
            val currentExpense = monthlyPrefs.getFloat("${originalMonthKey}_expense", 0f)

            with(monthlyPrefs.edit()) {
                if (originalType == "income") {
                    putFloat("${originalMonthKey}_income", currentIncome - originalAmount.toFloat())
                } else {
                    putFloat("${originalMonthKey}_expense", currentExpense - originalAmount.toFloat())
                }
                apply()
            }

            // Update category summary by removing old amount
            val categoryPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
            val originalCategoryKey = "${originalCategory}_${originalType}"
            val currentCategoryAmount = categoryPrefs.getFloat(originalCategoryKey, 0f)
            categoryPrefs.edit().putFloat(originalCategoryKey, currentCategoryAmount - originalAmount.toFloat()).apply()
        }

        // Create new transaction JSON
        val transactionJson = """
            {
                "amount": $amount,
                "description": "$description",
                "type": "$transactionType",
                "category": "$category",
                "date": "$date"
            }
        """.trimIndent()

        // Save updated transaction
        sharedPreferences.edit().putString(transactionKey, transactionJson).apply()

        // Update monthly summary with new amount
        val monthKey = date.substring(0, 7)
        val monthlyPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
        val currentIncome = monthlyPrefs.getFloat("${monthKey}_income", 0f)
        val currentExpense = monthlyPrefs.getFloat("${monthKey}_expense", 0f)

        with(monthlyPrefs.edit()) {
            if (transactionType == "income") {
                putFloat("${monthKey}_income", currentIncome + amount.toFloat())
            } else {
                putFloat("${monthKey}_expense", currentExpense + amount.toFloat())
            }
            apply()
        }

        // Update category summary with new amount
        val categoryPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
        val categoryKey = "${category}_${transactionType}"
        val currentCategoryAmount = categoryPrefs.getFloat(categoryKey, 0f)
        categoryPrefs.edit().putFloat(categoryKey, currentCategoryAmount + amount.toFloat()).apply()

        Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
        finish()
    }
}