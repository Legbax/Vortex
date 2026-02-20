package com.vortex.utils

import org.junit.Test
import org.junit.Assert.*
import com.vortex.SpoofingUtils

class EvasionTest {

    @Test
    fun testSensitivePaths() {
        assertTrue(SpoofingUtils.isSensitivePath("/system/bin/su"))
        assertTrue(SpoofingUtils.isSensitivePath("/data/adb"))
    }

    @Test
    fun testSensitiveCommands() {
        // String commands
        assertTrue(SpoofingUtils.isSensitiveCommand("su"))

        // Array commands
        assertTrue(SpoofingUtils.isSensitiveCommand(listOf("su")))

        // Debugging the failure
        val path = "/data/adb"
        val isPathSensitive = SpoofingUtils.isSensitivePath(path)

        // We know this passes from testSensitivePaths, but check again here
        assertTrue("Path $path should be sensitive", isPathSensitive)

        val cmdList = listOf("ls", path)
        val isCmdSensitive = SpoofingUtils.isSensitiveCommand(cmdList)

        assertTrue("Command $cmdList should be sensitive because $path is sensitive ($isPathSensitive)", isCmdSensitive)
    }
}
