package com.lancelot

import java.util.UUID

object SpoofingUtils {

    private val VALID_TACS = listOf(
        "86413405", "86413404",   // Redmi / Xiaomi
        "35271311", "35361311",   // Redmi
        "35449209", "35449210",   // Samsung A-series
        "35674910", "35674911"    // Google Pixel
    )

    fun generateValidImei(): String {
        val tac = VALID_TACS.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    fun generateValidIccid(mccMnc: String): String {
        val mnc = if (mccMnc.length >= 6) mccMnc.substring(3) else "260"
        val issuer = (10..99).random().toString()
        val prefixPart = "891$mnc$issuer"

        val accountLen = 18 - prefixPart.length
        val account = (1..accountLen).map { (0..9).random() }.joinToString("")
        val base = prefixPart + account
        return base + luhnChecksum(base)
    }

    fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var digit = number[i].digitToInt()
            if ((number.length - i + 1) % 2 == 0) digit *= 2
            if (digit > 9) digit -= 9
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    fun generateRandomId(len: Int) = (1..len).map { "0123456789abcdef".random() }.joinToString("")

    fun generateRandomMac(): String {
        val bytes = ByteArray(6)
        java.util.Random().nextBytes(bytes)
        bytes[0] = (bytes[0].toInt() and 0xFC or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    fun generateRealisticGmail(): String {
        val names = listOf(
            "juan", "jose", "luis", "carlos", "francisco", "antonio", "jorge", "miguel", "manuel", "pedro",
            "jesus", "alejandro", "david", "daniel", "ricardo", "fernando", "eduardo", "javier", "raul", "roberto"
        )
        val surnames = listOf(
            "rossi", "russo", "ferrari", "esposito", "bianchi", "romano", "colombo", "ricci", "marino", "greco"
        )
        val name = names.random()
        val surname = surnames.random()
        val randomNum = (1..9999).random().toString()

        return "$name$surname$randomNum@gmail.com"
    }

    fun generatePhoneNumber(npaList: List<String>): String {
        val npa = if (npaList.isNotEmpty()) npaList.random() else "202"

        var nxx = (200..999).random()
        if (nxx == 555) nxx = 556

        val subscriber = (0..9999).random().toString().padStart(4, '0')
        return "+1$npa$nxx$subscriber"
    }
}
