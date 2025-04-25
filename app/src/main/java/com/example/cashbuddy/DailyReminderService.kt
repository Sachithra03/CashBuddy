package com.example.cashbuddy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.util.*

class DailyReminderService(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(context)

    fun scheduleDailyReminder(hour: Int = 20, minute: Int = 0) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        // Save reminder state and time
        prefs.edit().apply {
            putBoolean("daily_reminder_enabled", true)
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
            apply()
        }
    }

    fun cancelDailyReminder() {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // Save reminder state
        prefs.edit().putBoolean("daily_reminder_enabled", false).apply()
    }

    fun isReminderEnabled(): Boolean {
        return prefs.getBoolean("daily_reminder_enabled", false)
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        if (enabled) {
            val hour = getReminderHour()
            val minute = getReminderMinute()
            scheduleDailyReminder(hour, minute)
        } else {
            cancelDailyReminder()
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().apply {
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
            apply()
        }
    }

    fun getReminderHour(): Int {
        return prefs.getInt("reminder_hour", 20) // Default to 8 PM
    }

    fun getReminderMinute(): Int {
        return prefs.getInt("reminder_minute", 0)
    }
} 