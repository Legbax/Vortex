package com.lancelot.utils

object ValidationUtils {
    fun isValidImei(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) return false
        return isLuhnValid(imei)
    }

    fun isLuhnValid(number: String): Boolean {
        var sum = 0
        val len = number.length
        for (i in 0 until len) {
            val digitChar = number[len - 1 - i]
            val digit = digitChar.digitToIntOrNull() ?: return false
            var d = digit
            if (i % 2 == 1) d *= 2
            if (d > 9) d -= 9
            sum += d
        }
        return sum % 10 == 0
    }
}
