package com.vortex.hooks

import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import com.vortex.utils.CryptoUtils
import com.vortex.MainHook
import com.vortex.BuildConfig

object GPUSpoofer {

    data class GpuProps(
        val renderer: String,
        val vendor: String,
        val version: String,
        val extensions: String
    )

    private fun getGpuProps(fp: MainHook.DeviceFingerprint): GpuProps {
        val platform = fp.boardPlatform.lowercase()
        val hardware = fp.hardware.lowercase()

        // Default to Adreno 660 (High-end) if unknown, but try to match
        var renderer = "Adreno (TM) 660"
        var vendor = "Qualcomm"
        var version = "OpenGL ES 3.2 V@415.0 (GIT@abc123)"

        // Common Extensions
        val extAdreno = "GL_EXT_texture_filter_anisotropic GL_OES_EGL_image GL_OES_vertex_array_object GL_KHR_debug"
        val extMali = "GL_EXT_debug_marker GL_ARM_rgba8 GL_EXT_texture_format_BGRA8888 GL_OES_depth_texture"

        var extensions = extAdreno

        when {
            // Snapdragon 888
            platform == "lahaina" -> {
                renderer = "Adreno (TM) 660"
            }
            // Snapdragon 865 / 870
            platform == "kona" -> {
                renderer = "Adreno (TM) 650"
            }
            // Snapdragon 855 / 860
            platform == "msmnile" -> {
                renderer = "Adreno (TM) 640"
            }
            // Snapdragon 765G / 750G
            platform == "lito" || platform == "holi" -> {
                renderer = "Adreno (TM) 620"
            }
            // Snapdragon 720G / 730G
            platform == "atoll" || hardware.contains("sm7150") -> {
                renderer = "Adreno (TM) 618"
            }
            // Snapdragon 662 / 665
            platform == "bengal" || platform == "trinket" -> {
                renderer = "Adreno (TM) 610"
            }
            // MediaTek Dimensity 700
            platform == "mt6833" || hardware == "mt6833" -> {
                vendor = "ARM"
                renderer = "Mali-G57 MC2"
                version = "OpenGL ES 3.2 v1.r26p0-01rel0.f3f6c64"
                extensions = extMali
            }
            // MediaTek Helio G80 / G85
            platform == "mt6768" || platform == "mt6769" || hardware == "mt6768" -> {
                vendor = "ARM"
                renderer = "Mali-G52 MC2"
                version = "OpenGL ES 3.2 v1.r22p0-01rel0"
                extensions = extMali
            }
            // MediaTek Helio G90T / G95
            platform == "mt6785" || hardware == "mt6785" -> {
                vendor = "ARM"
                renderer = "Mali-G76 MC4"
                extensions = extMali
            }
            // Exynos 9610/9611
            platform == "exynos9610" -> {
                vendor = "ARM"
                renderer = "Mali-G72 MP3"
                extensions = extMali
            }
            // Exynos 850
            platform == "exynos850" -> {
                vendor = "ARM"
                renderer = "Mali-G52"
                extensions = extMali
            }
            // Exynos 9825
            platform == "exynos9825" -> {
                vendor = "ARM"
                renderer = "Mali-G76 MP12"
                extensions = extMali
            }
        }

        return GpuProps(renderer, vendor, version, extensions)
    }

    private const val EGL_VENDOR = "Android"
    private const val EGL_VERSION = "1.4 Android META-EGL"
    private const val EGL_EXTENSIONS = "EGL_KHR_image_base EGL_KHR_fence_sync EGL_ANDROID_image_native_buffer EGL_KHR_swap_buffers_with_damage EGL_KHR_gl_texture_2D_image EGL_EXT_create_context_robustness"

    fun init(classLoader: ClassLoader) {
        val prefs = XSharedPreferences("com.vortex", "vortex_prefs")
        prefs.reload()

        fun getStr(key: String, def: String): String {
            val raw = prefs.getString(key, null)
            return CryptoUtils.decrypt(raw) ?: def
        }

        fun getBool(key: String, def: Boolean): Boolean {
            val v = getStr(key, "")
            return if (v.isEmpty()) def else v.toBooleanStrictOrNull() ?: def
        }

        if (!getBool("gpu_spoof_enabled", false)) return

        val currentFingerprintId = getStr("profile", "Redmi 9")
        // Use MainHook's fingerprints to ensure correlation
        val fp = MainHook.DEVICE_FINGERPRINTS[currentFingerprintId] ?: MainHook.DEVICE_FINGERPRINTS["Redmi 9"]!!
        val gpuProps = getGpuProps(fp)

        try {
            XposedHelpers.findAndHookMethod(
                EGL14::class.java, "eglQueryString",
                android.opengl.EGLDisplay::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[1] as Int) {
                            EGL14.EGL_VENDOR -> param.result = EGL_VENDOR
                            EGL14.EGL_VERSION -> param.result = EGL_VERSION
                            EGL14.EGL_EXTENSIONS -> param.result = EGL_EXTENSIONS
                        }
                    }
                }
            )

            val glesHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    when (param.args[0] as Int) {
                        GLES20.GL_RENDERER -> param.result = gpuProps.renderer
                        GLES20.GL_VENDOR -> param.result = gpuProps.vendor
                        GLES20.GL_VERSION -> param.result = gpuProps.version
                        GLES20.GL_EXTENSIONS -> param.result = gpuProps.extensions
                    }
                }
            }

            XposedHelpers.findAndHookMethod(GLES20::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES30::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES31::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES32::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)

            if (BuildConfig.DEBUG) {
                XposedBridge.log("[Vortex] GPU Spoof Active: ${gpuProps.renderer} for $currentFingerprintId")
            }
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                XposedBridge.log("[Vortex] GPUSpoofer init failed: ${e.message}")
            }
        }
    }
}
