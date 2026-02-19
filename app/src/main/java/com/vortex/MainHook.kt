package com.vortex

import android.accounts.Account
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.net.NetworkInterface
import java.util.Random
import com.vortex.utils.CryptoUtils

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "Vortex"
        private const val PREFS_NAME = "vortex_prefs"

        fun log(message: String) {
            XposedBridge.log("[$TAG] $message")
        }

        fun logError(message: String, throwable: Throwable? = null) {
            XposedBridge.log("[$TAG][ERROR] $message")
            throwable?.let { XposedBridge.log(it) }
        }

        private val TARGET_APPS = setOf(
            "com.snapchat.android",
            "com.instagram.android",
            "com.facebook.katana",
            "com.whatsapp",
            "com.google.android.gms",
            "com.google.android.gms.unstable",
            "com.android.vending"
        )

        @Volatile private var cachedImei: String? = null
        @Volatile private var cachedImei2: String? = null
        @Volatile private var cachedImsi: String? = null
        @Volatile private var cachedIccid: String? = null
        @Volatile private var cachedPhoneNumber: String? = null
        @Volatile private var cachedAndroidId: String? = null
        @Volatile private var cachedGsfId: String? = null
        @Volatile private var cachedGaid: String? = null
        @Volatile private var cachedWifiMac: String? = null
        @Volatile private var cachedBtMac: String? = null
        @Volatile private var cachedGmail: String? = null
        @Volatile private var cachedSerial: String? = null
        @Volatile private var cachedMccMnc: String? = null
        @Volatile private var cachedMediaDrmId: String? = null
        @Volatile private var cachedWifiSsid: String? = null
        @Volatile private var cachedWifiBssid: String? = null

        @Volatile private var mockLatitude: Double = 0.0
        @Volatile private var mockLongitude: Double = 0.0
        @Volatile private var mockAltitude: Double = 0.0
        @Volatile private var mockAccuracy: Float = 10.0f
        @Volatile private var mockBearing: Float = 0.0f
        @Volatile private var mockSpeed: Float = 0.0f
        @Volatile private var mockLocationEnabled: Boolean = false
        @Volatile private var mockLocationInitialized: Boolean = false // [FIX D9]

        data class UsCarrier(
            val name: String,
            val mccMnc: String,
            val spn: String,
            val npas: List<String>
        )

        private val US_CARRIERS = listOf(
            UsCarrier("T-Mobile",       "310260", "T-Mobile",       listOf("206","253","360","425","509","347","646","917","929","718","212")),
            UsCarrier("AT&T",           "310410", "AT&T",           listOf("214","469","972","817","682","940","404","678","770","470","312")),
            UsCarrier("Verizon",        "310012", "Verizon",        listOf("201","551","732","848","908","973","609","856","862","908")),
            UsCarrier("Sprint (legacy)","310120", "Sprint",         listOf("312","773","847","224","331","630","708","872","779","815")),
            UsCarrier("US Cellular",    "311580", "US Cellular",    listOf("217","309","618","815","319","563","920","414","262","608")),
            UsCarrier("Cricket",        "310150", "Cricket",        listOf("832","713","281","346","979","936","210","726","830","956")),
            UsCarrier("Metro by T-Mobile","310490","Metro by T-Mobile",listOf("305","786","954","754","561","407","321","689","386","352")),
            UsCarrier("Boost Mobile",   "311870", "Boost Mobile",   listOf("323","213","310","424","818","747","626","562","661","760")),
            UsCarrier("Google Fi",      "310260", "Google Fi",      listOf("202","703","571","240","301","410","443","667")),
            UsCarrier("Tracfone",       "310410", "Tracfone",       listOf("786","305","954","561","407","321","352","863","941","239"))
        )

        fun getUsCarriers(): List<UsCarrier> = US_CARRIERS

        val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9",
                device = "lancelot", product = "lancelot_global",
                hardware = "mt6768", board = "lancelot", bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.3.0.RJCMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6768", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6768", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "lancelot_global-user 11 RP1A.200720.011 V12.5.3.0.RJCMIXM release-keys",
                buildFlavor = "lancelot_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO X3 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "M2102J20SG",
                device = "vayu", product = "vayu_global",
                hardware = "qcom", board = "vayu", bootloader = "unknown",
                fingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.3.0.RJUMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "V12.5.3.0.RJUMIXM", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250-AC", zygote = "zygote64_32",
                vendorFingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.3.0.RJUMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "vayu_global-user 11 RKQ1.200826.002 V12.5.3.0.RJUMIXM release-keys",
                buildFlavor = "vayu_global-user",
                buildHost = "c3-miui-ota-bd98", buildUser = "builder",
                buildDateUtc = "1622630400", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 10T" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "M2007J3SY",
                device = "apollo", product = "apollo_global",
                hardware = "qcom", board = "apollo", bootloader = "unknown",
                fingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.1.0.RJDMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "V12.5.1.0.RJDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.1.0.RJDMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "apollo_global-user 11 RKQ1.200826.002 V12.5.1.0.RJDMIXM release-keys",
                buildFlavor = "apollo_global-user",
                buildHost = "c3-miui-ota-bd05", buildUser = "builder",
                buildDateUtc = "1622112000", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            )
        )
    }

    data class DeviceFingerprint(
        val manufacturer: String, val brand: String, val model: String,
        val device: String, val product: String, val hardware: String,
        val board: String, val bootloader: String, val fingerprint: String,
        val buildId: String, val tags: String, val type: String,
        val radioVersion: String, val incremental: String, val sdkInt: Int,
        val release: String, val boardPlatform: String, val eglDriver: String,
        val openGlEs: String, val hardwareChipname: String, val zygote: String,
        val vendorFingerprint: String, val display: String,
        val buildDescription: String, val buildFlavor: String,
        val buildHost: String, val buildUser: String,
        val buildDateUtc: String, val securityPatch: String,
        val buildVersionCodename: String, val buildVersionPreviewSdk: String
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName !in TARGET_APPS) return

        try {
            val prefs = XSharedPreferences("com.vortex", PREFS_NAME)
            prefs.reload()

            fun getStr(key: String, def: String): String {
                val raw = prefs.getString(key, null)
                return CryptoUtils.decrypt(raw) ?: def
            }

            val profileName = getStr("profile", "Redmi 9")
            val fp = DEVICE_FINGERPRINTS[profileName] ?: DEVICE_FINGERPRINTS["Redmi 9"]!!

            fun getBool(key: String, def: Boolean): Boolean {
                val v = getStr(key, "")
                return if (v.isEmpty()) def else v.toBooleanStrictOrNull() ?: def
            }

            synchronized(this) { initializeCache(prefs, ::getStr) }

            hookBuildFields(lpparam, fp)
            hookSystemProperties(lpparam, fp)
            hookTelephonyManager(lpparam, fp) // [FIX D12] Pass fp
            hookSettingsSecure(lpparam)
            hookNetworkInterfaces(lpparam)
            hookWifiManager(lpparam)
            hookBluetoothAdapter(lpparam)
            hookLocation(lpparam, prefs, ::getStr)
            hookAccountManager(lpparam)
            hookFingerprintedPartitions(lpparam, fp) // [FIX D14] Pass fp
            hookMediaDrm(lpparam)
            hookWifiInfo(lpparam)

            // Conditional Hooks
            if (getBool("hook_webview", true)) hookWebView(lpparam, fp)
            if (getBool("hook_packages", true)) hookPackageManager(lpparam)
            if (getBool("hook_hide_debug", true)) hookApplicationFlags(lpparam)

            if (getBool("hook_hide_root", true)) {
                 hookFile(lpparam)
                 hookProcessBuilderAndRuntime(lpparam)
            }

            hookSSLPinning(lpparam)
            hookSensors(lpparam)

        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("Vortex error: ${e.message}")
        }
    }

    private fun initializeCache(prefs: XSharedPreferences, getStr: (String, String) -> String) {
        val profileName = getStr("profile", "Redmi 9")
        val mccMnc = getStr("mcc_mnc", "310260")

        if (cachedMccMnc == null) cachedMccMnc = mccMnc
        if (cachedImei == null)        cachedImei        = getStr("imei",          SpoofingUtils.generateValidImei(profileName))
        if (cachedImei2 == null)       cachedImei2       = getStr("imei2",         SpoofingUtils.generateValidImei(profileName))
        if (cachedAndroidId == null)   cachedAndroidId   = getStr("android_id",    SpoofingUtils.generateRandomId(16))
        if (cachedGsfId == null)       cachedGsfId       = getStr("gsf_id",        SpoofingUtils.generateRandomId(16))
        if (cachedGaid == null)        cachedGaid        = getStr("gaid",          SpoofingUtils.generateRandomGaid())
        val brand = DEVICE_FINGERPRINTS[profileName]?.brand ?: ""
        if (cachedSerial == null)      cachedSerial      = getStr("serial",        SpoofingUtils.generateRandomSerial(brand))
        if (cachedWifiMac == null)     cachedWifiMac     = getStr("wifi_mac",      SpoofingUtils.generateRandomMac())
        if (cachedBtMac == null)       cachedBtMac       = getStr("bluetooth_mac", SpoofingUtils.generateRandomMac())
        if (cachedGmail == null)       cachedGmail       = getStr("gmail",         SpoofingUtils.generateRealisticGmail())
        if (cachedMediaDrmId == null)    cachedMediaDrmId    = getStr("media_drm_id",   SpoofingUtils.generateRandomId(32))

        // [FIX D3] SSID Realista
        if (cachedWifiSsid == null)      cachedWifiSsid      = getStr("wifi_ssid",      SpoofingUtils.generateRealisticSsid())
        if (cachedWifiBssid == null)     cachedWifiBssid     = getStr("wifi_bssid",     SpoofingUtils.generateRandomMac())
        if (cachedImsi == null)        cachedImsi        = getStr("imsi",          SpoofingUtils.generateValidImsi(mccMnc))
        if (cachedIccid == null)       cachedIccid       = getStr("iccid",         SpoofingUtils.generateValidIccid(mccMnc))

        if (cachedPhoneNumber == null) {
            val savedPhone = getStr("phone_number", "")
            cachedPhoneNumber = if (savedPhone.isNotEmpty()) savedPhone else {
                val carrier = US_CARRIERS.find { it.mccMnc == mccMnc }
                SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList())
            }
        }

        // [FIX D9] Inicializar mock location bearing/speed una sola vez
        if (!mockLocationInitialized) {
            val rng = Random()
            mockBearing = rng.nextFloat() * 360f
            mockSpeed   = (rng.nextFloat() * 2.5f).coerceAtLeast(0.1f)
            mockLocationInitialized = true
        }
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            val b = Build::class.java
            XposedHelpers.setStaticObjectField(b, "MANUFACTURER", fp.manufacturer)
            XposedHelpers.setStaticObjectField(b, "BRAND",        fp.brand)
            XposedHelpers.setStaticObjectField(b, "MODEL",        fp.model)
            XposedHelpers.setStaticObjectField(b, "DEVICE",       fp.device)
            XposedHelpers.setStaticObjectField(b, "PRODUCT",      fp.product)
            XposedHelpers.setStaticObjectField(b, "HARDWARE",     fp.hardware)
            XposedHelpers.setStaticObjectField(b, "BOARD",        fp.board)
            XposedHelpers.setStaticObjectField(b, "BOOTLOADER",   fp.bootloader)
            XposedHelpers.setStaticObjectField(b, "FINGERPRINT",  fp.fingerprint)
            XposedHelpers.setStaticObjectField(b, "ID",           fp.buildId)
            XposedHelpers.setStaticObjectField(b, "TAGS",         fp.tags)
            XposedHelpers.setStaticObjectField(b, "TYPE",         fp.type)
            XposedHelpers.setStaticObjectField(b, "DISPLAY",      fp.display)
            XposedHelpers.setStaticObjectField(b, "HOST",         fp.buildHost)
            XposedHelpers.setStaticObjectField(b, "USER",         fp.buildUser)
            XposedHelpers.setStaticObjectField(b, "SERIAL",       cachedSerial)
            XposedHelpers.setStaticLongField(b, "TIME", fp.buildDateUtc.toLong() * 1000L)

            // [FIX D7] ABIs
            val abis = arrayOf("arm64-v8a", "armeabi-v7a", "armeabi")
            XposedHelpers.setStaticObjectField(b, "SUPPORTED_ABIS",        abis)
            XposedHelpers.setStaticObjectField(b, "SUPPORTED_64_BIT_ABIS", arrayOf("arm64-v8a"))
            XposedHelpers.setStaticObjectField(b, "SUPPORTED_32_BIT_ABIS", arrayOf("armeabi-v7a", "armeabi"))
            XposedHelpers.setStaticObjectField(b, "CPU_ABI",  "arm64-v8a")
            XposedHelpers.setStaticObjectField(b, "CPU_ABI2", "armeabi-v7a")

            val v = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(v,    "SDK_INT",        fp.sdkInt)
            XposedHelpers.setStaticObjectField(v, "RELEASE",        fp.release)
            XposedHelpers.setStaticObjectField(v, "INCREMENTAL",    fp.incremental)
            XposedHelpers.setStaticObjectField(v, "SECURITY_PATCH", fp.securityPatch)
            XposedHelpers.setStaticObjectField(v, "CODENAME",       fp.buildVersionCodename)
            XposedHelpers.setStaticIntField(v,    "PREVIEW_SDK_INT",fp.buildVersionPreviewSdk.toInt())

            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedSerial } })
            } catch (_: NoSuchMethodError) {}
        } catch (_: Throwable) {}
    }

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)
            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    val res = when (key) {
                        "ro.product.manufacturer"         -> fp.manufacturer
                        "ro.product.brand"                -> fp.brand
                        "ro.product.model"                -> fp.model
                        "ro.product.device"               -> fp.device
                        "ro.product.name"                 -> fp.product
                        "ro.hardware"                     -> fp.hardware
                        "ro.product.board"                -> fp.board
                        "ro.bootloader"                   -> fp.bootloader
                        "ro.build.fingerprint"            -> fp.fingerprint
                        "ro.build.id"                     -> fp.buildId
                        "ro.build.tags"                   -> fp.tags
                        "ro.build.type"                   -> fp.type
                        "gsm.version.baseband"            -> fp.radioVersion
                        "ro.serialno"                     -> cachedSerial
                        "ro.build.version.release"        -> fp.release
                        "ro.build.version.sdk"            -> fp.sdkInt.toString()
                        "ro.build.display.id"             -> fp.display
                        "ro.build.description"            -> fp.buildDescription
                        "ro.build.flavor"                 -> fp.buildFlavor
                        "ro.vendor.build.fingerprint"     -> fp.vendorFingerprint
                        "ro.board.platform"               -> fp.boardPlatform
                        "ro.hardware.egl"                 -> fp.eglDriver
                        "ro.opengles.version"             -> fp.openGlEs
                        "ro.hardware.chipname"            -> fp.hardwareChipname
                        "ro.zygote"                       -> fp.zygote
                        "ro.build.host"                   -> fp.buildHost
                        "ro.build.user"                   -> fp.buildUser
                        "ro.build.date.utc"               -> fp.buildDateUtc
                        "ro.build.version.security_patch" -> fp.securityPatch
                        "ro.build.version.codename"       -> fp.buildVersionCodename
                        "ro.build.version.preview_sdk"    -> fp.buildVersionPreviewSdk
                        "ro.build.characteristics"        -> "default"
                        // [FIX D7] ABIs properties
                        "ro.product.cpu.abi"       -> "arm64-v8a"
                        "ro.product.cpu.abi2"      -> "armeabi-v7a"
                        "ro.product.cpu.abilist"   -> "arm64-v8a,armeabi-v7a,armeabi"
                        "ro.product.cpu.abilist64" -> "arm64-v8a"
                        "ro.product.cpu.abilist32" -> "armeabi-v7a,armeabi"

                        // Particiones
                        "ro.product.system.manufacturer"  -> fp.manufacturer
                        "ro.product.system.brand"         -> fp.brand
                        "ro.product.system.model"         -> fp.model
                        "ro.product.system.device"        -> fp.device
                        "ro.product.system.name"          -> fp.product
                        "ro.product.vendor.manufacturer"  -> fp.manufacturer
                        "ro.product.vendor.brand"         -> fp.brand
                        "ro.product.vendor.model"         -> fp.model
                        "ro.product.vendor.device"        -> fp.device
                        "ro.product.vendor.name"          -> fp.product
                        "ro.product.odm.manufacturer"     -> fp.manufacturer
                        "ro.product.odm.brand"            -> fp.brand
                        "ro.product.odm.model"            -> fp.model
                        "ro.product.odm.device"           -> fp.device
                        "ro.product.odm.name"             -> fp.product
                        "ro.product.product.manufacturer" -> fp.manufacturer
                        "ro.product.product.brand"        -> fp.brand
                        "ro.product.product.model"        -> fp.model
                        "ro.product.product.device"       -> fp.device
                        "ro.product.product.name"         -> fp.product

                        "ro.kernel.qemu"                  -> "0"
                        "service.adb.root"                -> "0"
                        "ro.boot.serialno"                -> cachedSerial
                        "ro.boot.hardware"                -> fp.hardware
                        "ro.boot.bootloader"              -> fp.bootloader
                        // [FIX D11] Eliminar conflictos con TrickyStore
                        "persist.sys.usb.config"          -> "none"

                        // Props de seguridad y bootloader (seguras)
                        "ro.boot.flash.locked"       -> "1"
                        "sys.oem_unlock_allowed"     -> "0"
                        "ro.secure"                  -> "1"
                        "ro.debuggable"              -> "0"

                        else -> null
                    }
                    if (res != null) param.result = res
                }
            }
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, hook)
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, String::class.java, hook)
        } catch (_: Throwable) {}
    }

    // [FIX D12] Agregado fp como parametro
    private fun hookTelephonyManager(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            val tm = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)
            val mccMnc = cachedMccMnc ?: "310260"
            val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS[0]

            fun after(result: Any?) = object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) { p.result = result }
            }

            XposedHelpers.findAndHookMethod(tm, "getDeviceId",   after(cachedImei))
            XposedHelpers.findAndHookMethod(tm, "getSubscriberId", after(cachedImsi))
            XposedHelpers.findAndHookMethod(tm, "getSimSerialNumber", after(cachedIccid))
            XposedHelpers.findAndHookMethod(tm, "getLine1Number", after(cachedPhoneNumber))
            XposedHelpers.findAndHookMethod(tm, "getNetworkOperator",     after(mccMnc))
            XposedHelpers.findAndHookMethod(tm, "getSimOperator",         after(mccMnc))
            XposedHelpers.findAndHookMethod(tm, "getNetworkOperatorName", after(carrier.name))
            XposedHelpers.findAndHookMethod(tm, "getSimOperatorName",     after(carrier.name))
            XposedHelpers.findAndHookMethod(tm, "getNetworkCountryIso",   after("us"))
            XposedHelpers.findAndHookMethod(tm, "getSimCountryIso",       after("us"))

            // [FIX D8] Network Types LTE
            XposedHelpers.findAndHookMethod(tm, "getDataNetworkType",  after(13)) // LTE
            XposedHelpers.findAndHookMethod(tm, "getNetworkType",      after(13)) // LTE
            XposedHelpers.findAndHookMethod(tm, "getVoiceNetworkType", after(13)) // LTE

            // [FIX D4] GSM + MEID null
            XposedHelpers.findAndHookMethod(tm, "getPhoneType", after(1)) // PHONE_TYPE_GSM = 1
            try {
                 XposedHelpers.findAndHookMethod(tm, "getMeid", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = null } })
                 XposedHelpers.findAndHookMethod(tm, "getMeid", Int::class.javaPrimitiveType, object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = null } })
            } catch (_: NoSuchMethodError) {}

            // [FIX D12] Baseband version
            try {
                if (fp.radioVersion.isNotEmpty()) {
                    XposedHelpers.findAndHookMethod(tm, "getBasebandVersion", after(fp.radioVersion))
                }
            } catch (_: NoSuchMethodError) {}

            try {
                XposedHelpers.findAndHookMethod(tm, "getDeviceId", Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) {
                            p.result = if ((p.args[0] as Int) == 0) cachedImei else cachedImei2
                        }
                    })
                XposedHelpers.findAndHookMethod(tm, "getImei", after(cachedImei))
                XposedHelpers.findAndHookMethod(tm, "getImei", Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) {
                            p.result = if ((p.args[0] as Int) == 0) cachedImei else cachedImei2
                        }
                    })
            } catch (_: NoSuchMethodError) {}
        } catch (_: Throwable) {}
    }

    private fun hookSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getString",
                android.content.ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[1] as String) {
                            Settings.Secure.ANDROID_ID   -> {
                                // [FIX D5] SSAID Uniforme (eliminar logica Snapchat especifica para evitar mismatch con GMS)
                                param.result = cachedAndroidId
                            }
                            "advertising_id"             -> param.result = cachedGaid
                            "gsf_id", "android_id_gsf"  -> param.result = cachedGsfId
                        }
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun hookNetworkInterfaces(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getHardwareAddress", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val ni = param.thisObject as java.net.NetworkInterface
                        val name = ni.name ?: return
                        when {
                            name.startsWith("wlan") ->
                                param.result = macToBytes(cachedWifiMac ?: SpoofingUtils.generateRandomMac())
                            name.startsWith("bt") || name.startsWith("bluetooth") ->
                                param.result = macToBytes(cachedBtMac ?: SpoofingUtils.generateRandomMac())
                        }
                    } catch (_: Exception) {}
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookWifiManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getMacAddress", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = cachedWifiMac ?: SpoofingUtils.generateRandomMac()
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookWifiInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = "\"$cachedWifiSsid\"" } })
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getBSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedWifiBssid } })
        } catch (e: Throwable) {}
    }

    private fun hookBluetoothAdapter(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val btClass = XposedHelpers.findClass("android.bluetooth.BluetoothAdapter", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(btClass, "getAddress", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = cachedBtMac ?: SpoofingUtils.generateRandomMac()
                }
            })
        } catch (_: Throwable) {}
    }

    // [FIX D6] MediaDrm Widevine props reales
    private fun hookMediaDrm(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val drmClass = XposedHelpers.findClass("android.media.MediaDrm", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(drmClass, "getPropertyByteArray", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if ((key == "deviceUniqueId" || key == "deviceId") && cachedMediaDrmId != null) {
                         param.result = hexStringToByteArray(cachedMediaDrmId!!)
                    }
                }
            })

            XposedHelpers.findAndHookMethod(drmClass, "getPropertyString", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    when (key) {
                        "vendor"      -> param.result = "Google"
                        "version"     -> param.result = "16.0.0" // Widevine L1 típico A11
                        "description" -> param.result = "Widevine CDM v1.0"
                        // deviceUniqueId NO existe en getPropertyString para Widevine
                    }
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookLocation(lpparam: XC_LoadPackage.LoadPackageParam,
                             prefs: XSharedPreferences,
                             getStr: (String, String) -> String) {
        try {
            mockLocationEnabled = prefs.getBoolean("mock_location_enabled", false)
            if (!mockLocationEnabled) return

            mockLatitude  = getStr("mock_latitude",  "40.7128").toDoubleOrNull() ?: 40.7128
            mockLongitude = getStr("mock_longitude", "-74.0060").toDoubleOrNull() ?: -74.0060
            mockAltitude  = getStr("mock_altitude",  "10.0").toDoubleOrNull()  ?: 10.0
            mockAccuracy  = getStr("mock_accuracy",  "5.0").toFloatOrNull()    ?: 5.0f

            // [FIX D9] No re-generar bearing/speed aquí, ya está en initializeCache

            // Jitter gaussiano
            val rng = Random()
            val jitterDeg = 0.00001
            mockLatitude  += rng.nextGaussian() * jitterDeg
            mockLongitude += rng.nextGaussian() * jitterDeg
            mockAltitude  += rng.nextGaussian() * 0.5
            mockAccuracy   = (mockAccuracy + (rng.nextGaussian() * 1.5)).toFloat().coerceAtLeast(1f)

            val locClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)
            fun after(fn: () -> Any?) = object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) { p.result = fn() }
            }

            XposedHelpers.findAndHookMethod(locClass, "getLatitude",  after { mockLatitude })
            XposedHelpers.findAndHookMethod(locClass, "getLongitude", after { mockLongitude })
            XposedHelpers.findAndHookMethod(locClass, "getAltitude",  after { mockAltitude })
            XposedHelpers.findAndHookMethod(locClass, "hasAltitude",  after { true })
            XposedHelpers.findAndHookMethod(locClass, "getAccuracy",  after { mockAccuracy })
            XposedHelpers.findAndHookMethod(locClass, "getBearing",   after { mockBearing })
            XposedHelpers.findAndHookMethod(locClass, "hasBearing",   after { true })
            XposedHelpers.findAndHookMethod(locClass, "getSpeed",     after { mockSpeed })
            XposedHelpers.findAndHookMethod(locClass, "hasSpeed",     after { true })
            try {
                XposedHelpers.findAndHookMethod(locClass, "isFromMockProvider", after { false })
            } catch (_: NoSuchMethodError) {}

            val lmClass = XposedHelpers.findClass("android.location.LocationManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(lmClass, "getLastKnownLocation", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val loc = Location(param.args[0] as String)
                        loc.latitude  = mockLatitude
                        loc.longitude = mockLongitude
                        loc.altitude  = mockAltitude
                        loc.accuracy  = mockAccuracy
                        loc.bearing   = mockBearing
                        loc.speed     = mockSpeed
                        loc.time      = System.currentTimeMillis()
                        loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        try {
                            val m = Location::class.java.getDeclaredMethod("setIsFromMockProvider", Boolean::class.javaPrimitiveType)
                            m.isAccessible = true; m.invoke(loc, false)
                        } catch (_: Exception) {}
                        param.result = loc
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            val cls = XposedHelpers.findClass("android.webkit.WebSettings", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getUserAgentString", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ua = param.result as? String ?: return
                    val patched = ua
                        .replace(Regex(";\\s+[^;]+?\\s+Build/[^)]+"), "; ${fp.model} Build/${fp.buildId}")
                        .replace(Regex("Android [0-9.]+"), "Android ${fp.release}")
                    param.result = patched
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookAccountManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.accounts.AccountManager", lpparam.classLoader)
            val fakeGoogle = if (cachedGmail != null) arrayOf(Account(cachedGmail, "com.google")) else emptyArray()

            XposedHelpers.findAndHookMethod(cls, "getAccounts", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val orig = param.result as Array<Account>
                    val filtered = orig.filter { it.type != "com.google" }.toMutableList()
                    filtered.addAll(fakeGoogle)
                    param.result = filtered.toTypedArray()
                }
            })
            XposedHelpers.findAndHookMethod(cls, "getAccountsByType", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "com.google") param.result = fakeGoogle
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(cls, "getInstallerPackageName", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        p.result = "com.android.vending"
                    }
                })

            try {
                XposedHelpers.findAndHookMethod(cls, "getInstallSourceInfo", String::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            // [FIX D10] Constructor no existe en algunas versiones, eliminado para evitar crashes
                            // Si se requiere en el futuro, usar implementacion robusta por reflexion de campos
                        }
                    })
            } catch (_: NoSuchMethodError) {}

        } catch (_: Throwable) {}
    }

    private fun hookApplicationFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getApplicationInfo",
                String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val appInfo = param.result as? android.content.pm.ApplicationInfo ?: return
                        appInfo.flags = appInfo.flags and
                            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv() and
                            0x100.inv()
                    }
                })
        } catch (_: Throwable) {}
    }

    // [FIX D14] Particiones reales
    private fun hookFingerprintedPartitions(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            XposedHelpers.findAndHookMethod(Build::class.java, "getFingerprintedPartitions",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val partClass = XposedHelpers.findClass(
                                "android.os.Build\$Partition", lpparam.classLoader)
                            val ctor = partClass.getDeclaredConstructor(
                                String::class.java, String::class.java, Long::class.javaPrimitiveType)
                            ctor.isAccessible = true
                            val t = fp.buildDateUtc.toLong() * 1000L
                            param.result = listOf(
                                ctor.newInstance("system",  fp.fingerprint,       t),
                                ctor.newInstance("vendor",  fp.vendorFingerprint, t),
                                ctor.newInstance("product", fp.fingerprint,       t)
                            )
                        } catch (_: Throwable) {
                            param.result = null
                        }
                    }
                })
        } catch (_: Throwable) {}
    }

    private fun macToBytes(mac: String): ByteArray {
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) throw IllegalArgumentException("Bad MAC")
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (_: Exception) { byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x01) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun hookFile(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val fileClass = XposedHelpers.findClass("java.io.File", lpparam.classLoader)
            val rootPaths = setOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")
            XposedHelpers.findAndHookMethod(fileClass, "exists", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as java.io.File
                    if (rootPaths.contains(file.absolutePath)) {
                        param.result = false
                    }
                }
            })
        } catch (_: Throwable) {}
    }

    private fun hookProcessBuilderAndRuntime(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val runtimeClass = XposedHelpers.findClass("java.lang.Runtime", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(runtimeClass, "exec", String::class.java, object : XC_MethodHook() {
                 override fun beforeHookedMethod(param: MethodHookParam) {
                     val cmd = param.args[0] as String
                     if (cmd == "su" || cmd.startsWith("su ")) {
                         param.throwable = java.io.IOException("Permission denied")
                     }
                 }
            })
        } catch (_: Throwable) {}
    }

    private fun hookSSLPinning(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.snapchat.android") return

        try {
            // 1. OkHttp CertificatePinner
            val pinnerClass = XposedHelpers.findClass("okhttp3.CertificatePinner", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(pinnerClass, "check", String::class.java, List::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) { param.result = null } // Bypass
            })

            // 2. Java X509TrustManager
            val trustManagerClass = XposedHelpers.findClass("javax.net.ssl.X509TrustManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(trustManagerClass, "checkServerTrusted", Array<java.security.cert.X509Certificate>::class.java, String::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) { param.result = null } // Bypass
            })

            // 3. SSLContext Init (Deep Bypass)
            val sslContextClass = XposedHelpers.findClass("javax.net.ssl.SSLContext", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(sslContextClass, "init", Array<javax.net.ssl.KeyManager>::class.java, Array<javax.net.ssl.TrustManager>::class.java, java.security.SecureRandom::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Inyectar TrustManager permisivo
                    val trustingManager = object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
                    }
                    param.args[1] = arrayOf(trustingManager)
                }
            })
            MainHook.log("SSL Pinning neutralizado para Snapchat.")
        } catch (e: Throwable) { MainHook.logError("Error en SSL Pinning Hook", e) }
    }

    private fun hookSensors(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sensorClass = android.hardware.Sensor::class.java
            // Hook getters directamente para modificar la "lectura" de las specs del sensor
            val spoofSpecs = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val sensor = param.thisObject as android.hardware.Sensor
                    val type = sensor.type
                    // Lógica de perfiles (Simplificada para ejemplo)
                    if (type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                        when (param.method.name) {
                            "getMaximumRange" -> param.result = 19.6f // Xiaomi standard
                            "getPower" -> param.result = 0.25f
                            "getResolution" -> param.result = 0.01f
                            "getVendor" -> param.result = "Vortex Virtual Sensor" // O el vendor del perfil
                        }
                    }
                }
            }

            // Aplicar a métodos clave
            XposedHelpers.findAndHookMethod(sensorClass, "getMaximumRange", spoofSpecs)
            XposedHelpers.findAndHookMethod(sensorClass, "getPower", spoofSpecs)
            XposedHelpers.findAndHookMethod(sensorClass, "getResolution", spoofSpecs)
            XposedHelpers.findAndHookMethod(sensorClass, "getVendor", spoofSpecs)

        } catch (e: Throwable) { MainHook.logError("Error hooking Sensors", e) }
    }
}
