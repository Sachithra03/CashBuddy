package com.example.cashbuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Show the daily reminder notification
        NotificationHelper(context).showDailyReminder()
    }
} 