package com.vortex.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {
    @Test
    fun testValidImei() {
        assertTrue(ValidationUtils.isValidImei("864134050000001")) // IMEI real válido
    }

    @Test
    fun testInvalidChecksumImei() {
        assertFalse(ValidationUtils.isValidImei("864134050000002")) // Checksum incorrecto
    }

    @Test
    fun testInvalidLengthImei() {
        assertFalse(ValidationUtils.isValidImei("86413405")) // Demasiado corto
    }

    @Test
    fun testNonDigitImei() {
        assertFalse(ValidationUtils.isValidImei("86413405000000A")) // Contiene letras
    }
}
