package com.vortex.utils

import com.vortex.SpoofingUtils
import org.junit.Test
import org.junit.Assert.*

class EvasionTest {

    @Test
    fun testSensitivePaths() {
        // Sensitive paths
        assertTrue(SpoofingUtils.isSensitivePath("/system/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/system/xbin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/vendor/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/local/xbin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/system/app/Superuser.apk"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/adb/magisk/busybox"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/.magisk"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/.magisk/mirror"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/user_de/0/de.robv.android.xposed.installer"))

        // Non-sensitive paths
        assertFalse(SpoofingUtils.isSensitivePath("/system/bin/ls"))
        assertFalse(SpoofingUtils.isSensitivePath("/data/local/tmp/myfile"))
        assertFalse(SpoofingUtils.isSensitivePath("/sdcard/Download/image.jpg"))
        assertFalse(SpoofingUtils.isSensitivePath("/system/app/Calculator.apk"))

        // False positive check: Prefix match without separator
        // e.g. /system/bin/supply should NOT match /system/bin/su
        assertFalse(SpoofingUtils.isSensitivePath("/system/bin/supply"))
    }

    @Test
    fun testSensitiveCommands() {
        // Sensitive commands
        assertTrue(SpoofingUtils.isSensitiveCommand("su"))
        assertTrue(SpoofingUtils.isSensitiveCommand("/system/bin/su"))
        assertTrue(SpoofingUtils.isSensitiveCommand("magisk -v"))
        assertTrue(SpoofingUtils.isSensitiveCommand("getenforce"))
        assertTrue(SpoofingUtils.isSensitiveCommand("getprop ro.secure"))
        assertTrue(SpoofingUtils.isSensitiveCommand("mount | grep magisk"))

        // Which check
        assertTrue(SpoofingUtils.isSensitiveCommand("which su"))
        assertTrue(SpoofingUtils.isSensitiveCommand("which magisk"))

        // Non-sensitive commands (BENIGN CASES)
        assertFalse(SpoofingUtils.isSensitiveCommand("ls -la"))
        assertFalse(SpoofingUtils.isSensitiveCommand("echo success")) // "success" contains "su"
        assertFalse(SpoofingUtils.isSensitiveCommand("cat /proc/cpuinfo"))
        assertFalse(SpoofingUtils.isSensitiveCommand("getprop ro.build.version.sdk")) // Safe getprop
        assertFalse(SpoofingUtils.isSensitiveCommand("getprop ro.product.model"))
        assertFalse(SpoofingUtils.isSensitiveCommand("mount")) // mount without magisk args

        // Array variants
        assertTrue(SpoofingUtils.isSensitiveCommand(arrayOf("su", "-c", "id")))
        assertTrue(SpoofingUtils.isSensitiveCommand(arrayOf("magisk", "-v")))
        assertFalse(SpoofingUtils.isSensitiveCommand(arrayOf("ls", "-la")))
        assertFalse(SpoofingUtils.isSensitiveCommand(arrayOf("echo", "success")))
    }
}
