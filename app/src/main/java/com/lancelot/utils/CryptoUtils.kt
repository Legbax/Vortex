package com.lancelot.utils

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    // Shared key for encryption/decryption
    // Obfuscated key generation (lazy loaded)
    private val KEY_BYTES by lazy {
        byteArrayOf(
            76, 97, 110, 99, 101, 108, 111, 116,
            83, 116, 101, 97, 108, 116, 104, 49
        )
    }

    private const val ALGO = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // ThreadLocal for Cipher reuse to improve performance
    private val CIPHER_THREAD_LOCAL = object : ThreadLocal<Cipher>() {
        override fun initialValue(): Cipher {
            return Cipher.getInstance(TRANSFORMATION)
        }
    }

    private val secureRandom = SecureRandom()

    fun encrypt(value: String): String {
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGO)
            val cipher = CIPHER_THREAD_LOCAL.get() ?: Cipher.getInstance(TRANSFORMATION)

            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

            // Combine IV + Ciphertext
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            "GCM:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fail safe
            ""
        }
    }

    fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrEmpty()) return null
        return try {
            if (encrypted.startsWith("GCM:")) {
                val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)

                // Extract IV and Ciphertext
                val iv = decodedBytes.copyOfRange(0, GCM_IV_LENGTH)
                val ciphertext = decodedBytes.copyOfRange(GCM_IV_LENGTH, decodedBytes.size)

                val key = SecretKeySpec(KEY_BYTES, ALGO)
                val cipher = CIPHER_THREAD_LOCAL.get() ?: Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            } else if (encrypted.startsWith("ENC:")) {
                // Legacy AES/ECB fallback (if needed for old prefs, though GCM is now standard)
                // We recreate cipher here because ThreadLocal is configured for GCM
                val key = SecretKeySpec(KEY_BYTES, ALGO)
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key)
                val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
                String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
