package com.lancelot

import java.util.UUID
import java.util.Random

object SpoofingUtils {

    private val VALID_TACS = listOf(
        "86413405", "86413404",   // Redmi / Xiaomi
        "35271311", "35361311",   // Redmi
        "35449209", "35449210",   // Samsung A-series
        "35674910", "35674911"    // Google Pixel
    )

    // Optimized paths to hide (Set for O(1) access)
    private val SENSITIVE_PATHS = setOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su",
        "/data/local/su", "/data/local/xbin/su", "/data/local/bin/su",
        "/system/sd/xbin/su", "/system/bin/failsafe/su",
        "/su/bin/su", "/system/xbin/daemonsu",
        "/system/app/Superuser.apk", "/system/app/SuperSU.apk"
    )

    // Sensitive commands to block
    private val SENSITIVE_COMMANDS = setOf(
        "su", "which su", "mount", "getprop ro.secure"
    )

    fun isSensitivePath(path: String): Boolean {
        // Optimization 1: Only check absolute paths starting with /
        if (path.isEmpty() || path[0] != '/') return false

        // Anti-Auto-Sabotage: Never hide our own package files
        if (path.contains("com.lancelot")) return false

        // Optimization 2: Check sensitive set first (fastest)
        if (SENSITIVE_PATHS.contains(path)) return true

        // Optimization 3: Check for keywords only if path length suggests it might contain them
        // Avoid scanning very short paths for long keywords
        if (path.length > 6) {
            if (path.startsWith("/data/adb/modules")) return true // Magisk modules

            // Fallback to slower contains for specific keywords
            if (path.contains("magisk", true) ||
                path.contains("xposed", true) ||
                path.contains("lsposed", true)) {
                return true
            }
        }
        return false
    }

    fun isSensitiveCommand(command: List<String>): Boolean {
        if (command.isEmpty()) return false

        // Convert full command to single string for easier matching
        val fullCmd = command.joinToString(" ").lowercase()

        // 1. Direct match with SENSITIVE_COMMANDS set or start
        if (SENSITIVE_COMMANDS.contains(fullCmd)) return true
        if (fullCmd.startsWith("su ")) return true

        // 2. Sensitive keywords
        if (fullCmd.contains("magisk") ||
            fullCmd.contains("xposed") ||
            fullCmd.contains("lsposed") ||
            fullCmd.contains("busybox")) {
            return true
        }

        // 3. Known root checks (if not caught by set)
        if (fullCmd.contains("which su") ||
            fullCmd.contains("ls /sbin") ||
            fullCmd.contains("ls /data/adb")) {
            return true
        }

        // 4. Check if any argument is a sensitive path
        // (Reusing the path logic, but we need to filter for potential paths)
        for (part in command) {
            if (part.startsWith("/") && isSensitivePath(part)) {
                return true
            }
        }

        return false
    }

    fun generateValidImei(): String {
        val tac = VALID_TACS.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        val check = luhnChecksum(base)
        return base + check
    }

    fun generateValidIccid(mccMnc: String): String {
        val mnc = if (mccMnc.length >= 6) mccMnc.substring(3) else "260"
        // Try to match standard issuer length
        val issuer = (10..99).random().toString()
        val prefixPart = "891$mnc$issuer"

        // Target 19 digits usually, sometimes 20. 89 + 1 + 3 + 2 = 8 chars.
        // Need 10-11 more.
        val accountLen = 18 - prefixPart.length
        val account = (1..accountLen).map { (0..9).random() }.joinToString("")
        val base = prefixPart + account
        val check = luhnChecksum(base)
        return base + check
    }

    fun generateValidImsi(mccMnc: String): String {
        // IMSI = MCC + MNC + MSIN (usually 15 digits total)
        // MCC=3, MNC=2or3. Total 5 or 6.
        // Need 9 or 10 random digits.
        val needed = 15 - mccMnc.length
        val msin = (1..needed).map { (0..9).random() }.joinToString("")
        return mccMnc + msin
    }

    fun isLuhnValid(number: String): Boolean {
        if (number.isEmpty() || !number.all { it.isDigit() }) return false
        var sum = 0
        val len = number.length
        val parity = len % 2
        for (i in 0 until len) {
            var digit = number[i].digitToInt()
            if (i % 2 == parity) {
                digit *= 2
            }
            if (digit > 9) {
                digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    private fun luhnChecksum(number: String): Int {
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

    fun generateRandomGaid(): String {
        return "${generateRandomId(8)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(12)}"
    }

    fun generateRandomSerial(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..12).map { chars.random() }.joinToString("")
    }

    fun generateRandomMac(): String {
        val bytes = ByteArray(6)
        Random().nextBytes(bytes)
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
