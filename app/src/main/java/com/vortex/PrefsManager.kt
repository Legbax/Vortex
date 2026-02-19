package com.vortex

import android.content.Context
import com.vortex.utils.CryptoUtils

object PrefsManager {
    const val PREFS_NAME = "vortex_prefs"   // ← nombre cambiado también

    fun saveString(context: Context, key: String, value: String) {
        val enc = CryptoUtils.encrypt(value) ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, enc).apply()
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        val enc = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        return CryptoUtils.decrypt(enc) ?: default
    }

    /** Booleanos también se cifran para evitar lectura forense con root */
    fun saveBoolean(context: Context, key: String, value: Boolean) {
        saveString(context, key, value.toString())
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        val v = getString(context, key, "")
        return if (v.isEmpty()) default else v.toBooleanStrictOrNull() ?: default
    }
}
