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

object GPUSpoofer {
    private const val GLES_RENDERER = "Adreno (TM) 660"
    private const val GLES_VENDOR = "Qualcomm"
    private const val GLES_VERSION = "OpenGL ES 3.2 V@415.0 (GIT@abc123)"
    private const val GLES_EXTENSIONS = "GL_EXT_texture_filter_anisotropic GL_OES_EGL_image GL_OES_vertex_array_object"

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

        // Using 'profile' as the source of truth for fingerprint identity, as per MainHook usage
        val currentFingerprintId = getStr("profile", "Redmi 9")
        // Note: Xposed modules cannot easily write back to prefs from the target process.
        // Logic relying on saving 'gpu_last_fingerprint_id' is skipped in this context.

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
                        GLES20.GL_RENDERER -> param.result = GLES_RENDERER
                        GLES20.GL_VENDOR -> param.result = GLES_VENDOR
                        GLES20.GL_VERSION -> param.result = GLES_VERSION
                        GLES20.GL_EXTENSIONS -> param.result = GLES_EXTENSIONS
                    }
                }
            }

            XposedHelpers.findAndHookMethod(GLES20::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES30::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES31::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)
            XposedHelpers.findAndHookMethod(GLES32::class.java, "glGetString", Int::class.javaPrimitiveType, glesHook)

            XposedBridge.log("[Vortex] EGL/GLES GPU Spoof v8.0 ACTIVATED")
        } catch (e: Throwable) {
            XposedBridge.log("[Vortex] GPUSpoofer init failed: ${e.message}")
        }
    }
}
