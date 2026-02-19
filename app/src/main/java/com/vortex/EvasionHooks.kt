package com.vortex

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object EvasionHooks {

    fun hookFile(lpparam: XC_LoadPackage.LoadPackageParam) {
        val fileClass = File::class.java

        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val file = param.thisObject as File
                // Use absolutePath for consistent checking
                if (SpoofingUtils.isSensitivePath(file.absolutePath)) {
                    param.result = false
                }
            }
        }

        // Hook critical boolean checks
        try {
            XposedBridge.hookAllMethods(fileClass, "exists", hook)
            XposedBridge.hookAllMethods(fileClass, "canRead", hook)
            XposedBridge.hookAllMethods(fileClass, "canExecute", hook)
        } catch (_: Throwable) {}
    }

    fun hookRuntime(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val runtimeClass = java.lang.Runtime::class.java
            val processBuilderClass = java.lang.ProcessBuilder::class.java

            // Hook Runtime.exec
            val execHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val args = param.args
                    var isSensitive = false

                    if (args.isNotEmpty()) {
                        if (args[0] is String) {
                            if (SpoofingUtils.isSensitiveCommand(args[0] as String)) isSensitive = true
                        } else if (args[0] is Array<*>) {
                            // exec(String[] cmdarray, ...)
                            @Suppress("UNCHECKED_CAST")
                            val cmdArray = args[0] as? Array<String>
                            if (SpoofingUtils.isSensitiveCommand(cmdArray)) isSensitive = true
                        }
                    }

                    if (isSensitive) {
                        param.result = DummyProcess()
                    }
                }
            }

            XposedBridge.hookAllMethods(runtimeClass, "exec", execHook)

            // Hook ProcessBuilder.start
            XposedBridge.hookAllMethods(processBuilderClass, "start", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val pb = param.thisObject as ProcessBuilder
                    val command = pb.command() // Returns List<String>
                    val cmdArray = command.toTypedArray()

                    if (SpoofingUtils.isSensitiveCommand(cmdArray)) {
                        param.result = DummyProcess()
                    }
                }
            })

        } catch (_: Throwable) {}
    }

    // Dummy Process implementation that mimics a failed execution (exit code 1)
    private class DummyProcess : Process() {
        override fun getOutputStream(): OutputStream = object : OutputStream() {
            override fun write(b: Int) {}
        }

        override fun getInputStream(): InputStream = object : InputStream() {
            override fun read(): Int = -1
        }

        override fun getErrorStream(): InputStream = object : InputStream() {
            override fun read(): Int = -1
        }

        override fun waitFor(): Int = 1
        override fun exitValue(): Int = 1
        override fun destroy() {}
    }
}
