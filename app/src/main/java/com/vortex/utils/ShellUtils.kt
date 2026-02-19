package com.vortex.utils

import java.io.DataOutputStream

object ShellUtils {

    fun execRootCmd(cmd: String): Boolean {
        var out: DataOutputStream? = null
        return try {
            val p = Runtime.getRuntime().exec("su")
            out = DataOutputStream(p.outputStream)
            out.writeBytes(cmd + "\n")
            out.flush()
            out.writeBytes("exit\n")
            out.flush()
            p.waitFor()
            p.exitValue() == 0
        } catch (e: Exception) {
            false
        } finally {
            try { out?.close() } catch (e: Exception) {}
        }
    }
}
