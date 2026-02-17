package com.lancelot

import android.content.ContentResolver
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.accounts.Account
import android.telephony.TelephonyManager
import android.util.Base64
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "spoof_prefs"

        // --- Manual Encryption Implementation (AES) ---
        private const val ALGO = "AES"
        private val KEY_BYTES = byteArrayOf(
            0x4c, 0x61, 0x6e, 0x63, 0x65, 0x6c, 0x6f, 0x74,
            0x53, 0x74, 0x65, 0x61, 0x6c, 0x74, 0x68, 0x31
        ) // "LancelotStealth1"

        private fun decrypt(encrypted: String?): String? {
            if (encrypted.isNullOrEmpty()) return null
            return try {
                val key = SecretKeySpec(KEY_BYTES, ALGO)
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, key)
                val decodedBytes = Base64.decode(encrypted, Base64.NO_WRAP)
                String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                if (!encrypted.startsWith("ENC:")) encrypted else null
            }
        }

        // Cache para valores consistentes por sesión
        private var cachedImei: String? = null
        private var cachedImei2: String? = null
        private var cachedImsi: String? = null
        private var cachedIccid: String? = null
        private var cachedPhoneNumber: String? = null
        private var cachedAndroidId: String? = null
        private var cachedGsfId: String? = null
        private var cachedGaid: String? = null
        private var cachedWifiMac: String? = null
        private var cachedBtMac: String? = null
        private var cachedGmail: String? = null
        private var cachedSerial: String? = null

        // Mock Location settings
        private var mockLatitude: Double = 0.0
        private var mockLongitude: Double = 0.0
        private var mockAltitude: Double = 0.0
        private var mockAccuracy: Float = 10.0f
        private var mockBearing: Float = 0.0f
        private var mockSpeed: Float = 0.0f
        private var mockLocationEnabled: Boolean = false

        // US Carrier Data
        data class UsCarrier(
            val name: String,
            val mccMnc: String,
            val npas: List<String>
        )

        private val US_CARRIERS = listOf(
            UsCarrier("T-Mobile", "310260", listOf("206","253","360","425","509","564","347","646","917","929","718","212")),
            UsCarrier("AT&T", "310410", listOf("214","469","972","817","682","940","404","678","770","470","312","872")),
            UsCarrier("Verizon", "310012", listOf("201","551","732","848","908","973","609","856","862","732","908","862")),
            UsCarrier("Sprint (legacy)", "310120", listOf("312","773","847","224","331","630","708","872","779","815","618","309")),
            UsCarrier("US Cellular", "311580", listOf("217","309","618","815","319","563","920","414","262","608","715","906")),
            UsCarrier("Cricket", "310150", listOf("832","713","281","346","979","936","210","726","830","956","361","430")),
            UsCarrier("Metro PCS", "310260", listOf("305","786","954","754","561","407","321","689","386","352","904","727")),
            UsCarrier("Boost Mobile", "311870", listOf("323","213","310","424","818","747","626","562","661","760","442","951")),
            UsCarrier("Google Fi", "310260", listOf("202","703","571","240","301","410","443","667","301","202","571","703")),
            UsCarrier("Tracfone", "310410", listOf("786","305","954","561","407","321","352","863","941","239","850","904"))
        )

        // Public accessor for UI
        fun getUsCarriers(): List<UsCarrier> = US_CARRIERS

        // Full High-Fidelity Profile Map
        private val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9", device = "lancelot", product = "lancelot_global",
                hardware = "mt6768", board = "lancelot", bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.3.0.RJCMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6768", eglDriver = "mali", openGlEs = "196610",
                display = "V12.5.3.0.RJCMIXM", buildDescription = "lancelot_global-user 11 RP1A.200720.011 V12.5.3.0.RJCMIXM release-keys"
            ),
            "Redmi Note 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9", device = "merlin", product = "merlin_global",
                hardware = "mt6769", board = "merlin", bootloader = "unknown",
                fingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.2.0.RJOMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6769", eglDriver = "mali", openGlEs = "196610",
                display = "V12.5.2.0.RJOMIXM", buildDescription = "merlin_global-user 11 RP1A.200720.011 V12.5.2.0.RJOMIXM release-keys"
            ),
            "Redmi 9A" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9A", device = "dandelion", product = "dandelion_global",
                hardware = "mt6762", board = "dandelion", bootloader = "unknown",
                fingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.4.0.RCDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608",
                display = "V12.5.4.0.RCDMIXM", buildDescription = "dandelion_global-user 11 RP1A.200720.011 V12.5.4.0.RCDMIXM release-keys"
            ),
            "Redmi 9C" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9C", device = "angelica", product = "angelica_global",
                hardware = "mt6762", board = "angelica", bootloader = "unknown",
                fingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.1.0.RCRMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608",
                display = "V12.5.1.0.RCRMIXM", buildDescription = "angelica_global-user 11 RP1A.200720.011 V12.5.1.0.RCRMIXM release-keys"
            ),
            "Redmi Note 9S" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9S", device = "curtana", product = "curtana_global",
                hardware = "qcom", board = "curtana", bootloader = "unknown",
                fingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.1.0.RJWMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                display = "V12.5.1.0.RJWMIXM", buildDescription = "curtana_global-user 11 RKQ1.200826.002 V12.5.1.0.RJWMIXM release-keys"
            ),
            "Redmi Note 9 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9 Pro", device = "joyeuse", product = "joyeuse_global",
                hardware = "qcom", board = "joyeuse", bootloader = "unknown",
                fingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.3.0.RJZMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                display = "V12.5.3.0.RJZMIXM", buildDescription = "joyeuse_global-user 11 RKQ1.200826.002 V12.5.3.0.RJZMIXM release-keys"
            )
        )
    }

    data class DeviceFingerprint(
        val manufacturer: String,
        val brand: String,
        val model: String,
        val device: String,
        val product: String,
        val hardware: String,
        val board: String,
        val bootloader: String,
        val fingerprint: String,
        val buildId: String,
        val tags: String,
        val type: String,
        val radioVersion: String,
        val incremental: String,
        val sdkInt: Int,
        val release: String,
        val boardPlatform: String,   // ro.board.platform
        val eglDriver: String,       // ro.hardware.egl
        val openGlEs: String,        // ro.opengles.version
        val display: String,         // Build.DISPLAY
        val buildDescription: String // ro.build.description
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" ||
            lpparam.packageName.startsWith("com.android.") ||
            lpparam.packageName == "com.lancelot") {
            return
        }

        try {
            val prefsFile = File("/data/data/com.lancelot/shared_prefs/$PREFS_NAME.xml")
            val prefs = if (prefsFile.exists() && prefsFile.canRead()) {
                XSharedPreferences("com.lancelot", PREFS_NAME).apply {
                    reload()
                }
            } else {
                null
            }

            // Lectura de valores encriptados
            fun getEncryptedString(key: String, def: String): String {
                val raw = prefs?.getString(key, null)
                return if (raw != null && raw.startsWith("ENC:")) {
                    decrypt(raw.substring(4)) ?: def
                } else {
                    raw ?: def
                }
            }

            val profileName = getEncryptedString("profile", "Redmi 9 (Original)")
            val fingerprint = getDeviceFingerprint(profileName)

            // Cache initialization using encrypted values
            initializeCache(prefs, ::getEncryptedString)

            // Hooks
            hookBuildFields(lpparam, fingerprint, prefs, ::getEncryptedString)
            hookSystemProperties(lpparam, fingerprint, prefs, ::getEncryptedString)
            hookTelephonyManager(lpparam, prefs, ::getEncryptedString)
            hookUnifiedSettingsSecure(lpparam, prefs, ::getEncryptedString) // Unified Hook
            hookNetworkInterfaces(lpparam, prefs, ::getEncryptedString)
            hookLocation(lpparam, prefs, ::getEncryptedString)
            hookWebView(lpparam, fingerprint)
            hookAccountManager(lpparam)
            hookXposedDetection(lpparam) // Extended
            hookPackageManager(lpparam)
            hookApplicationFlags(lpparam)

        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("Lancelot Error: ${e.message}")
        }
    }

    private fun initializeCache(prefs: XSharedPreferences?, getString: (String, String) -> String) {
        if (cachedImei == null) cachedImei = getString("imei", generateValidImei())
        if (cachedImei2 == null) cachedImei2 = getString("imei2", generateValidImei())
        if (cachedAndroidId == null) cachedAndroidId = getString("android_id", generateRandomId(16))
        if (cachedGsfId == null) cachedGsfId = getString("gsf_id", generateRandomId(16))
        if (cachedGaid == null) cachedGaid = getString("gaid", generateRandomGaid())
        if (cachedWifiMac == null) cachedWifiMac = getString("wifi_mac", generateRandomMac())
        if (cachedBtMac == null) cachedBtMac = getString("bluetooth_mac", generateRandomMac())
        if (cachedGmail == null) cachedGmail = getString("gmail", "test.user" + (1000..9999).random() + "@gmail.com")
        if (cachedSerial == null) cachedSerial = getString("serial", generateRandomSerial())

        // Select a random US carrier for consistency
        val carrier = US_CARRIERS.random()

        val mccMnc = getString("mcc_mnc", carrier.mccMnc)
        if (cachedImsi == null) cachedImsi = mccMnc + (1..10).map { (0..9).random() }.joinToString("")

        // Use carrier-aware generators for consistency
        if (cachedIccid == null) cachedIccid = generateValidIccid(mccMnc)

        // Force US country
        val simCountry = "us"
        if (cachedPhoneNumber == null) cachedPhoneNumber = generatePhoneNumber(mccMnc)
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        val cleanName = profileName.replace(" - Android 11", "").trim()
        // Default to Redmi 9 if no match found
        return DEVICE_FINGERPRINTS.entries.find { it.key == cleanName }?.value ?: DEVICE_FINGERPRINTS["Redmi 9"]!!
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam,
                                 fingerprint: DeviceFingerprint,
                                 prefs: XSharedPreferences?,
                                 getString: (String, String) -> String) {
        try {
            val buildClass = Build::class.java
            XposedHelpers.setStaticObjectField(buildClass, "MANUFACTURER", fingerprint.manufacturer)
            XposedHelpers.setStaticObjectField(buildClass, "BRAND", fingerprint.brand)
            XposedHelpers.setStaticObjectField(buildClass, "MODEL", fingerprint.model)
            XposedHelpers.setStaticObjectField(buildClass, "DEVICE", fingerprint.device)
            XposedHelpers.setStaticObjectField(buildClass, "PRODUCT", fingerprint.product)
            XposedHelpers.setStaticObjectField(buildClass, "HARDWARE", fingerprint.hardware)
            XposedHelpers.setStaticObjectField(buildClass, "BOARD", fingerprint.board)
            XposedHelpers.setStaticObjectField(buildClass, "BOOTLOADER", fingerprint.bootloader)
            XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT", fingerprint.fingerprint)
            XposedHelpers.setStaticObjectField(buildClass, "ID", fingerprint.buildId)
            XposedHelpers.setStaticObjectField(buildClass, "TAGS", fingerprint.tags)
            XposedHelpers.setStaticObjectField(buildClass, "TYPE", fingerprint.type)

            XposedHelpers.setStaticObjectField(buildClass, "DISPLAY", fingerprint.display)
            XposedHelpers.setStaticObjectField(buildClass, "HOST", "pangu-build-component-system-177793")
            XposedHelpers.setStaticObjectField(buildClass, "USER", "builder")

            XposedHelpers.setStaticObjectField(buildClass, "DISPLAY", fingerprint.display)
            XposedHelpers.setStaticObjectField(buildClass, "HOST", "pangu-build-component-system-177793")
            XposedHelpers.setStaticObjectField(buildClass, "USER", "builder")

            XposedHelpers.setStaticObjectField(buildClass, "SERIAL", cachedSerial)

            val versionClass = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(versionClass, "SDK_INT", 30)
            XposedHelpers.setStaticObjectField(versionClass, "RELEASE", "11")
            XposedHelpers.setStaticObjectField(versionClass, "INCREMENTAL", fingerprint.incremental)

            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = cachedSerial
                        }
                    }
                )
            } catch (e: NoSuchMethodError) {}
        } catch (e: Throwable) {}
    }

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam,
                                      fingerprint: DeviceFingerprint,
                                      prefs: XSharedPreferences?,
                                      getString: (String, String) -> String) {
        try {
            val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    val res = when (key) {
                        "ro.product.manufacturer" -> fingerprint.manufacturer
                        "ro.product.brand" -> fingerprint.brand
                        "ro.product.model" -> fingerprint.model
                        "ro.product.device" -> fingerprint.device
                        "ro.product.name" -> fingerprint.product
                        "ro.hardware" -> fingerprint.hardware
                        "ro.product.board" -> fingerprint.board
                        "ro.bootloader" -> fingerprint.bootloader
                        "ro.build.fingerprint" -> fingerprint.fingerprint
                        "ro.build.id" -> fingerprint.buildId
                        "ro.build.tags" -> fingerprint.tags
                        "ro.build.type" -> fingerprint.type
                        "gsm.version.baseband" -> fingerprint.radioVersion
                        "ro.serialno" -> cachedSerial
                        "ro.build.version.release" -> "11"
                        "ro.build.version.sdk" -> "30"

                        "ro.debuggable"             -> "0"
                        "ro.secure"                 -> "1"
                        "ro.build.display.id"       -> fingerprint.display
                        "ro.build.description"      -> fingerprint.buildDescription
                        "ro.build.characteristics"  -> "default"
                        "ro.build.flavor"           -> "${fingerprint.product}-user"
                        "ro.vendor.build.fingerprint" -> fingerprint.fingerprint

                        // Hardware specific properties
                        "ro.board.platform"         -> fingerprint.boardPlatform
                        "ro.hardware.egl"           -> fingerprint.eglDriver
                        "ro.opengles.version"       -> fingerprint.openGlEs

                        // Partition specific properties
                        "ro.product.system.manufacturer" -> fingerprint.manufacturer
                        "ro.product.system.brand" -> fingerprint.brand
                        "ro.product.system.model" -> fingerprint.model
                        "ro.product.system.device" -> fingerprint.device
                        "ro.product.system.name" -> fingerprint.product

                        "ro.product.vendor.manufacturer" -> fingerprint.manufacturer
                        "ro.product.vendor.brand" -> fingerprint.brand
                        "ro.product.vendor.model" -> fingerprint.model
                        "ro.product.vendor.device" -> fingerprint.device
                        "ro.product.vendor.name" -> fingerprint.product

                        "ro.product.odm.manufacturer" -> fingerprint.manufacturer
                        "ro.product.odm.brand" -> fingerprint.brand
                        "ro.product.odm.model" -> fingerprint.model
                        "ro.product.odm.device" -> fingerprint.device
                        "ro.product.odm.name" -> fingerprint.product

                        "ro.product.product.manufacturer" -> fingerprint.manufacturer
                        "ro.product.product.brand" -> fingerprint.brand
                        "ro.product.product.model" -> fingerprint.model
                        "ro.product.product.device" -> fingerprint.device
                        "ro.product.product.name" -> fingerprint.product
                        else -> null
                    }
                    if (res != null) param.result = res
                }
            }

            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, hook)
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, String::class.java, hook)
        } catch (e: Throwable) {}
    }

    private fun hookTelephonyManager(lpparam: XC_LoadPackage.LoadPackageParam,
                                      prefs: XSharedPreferences?,
                                      getString: (String, String) -> String) {
        try {
            val tmClass = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(tmClass, "getDeviceId", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedImei } })
            try { XposedHelpers.findAndHookMethod(tmClass, "getDeviceId", Int::class.javaPrimitiveType, object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { val slot = param.args[0] as Int; param.result = if (slot == 0) cachedImei else cachedImei2 } }) } catch (e: NoSuchMethodError) {}
            try { XposedHelpers.findAndHookMethod(tmClass, "getImei", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedImei } })
                  XposedHelpers.findAndHookMethod(tmClass, "getImei", Int::class.javaPrimitiveType, object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { val slot = param.args[0] as Int; param.result = if (slot == 0) cachedImei else cachedImei2 } }) } catch (e: NoSuchMethodError) {}

            XposedHelpers.findAndHookMethod(tmClass, "getSubscriberId", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedImsi } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimSerialNumber", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedIccid } })
            XposedHelpers.findAndHookMethod(tmClass, "getLine1Number", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedPhoneNumber } })

            // Extract MCC/MNC from cached IMSI (first 6 chars usually) or default
            val mccMnc = if (cachedImsi != null && cachedImsi!!.length >= 6) cachedImsi!!.substring(0, 6) else "310260"
            val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS[1] // Default T-Mobile

            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperator", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperator", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperatorName", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperatorName", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkCountryIso", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = "us" } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimCountryIso", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = "us" } })

        } catch (e: Throwable) {}
    }

    private fun hookUnifiedSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam,
                                    prefs: XSharedPreferences?,
                                    getString: (String, String) -> String) {
        try {
            val settingsSecureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(settingsSecureClass, "getString",
                ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.args[1] as String
                        when (name) {
                            Settings.Secure.ANDROID_ID -> param.result = cachedAndroidId
                            "advertising_id" -> param.result = cachedGaid
                            "gsf_id", "android_id_gsf" -> param.result = cachedGsfId
                        }
                    }
                }
            )
        } catch (e: Throwable) {}
    }

    private fun hookNetworkInterfaces(lpparam: XC_LoadPackage.LoadPackageParam,
                                       prefs: XSharedPreferences?,
                                       getString: (String, String) -> String) {
        try {
            val niClass = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(niClass, "getHardwareAddress", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val ni = param.thisObject as NetworkInterface
                        val name = ni.name
                        when {
                            name.startsWith("wlan") -> param.result = macStringToBytes(cachedWifiMac!!)
                            name.startsWith("bt") || name.startsWith("bluetooth") -> param.result = macStringToBytes(cachedBtMac!!)
                        }
                    } catch (e: Exception) {}
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookLocation(lpparam: XC_LoadPackage.LoadPackageParam,
                            prefs: XSharedPreferences?,
                            getString: (String, String) -> String) {
        try {
            // Read directly from boolean (unencrypted) but coords are strings
            mockLocationEnabled = prefs?.getBoolean("mock_location_enabled", false) ?: false
            val latStr = getString("mock_latitude", "0.0")
            val lonStr = getString("mock_longitude", "0.0")
            val altStr = getString("mock_altitude", "0.0")
            val accStr = getString("mock_accuracy", "10.0")

            mockLatitude = latStr.toDoubleOrNull() ?: 0.0
            mockLongitude = lonStr.toDoubleOrNull() ?: 0.0
            mockAltitude = altStr.toDoubleOrNull() ?: 0.0
            mockAccuracy = accStr.toFloatOrNull() ?: 10.0f

            // Random variability for realism
            mockBearing = (Math.random() * 360.0).toFloat()
            mockSpeed = (Math.random() * 5.0).toFloat()

            if (!mockLocationEnabled) return

            val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(locationClass, "getLatitude", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockLatitude } })
            XposedHelpers.findAndHookMethod(locationClass, "getLongitude", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockLongitude } })
            XposedHelpers.findAndHookMethod(locationClass, "getAltitude", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockAltitude } })
            XposedHelpers.findAndHookMethod(locationClass, "hasAltitude", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = true } })
            XposedHelpers.findAndHookMethod(locationClass, "getAccuracy", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockAccuracy } })
            XposedHelpers.findAndHookMethod(locationClass, "getBearing", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockBearing } })
            XposedHelpers.findAndHookMethod(locationClass, "hasBearing", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = true } })
            XposedHelpers.findAndHookMethod(locationClass, "getSpeed", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = mockSpeed } })
            XposedHelpers.findAndHookMethod(locationClass, "hasSpeed", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { if (mockLocationEnabled) param.result = true } })

            val locationManagerClass = XposedHelpers.findClass("android.location.LocationManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(locationManagerClass, "getLastKnownLocation", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (mockLocationEnabled) {
                        val location = Location(param.args[0] as String)
                        location.latitude = mockLatitude
                        location.longitude = mockLongitude
                        location.altitude = mockAltitude
                        location.accuracy = mockAccuracy
                        location.bearing = mockBearing
                        location.speed = mockSpeed
                        location.time = System.currentTimeMillis()
                        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        param.result = location
                    }
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
        try {
            val webSettingsClass = XposedHelpers.findClass("android.webkit.WebSettings", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(webSettingsClass, "getUserAgentString", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val originalUserAgent = param.result as String
                    val newUserAgent = originalUserAgent.replace(Regex(";\\s+[^;]+?\\s+Build/"), "; ${fingerprint.model} Build/")
                    param.result = newUserAgent
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookAccountManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val amClass = XposedHelpers.findClass("android.accounts.AccountManager", lpparam.classLoader)

            // Hook getAccounts()
            XposedHelpers.findAndHookMethod(amClass, "getAccounts", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val accounts = param.result as Array<Account>
                    // Remove existing google accounts and add fake one
                    val newAccounts = accounts.filter { it.type != "com.google" }.toMutableList()
                    if (cachedGmail != null) {
                        newAccounts.add(Account(cachedGmail, "com.google"))
                    }
                    param.result = newAccounts.toTypedArray()
                }
            })

            // Hook getAccountsByType(String type)
            XposedHelpers.findAndHookMethod(amClass, "getAccountsByType", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val type = param.args[0] as String
                    if (type == "com.google") {
                        if (cachedGmail != null) {
                            param.result = arrayOf(Account(cachedGmail, "com.google"))
                        } else {
                            param.result = emptyArray<Account>()
                        }
                    }
                }
            })
        } catch (e: Throwable) {
            XposedBridge.log("AccountManager hook error: ${e.message}")
        }
    }

    private fun hookXposedDetection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(Throwable::class.java, "getStackTrace", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val stackTrace = param.result as Array<StackTraceElement>
                    val filtered = stackTrace.filterNot {
                        it.className.contains("xposed", ignoreCase = true) ||
                        it.className.contains("edxposed", ignoreCase = true) ||
                        it.className.contains("lsposed", ignoreCase = true) ||
                        it.className.contains("de.robv", ignoreCase = true) ||
                        it.className.contains("zygisk", ignoreCase = true) ||
                        it.className.contains("shamiko", ignoreCase = true) ||
                        it.className.contains("riru", ignoreCase = true) ||
                        it.className.contains("taichi", ignoreCase = true) ||
                        it.className.contains("bridge", ignoreCase = true) ||
                        it.className.contains("hook", ignoreCase = true)
                    }
                    param.result = filtered.toTypedArray()
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                lpparam.classLoader,
                "getInstallerPackageName",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Snapchat verifica que fue instalado desde Play Store
                        param.result = "com.android.vending"
                    }
                }
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("PackageManager hook error: ${e.message}")
        }
    }

    private fun hookApplicationFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Limpiar FLAG_DEBUGGABLE (0x2) y FLAG_ALLOW_MOCK_LOCATION (0x100)
            XposedHelpers.findAndHookMethod(
                "android.content.pm.ApplicationInfo",
                lpparam.classLoader,
                "getFlags",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val flags = param.result as? Int ?: return
                        param.result = flags and 0x100.inv() and 0x2.inv()
                    }
                }
            )
        } catch (e: Throwable) {
        }
    }

    // ========== GENERADORES Y UTILIDADES ==========

    private fun generateRandomId(length: Int): String = (1..length).map { "0123456789abcdef".random() }.joinToString("")
    private fun generateValidImei(): String {
        // Updated with verified real TACs
        val tac = listOf("86413405", "86413404", "35271311", "35361311").random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }
    private fun generateValidIccid(mccMnc: String): String {
        // ICCID format: 89 + Country(1) + MNC(2/3) + Issuer + Account...
        // US (CC=1): 89 + 1 + MNC(3) + ...
        val mnc = if (mccMnc.length >= 6) mccMnc.substring(3) else "260"
        val issuer = (10..99).random().toString()
        val prefixPart = "891$mnc$issuer"

        val accountLen = 18 - prefixPart.length
        val account = (1..accountLen).map { (0..9).random() }.joinToString("")
        val base = prefixPart + account
        return base + luhnChecksum(base)
    }

    private fun generatePhoneNumber(mccMnc: String): String {
        // Find carrier matching MNC to use real area codes (NPAs)
        val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS.random()
        val npa = if (carrier.npas.isNotEmpty()) carrier.npas.random() else "202" // Default fallback

        // NXX: 200-999 (exclude 555)
        var nxx = (200..999).random()
        if (nxx == 555) nxx = 556

        val subscriber = (0..9999).random().toString().padStart(4, '0')
        return "+1$npa$nxx$subscriber"
    }
    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var digit = number[i].digitToInt()
            // FIX: Luhn position adjustment
            if ((number.length - i + 1) % 2 == 0) digit *= 2
            if (digit > 9) digit -= 9
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }
    private fun generateRandomSerial(): String = (1..12).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
    private fun generateRandomGaid(): String = "${generateRandomId(8)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(12)}"
    private fun generateRandomMac(): String {
        val firstByte = (0x02 or (kotlin.random.Random.nextInt(256) and 0xFC)).toString(16).padStart(2, '0').uppercase()
        val rest = (1..5).map { kotlin.random.Random.nextInt(256).toString(16).padStart(2, '0').uppercase() }.joinToString(":")
        return "$firstByte:$rest"
    }
    private fun macStringToBytes(mac: String): ByteArray {
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) throw IllegalArgumentException()
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (e: Exception) { byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00) }
    }
}
