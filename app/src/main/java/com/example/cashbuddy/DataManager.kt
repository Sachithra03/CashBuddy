package com.example.cashbuddy

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DataManager(private val context: Context) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    companion object {
        private const val EXPORT_DIRECTORY = "CashBuddy"
        private const val EXPORT_FILENAME = "cashbuddy_backup_"
    }

    fun backupData(): Boolean {
        try {
            // Create backup directory if it doesn't exist
            val exportDir = File(context.getExternalFilesDir(null), EXPORT_DIRECTORY)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Create backup file with timestamp
            val timestamp = dateFormat.format(Date())
            val exportFile = File(exportDir, "${EXPORT_FILENAME}$timestamp.json")

            // Create backup data object
            val backupData = BackupData(
                transactions = getAllTransactions(),
                budgets = getAllBudgets(),
                incomes = getAllIncomes(),
                expenses = getAllExpenses(),
                categories = getAllCategories()
            )

            // Convert to JSON and write to file
            val jsonString = gson.toJson(backupData)
            FileOutputStream(exportFile).use { output ->
                output.write(jsonString.toByteArray())
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun restoreData(filePath: String): Boolean {
        try {
            val importFile = File(filePath)
            if (!importFile.exists()) {
                return false
            }

            // Read backup file
            val jsonString = FileInputStream(importFile).bufferedReader().use { it.readText() }
            val backupData = gson.fromJson(jsonString, BackupData::class.java)

            // Restore data
            restoreTransactions(backupData.transactions)
            restoreBudgets(backupData.budgets)
            restoreIncomes(backupData.incomes)
            restoreExpenses(backupData.expenses)
            restoreCategories(backupData.categories)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun resetData() {
        // Clear all shared preferences
        val prefsToClear = listOf(
            "transactions",
            "budgets",
            "incomes",
            "expenses",
            "categories",
            "monthly_summary",
            "notifications"
        )

        prefsToClear.forEach { prefName ->
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        // Delete database
        context.deleteDatabase("cashbuddy.db")

        // Delete backup files
        val backupDir = File(context.getExternalFilesDir(null), EXPORT_DIRECTORY)
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    }

    private fun getAllTransactions(): Map<String, String> {
        return context.getSharedPreferences("transactions", Context.MODE_PRIVATE).all as Map<String, String>
    }

    private fun getAllBudgets(): Map<String, String> {
        return context.getSharedPreferences("budgets", Context.MODE_PRIVATE).all as Map<String, String>
    }

    private fun getAllIncomes(): Map<String, String> {
        return context.getSharedPreferences("incomes", Context.MODE_PRIVATE).all as Map<String, String>
    }

    private fun getAllExpenses(): Map<String, String> {
        return context.getSharedPreferences("expenses", Context.MODE_PRIVATE).all as Map<String, String>
    }

    private fun getAllCategories(): Map<String, String> {
        return context.getSharedPreferences("categories", Context.MODE_PRIVATE).all as Map<String, String>
    }

    private fun restoreTransactions(transactions: Map<String, String>) {
        val editor = context.getSharedPreferences("transactions", Context.MODE_PRIVATE).edit()
        editor.clear()
        transactions.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    private fun restoreBudgets(budgets: Map<String, String>) {
        val editor = context.getSharedPreferences("budgets", Context.MODE_PRIVATE).edit()
        editor.clear()
        budgets.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    private fun restoreIncomes(incomes: Map<String, String>) {
        val editor = context.getSharedPreferences("incomes", Context.MODE_PRIVATE).edit()
        editor.clear()
        incomes.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    private fun restoreExpenses(expenses: Map<String, String>) {
        val editor = context.getSharedPreferences("expenses", Context.MODE_PRIVATE).edit()
        editor.clear()
        expenses.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    private fun restoreCategories(categories: Map<String, String>) {
        val editor = context.getSharedPreferences("categories", Context.MODE_PRIVATE).edit()
        editor.clear()
        categories.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.apply()
    }

    data class BackupData(
        val transactions: Map<String, String>,
        val budgets: Map<String, String>,
        val incomes: Map<String, String>,
        val expenses: Map<String, String>,
        val categories: Map<String, String>
    )
} 