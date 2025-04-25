package com.example.cashbuddy

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray


class UserManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("users", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun registerUser(user: User): Boolean {
        // Check if user already exists
        if (getUserByEmail(user.email) != null) {
            return false
        }

        // Get existing users
        val users = getAllUsers().toMutableList()
        
        // Add new user
        users.add(user)
        
        // Save updated users list
        saveUsers(users)
        
        // Set current user
        setCurrentUser(user.email)
        
        return true
    }

    fun loginUser(email: String, password: String): Boolean {
        val user = getUserByEmail(email)
        return if (user != null && user.password == password) {
            setCurrentUser(email)
            true
        } else {
            false
        }
    }

    fun logoutUser() {
        sharedPreferences.edit().remove("current_user_email").apply()
    }

    fun getCurrentUser(): User? {
        val currentUserEmail = sharedPreferences.getString("current_user_email", null)
        return if (currentUserEmail != null) {
            getUserByEmail(currentUserEmail)
        } else {
            null
        }
    }

    private fun setCurrentUser(email: String) {
        sharedPreferences.edit().putString("current_user_email", email).apply()
    }

    private fun getUserByEmail(email: String): User? {
        return getAllUsers().find { it.email == email }
    }

    private fun getAllUsers(): List<User> {
        val usersJson = sharedPreferences.getString("users_list", "[]")
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(usersJson, type)
    }

    private fun saveUsers(users: List<User>) {
        val usersJson = gson.toJson(users)
        sharedPreferences.edit().putString("users_list", usersJson).apply()
    }

    fun verifyPassword(email: String, password: String): Boolean {
        val user = getUserByEmail(email)
        return user?.password == password
    }

    fun updatePassword(email: String, newPassword: String) {
        val users = getAllUsers().toMutableList()
        val userIndex = users.indexOfFirst { it.email == email }
        if (userIndex != -1) {
            val user = users[userIndex]
            users[userIndex] = User(user.name, user.email, newPassword)
            saveUsers(users)
        }
    }

    fun deleteUser(email: String) {
        val usersJson = sharedPreferences.getString("users_list", "[]")
        val usersArray = JSONArray(usersJson)
        
        // Find and remove the user
        for (i in 0 until usersArray.length()) {
            val userJson = usersArray.getJSONObject(i)
            if (userJson.getString("email") == email) {
                usersArray.remove(i)
                break
            }
        }
        
        // Save updated users list
        sharedPreferences.edit().putString("users_list", usersArray.toString()).apply()
        
        // Clear current user if it's the deleted user
        val currentUser = getCurrentUser()
        if (currentUser?.email == email) {
            sharedPreferences.edit().remove("current_user_email").apply()
        }
    }
} 