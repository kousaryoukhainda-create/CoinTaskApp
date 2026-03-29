package com.cointask.utils

import android.content.Context
import android.content.SharedPreferences
import com.cointask.data.models.UserRole

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("coin_task_prefs", Context.MODE_PRIVATE)
    
    fun saveUserSession(userId: Int, email: String, role: UserRole) {
        prefs.edit().apply {
            putInt("user_id", userId)
            putString("email", email)
            putString("role", role.name)
            putBoolean("is_logged_in", true)
            apply()
        }
    }
    
    fun getUserId(): Int = prefs.getInt("user_id", -1)
    fun getUserEmail(): String = prefs.getString("email", "") ?: ""
    fun getUserRole(): UserRole = UserRole.valueOf(prefs.getString("role", "USER") ?: "USER")
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun clearSession() = prefs.edit().clear().apply()
}
