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
        prefs.makeWorldReadable()
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

        // Lee la semilla de la UI (stateless). Si no hay botón pulsado, usa el profile.
        val ja3SeedStr = getStr("ja3_seed", currentFingerprintId)

        try {
            val staticSeed = ja3SeedStr.hashCode().toLong()

            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.SSLSocket", classLoader,
                "setEnabledCipherSuites", Array<String>::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val suites = param.args[0] as? Array<String> ?: return
                        val suitesList = suites.toMutableList()

                        // CRÍTICO OPSEC: Random se instancia AQUÍ ADENTRO.
                        // Garantiza que cada socket baraje la lista exactamente en el mismo orden.
                        val random = Random(staticSeed)
                        suitesList.shuffle(random)

                        param.args[0] = suitesList.toTypedArray()
                    }
                }
            )
            XposedBridge.log("[Vortex] JA3/TLS Randomizer v8.1 ACTIVATED (Stable seed: $staticSeed)")
        } catch (e: Throwable) {
            XposedBridge.log("[Vortex] TLSRandomizer init failed: ${e.message}")
        }
    }
}
