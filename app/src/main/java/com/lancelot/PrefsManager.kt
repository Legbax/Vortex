package com.lancelot

import android.content.Context
import com.lancelot.utils.CryptoUtils

object PrefsManager {
    private const val PREFS_NAME = "spoof_prefs"

    fun saveString(context: Context, key: String, value: String) {
        val encrypted = CryptoUtils.encrypt(value)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(key, null)
        return CryptoUtils.decrypt(encrypted) ?: default
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, default)
    }
}
