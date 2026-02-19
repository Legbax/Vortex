package com.vortex

import java.util.Random

object SpoofingUtils {

    // TACs mapeados POR FABRICANTE para correlacionar con el perfil activo.
    // Fuente: GSMA IMEI DB pública. Cada entrada: TAC → (modelo aproximado)
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

    /**
     * Genera un IMEI válido correlacionado con la marca del perfil activo.
     * Si no se pasa perfil, usa la lista genérica.
     */
    fun generateValidImei(profileName: String = ""): String {
        // Buscar la marca del perfil en DEVICE_FINGERPRINTS
        val brand = MainHook.DEVICE_FINGERPRINTS[profileName]?.brand ?: ""
        val tacList = TACS_BY_BRAND[brand] ?: TACS_BY_BRAND["default"]!!
        val tac = tacList.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    /**
     * Genera un ICCID válido para el MCC/MNC dado.
     * Formato estándar: 89 + MCC(3) + MNC(2-3) + account(9-10) + check
     */
    fun generateValidIccid(mccMnc: String): String {
        // mccMnc típicamente 6 dígitos para US (MCC=310, MNC=3 dígitos)
        val issuer = (10..99).random().toString()
        val prefix = "89$mccMnc$issuer"             // "89" + 6 + 2 = 10 chars
        val needed = 18 - prefix.length              // necesitamos llegar a 19 sin check
        val account = (1..needed.coerceAtLeast(1)).map { (0..9).random() }.joinToString("")
        val base = prefix + account
        return base + luhnChecksum(base)             // total: 19-20 dígitos
    }

    /**
     * Genera un IMSI válido de exactamente 15 dígitos.
     * IMSI = MCC(3) + MNC(3) + MSIN(9) = 15 dígitos totales
     */
    fun generateValidImsi(mccMnc: String): String {
        // mccMnc debe tener 6 dígitos para US (MCC 310 + MNC 3 dígitos)
        val msin = (1..9).map { (0..9).random() }.joinToString("")   // siempre 9 dígitos
        return mccMnc + msin                                          // 6 + 9 = 15 ✓
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

    fun generateRandomId(len: Int) = (1..len).map { "0123456789abcdef".random() }.joinToString("")

    /**
     * GAID como UUID v4 correcto:
     * xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     * donde y ∈ {8,9,a,b}
     */
    fun generateRandomGaid(): String {
        val p1 = generateRandomId(8)
        val p2 = generateRandomId(4)
        val p3 = "4" + generateRandomId(3)                    // versión 4 ✓
        val p4 = listOf("8","9","a","b").random() + generateRandomId(3)  // variante ✓
        val p5 = generateRandomId(12)
        return "$p1-$p2-$p3-$p4-$p5"
    }

    /**
     * Serial: formato realista por marca.
     * Xiaomi/Redmi: alfanumérico 8-12 chars
     * Samsung: RX + año + mes + letras + dígitos
     */
    fun generateRandomSerial(brand: String = ""): String {
        return when (brand.lowercase()) {
            "samsung" -> {
                val year = listOf("21","22").random()
                val month = (1..12).random().toString().padStart(2,'0')
                val suffix = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789".random() }.joinToString("")
                "R${year}${month}${suffix}"
            }
            "google" -> {
                // Pixel: 14 chars alfanuméricos
                (1..14).map { "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".random() }.joinToString("")
            }
            else -> {
                // Xiaomi/genérico: 8-12 chars
                val len = (8..12).random()
                (1..len).map { "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".random() }.joinToString("")
            }
        }
    }

    fun generateRandomMac(): String {
        val bytes = ByteArray(6)
        Random().nextBytes(bytes)
        // Bit 0 del primer octeto = 0 (unicast), bit 1 = 1 (locally administered)
        bytes[0] = (bytes[0].toInt() and 0xFE or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Gmail realista con nombres anglo-americanos coherentes con US carrier.
     * Se eliminan los nombres españoles/apellidos italianos del original.
     */
    fun generateRealisticGmail(): String {
        val first = listOf(
            "james","john","robert","michael","william","david","joseph","charles",
            "thomas","daniel","matthew","anthony","mark","donald","steven","paul",
            "andrew","joshua","kenneth","kevin","brian","george","timothy","ronald",
            "edward","jason","jeffrey","ryan","jacob","gary","nicholas","eric"
        ).random()
        val last = listOf(
            "smith","johnson","williams","brown","jones","garcia","miller","davis",
            "wilson","taylor","anderson","thomas","jackson","white","harris","martin",
            "thompson","young","robinson","lewis","walker","allen","hall","wright",
            "scott","green","adams","baker","nelson","carter","mitchell","perez"
        ).random()
        val sep = listOf("", ".", "_").random()
        val num = if (Random().nextBoolean()) (1..9999).random().toString() else ""
        return "$first$sep$last$num@gmail.com"
    }

    fun generatePhoneNumber(npaList: List<String>): String {
        val npa = if (npaList.isNotEmpty()) npaList.random() else "212"
        var nxx = (200..999).random()
        if (nxx == 555) nxx = 556    // 555-xxxx son ficticios
        val sub = (0..9999).random().toString().padStart(4, '0')
        return "+1$npa$nxx$sub"
    }
}
