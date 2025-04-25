package com.example.cashbuddy

data class User(
    val name: String,
    val email: String,
    val password: String
) {
    companion object {
        fun fromSharedPreferences(prefs: android.content.SharedPreferences): User? {
            val name = prefs.getString("name", null)
            val email = prefs.getString("email", null)
            val password = prefs.getString("password", null)

            return if (name != null && email != null && password != null) {
                User(name, email, password)
            } else {
                null
            }
        }
    }
} 