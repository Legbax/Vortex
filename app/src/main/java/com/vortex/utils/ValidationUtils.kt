package com.vortex.utils

object ValidationUtils {

    /** IMEI: exactamente 15 dígitos, checksum Luhn válido */
    fun isValidImei(v: String) =
        v.length == 15 && v.all(Char::isDigit) && luhnOk(v)

    /** ICCID: 19-20 dígitos, empieza con "89", Luhn válido */
    fun isValidIccid(v: String) =
        v.length in 19..20 && v.all(Char::isDigit) && v.startsWith("89") && luhnOk(v)

    /** IMSI: 14-15 dígitos totales */
    fun isValidImsi(v: String) =
        v.length in 14..15 && v.all(Char::isDigit)

    /** Android ID: 16 hex lowercase */
    fun isValidAndroidId(v: String) =
        v.length == 16 && v.all { it.isDigit() || it in 'a'..'f' }

    /** GAID: UUID v4 — tercer grupo empieza con 4, cuarto con 8/9/a/b */
    fun isValidGaid(v: String) =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
            RegexOption.IGNORE_CASE).matches(v)

    /** MAC: XX:XX:XX:XX:XX:XX */
    fun isValidMac(v: String) =
        Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$").matches(v)

    private fun luhnOk(n: String): Boolean {
        var sum = 0; val len = n.length; val p = len % 2
        for (i in 0 until len) {
            var d = n[i].digitToInt()
            if (i % 2 == p) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        return sum % 10 == 0
    }
}
