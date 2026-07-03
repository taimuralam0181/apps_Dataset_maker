package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rx_extract_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
    }

    fun saveAuth(token: String, name: String?, email: String?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun clearAuth() {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
