package com.vortex.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PropertyUtilsTest {

    private val testFingerprint = DeviceFingerprint(
        manufacturer = "TestManu",
        brand = "TestBrand",
        model = "TestModel",
        device = "testdevice",
        product = "testproduct",
        hardware = "testhardware",
        board = "testboard",
        bootloader = "testbootloader",
        fingerprint = "Test/testproduct/testdevice:11/TEST/12345:user/release-keys",
        buildId = "TEST",
        tags = "release-keys",
        type = "user",
        radioVersion = "1.0",
        incremental = "12345",
        sdkInt = 30,
        release = "11",
        boardPlatform = "testplatform",
        eglDriver = "testegl",
        openGlEs = "3.2",
        hardwareChipname = "testchip",
        zygote = "zygote64_32",
        vendorFingerprint = "Test/testproduct/testdevice:11/TEST/12345:vendor/release-keys",
        display = "TEST-DISPLAY",
        buildDescription = "test description",
        buildFlavor = "test-user",
        buildHost = "testhost",
        buildUser = "testuser",
        buildDateUtc = "1600000000",
        securityPatch = "2021-01-01",
        buildVersionCodename = "REL",
        buildVersionPreviewSdk = "0"
    )

    private val testSerial = "TESTSERIAL123"

    @Test
    fun testStandardProperties() {
        assertEquals("TestManu", PropertyUtils.getSpoofedProperty("ro.product.manufacturer", testFingerprint, testSerial))
        assertEquals("TestModel", PropertyUtils.getSpoofedProperty("ro.product.model", testFingerprint, testSerial))
        assertEquals("11", PropertyUtils.getSpoofedProperty("ro.build.version.release", testFingerprint, testSerial))
        assertEquals("30", PropertyUtils.getSpoofedProperty("ro.build.version.sdk", testFingerprint, testSerial))
        assertEquals(testSerial, PropertyUtils.getSpoofedProperty("ro.serialno", testFingerprint, testSerial))
    }

    @Test
    fun testSystemExtProperties() {
        // These are the new properties added for Lancelot consistency
        assertEquals("TestManu", PropertyUtils.getSpoofedProperty("ro.product.system_ext.manufacturer", testFingerprint, testSerial))
        assertEquals("TestBrand", PropertyUtils.getSpoofedProperty("ro.product.system_ext.brand", testFingerprint, testSerial))
        assertEquals("TestModel", PropertyUtils.getSpoofedProperty("ro.product.system_ext.model", testFingerprint, testSerial))
        assertEquals("testdevice", PropertyUtils.getSpoofedProperty("ro.product.system_ext.device", testFingerprint, testSerial))
        assertEquals("testproduct", PropertyUtils.getSpoofedProperty("ro.product.system_ext.name", testFingerprint, testSerial))
    }

    @Test
    fun testAdditionalConsistencyProperties() {
        assertEquals("12345", PropertyUtils.getSpoofedProperty("ro.build.version.incremental", testFingerprint, testSerial))
        assertEquals(testFingerprint.vendorFingerprint, PropertyUtils.getSpoofedProperty("ro.bootimage.build.fingerprint", testFingerprint, testSerial))
        assertEquals(testFingerprint.vendorFingerprint, PropertyUtils.getSpoofedProperty("ro.odm.build.fingerprint", testFingerprint, testSerial))
        assertEquals(testFingerprint.fingerprint, PropertyUtils.getSpoofedProperty("ro.system_ext.build.fingerprint", testFingerprint, testSerial))
    }

    @Test
    fun testUnknownKey() {
        assertNull(PropertyUtils.getSpoofedProperty("ro.unknown.property", testFingerprint, testSerial))
        assertNull(PropertyUtils.getSpoofedProperty("sys.some.prop", testFingerprint, testSerial))
    }
}
