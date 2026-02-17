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
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "spoof_prefs"
        private const val ALGO = "AES"
        // Matches PrefsManager.kt
        private val KEY_BYTES = byteArrayOf(
            76, 97, 110, 99, 101, 108, 111, 116,
            83, 116, 101, 97, 108, 116, 104, 49
        )
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private fun decrypt(encrypted: String?): String? {
            if (encrypted.isNullOrEmpty()) return null
            return try {
                if (encrypted.startsWith("GCM:")) {
                    val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
                    val iv = decodedBytes.copyOfRange(0, GCM_IV_LENGTH)
                    val ciphertext = decodedBytes.copyOfRange(GCM_IV_LENGTH, decodedBytes.size)

                    val key = SecretKeySpec(KEY_BYTES, ALGO)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                    cipher.init(Cipher.DECRYPT_MODE, key, spec)

                    String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
                } else if (encrypted.startsWith("ENC:")) {
                    // Legacy Support
                    val key = SecretKeySpec(KEY_BYTES, ALGO)
                    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, key)
                    val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
                    String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
                } else {
                    null // Safe fallback
                }
            } catch (e: Exception) {
                null // Safe fallback
            }
        }

        // Cache for consistent values per session
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

        fun getUsCarriers(): List<UsCarrier> = US_CARRIERS

        val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9" to DeviceFingerprint("Xiaomi", "Redmi", "Redmi 9", "lancelot", "lancelot_global", "mt6768", "lancelot", "unknown", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47", "V12.5.3.0.RJCMIXM", 30, "11", "mt6768", "mali", "196610", "MT6768", "zygote64_32", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "V12.5.3.0.RJCMIXM", "lancelot_global-user 11 RP1A.200720.011 V12.5.3.0.RJCMIXM release-keys", "lancelot_global-user", "pangu-build-component-system-177793", "builder", "1632960000", "2021-09-01", "REL", "0"),
             "Redmi Note 9" to DeviceFingerprint("Xiaomi", "Redmi", "Redmi Note 9", "merlin", "merlin_global", "mt6769", "merlin", "unknown", "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47", "V12.5.2.0.RJOMIXM", 30, "11", "mt6769", "mali", "196610", "MT6769", "zygote64_32", "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys", "V12.5.2.0.RJOMIXM", "merlin_global-user 11 RP1A.200720.011 V12.5.2.0.RJOMIXM release-keys", "merlin_global-user", "pangu-build-component-system-177793", "builder", "1632960000", "2021-09-01", "REL", "0"),
            "Redmi 9A" to DeviceFingerprint("Xiaomi", "Redmi", "Redmi 9A", "dandelion", "dandelion_global", "mt6762", "dandelion", "unknown", "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47", "V12.5.4.0.RCDMIXM", 30, "11", "mt6762", "pvrsrvkm", "196608", "MT6762", "zygote64_32", "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys", "V12.5.4.0.RCDMIXM", "dandelion_global-user 11 RP1A.200720.011 V12.5.4.0.RCDMIXM release-keys", "dandelion_global-user", "pangu-build-component-system-177793", "builder", "1633046400", "2021-10-01", "REL", "0"),
            "Redmi 9C" to DeviceFingerprint("Xiaomi", "Redmi", "Redmi 9C", "angelica", "angelica_global", "mt6762", "angelica", "unknown", "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47", "V12.5.1.0.RCRMIXM", 30, "11", "mt6762", "pvrsrvkm", "196608", "MT6762", "zygote64_32", "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys", "V12.5.1.0.RCRMIXM", "angelica_global-user 11 RP1A.200720.011 V12.5.1.0.RCRMIXM release-keys", "angelica_global-user", "pangu-build-component-system-177793", "builder", "1632960000", "2021-09-01", "REL", "0"),
            "POCO X3 NFC" to DeviceFingerprint("Xiaomi", "POCO", "POCO X3 NFC", "surya", "surya_global", "qcom", "surya", "unknown", "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys", "RKQ1.200826.002", "release-keys", "user", "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1", "V12.5.7.0.RJGMIXM", 30, "11", "atoll", "adreno", "196610", "SM7150", "zygote64_32", "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys", "V12.5.7.0.RJGMIXM", "surya_global-user 11 RKQ1.200826.002 V12.5.7.0.RJGMIXM release-keys", "surya_global-user", "pangu-build-component-system-177793", "builder", "1634860800", "2021-10-01", "REL", "0"),
            "Samsung Galaxy A52" to DeviceFingerprint("samsung", "samsung", "SM-A525F", "a52q", "a52qxx", "qcom", "a52q", "A525FXXU4CVJB", "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys", "RP1A.200720.012", "release-keys", "user", "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1", "A525FXXU4CVJB", 30, "11", "trinket", "adreno", "196610", "SM7125", "zygote64_32", "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys", "RP1A.200720.012.A525FXXU4CVJB", "a52qxx-user 11 RP1A.200720.012 A525FXXU4CVJB release-keys", "a52qxx-user", "21R3NF12", "dpi", "1639497600", "2021-12-01", "REL", "0"),
            "Google Pixel 5" to DeviceFingerprint("Google", "google", "Pixel 5", "redfin", "redfin", "qcom", "redfin", "r8-0.3-7219051", "google/redfin/redfin:11/RQ3A.210805.001.A1/7512229:user/release-keys", "RQ3A.210805.001.A1", "release-keys", "user", "g725-00164-210812-B-7522969", "7512229", 30, "11", "lito", "adreno", "196610", "SM7250", "zygote64_32", "google/redfin_vend/redfin:11/RQ3A.210805.001.A1/7512229:vendor/release-keys", "RQ3A.210805.001.A1", "redfin-user 11 RQ3A.210805.001.A1 7512229 release-keys", "redfin-user", "abfarm-release-rbe-64.hot.corp.google.com", "android-build", "1628100000", "2021-08-05", "REL", "0")
        )
    }

    data class DeviceFingerprint(
        val manufacturer: String, val brand: String, val model: String, val device: String, val product: String,
        val hardware: String, val board: String, val bootloader: String, val fingerprint: String, val buildId: String,
        val tags: String, val type: String, val radioVersion: String, val incremental: String, val sdkInt: Int,
        val release: String, val boardPlatform: String, val eglDriver: String, val openGlEs: String,
        val hardwareChipname: String, val zygote: String, val vendorFingerprint: String, val display: String,
        val buildDescription: String, val buildFlavor: String, val buildHost: String, val buildUser: String,
        val buildDateUtc: String, val securityPatch: String, val buildVersionCodename: String, val buildVersionPreviewSdk: String
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" ||
            lpparam.packageName.startsWith("com.android.") ||
            lpparam.packageName == "com.lancelot") {
            return
        }

        try {
            val prefs = XSharedPreferences("com.lancelot", PREFS_NAME)
            prefs.reload()

            fun getEncryptedString(key: String, def: String): String {
                val raw = prefs.getString(key, null)
                return if (raw != null) {
                    decrypt(raw) ?: def
                } else {
                    def
                }
            }

            val profileName = getEncryptedString("profile", "Redmi 9")
            val fingerprint = getDeviceFingerprint(profileName)

            initializeCache(prefs, ::getEncryptedString)

            hookBuildFields(lpparam, fingerprint, prefs, ::getEncryptedString)
            hookSystemProperties(lpparam, fingerprint, prefs, ::getEncryptedString)
            hookTelephonyManager(lpparam, prefs, ::getEncryptedString)
            hookUnifiedSettingsSecure(lpparam, prefs, ::getEncryptedString)
            hookNetworkInterfaces(lpparam, prefs, ::getEncryptedString)
            hookLocation(lpparam, prefs, ::getEncryptedString)
            hookWebView(lpparam, fingerprint)
            hookAccountManager(lpparam)
            hookXposedDetection(lpparam)
            hookPackageManager(lpparam)
            hookApplicationFlags(lpparam)

        } catch (e: Throwable) {
            try {
                if (BuildConfig.DEBUG) XposedBridge.log("Lancelot Error: ${e.message}")
            } catch (ex: Throwable) {}
        }
    }

    private fun initializeCache(prefs: XSharedPreferences, getString: (String, String) -> String) {
        if (cachedImei == null) cachedImei = getString("imei", generateValidImei())
        if (cachedImei2 == null) cachedImei2 = getString("imei2", generateValidImei())
        if (cachedAndroidId == null) cachedAndroidId = getString("android_id", generateRandomId(16))
        if (cachedGsfId == null) cachedGsfId = getString("gsf_id", generateRandomId(16))
        if (cachedGaid == null) cachedGaid = getString("gaid", generateRandomGaid())
        if (cachedWifiMac == null) cachedWifiMac = getString("wifi_mac", generateRandomMac())
        if (cachedBtMac == null) cachedBtMac = getString("bluetooth_mac", generateRandomMac())
        if (cachedGmail == null) cachedGmail = getString("gmail", "test.user" + (1000..9999).random() + "@gmail.com")
        if (cachedSerial == null) cachedSerial = getString("serial", generateRandomSerial())

        val defaultMccMnc = US_CARRIERS.random().mccMnc
        val mccMnc = getString("mcc_mnc", defaultMccMnc)

        if (cachedImsi == null) cachedImsi = mccMnc + (1..10).map { (0..9).random() }.joinToString("")
        if (cachedIccid == null) cachedIccid = generateValidIccid(mccMnc)
        if (cachedPhoneNumber == null) cachedPhoneNumber = generatePhoneNumber(mccMnc)
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        val cleanName = profileName.replace(" - Android 11", "").trim()
        return DEVICE_FINGERPRINTS.entries.find { it.key == cleanName }?.value
            ?: DEVICE_FINGERPRINTS.values.firstOrNull()
            ?: DeviceFingerprint("Xiaomi", "Redmi", "Redmi 9", "lancelot", "lancelot_global", "mt6768", "lancelot", "unknown", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47", "V12.5.3.0.RJCMIXM", 30, "11", "mt6768", "mali", "196610", "MT6768", "zygote64_32", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "V12.5.3.0.RJCMIXM", "lancelot_global-user", "lancelot_global-user", "pangu", "builder", "1632960000", "2021-09-01", "REL", "0")
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam,
                                 fingerprint: DeviceFingerprint,
                                 prefs: XSharedPreferences,
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
            XposedHelpers.setStaticObjectField(buildClass, "HOST", fingerprint.buildHost)
            XposedHelpers.setStaticObjectField(buildClass, "USER", fingerprint.buildUser)
            XposedHelpers.setStaticLongField(buildClass, "TIME", fingerprint.buildDateUtc.toLong() * 1000)

            XposedHelpers.setStaticObjectField(buildClass, "SERIAL", cachedSerial)

            val versionClass = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(versionClass, "SDK_INT", fingerprint.sdkInt)
            XposedHelpers.setStaticObjectField(versionClass, "RELEASE", fingerprint.release)
            XposedHelpers.setStaticObjectField(versionClass, "INCREMENTAL", fingerprint.incremental)
            XposedHelpers.setStaticObjectField(versionClass, "SECURITY_PATCH", fingerprint.securityPatch)
            XposedHelpers.setStaticObjectField(versionClass, "CODENAME", fingerprint.buildVersionCodename)
            XposedHelpers.setStaticIntField(versionClass, "PREVIEW_SDK_INT", fingerprint.buildVersionPreviewSdk.toInt())

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
                                      prefs: XSharedPreferences,
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
                        "ro.build.version.release" -> fingerprint.release
                        "ro.build.version.sdk" -> fingerprint.sdkInt.toString()

                        "ro.debuggable"             -> "0"
                        "ro.secure"                 -> "1"
                        "ro.build.display.id"       -> fingerprint.display
                        "ro.build.description"      -> fingerprint.buildDescription
                        "ro.build.characteristics"  -> "default"
                        "ro.build.flavor"           -> fingerprint.buildFlavor
                        "ro.vendor.build.fingerprint" -> fingerprint.vendorFingerprint

                        "ro.board.platform"         -> fingerprint.boardPlatform
                        "ro.hardware.egl"           -> fingerprint.eglDriver
                        "ro.opengles.version"       -> fingerprint.openGlEs
                        "ro.hardware.chipname"      -> fingerprint.hardwareChipname
                        "ro.zygote"                 -> fingerprint.zygote

                        "ro.build.host"             -> fingerprint.buildHost
                        "ro.build.user"             -> fingerprint.buildUser
                        "ro.build.date.utc"         -> fingerprint.buildDateUtc
                        "ro.build.version.security_patch" -> fingerprint.securityPatch
                        "ro.build.version.codename" -> fingerprint.buildVersionCodename
                        "ro.build.version.preview_sdk" -> fingerprint.buildVersionPreviewSdk

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

                        "ro.build.version.all_codenames" -> fingerprint.buildVersionCodename
                        "ro.build.version.min_supported_target_sdk" -> "23"
                        "ro.kernel.qemu" -> "0"
                        "persist.sys.usb.config" -> "none"
                        "service.adb.root" -> "0"

                        "ro.boot.serialno" -> cachedSerial
                        "ro.boot.hardware" -> fingerprint.hardware
                        "ro.boot.bootloader" -> fingerprint.bootloader
                        "ro.boot.verifiedbootstate" -> "green"
                        "ro.boot.flash.locked" -> "1"
                        "ro.boot.vbmeta.device_state" -> "locked"
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
                                      prefs: XSharedPreferences,
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

            val mccMnc = if (cachedImsi != null && cachedImsi!!.length >= 6) cachedImsi!!.substring(0, 6) else "310260"
            val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS[1]

            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperator", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperator", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperatorName", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperatorName", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkCountryIso", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = "us" } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimCountryIso", object : XC_MethodHook() { override fun afterHookedMethod(param: MethodHookParam) { param.result = "us" } })

        } catch (e: Throwable) {}
    }

    private fun hookUnifiedSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam,
                                    prefs: XSharedPreferences,
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
                                       prefs: XSharedPreferences,
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
                            prefs: XSharedPreferences,
                            getString: (String, String) -> String) {
        try {
            mockLocationEnabled = prefs.getBoolean("mock_location_enabled", false)
            val latStr = getString("mock_latitude", "0.0")
            val lonStr = getString("mock_longitude", "0.0")
            val altStr = getString("mock_altitude", "0.0")
            val accStr = getString("mock_accuracy", "10.0")

            mockLatitude = latStr.toDoubleOrNull() ?: 0.0
            mockLongitude = lonStr.toDoubleOrNull() ?: 0.0
            mockAltitude = altStr.toDoubleOrNull() ?: 0.0
            mockAccuracy = accStr.toFloatOrNull() ?: 10.0f

            val random = Random()
            mockBearing = random.nextFloat() * 360.0f
            mockSpeed = random.nextFloat() * 5.0f

            // Gaussian Noise
            mockAccuracy = (mockAccuracy + (random.nextGaussian() * 2.0)).toFloat().coerceAtLeast(1.0f)
            mockAltitude = mockAltitude + (random.nextGaussian() * 0.5)

            // Small jitter for location
            val jitterDeg = 0.00001
            mockLatitude += random.nextGaussian() * jitterDeg
            mockLongitude += random.nextGaussian() * jitterDeg

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

            try {
                XposedHelpers.findAndHookMethod(locationClass, "isFromMockProvider", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) param.result = false
                    }
                })
            } catch (e: NoSuchMethodError) {}

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

                        try {
                            val setMockMethod = Location::class.java.getDeclaredMethod("setIsFromMockProvider", Boolean::class.javaPrimitiveType)
                            setMockMethod.isAccessible = true
                            setMockMethod.invoke(location, false)
                        } catch (e: Exception) {}

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

            XposedHelpers.findAndHookMethod(amClass, "getAccounts", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val accounts = param.result as Array<Account>
                    val newAccounts = accounts.filter { it.type != "com.google" }.toMutableList()
                    if (cachedGmail != null) {
                        newAccounts.add(Account(cachedGmail, "com.google"))
                    }
                    param.result = newAccounts.toTypedArray()
                }
            })

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
            if (BuildConfig.DEBUG) XposedBridge.log("AccountManager hook error: ${e.message}")
        }
    }

    private fun hookXposedDetection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(Throwable::class.java, "getStackTrace", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val stackTrace = param.result as Array<StackTraceElement>
                    val filtered = stackTrace.filterNot {
                        it.className.startsWith("de.robv.android.xposed") ||
                        it.className.startsWith("io.github.lsposed") ||
                        it.className.startsWith("com.elderdrivers.riru") ||
                        it.className.startsWith("org.lsposed") ||
                        it.className.startsWith("top.canyie.dreamland") ||
                        it.className.startsWith("me.weishu.exposed") ||
                        it.className.contains("LSPosed", ignoreCase = true) ||
                        it.className.contains("XposedBridge", ignoreCase = true)
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
        val tac = listOf("86413405", "86413404", "35271311", "35361311").random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }
    private fun generateValidIccid(mccMnc: String): String {
        val mnc = if (mccMnc.length >= 6) mccMnc.substring(3) else "260"
        val issuer = (10..99).random().toString()
        val prefixPart = "891$mnc$issuer"

        val accountLen = 18 - prefixPart.length
        val account = (1..accountLen).map { (0..9).random() }.joinToString("")
        val base = prefixPart + account
        return base + luhnChecksum(base)
    }

    private fun generatePhoneNumber(mccMnc: String): String {
        val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS.random()
        val npa = if (carrier.npas.isNotEmpty()) carrier.npas.random() else "202"

        var nxx = (200..999).random()
        if (nxx == 555) nxx = 556

        val subscriber = (0..9999).random().toString().padStart(4, '0')
        return "+1$npa$nxx$subscriber"
    }
    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var digit = number[i].digitToInt()
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
