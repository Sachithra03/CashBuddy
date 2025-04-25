package com.example.cashbuddy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getTransactionsByType(userId: String, type: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(userId: String, category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(userId: String, startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactions(userId: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(userId: String, type: String, startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type AND category = :category AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalAmountByTypeCategoryAndDateRange(userId: String, type: String, category: String, startDate: Date, endDate: Date): Double?
} 