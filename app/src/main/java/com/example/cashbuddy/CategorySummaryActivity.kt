package com.example.cashbuddy

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

class CategorySummaryActivity : AppCompatActivity() {
    private lateinit var expensePieChart: PieChart
    private lateinit var incomePieChart: PieChart
    private lateinit var totalExpensesTextView: TextView
    private lateinit var totalIncomeTextView: TextView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var userManager: UserManager
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_summary)

        // Check if user is logged in
        userManager = UserManager(this)
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupToolbar()
        initializeViews()
        setupBottomNavigation()
        loadCategoryData()
    }

    override fun onResume() {
        super.onResume()
        // Check login state on resume
        if (userManager.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        loadCategoryData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<TextView>(R.id.toolbarTitle).text = "Category Summary"
    }

    private fun initializeViews() {
        expensePieChart = findViewById(R.id.expensePieChart)
        incomePieChart = findViewById(R.id.incomePieChart)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView)
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)

        setupPieChart(expensePieChart, "Expenses by Category")
        setupPieChart(incomePieChart, "Income by Category")
    }

    private fun setupPieChart(pieChart: PieChart, title: String) {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            centerText = title
            setCenterTextSize(16f)
            legend.isEnabled = true
            legend.textSize = 12f
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            animateY(1000)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_view -> {
                    startActivity(Intent(this, ViewTransactionsActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_budget -> {
                    startActivity(Intent(this, UpdateBudgetActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_summary -> {
                    true
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.menu.findItem(R.id.navigation_summary).isChecked = true
        bottomNavigationView.setItemIconTintList(getColorStateList(R.color.navigation_selector))
        bottomNavigationView.setItemTextColor(getColorStateList(R.color.navigation_selector))
    }

    private fun loadCategoryData() {
        val currentUser = userManager.getCurrentUser() ?: return
        val sharedPreferences = getSharedPreferences("transactions_${currentUser.email}", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        val expenseCategories = mutableMapOf<String, Double>()
        val incomeCategories = mutableMapOf<String, Double>()
        var totalExpenses = 0.0
        var totalIncome = 0.0

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        allEntries.forEach { (key, value) ->
            if (key.startsWith("transaction_")) {
                try {
                    val jsonObject = JSONObject(value.toString())
                    val amount = jsonObject.getDouble("amount")
                    val type = jsonObject.getString("type")
                    val category = jsonObject.getString("category")
                    val date = jsonObject.getString("date")

                    val transactionDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                    val transactionCalendar = Calendar.getInstance()
                    transactionCalendar.time = transactionDate!!

                    if (transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(Calendar.YEAR) == currentYear) {
                        if (type == "expense") {
                            expenseCategories[category] = (expenseCategories[category] ?: 0.0) + amount
                            totalExpenses += amount
                        } else if (type == "income") {
                            incomeCategories[category] = (incomeCategories[category] ?: 0.0) + amount
                            totalIncome += amount
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        totalExpensesTextView.text = "Total Expenses: ${formatter.format(totalExpenses)}"
        totalIncomeTextView.text = "Total Income: ${formatter.format(totalIncome)}"

        updatePieChart(expensePieChart, expenseCategories)
        updatePieChart(incomePieChart, incomeCategories)

        val sortedCategories = expenseCategories.entries
            .sortedByDescending { it.value }
            .map { CategoryItem(it.key, it.value) }

        categoryRecyclerView.layoutManager = LinearLayoutManager(this)
        categoryRecyclerView.adapter = CategoryAdapter(sortedCategories)
    }

    private fun updatePieChart(pieChart: PieChart, categoryData: Map<String, Double>) {
        val entries = categoryData.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
        }

        pieChart.data = PieData(dataSet)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    data class CategoryItem(
        val name: String,
        val amount: Double
    )

    private inner class CategoryAdapter(
        private val categories: List<CategoryItem>
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryNameTextView: TextView = view.findViewById(R.id.categoryNameTextView)
            val categoryAmountTextView: TextView = view.findViewById(R.id.categoryAmountTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.categoryNameTextView.text = category.name
            holder.categoryAmountTextView.text = formatter.format(category.amount)
        }

        override fun getItemCount() = categories.size
    }
}