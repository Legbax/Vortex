package com.vortex.utils

import java.io.DataOutputStream

object RootUtils {

    fun execute(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun forceStop(packageName: String) = execute("am force-stop $packageName")

    fun clearData(packageName: String) = execute("pm clear $packageName")

    fun rebootDevice() = execute("reboot")
}
