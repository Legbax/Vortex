package com.vortex

import android.content.Context
import com.vortex.utils.CryptoUtils
import java.io.File

object PrefsManager {
    const val PREFS_NAME = "vortex_prefs"

    fun saveString(context: Context, key: String, value: String) {
        val enc = CryptoUtils.encrypt(value) ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, enc).commit()
        fixPermissions(context)
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null)
        return CryptoUtils.decrypt(raw) ?: default
    }

    /** Booleanos también se cifran para evitar lectura forense con root */
    fun saveBoolean(context: Context, key: String, value: Boolean) {
        saveString(context, key, value.toString())
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        val v = getString(context, key, "")
        return if (v.isEmpty()) default else v.toBooleanStrictOrNull() ?: default
    }

    private fun fixPermissions(context: Context) {
        try {
            val file = File(context.filesDir.parent, "shared_prefs/$PREFS_NAME.xml")
            if (file.exists()) {
                file.setReadable(true, false)
            }
        } catch (e: Exception) { }
    }
}
