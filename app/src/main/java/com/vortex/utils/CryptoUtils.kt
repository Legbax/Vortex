package com.vortex.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGO    = "AES/GCM/NoPadding"
    private const val TAG_LEN = 128
    private const val IV_LEN  = 12

    // Clave AES-256 fija, ofuscada por ProGuard en release.
    // Se usa clave estática porque XSharedPreferences necesita
    // que el proceso hook (UID diferente) pueda leer el mismo valor.
    private val KEY = SecretKeySpec(
        byteArrayOf(
            0x56,0x4F,0x52,0x54,0x45,0x58,0x4B,0x45,
            0x59,0x32,0x30,0x32,0x36,0x56,0x4F,0x52,
            0x54,0x45,0x58,0x4D,0x4F,0x44,0x55,0x4C,
            0x45,0x53,0x45,0x43,0x52,0x45,0x54,0x21
        ), "AES"
    )

    fun encrypt(plaintext: String): String? = try {
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, KEY)
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    } catch (_: Exception) { null }

    fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrEmpty()) return null
        return try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            if (raw.size <= IV_LEN) return null
            val iv = raw.sliceArray(0 until IV_LEN)
            val ct = raw.sliceArray(IV_LEN until raw.size)
            val cipher = Cipher.getInstance(ALGO)
            cipher.init(Cipher.DECRYPT_MODE, KEY, GCMParameterSpec(TAG_LEN, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }
}
