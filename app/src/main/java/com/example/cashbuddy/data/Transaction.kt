package com.example.cashbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val title: String,
    val type: String, // "income" or "expense"
    val category: String,
    val date: Date,
    val userId: String // Email of the user who owns this transaction
) 