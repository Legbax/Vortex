package com.vortex

import java.util.Random
import java.util.UUID

object SpoofingUtils {

    private val SENSITIVE_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/su/bin/su",
        "/proc/net/unix",
        "/data/adb",
        "/data/adb/modules",
        "/data/adb/magisk",
        "/data/adb/ksu"
    )

    private val SENSITIVE_PATH_PREFIXES = listOf(
        "/data/adb/",
        "/data/user_de/0/de.robv.android.xposed.installer"
    )

    private val SENSITIVE_COMMANDS = listOf(
        "su",
        "which su",
        "busybox",
        "magisk",
        "/system/bin/su",
        "/system/xbin/su"
    )

    fun isSensitivePath(path: String): Boolean {
        if (path in SENSITIVE_PATHS) return true
        if (SENSITIVE_PATH_PREFIXES.any { path.startsWith(it) }) return true
        if (path.endsWith("/su") && !path.contains("/src/")) return true
        return false
    }

    fun isSensitiveCommand(command: String): Boolean {
        return SENSITIVE_COMMANDS.any { command == it || command.startsWith("$it ") }
    }

    fun isSensitiveCommand(args: List<String>): Boolean {
        if (args.isEmpty()) return false
        val cmd = args[0]
        if (isSensitiveCommand(cmd)) return true
        for (arg in args) {
             if (isSensitivePath(arg)) return true
        }
        return false
    }

    // TACs mapeados POR FABRICANTE para correlacionar con el perfil activo.
    private val TACS_BY_BRAND = mapOf(
        "Xiaomi"   to listOf("86413404", "86413405", "35271311", "35361311", "86814904"),
        "POCO"     to listOf("86814904", "86814905", "35847611", "35847612"),
        "Redmi"    to listOf("86413404", "86413405", "35271311", "35271312"),
        "samsung"  to listOf("35449209", "35449210", "35355610", "35735110", "35735111"),
        "Google"   to listOf("35674910", "35674911", "35308010", "35308011"),
        "OnePlus"  to listOf("86882504", "86882505", "35438210", "35438211"),
        "motorola" to listOf("35617710", "35617711", "35327510", "35327511"),
        "Nokia"    to listOf("35720210", "35720211", "35489310"),
        "realme"   to listOf("86828804", "86828805", "35388910"),
        "vivo"     to listOf("86979604", "86979605", "35503210"),
        "OPPO"     to listOf("86885004", "86885005", "35604210"),
        "asus"     to listOf("35851710", "35851711", "35325010"),
        "default"  to listOf("35271311", "35449209", "35674910")
    )

    private val OUIS = listOf(
        byteArrayOf(0x40, 0x4E, 0x36),  // Qualcomm Atheros
        byteArrayOf(0x60, 0x57, 0x18),  // MediaTek / Ralink
        byteArrayOf(0x8C.toByte(), 0xDE.toByte(), 0x52),  // Realtek
        byteArrayOf(0xD4.toByte(), 0x61, 0x9D.toByte()),  // Broadcom
        byteArrayOf(0xF0.toByte(), 0x1F, 0xAF.toByte()),  // Qualcomm (nuevo)
        byteArrayOf(0xA4.toByte(), 0xC3.toByte(), 0xF0.toByte()),  // Google
        byteArrayOf(0x00, 0x23, 0x76),  // Intel
        byteArrayOf(0x00, 0x26, 0x86.toByte()),  // Cisco-Linksys
        byteArrayOf(0xD4.toByte(), 0xBE.toByte(), 0xD9.toByte()),  // Samsung
        byteArrayOf(0xAC.toByte(), 0x37, 0x43)   // Huawei/MediaTek
    )

    /**
     * Genera un IMEI válido correlacionado con la marca del perfil activo.
     */
    fun generateValidImei(profileName: String = "", seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val brand = MainHook.DEVICE_FINGERPRINTS[profileName]?.brand ?: ""
        val tacList = TACS_BY_BRAND[brand] ?: TACS_BY_BRAND["default"]!!
        // Use deterministic index if seeded, otherwise random
        val tac = if (seed != null) tacList[Math.abs(rng.nextInt()) % tacList.size] else tacList.random()

        val serial = (1..6).map { rng.nextInt(10) }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    fun generateValidIccid(mccMnc: String, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val issuer = (10 + rng.nextInt(90)).toString() // 10..99
        val prefix = "89$mccMnc$issuer"
        val needed = 18 - prefix.length
        val account = (1..needed.coerceAtLeast(1)).map { rng.nextInt(10) }.joinToString("")
        val base = prefix + account
        return base + luhnChecksum(base)
    }

    fun generateValidImsi(mccMnc: String, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        // [FIX D10] Primer dígito MSIN: 2-9 (evitar 0,1 reservados)
        val firstDigit = 2 + rng.nextInt(8)
        val rest = (1..8).map { rng.nextInt(10) }.joinToString("")
        return mccMnc + firstDigit.toString() + rest
    }

    fun isLuhnValid(number: String): Boolean {
        if (number.isEmpty() || !number.all { it.isDigit() }) return false
        var sum = 0; val len = number.length; val p = len % 2
        for (i in 0 until len) {
            var d = number[i].digitToInt()
            if (i % 2 == p) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return sum % 10 == 0
    }

    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var d = number[i].digitToInt()
            if ((number.length - i + 1) % 2 == 0) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return (10 - (sum % 10)) % 10
    }

    fun generateRandomId(len: Int, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val chars = "0123456789abcdef"
        return (1..len).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    fun generateRandomGaid(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        // Si usamos seed, construimos UUID determinista, si no, randomUUID
        if (seed != null) {
            val mostSigBits = rng.nextLong()
            val leastSigBits = rng.nextLong()
            return UUID(mostSigBits, leastSigBits).toString()
        }
        return UUID.randomUUID().toString()
    }

    fun generateRandomSerial(brand: String = "", seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val alphaNum = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        return when (brand.lowercase()) {
            "samsung" -> {
                val year = if (rng.nextBoolean()) "21" else "22"
                val month = (1 + rng.nextInt(12)).toString().padStart(2,'0')
                val suffix = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"[rng.nextInt(34)] }.joinToString("")
                "R${year}${month}${suffix}"
            }
            "google" -> {
                (1..14).map { alphaNum[rng.nextInt(alphaNum.length)] }.joinToString("")
            }
            else -> {
                val len = 8 + rng.nextInt(5) // 8..12
                (1..len).map { alphaNum[rng.nextInt(alphaNum.length)] }.joinToString("")
            }
        }
    }

    fun generateRandomMac(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        // [FIX D13] Usar OUI real
        val oui = OUIS[rng.nextInt(OUIS.size)]
        val suffix = ByteArray(3)
        rng.nextBytes(suffix)
        val full = oui + suffix
        // No forzar bit 'locally administered' (0x02) para parecer hardware real
        return full.joinToString(":") { "%02X".format(it) }
    }

    fun generateRealisticGmail(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val first = listOf(
            "james","john","robert","michael","william","david","joseph","charles",
            "thomas","daniel","matthew","anthony","mark","donald","steven","paul",
            "andrew","joshua","kenneth","kevin","brian","george","timothy","ronald",
            "edward","jason","jeffrey","ryan","jacob","gary","nicholas","eric"
        )
        val last = listOf(
            "smith","johnson","williams","brown","jones","garcia","miller","davis",
            "wilson","taylor","anderson","thomas","jackson","white","harris","martin",
            "thompson","young","robinson","lewis","walker","allen","hall","wright",
            "scott","green","adams","baker","nelson","carter","mitchell","perez"
        )

        val f = first[rng.nextInt(first.size)]
        val l = last[rng.nextInt(last.size)]
        val sep = listOf("", ".", "_")[rng.nextInt(3)]
        val num = if (rng.nextBoolean()) (1 + rng.nextInt(9999)).toString() else ""
        return "$f$sep$l$num@gmail.com"
    }

    fun generatePhoneNumber(npaList: List<String>, seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val npa = if (npaList.isNotEmpty()) npaList[rng.nextInt(npaList.size)] else "212"
        var nxx = 200 + rng.nextInt(800)
        if (nxx == 555) nxx = 556
        val sub = rng.nextInt(10000).toString().padStart(4, '0')
        return "+1$npa$nxx$sub"
    }

    // [FIX D3] SSID Realista
    fun generateRealisticSsid(seed: Long? = null): String {
        val rng = if (seed != null) Random(seed) else Random()
        val prefixes = listOf(
            "NETGEAR", "Linksys", "xfinitywifi", "ATT", "Spectrum",
            "TP-Link", "ASUS", "Archer", "dlink", "Belkin", "MyHome"
        )
        val prefix = prefixes[rng.nextInt(prefixes.size)]
        val suffixType = rng.nextInt(3)
        val suffix = when (suffixType) {
            0 -> "_${1000 + rng.nextInt(9000)}"
            1 -> "-${generateRandomId(4, seed).uppercase()}"
            else -> (1 + rng.nextInt(9)).toString()
        }
        return "$prefix$suffix"
    }

    /**
     * Genera un mapa con TODOS los valores esperados para un perfil dado.
     * Usado por StatusFragment para verificar consistencia.
     * Se usa una semilla determinista basada en el nombre del perfil para
     * que siempre devuelva los mismos valores "ideales" para ese perfil.
     */
    fun generateAllForProfile(profileName: String, mccMnc: String = "310260"): Map<String, String> {
        // Semilla determinista: hash del nombre del perfil + mccmnc
        // Esto asegura que "Expected" sea constante para una configuración dada.
        val seed = (profileName.hashCode() + mccMnc.hashCode()).toLong()
        val fp = MainHook.DEVICE_FINGERPRINTS[profileName]
        val brand = fp?.brand ?: ""

        // Obtener carrier (si existe) para NPAs
        val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
        val npas = carrier?.npas ?: emptyList()

        return mapOf(
            "imei"           to generateValidImei(profileName, seed),
            "imei2"          to generateValidImei(profileName, seed + 1), // semilla diferente
            "imsi"           to generateValidImsi(mccMnc, seed),
            "iccid"          to generateValidIccid(mccMnc, seed),
            "phone_number"   to generatePhoneNumber(npas, seed),
            "android_id"     to generateRandomId(16, seed),
            "ssaid_snapchat" to generateRandomId(16, seed + 2),
            "gaid"           to generateRandomGaid(seed),
            "gsf_id"         to generateRandomId(16, seed + 3),
            "media_drm_id"   to generateRandomId(32, seed),
            "serial"         to generateRandomSerial(brand, seed),
            "wifi_mac"       to generateRandomMac(seed),
            "bluetooth_mac"  to generateRandomMac(seed + 1),
            "gmail"          to generateRealisticGmail(seed),
            "wifi_ssid"      to generateRealisticSsid(seed),
            "wifi_bssid"     to generateRandomMac(seed + 2)
        )
    }
}
