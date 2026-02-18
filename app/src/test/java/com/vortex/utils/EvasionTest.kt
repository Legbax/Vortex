package com.vortex.utils

import com.vortex.SpoofingUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvasionTest {

    @Test
    fun testSensitivePathExactMatch() {
        assertTrue(SpoofingUtils.isSensitivePath("/system/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/su"))
    }

    @Test
    fun testMagiskKeyword() {
        assertTrue(SpoofingUtils.isSensitivePath("/data/local/tmp/magisk.db"))
        assertTrue(SpoofingUtils.isSensitivePath("/sbin/.magisk"))
    }

    @Test
    fun testXposedKeyword() {
        assertTrue(SpoofingUtils.isSensitivePath("/data/user/0/de.robv.android.xposed.installer"))
    }

    @Test
    fun testMagiskModulesPrefix() {
        assertTrue(SpoofingUtils.isSensitivePath("/data/adb/modules/some_module"))
    }

    @Test
    fun testSafePaths() {
        assertFalse(SpoofingUtils.isSensitivePath("/system/bin/ls"))
        assertFalse(SpoofingUtils.isSensitivePath("/sdcard/Download/image.jpg"))
    }

    @Test
    fun testAntiSelfSabotage() {
        // Should NOT hide Lancelot's own files even if they contain "xposed" or other keywords in path context
        // Note: The logic in SpoofingUtils checks "com.vortex".
        // Let's assume a path like /data/data/com.vortex/files/xposed_log.txt
        assertFalse(SpoofingUtils.isSensitivePath("/data/data/com.vortex/files/xposed_log.txt"))
        assertFalse(SpoofingUtils.isSensitivePath("/data/app/com.vortex-1/base.apk"))
    }

    @Test
    fun testRelativePathsIgnored() {
        // Optimization check: relative paths are ignored
        assertFalse(SpoofingUtils.isSensitivePath("su"))
        assertFalse(SpoofingUtils.isSensitivePath("magisk"))
    }
}
