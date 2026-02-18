package com.lancelot.utils

import com.lancelot.SpoofingUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvasionTest {

    @Test
    fun testSensitivePaths() {
        // Test exact matches
        assertTrue(SpoofingUtils.isSensitivePath("/system/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/system/xbin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/vendor/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/system/app/Superuser.apk"))
        assertTrue(SpoofingUtils.isSensitivePath("/system/framework/XposedBridge.jar"))
    }

    @Test
    fun testSensitivePrefixes() {
        // Test prefix matches
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/.magisk/mirror/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/dev/magisk/0"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/adb/modules/lancelot"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/user_de/0/de.robv.android.xposed.installer/conf/modules.list"))
    }

    @Test
    fun testNormalPaths() {
        // Test non-sensitive paths
        assertFalse(SpoofingUtils.isSensitivePath("/sdcard/DCIM/Camera/IMG_2023.jpg"))
        assertFalse(SpoofingUtils.isSensitivePath("/data/data/com.android.vending"))
        assertFalse(SpoofingUtils.isSensitivePath("/system/bin/ls"))
        assertFalse(SpoofingUtils.isSensitivePath("/system/framework/framework.jar"))
    }

    @Test
    fun testEdgeCases() {
        // Test edge cases
        assertFalse(SpoofingUtils.isSensitivePath(""))
        assertFalse(SpoofingUtils.isSensitivePath("su")) // Relative path, not blocked by our logic
        assertFalse(SpoofingUtils.isSensitivePath("/s")) // Too short
        assertFalse(SpoofingUtils.isSensitivePath("/tmp")) // Too short
    }
}
