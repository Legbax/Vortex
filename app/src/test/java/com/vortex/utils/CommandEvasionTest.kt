package com.vortex.utils

import com.vortex.SpoofingUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandEvasionTest {

    @Test
    fun testSensitiveCommands() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("su", "-c", "ls")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("which", "su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("/system/bin/sh", "-c", "which su")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("mount")))
    }

    @Test
    fun testSensitivePathInCommand() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "/sbin/.magisk")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("cat", "/data/adb/modules/test")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "-la", "/data/user/0/de.robv.android.xposed.installer")))
    }

    @Test
    fun testKeywords() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "magisk")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("grep", "xposed")))
    }

    @Test
    fun testExtendedKeywords() {
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("frida-server")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "/data/local/tmp/frida")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ps", "-A", "|", "grep", "zygisk")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("ls", "/data/adb/riru")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("getprop", "ro.debuggable")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("getprop", "ro.build.tags")))
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("getenforce")))
    }

    @Test
    fun testSafeCommands() {
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("ls", "-la")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("cat", "/proc/cpuinfo")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("ping", "google.com")))
        assertFalse(SpoofingUtils.isSensitiveCommand(listOf("logcat", "-d")))
    }

    @Test
    fun testEmptyCommand() {
        assertFalse(SpoofingUtils.isSensitiveCommand(emptyList()))
    }
}
