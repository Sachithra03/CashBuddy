package com.example.cashbuddy

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ViewTransactionsActivity : AppCompatActivity() {
    private lateinit var transactionsRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var userManager: UserManager
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    private var transactions = mutableListOf<Transaction>()
    private var currentFilter = "all"

    data class Transaction(
        val key: String,
        val amount: Double,
        val title: String,
        val type: String,
        val category: String,
        val date: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_transactions)

        // Check if user is logged in
        userManager = UserManager(this)
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Set up add transaction button
        findViewById<FloatingActionButton>(R.id.addTransactionButton).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        setupToolbar()
        initializeViews()
        setupBottomNavigation()
        setupFilterSpinner()
        loadTransactions()
    }

    override fun onResume() {
        super.onResume()
        // Check login state on resume
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        loadTransactions()
    }

    private fun setupToolbar() {
        // Set toolbar title
        findViewById<TextView>(R.id.toolbarTitle).text = "Transactions"
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_view -> true
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
        bottomNavigationView.setItemIconTintList(getColorStateList(R.color.navigation_selector))
        bottomNavigationView.setItemTextColor(getColorStateList(R.color.navigation_selector))
    }

    private fun initializeViews() {
        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        filterSpinner = findViewById(R.id.filterSpinner)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("All", "Income", "Expenses")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = when (position) {
                    0 -> "all"
                    1 -> "income"
                    2 -> "expense"
                    else -> "all"
                }
                loadTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentFilter = "all"
                loadTransactions()
            }
        }
    }

    private fun loadTransactions() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        transactions.clear()

        allEntries.forEach { (key, value) ->
            if (key.startsWith("transaction_")) {
                try {
                    val jsonObject = JSONObject(value.toString())
                    val amount = jsonObject.getDouble("amount")
                    val title = jsonObject.getString("description")
                    val type = jsonObject.getString("type").lowercase(Locale.ROOT)
                    val category = jsonObject.getString("category")
                    val date = jsonObject.getString("date")

                    if (currentFilter == "all" || currentFilter == type) {
                        transactions.add(Transaction(key, amount, title, type, category, date))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        transactions.sortByDescending { it.date }

        if (transactions.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            transactionsRecyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            transactionsRecyclerView.visibility = View.VISIBLE
            transactionsRecyclerView.adapter = TransactionAdapter(transactions)
        }
    }

    private inner class TransactionAdapter(private var transactions: List<Transaction>) : 
        RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val amountTextView: TextView = view.findViewById(R.id.amountTextView)
            val descriptionTextView: TextView = view.findViewById(R.id.descriptionTextView)
            val typeTextView: TextView = view.findViewById(R.id.typeTextView)
            val dateTextView: TextView = view.findViewById(R.id.dateTextView)
            val cardView: MaterialCardView = view.findViewById(R.id.transactionCard)

            init {
                // Single click for edit
                view.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val transaction = transactions[position]
                        launchEditTransaction(transaction)
                    }
                }

                // Long press for delete
                view.setOnLongClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val transaction = transactions[position]
                        showDeleteConfirmation(transaction)
                    }
                    true
                }
            }

            private fun launchEditTransaction(transaction: Transaction) {
                val intent = Intent(this@ViewTransactionsActivity, EditTransactionActivity::class.java).apply {
                    putExtra("transaction_key", transaction.key)
                    putExtra("amount", transaction.amount)
                    putExtra("description", transaction.title)
                    putExtra("type", transaction.type)
                    putExtra("category", transaction.category)
                    putExtra("date", transaction.date)
                }
                startActivity(intent)
            }

            private fun showDeleteConfirmation(transaction: Transaction) {
                val transactionType = if (transaction.type == "income") "Income" else "Expense"
                
                AlertDialog.Builder(this@ViewTransactionsActivity)
                    .setTitle("Delete $transactionType")
                    .setMessage("Are you sure you want to delete this $transactionType?")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteTransaction(transaction)
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .apply {
                        setOnShowListener {
                            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.error))
                            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.text_secondary))
                        }
                    }
                    .show()
            }

            private fun deleteTransaction(transaction: Transaction) {
                val currentUser = userManager.getCurrentUser() ?: return
                
                // Remove from transactions
                val transactionPrefs = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
                transactionPrefs.edit().remove(transaction.key).apply()
                
                // Update monthly summary
                val monthKey = transaction.date.substring(0, 7)
                val monthlyPrefs = getSharedPreferences("monthly_summary_${currentUser.email}", Context.MODE_PRIVATE)
                val currentIncome = monthlyPrefs.getFloat("${monthKey}_income", 0f)
                val currentExpense = monthlyPrefs.getFloat("${monthKey}_expense", 0f)
                
                with(monthlyPrefs.edit()) {
                    if (transaction.type == "income") {
                        putFloat("${monthKey}_income", currentIncome - transaction.amount.toFloat())
                    } else {
                        putFloat("${monthKey}_expense", currentExpense - transaction.amount.toFloat())
                    }
                    apply()
                }
                
                // Update category summary
                val categoryPrefs = getSharedPreferences("categories_${currentUser.email}", Context.MODE_PRIVATE)
                val categoryKey = "${transaction.category}_${transaction.type}"
                val currentCategoryAmount = categoryPrefs.getFloat(categoryKey, 0f)
                categoryPrefs.edit().putFloat(categoryKey, currentCategoryAmount - transaction.amount.toFloat()).apply()
                
                // Show success message and refresh
                Toast.makeText(
                    this@ViewTransactionsActivity,
                    "${transaction.type.capitalize()} deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()
                
                loadTransactions()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val transaction = transactions[position]
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
            
            // Set amount with color based on type
            holder.amountTextView.text = formatter.format(transaction.amount)
            holder.amountTextView.setTextColor(
                if (transaction.type == "income") 
                    holder.itemView.context.getColor(R.color.income_green)
                else 
                    holder.itemView.context.getColor(R.color.expense_red)
            )
            
            // Set other transaction details
            holder.descriptionTextView.text = transaction.title
            holder.typeTextView.text = transaction.type.capitalize()
            holder.dateTextView.text = formatDate(transaction.date)
            
            // Set card background based on type
            holder.cardView.setCardBackgroundColor(
                if (transaction.type == "income")
                    holder.itemView.context.getColor(R.color.income_background)
                else
                    holder.itemView.context.getColor(R.color.expense_background)
            )
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date)
            } catch (e: Exception) {
                dateString
            }
        }

        override fun getItemCount() = transactions.size

        fun updateTransactions(newTransactions: List<Transaction>) {
            transactions = newTransactions
            notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}