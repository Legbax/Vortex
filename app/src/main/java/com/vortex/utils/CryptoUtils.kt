package com.vortex.utils

import android.util.Base64
import com.vortex.BuildConfig
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGO = "AES/GCM/NoPadding"
    private const val PREFIX = "ENC:"

    private val SECRET_KEY by lazy {
        val time = OriginalBuildValues.ORIGINAL_BUILD_TIME.toString()
        val salt = "Vortex2026SecureSalt"
        val keyMaterial = (time + salt).toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        SecretKeySpec(hash, "AES")   // AES-256
    }

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGO)
            val iv = ByteArray(12).apply { java.security.SecureRandom().nextBytes(this) }
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, gcmSpec)
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val combined = iv + encrypted
            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("CryptoUtils", "Encrypt failed", e)
            ""
        }
    }

    fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrEmpty() || !encrypted.startsWith(PREFIX)) {
            return encrypted
        }

        return try {
            val data = Base64.decode(encrypted.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)

            val cipher = Cipher.getInstance(ALGO)
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, gcmSpec)

            val decrypted = cipher.doFinal(ciphertext)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("CryptoUtils", "Decrypt failed", e)
            null
        }
    }
}
