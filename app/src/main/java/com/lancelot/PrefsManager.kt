package com.lancelot

import android.content.Context
import android.util.Base64
import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object PrefsManager {
    private const val PREFS_NAME = "spoof_prefs"
    private const val ALGO = "AES"

    // Obfuscated key generation (simple array for now, but avoids "LancelotStealth1" string in dex)
    private val KEY_BYTES by lazy {
        byteArrayOf(
            76, 97, 110, 99, 101, 108, 111, 116,
            83, 116, 101, 97, 108, 116, 104, 49
        )
    }

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun saveString(context: Context, key: String, value: String) {
        val encrypted = encrypt(value)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, encrypted).apply()
        // No chmod call here.
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(key, null)
        return decrypt(encrypted) ?: default
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
        // No chmod call here.
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, default)
    }

    private fun encrypt(value: String): String {
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGO)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            java.security.SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            "GCM:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fail safe: return safe default or fallback if needed, but for encrypt just return value?
            // Returning raw value here is risky if it's sensitive.
            // Better to return empty or error indicator.
            ""
        }
    }

    private fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrEmpty()) return null
        return try {
            if (encrypted.startsWith("GCM:")) {
                val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
                val iv = decodedBytes.copyOfRange(0, GCM_IV_LENGTH)
                val ciphertext = decodedBytes.copyOfRange(GCM_IV_LENGTH, decodedBytes.size)

                val key = SecretKeySpec(KEY_BYTES, ALGO)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            } else if (encrypted.startsWith("ENC:")) {
                // Legacy AES/ECB
                val key = SecretKeySpec(KEY_BYTES, ALGO)
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key)
                val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
                String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
            } else {
                // Never return raw string if it doesn't look encrypted, could be tampering.
                // Return null to trigger default fallback.
                null
            }
        } catch (e: Exception) {
            // Fail safe: return null, never the corrupted/raw string.
            null
        }
    }
}
