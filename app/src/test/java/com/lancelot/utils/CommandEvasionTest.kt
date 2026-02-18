package com.lancelot.utils

import com.lancelot.SpoofingUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandEvasionTest {

    @Test
    fun testSuBinary() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("/system/bin/su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("/sbin/su")))
    }

    @Test
    fun testSuInArgs() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("sh", "-c", "su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("sh", "-c", "/system/bin/su")))
    }

    @Test
    fun testMagiskKeywords() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("sh", "-c", "ls /sbin/.magisk")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "/data/adb/modules")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("magisk", "-v")))
    }

    @Test
    fun testXposedKeywords() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("grep", "xposed", "/proc/self/maps")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "/data/user/0/de.robv.android.xposed.installer")))
    }

    @Test
    fun testSensitivePathsInCommand() {
        // Even if not at the start, if it contains the sensitive path string
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("cat", "/data/adb/modules/test/module.prop")))
    }

    @Test
    fun testSafeCommands() {
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("ls", "-la")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("cat", "/proc/cpuinfo")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("logcat", "-d")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("getprop", "ro.build.version.sdk")))
    }

    @Test
    fun testSimilarButSafe() {
        // "support" contains "su", but should not be flagged if we check for "su" as exact word in args
        // BUT my implementation checks "binary == su" or "part == su" or keywords.
        // "support" does not contain "magisk" etc.
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("echo", "support")))

        // "superuser" is a keyword, so it should be flagged
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("echo", "superuser")))
    }
}
