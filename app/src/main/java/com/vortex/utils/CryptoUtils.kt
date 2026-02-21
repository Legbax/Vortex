package com.vortex.utils

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val PREFIX = "ENC:"

    private val KEY: SecretKeySpec by lazy {
        val seed = "Vortex2026SecureKeyForPrefsEncryption!".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        SecretKeySpec(digest.digest(seed), "AES")
    }

    fun encrypt(data: String): String? {
        if (data.isEmpty()) return ""
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, KEY)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            return PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            return null
        }
    }

    fun decrypt(data: String?): String? {
        if (data.isNullOrEmpty()) return ""
        if (!data.startsWith(PREFIX)) return data // Return raw if not encrypted (legacy support)

        try {
            val raw = Base64.decode(data.substring(PREFIX.length), Base64.NO_WRAP)
            val iv = ByteArray(IV_LENGTH)
            val encrypted = ByteArray(raw.size - IV_LENGTH)

            System.arraycopy(raw, 0, iv, 0, IV_LENGTH)
            System.arraycopy(raw, IV_LENGTH, encrypted, 0, encrypted.size)

            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, KEY, spec)

            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            return null
        }
    }

    // --- VORTEX PROXY CONFIG (AES-GCM Seguro) ---
    data class ProxyConfig(
        val host: String, val port: Int,
        val user: String, val pass: String,
        val isGlobal: Boolean,
        val includeWebView: Boolean
    )

    fun saveProxyConfig(context: Context, config: ProxyConfig) {
        val data = "${config.host}|${config.port}|${config.user}|${config.pass}|${config.isGlobal}|${config.includeWebView}"
        val encrypted = encrypt(data) ?: return
        context.getSharedPreferences("vortex_secure", Context.MODE_PRIVATE)
            .edit().putString("proxy_conf_secure", encrypted).apply()
    }

    fun getProxyConfig(context: Context): ProxyConfig? {
        val prefs = context.getSharedPreferences("vortex_secure", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("proxy_conf_secure", null) ?: return null
        val decrypted = decrypt(encrypted) ?: return null
        val parts = decrypted.split("|")
        if (parts.size != 6) return null
        return ProxyConfig(parts[0], parts[1].toIntOrNull() ?: 1080, parts[2], parts[3], parts[4].toBoolean(), parts[5].toBoolean())
    }
}
