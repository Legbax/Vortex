package com.lancelot.utils

import android.util.Base64
import android.util.Log
import com.lancelot.BuildConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGO = "AES/GCM/NoPadding"
    private const val PREFIX = "ENC:"

    // Dynamic key derived from Build.TIME (immutable per firmware)
    // Safe because OriginalBuildValues captures it before Xposed hooks
    private val SECRET_KEY by lazy {
        val time = OriginalBuildValues.ORIGINAL_BUILD_TIME.toString()
        val salt = "Lancelot2026SecureSalt"
        val keyMaterial = (time + salt).toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        SecretKeySpec(hash, "AES")   // AES-256
    }

    private val secureRandom = SecureRandom()

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGO)

            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)

            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, gcmSpec)

            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            // Combine IV + Ciphertext
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("CryptoUtils", "Encrypt failed", e)
            ""
        }
    }

    fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrEmpty()) return null
        // Check for our new prefix. If not present, maybe return raw if it's old/legacy
        if (!encrypted.startsWith(PREFIX)) {
            // Legacy handling or plain text fallback
            // If it starts with GCM: (old implementation), we can't decrypt it with the NEW key.
            // Since this is a breaking change for security, we accept old prefs might be lost/reset.
            return encrypted
        }

        return try {
            val decodedBytes = Base64.decode(encrypted.removePrefix(PREFIX), Base64.NO_WRAP)

            val iv = decodedBytes.copyOfRange(0, 12)
            val ciphertext = decodedBytes.copyOfRange(12, decodedBytes.size)

            val cipher = Cipher.getInstance(ALGO)
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("CryptoUtils", "Decrypt failed", e)
            null
        }
    }
}
