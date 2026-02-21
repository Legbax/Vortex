package com.vortex.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XSharedPreferences
import com.vortex.utils.CryptoUtils
import java.util.Random

object TLSRandomizer {
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

        if (!getBool("ja3_randomizer_enabled", false)) return

        val currentFingerprintId = getStr("profile", "Redmi 9")
        val forceRefresh = getBool("ja3_force_refresh", false)

        try {
            // If forceRefresh is on (and we can't reset it in Xposed), we generate a random seed every time.
            // Otherwise, we use a deterministic seed based on the profile to keep JA3 stable per session/profile.
            val seed = if (forceRefresh) {
                System.currentTimeMillis()
            } else {
                currentFingerprintId.hashCode().toLong()
            }
            val random = Random(seed)

            // Intercept SSLSocket (base of JSSE/Conscrypt in Android) to shuffle Cipher Suites
            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.SSLSocket", classLoader,
                "setEnabledCipherSuites", Array<String>::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val suites = param.args[0] as? Array<String> ?: return
                        val suitesList = suites.toMutableList()

                        // Shuffling the list order changes the JA3 hash generated in ClientHello
                        suitesList.shuffle(random)

                        param.args[0] = suitesList.toTypedArray()
                    }
                }
            )
            XposedBridge.log("[Vortex] JA3/TLS Randomizer v8.0 ACTIVATED (Seed: $seed)")
        } catch (e: Throwable) {
            XposedBridge.log("[Vortex] TLSRandomizer init failed: ${e.message}")
        }
    }
}
