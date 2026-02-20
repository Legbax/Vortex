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

            // =========================================================
            // GRUPO 1: XIAOMI / POCO / REDMI  (12 dispositivos)
            // =========================================================

            // [01] Redmi 9 — MT6768 — MIUI 12.5.6.0 (ROM exacta del usuario)
            "Redmi 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9",
                device = "lancelot", product = "lancelot_global",
                hardware = "mt6768", board = "lancelot", bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.6.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.6.0.RJCMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6768", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6768", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.6.0.RJCMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "lancelot_global-user 11 RP1A.200720.011 V12.5.6.0.RJCMIXM release-keys",
                buildFlavor = "lancelot_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [02] POCO X3 Pro — SM8150-AC (SD860) / msmnile — CORREGIDO
            "POCO X3 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "M2102J20SG",
                device = "vayu", product = "vayu_global",
                hardware = "qcom", board = "vayu", bootloader = "unknown",
                fingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.3.0.RJUMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM8150_GEN_PACK-1",
                incremental = "V12.5.3.0.RJUMIXM", sdkInt = 30, release = "11",
                boardPlatform = "msmnile", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8150-AC", zygote = "zygote64_32",
                vendorFingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.3.0.RJUMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "vayu_global-user 11 RKQ1.200826.002 V12.5.3.0.RJUMIXM release-keys",
                buildFlavor = "vayu_global-user",
                buildHost = "c3-miui-ota-bd98", buildUser = "builder",
                buildDateUtc = "1622630400", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [03] Mi 10T — SM8250 (SD865) / kona
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
            ),

            // [04] Redmi Note 10 Pro — SM7150-AB (SD732G) / atoll
            "Redmi Note 10 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "M2101K6G",
                device = "sweet", product = "sweet_global",
                hardware = "qcom", board = "sweet", bootloader = "unknown",
                fingerprint = "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.4.0.RKGMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00026-SM7150_GEN_PACK-1",
                incremental = "V12.5.4.0.RKGMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150-AB", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.4.0.RKGMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "sweet_global-user 11 RKQ1.200826.002 V12.5.4.0.RKGMIXM release-keys",
                buildFlavor = "sweet_global-user",
                buildHost = "pangu-build-component-system-178104", buildUser = "builder",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [05] Redmi Note 9 Pro — SM7125 (SD720G) / atoll
            "Redmi Note 9 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "M2003J6B2G",
                device = "joyeuse", product = "joyeuse_global",
                hardware = "qcom", board = "joyeuse", bootloader = "unknown",
                fingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.1.0.RJZMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00050-SM7150_GEN_PACK-1",
                incremental = "V12.5.1.0.RJZMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.1.0.RJZMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "joyeuse_global-user 11 RKQ1.200826.002 V12.5.1.0.RJZMIXM release-keys",
                buildFlavor = "joyeuse_global-user",
                buildHost = "pangu-build-component-system-175632", buildUser = "builder",
                buildDateUtc = "1622112000", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [06] Redmi Note 10 — SM6150 (SD678) / bengal
            "Redmi Note 10" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "M2101K7AI",
                device = "mojito", product = "mojito_global",
                hardware = "qcom", board = "mojito", bootloader = "unknown",
                fingerprint = "Redmi/mojito_global/mojito:11/RKQ1.200826.002/V12.5.6.0.RKFMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00055-SM6150_GEN_PACK-1",
                incremental = "V12.5.6.0.RKFMIXM", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6150", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/mojito_global/mojito:11/RKQ1.200826.002/V12.5.6.0.RKFMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "mojito_global-user 11 RKQ1.200826.002 V12.5.6.0.RKFMIXM release-keys",
                buildFlavor = "mojito_global-user",
                buildHost = "pangu-build-component-system-179001", buildUser = "builder",
                buildDateUtc = "1630454400", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [07] POCO M3 Pro 5G — MT6833 (Dimensity 700) / mt6833
            "POCO M3 Pro 5G" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "M2103K19PG",
                device = "camellia", product = "camellia_global",
                hardware = "mt6833", board = "camellia", bootloader = "unknown",
                fingerprint = "POCO/camellia_global/camellia:11/RP1A.200720.011/V12.5.2.0.RKRMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V73.6",
                incremental = "V12.5.2.0.RKRMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6833", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6833", zygote = "zygote64_32",
                vendorFingerprint = "POCO/camellia_global/camellia:11/RP1A.200720.011/V12.5.2.0.RKRMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "camellia_global-user 11 RP1A.200720.011 V12.5.2.0.RKRMIXM release-keys",
                buildFlavor = "camellia_global-user",
                buildHost = "pangu-build-component-system-176812", buildUser = "builder",
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [08] POCO X3 NFC — SM7150-AB (SD732G) / atoll
            "POCO X3 NFC" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "M2007J20CG",
                device = "surya", product = "surya_global",
                hardware = "qcom", board = "surya", bootloader = "unknown",
                fingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.2.0.RJGMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00026-SM7150_GEN_PACK-1",
                incremental = "V12.5.2.0.RJGMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150-AB", zygote = "zygote64_32",
                vendorFingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.2.0.RJGMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "surya_global-user 11 RKQ1.200826.002 V12.5.2.0.RJGMIXM release-keys",
                buildFlavor = "surya_global-user",
                buildHost = "c3-miui-ota-bd77", buildUser = "builder",
                buildDateUtc = "1622630400", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [09] Mi 11 Lite — SM7150-AB (SD732G) / atoll
            "Mi 11 Lite" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "M2101K9AG",
                device = "courbet", product = "courbet_global",
                hardware = "qcom", board = "courbet", bootloader = "unknown",
                fingerprint = "Xiaomi/courbet_global/courbet:11/RKQ1.200826.002/V12.5.3.0.RKAMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00026-SM7150_GEN_PACK-1",
                incremental = "V12.5.3.0.RKAMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150-AB", zygote = "zygote64_32",
                vendorFingerprint = "Xiaomi/courbet_global/courbet:11/RKQ1.200826.002/V12.5.3.0.RKAMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "courbet_global-user 11 RKQ1.200826.002 V12.5.3.0.RKAMIXM release-keys",
                buildFlavor = "courbet_global-user",
                buildHost = "pangu-build-component-system-180421", buildUser = "builder",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [10] Mi 11 — SM8350 (SD888) / lahaina
            "Mi 11" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "M2011K2G",
                device = "venus", product = "venus_global",
                hardware = "qcom", board = "venus", bootloader = "unknown",
                fingerprint = "Xiaomi/venus_global/venus:11/RKQ1.200826.002/V12.5.6.0.RKBMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.3.c1-00076-SM8350_GEN_PACK-1",
                incremental = "V12.5.6.0.RKBMIXM", sdkInt = 30, release = "11",
                boardPlatform = "lahaina", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8350", zygote = "zygote64_32",
                vendorFingerprint = "Xiaomi/venus_global/venus:11/RKQ1.200826.002/V12.5.6.0.RKBMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "venus_global-user 11 RKQ1.200826.002 V12.5.6.0.RKBMIXM release-keys",
                buildFlavor = "venus_global-user",
                buildHost = "pangu-build-component-system-182133", buildUser = "builder",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [11] Redmi 10X 4G — MT6873 (Dimensity 820) / mt6873
            "Redmi 10X 4G" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "M2004J7AC",
                device = "merlin", product = "merlin",
                hardware = "mt6873", board = "merlin", bootloader = "unknown",
                fingerprint = "Redmi/merlin/merlin:11/RP1A.200720.011/V12.5.3.0.QJJCNXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V110.6",
                incremental = "V12.5.3.0.QJJCNXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6873", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6873", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/merlin/merlin:11/RP1A.200720.011/V12.5.3.0.QJJCNXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "merlin-user 11 RP1A.200720.011 V12.5.3.0.QJJCNXM release-keys",
                buildFlavor = "merlin-user",
                buildHost = "pangu-build-component-system-175411", buildUser = "builder",
                buildDateUtc = "1622505600", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [12] POCO F3 — SM8250-AC (SD870) / kona
            "POCO F3" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "M2012K11AG",
                device = "alioth", product = "alioth_global",
                hardware = "qcom", board = "alioth", bootloader = "unknown",
                fingerprint = "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.5.0.RKHMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "V12.5.5.0.RKHMIXM", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250-AC", zygote = "zygote64_32",
                vendorFingerprint = "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.5.0.RKHMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "alioth_global-user 11 RKQ1.200826.002 V12.5.5.0.RKHMIXM release-keys",
                buildFlavor = "alioth_global-user",
                buildHost = "c3-miui-ota-bd88", buildUser = "builder",
                buildDateUtc = "1630454400", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 2: SAMSUNG  (10 dispositivos)
            // =========================================================

            // [13] Galaxy A52 — SM7125 (SD720G) / atoll
            "Galaxy A52" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A525F",
                device = "a52xnsxx", product = "a52xnsxx",
                hardware = "qcom", board = "sm6350", bootloader = "unknown",
                fingerprint = "samsung/a52xnsxx/a52x:11/RP1A.200720.012/A525FXXU4AUH1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00050-SM7150_GEN_PACK-1",
                incremental = "A525FXXU4AUH1", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a52xnsxx/a52x:11/RP1A.200720.012/A525FXXU4AUH1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a52xnsxx-user 11 RP1A.200720.012 A525FXXU4AUH1 release-keys",
                buildFlavor = "a52xnsxx-user",
                buildHost = "SWDD7390", buildUser = "dpi",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [14] Galaxy A72 — SM7125 (SD720G) / atoll
            "Galaxy A72" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A725F",
                device = "a72nsxx", product = "a72nsxx",
                hardware = "qcom", board = "sm6350", bootloader = "unknown",
                fingerprint = "samsung/a72nsxx/a72:11/RP1A.200720.012/A725FXXU3AUH2:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00050-SM7150_GEN_PACK-1",
                incremental = "A725FXXU3AUH2", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a72nsxx/a72:11/RP1A.200720.012/A725FXXU3AUH2:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a72nsxx-user 11 RP1A.200720.012 A725FXXU3AUH2 release-keys",
                buildFlavor = "a72nsxx-user",
                buildHost = "SWDD7391", buildUser = "dpi",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [15] Galaxy A32 5G — MT6853 (Dimensity 720) / mt6853
            "Galaxy A32 5G" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A326B",
                device = "a32xnsxx", product = "a32xnsxx",
                hardware = "mt6853", board = "mt6853", bootloader = "unknown",
                fingerprint = "samsung/a32xnsxx/a32x:11/RP1A.200720.012/A326BXXU4AUH1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.6",
                incremental = "A326BXXU4AUH1", sdkInt = 30, release = "11",
                boardPlatform = "mt6853", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6853", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a32xnsxx/a32x:11/RP1A.200720.012/A326BXXU4AUH1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a32xnsxx-user 11 RP1A.200720.012 A326BXXU4AUH1 release-keys",
                buildFlavor = "a32xnsxx-user",
                buildHost = "SWDD8201", buildUser = "dpi",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [16] Galaxy S20 FE 5G — SM8250 (SD865) / kona
            "Galaxy S20 FE" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-G781B",
                device = "r8q", product = "r8qnsxx",
                hardware = "qcom", board = "kona", bootloader = "unknown",
                fingerprint = "samsung/r8qnsxx/r8q:11/RP1A.200720.012/G781BXXU3CUJ2:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "G781BXXU3CUJ2", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "samsung/r8qnsxx/r8q:11/RP1A.200720.012/G781BXXU3CUJ2:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "r8qnsxx-user 11 RP1A.200720.012 G781BXXU3CUJ2 release-keys",
                buildFlavor = "r8qnsxx-user",
                buildHost = "SWDD5130", buildUser = "dpi",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [17] Galaxy A51 — Exynos 9611 / exynos9610
            "Galaxy A51" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A515F",
                device = "a51nsxx", product = "a51nsxx",
                hardware = "exynos9610", board = "exynos9610", bootloader = "unknown",
                fingerprint = "samsung/a51nsxx/a51:11/RP1A.200720.012/A515FXXU5CUJ1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "g9610-3286-210401",
                incremental = "A515FXXU5CUJ1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9610", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "exynos9610", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a51nsxx/a51:11/RP1A.200720.012/A515FXXU5CUJ1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a51nsxx-user 11 RP1A.200720.012 A515FXXU5CUJ1 release-keys",
                buildFlavor = "a51nsxx-user",
                buildHost = "SWDD6290", buildUser = "dpi",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [18] Galaxy M31 — Exynos 9611 / exynos9610
            "Galaxy M31" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-M315F",
                device = "m31nsxx", product = "m31nsxx",
                hardware = "exynos9610", board = "exynos9610", bootloader = "unknown",
                fingerprint = "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU4CUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "g9610-3286-210401",
                incremental = "M315FXXU4CUG1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9610", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "exynos9610", zygote = "zygote64_32",
                vendorFingerprint = "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU4CUG1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "m31nsxx-user 11 RP1A.200720.012 M315FXXU4CUG1 release-keys",
                buildFlavor = "m31nsxx-user",
                buildHost = "SWDD6310", buildUser = "dpi",
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [19] Galaxy A12 — MT6765 (Helio P35) / mt6765
            "Galaxy A12" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A125F",
                device = "a12nsxx", product = "a12nsxx",
                hardware = "mt6765", board = "mt6765", bootloader = "unknown",
                fingerprint = "samsung/a12nsxx/a12:11/RP1A.200720.012/A125FXXU5BUJ1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V56.6",
                incremental = "A125FXXU5BUJ1", sdkInt = 30, release = "11",
                boardPlatform = "mt6765", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6765", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a12nsxx/a12:11/RP1A.200720.012/A125FXXU5BUJ1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a12nsxx-user 11 RP1A.200720.012 A125FXXU5BUJ1 release-keys",
                buildFlavor = "a12nsxx-user",
                buildHost = "SWDD8800", buildUser = "dpi",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [20] Galaxy A21s — Exynos 850 / exynos850
            "Galaxy A21s" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A217F",
                device = "a21snsxx", product = "a21snsxx",
                hardware = "exynos850", board = "exynos850", bootloader = "unknown",
                fingerprint = "samsung/a21snsxx/a21s:11/RP1A.200720.012/A217FXXU4CUJ1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "g850-210401-5286",
                incremental = "A217FXXU4CUJ1", sdkInt = 30, release = "11",
                boardPlatform = "exynos850", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "exynos850", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a21snsxx/a21s:11/RP1A.200720.012/A217FXXU4CUJ1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a21snsxx-user 11 RP1A.200720.012 A217FXXU4CUJ1 release-keys",
                buildFlavor = "a21snsxx-user",
                buildHost = "SWDD7700", buildUser = "dpi",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [21] Galaxy A31 — MT6768 (Helio G80) / mt6768
            "Galaxy A31" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A315F",
                device = "a31nsxx", product = "a31nsxx",
                hardware = "mt6768", board = "mt6768", bootloader = "unknown",
                fingerprint = "samsung/a31nsxx/a31:11/RP1A.200720.012/A315FXXU4CUH1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "A315FXXU4CUH1", sdkInt = 30, release = "11",
                boardPlatform = "mt6768", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6768", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a31nsxx/a31:11/RP1A.200720.012/A315FXXU4CUH1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "a31nsxx-user 11 RP1A.200720.012 A315FXXU4CUH1 release-keys",
                buildFlavor = "a31nsxx-user",
                buildHost = "SWDD8100", buildUser = "dpi",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [22] Galaxy F62 — Exynos 9825 / exynos9820
            "Galaxy F62" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-E625F",
                device = "e1qnsxx", product = "e1qnsxx",
                hardware = "exynos9820", board = "exynos9820", bootloader = "unknown",
                fingerprint = "samsung/e1qnsxx/e1q:11/RP1A.200720.012/E625FXXU2BUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "g9820-3131-210401",
                incremental = "E625FXXU2BUG1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9820", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "exynos9820", zygote = "zygote64_32",
                vendorFingerprint = "samsung/e1qnsxx/e1q:11/RP1A.200720.012/E625FXXU2BUG1:user/release-keys",
                display = "RP1A.200720.012",
                buildDescription = "e1qnsxx-user 11 RP1A.200720.012 E625FXXU2BUG1 release-keys",
                buildFlavor = "e1qnsxx-user",
                buildHost = "SWDD5830", buildUser = "dpi",
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 3: ONEPLUS  (4 dispositivos)
            // =========================================================

            // [23] OnePlus 8T — SM8250 (SD865) / kona
            "OnePlus 8T" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "KB2001",
                device = "kebab", product = "kebab_EEA",
                hardware = "qcom", board = "msmnile", bootloader = "unknown",
                fingerprint = "OnePlus/kebab_EEA/OnePlus8T:11/RP1A.200720.011/2107142215:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "2107142215", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/kebab_EEA/OnePlus8T:11/RP1A.200720.011/2107142215:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "kebab_EEA-user 11 RP1A.200720.011 2107142215 release-keys",
                buildFlavor = "kebab_EEA-user",
                buildHost = "builder01.oneplus.com", buildUser = "builduser",
                buildDateUtc = "1626307200", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [24] OnePlus Nord — SM7250 (SD765G) / lito
            "OnePlus Nord" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "AC2003",
                device = "avicii", product = "avicii_EEA",
                hardware = "qcom", board = "lito", bootloader = "unknown",
                fingerprint = "OnePlus/avicii_EEA/OnePlus Nord:11/RKQ1.201022.002/2105101635:user/release-keys",
                buildId = "RKQ1.201022.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00075-SM7250_GEN_PACK-1",
                incremental = "2105101635", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/avicii_EEA/OnePlus Nord:11/RKQ1.201022.002/2105101635:user/release-keys",
                display = "RKQ1.201022.002",
                buildDescription = "avicii_EEA-user 11 RKQ1.201022.002 2105101635 release-keys",
                buildFlavor = "avicii_EEA-user",
                buildHost = "builder02.oneplus.com", buildUser = "builduser",
                buildDateUtc = "1620604800", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [25] OnePlus N10 5G — SM6350 (SD690) / lito
            "OnePlus N10 5G" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "BE2025",
                device = "billie", product = "billie_EEA",
                hardware = "qcom", board = "lito", bootloader = "unknown",
                fingerprint = "OnePlus/billie_EEA/OnePlus N10 5G:11/RP1A.200720.011/2104132208:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00040-SM6350_GEN_PACK-1",
                incremental = "2104132208", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6350", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/billie_EEA/OnePlus N10 5G:11/RP1A.200720.011/2104132208:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "billie_EEA-user 11 RP1A.200720.011 2104132208 release-keys",
                buildFlavor = "billie_EEA-user",
                buildHost = "builder03.oneplus.com", buildUser = "builduser",
                buildDateUtc = "1617235200", securityPatch = "2021-04-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [26] OnePlus 8 — SM8250 (SD865) / kona
            "OnePlus 8" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "IN2013",
                device = "instantnoodle", product = "instantnoodle_EEA",
                hardware = "qcom", board = "msmnile", bootloader = "unknown",
                fingerprint = "OnePlus/instantnoodle_EEA/OnePlus8:11/RP1A.200720.011/2105100150:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "2105100150", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/instantnoodle_EEA/OnePlus8:11/RP1A.200720.011/2105100150:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "instantnoodle_EEA-user 11 RP1A.200720.011 2105100150 release-keys",
                buildFlavor = "instantnoodle_EEA-user",
                buildHost = "builder04.oneplus.com", buildUser = "builduser",
                buildDateUtc = "1620518400", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 4: GOOGLE PIXEL  (4 dispositivos)
            // =========================================================

            // [27] Pixel 5 — SM7250 (SD765G) / lito
            "Pixel 5" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 5",
                device = "redfin", product = "redfin",
                hardware = "redfin", board = "redfin", bootloader = "r8.0.0-6692804",
                fingerprint = "google/redfin/redfin:11/RQ3A.210805.001.A1/7474174:user/release-keys",
                buildId = "RQ3A.210805.001.A1", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00075-SM7250_GEN_PACK-1",
                incremental = "7474174", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "google/redfin/redfin:11/RQ3A.210805.001.A1/7474174:user/release-keys",
                display = "RQ3A.210805.001.A1",
                buildDescription = "redfin-user 11 RQ3A.210805.001.A1 7474174 release-keys",
                buildFlavor = "redfin-user",
                buildHost = "abfarm-release-rbe-32-00025", buildUser = "android-build",
                buildDateUtc = "1627776000", securityPatch = "2021-08-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [28] Pixel 4a 5G — SM7250 (SD765G) / lito
            "Pixel 4a 5G" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a (5G)",
                device = "bramble", product = "bramble",
                hardware = "bramble", board = "bramble", bootloader = "r8.0.0-6692804",
                fingerprint = "google/bramble/bramble:11/RQ3A.210805.001.A1/7474174:user/release-keys",
                buildId = "RQ3A.210805.001.A1", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00075-SM7250_GEN_PACK-1",
                incremental = "7474174", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "google/bramble/bramble:11/RQ3A.210805.001.A1/7474174:user/release-keys",
                display = "RQ3A.210805.001.A1",
                buildDescription = "bramble-user 11 RQ3A.210805.001.A1 7474174 release-keys",
                buildFlavor = "bramble-user",
                buildHost = "abfarm-release-rbe-32-00026", buildUser = "android-build",
                buildDateUtc = "1627776000", securityPatch = "2021-08-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [29] Pixel 4a — SM730 (SD730G) / trinket
            "Pixel 4a" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a",
                device = "sunfish", product = "sunfish",
                hardware = "sunfish", board = "sunfish", bootloader = "s5-0.5-6765805",
                fingerprint = "google/sunfish/sunfish:11/RQ3A.210805.001/7390230:user/release-keys",
                buildId = "RQ3A.210805.001", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00022-SM7150_GEN_PACK-1",
                incremental = "7390230", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150", zygote = "zygote64_32",
                vendorFingerprint = "google/sunfish/sunfish:11/RQ3A.210805.001/7390230:user/release-keys",
                display = "RQ3A.210805.001",
                buildDescription = "sunfish-user 11 RQ3A.210805.001 7390230 release-keys",
                buildFlavor = "sunfish-user",
                buildHost = "abfarm-release-rbe-32-00027", buildUser = "android-build",
                buildDateUtc = "1627776000", securityPatch = "2021-08-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [30] Pixel 3a XL — SM670 (SD670) / sdm670
            "Pixel 3a XL" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 3a XL",
                device = "bonito", product = "bonito",
                hardware = "bonito", board = "bonito", bootloader = "b4s4-0.2-5613699",
                fingerprint = "google/bonito/bonito:11/RP1A.200720.011/6734798:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.3.0-00079-SDM670_GEN_PACK-1",
                incremental = "6734798", sdkInt = 30, release = "11",
                boardPlatform = "sdm670", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SDM670", zygote = "zygote64_32",
                vendorFingerprint = "google/bonito/bonito:11/RP1A.200720.011/6734798:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "bonito-user 11 RP1A.200720.011 6734798 release-keys",
                buildFlavor = "bonito-user",
                buildHost = "abfarm-release-rbe-32-00010", buildUser = "android-build",
                buildDateUtc = "1619827200", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 5: MOTOROLA  (4 dispositivos)
            // =========================================================

            // [31] Moto G Power (2021) — SM6115 (SD662) / bengal
            "Moto G Power 2021" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "motorola one 5G ace",
                device = "borneo", product = "borneo_retail",
                hardware = "qcom", board = "bengal", bootloader = "unknown",
                fingerprint = "motorola/borneo_retail/borneo:11/RPES31.Q1-25-22-8/22-8:user/release-keys",
                buildId = "RPES31.Q1-25-22-8", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00055-SM6115_GEN_PACK-1",
                incremental = "22-8", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6115", zygote = "zygote64_32",
                vendorFingerprint = "motorola/borneo_retail/borneo:11/RPES31.Q1-25-22-8/22-8:user/release-keys",
                display = "RPES31.Q1-25-22-8",
                buildDescription = "borneo_retail-user 11 RPES31.Q1-25-22-8 22-8 release-keys",
                buildFlavor = "borneo_retail-user",
                buildHost = "moto-build-prod-01", buildUser = "moto",
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [32] Moto G Stylus (2021) — SM6150 (SD678) / bengal
            "Moto G Stylus 2021" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g stylus (2021)",
                device = "nairo", product = "nairo_retail",
                hardware = "qcom", board = "bengal", bootloader = "unknown",
                fingerprint = "motorola/nairo_retail/nairo:11/RPNS31.Q1-45-20/1.0.0.0:user/release-keys",
                buildId = "RPNS31.Q1-45-20", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00055-SM6150_GEN_PACK-1",
                incremental = "1.0.0.0", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6150", zygote = "zygote64_32",
                vendorFingerprint = "motorola/nairo_retail/nairo:11/RPNS31.Q1-45-20/1.0.0.0:user/release-keys",
                display = "RPNS31.Q1-45-20",
                buildDescription = "nairo_retail-user 11 RPNS31.Q1-45-20 1.0.0.0 release-keys",
                buildFlavor = "nairo_retail-user",
                buildHost = "moto-build-prod-02", buildUser = "moto",
                buildDateUtc = "1622505600", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [33] Moto Edge — SM7250 (SD765G) / lito
            "Moto Edge" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto edge",
                device = "racer", product = "racer_retail",
                hardware = "qcom", board = "lito", bootloader = "unknown",
                fingerprint = "motorola/racer_retail/racer:11/RRAS31.Q1-16-113/1.0.0.0:user/release-keys",
                buildId = "RRAS31.Q1-16-113", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00075-SM7250_GEN_PACK-1",
                incremental = "1.0.0.0", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "motorola/racer_retail/racer:11/RRAS31.Q1-16-113/1.0.0.0:user/release-keys",
                display = "RRAS31.Q1-16-113",
                buildDescription = "racer_retail-user 11 RRAS31.Q1-16-113 1.0.0.0 release-keys",
                buildFlavor = "racer_retail-user",
                buildHost = "moto-build-prod-03", buildUser = "moto",
                buildDateUtc = "1617235200", securityPatch = "2021-04-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [34] Moto Edge Plus — SM8250-AB (SD865+) / kona
            "Moto Edge Plus" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "motorola edge+",
                device = "sofiar", product = "sofiar_retail",
                hardware = "qcom", board = "kona", bootloader = "unknown",
                fingerprint = "motorola/sofiar_retail/sofiar:11/RROS31.Q1-13-52/1.0.0.0:user/release-keys",
                buildId = "RROS31.Q1-13-52", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00125-SM8250_GEN_PACK-1",
                incremental = "1.0.0.0", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250-AB", zygote = "zygote64_32",
                vendorFingerprint = "motorola/sofiar_retail/sofiar:11/RROS31.Q1-13-52/1.0.0.0:user/release-keys",
                display = "RROS31.Q1-13-52",
                buildDescription = "sofiar_retail-user 11 RROS31.Q1-13-52 1.0.0.0 release-keys",
                buildFlavor = "sofiar_retail-user",
                buildHost = "moto-build-prod-04", buildUser = "moto",
                buildDateUtc = "1619827200", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 6: NOKIA  (2 dispositivos)
            // =========================================================

            // [35] Nokia 8.3 5G — SM7250 (SD765G) / lito
            "Nokia 8.3 5G" to DeviceFingerprint(
                manufacturer = "HMD Global", brand = "Nokia", model = "Nokia 8.3 5G",
                device = "nokia_8.3_5g", product = "BVUB_00WW",
                hardware = "qcom", board = "lito", bootloader = "unknown",
                fingerprint = "Nokia/BVUB_00WW/nokia_8.3_5g:11/RKQ1.200928.002/00WW_3_510:user/release-keys",
                buildId = "RKQ1.200928.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00075-SM7250_GEN_PACK-1",
                incremental = "00WW_3_510", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "Nokia/BVUB_00WW/nokia_8.3_5g:11/RKQ1.200928.002/00WW_3_510:user/release-keys",
                display = "RKQ1.200928.002",
                buildDescription = "BVUB_00WW-user 11 RKQ1.200928.002 00WW_3_510 release-keys",
                buildFlavor = "BVUB_00WW-user",
                buildHost = "hmd-build-01", buildUser = "hmd",
                buildDateUtc = "1627776000", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [36] Nokia 5.4 — SM7125 (SD720G) / atoll
            "Nokia 5.4" to DeviceFingerprint(
                manufacturer = "HMD Global", brand = "Nokia", model = "Nokia 5.4",
                device = "nokia_5.4", product = "CAV_00WW",
                hardware = "qcom", board = "atoll", bootloader = "unknown",
                fingerprint = "Nokia/CAV_00WW/nokia_5.4:11/RKQ1.200928.002/00WW_2_440:user/release-keys",
                buildId = "RKQ1.200928.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00050-SM7150_GEN_PACK-1",
                incremental = "00WW_2_440", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "Nokia/CAV_00WW/nokia_5.4:11/RKQ1.200928.002/00WW_2_440:user/release-keys",
                display = "RKQ1.200928.002",
                buildDescription = "CAV_00WW-user 11 RKQ1.200928.002 00WW_2_440 release-keys",
                buildFlavor = "CAV_00WW-user",
                buildHost = "hmd-build-02", buildUser = "hmd",
                buildDateUtc = "1622505600", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 7: REALME  (2 dispositivos)
            // =========================================================

            // [37] Realme 8 Pro — SM7125 (SD720G) / atoll
            "Realme 8 Pro" to DeviceFingerprint(
                manufacturer = "realme", brand = "realme", model = "RMX3091",
                device = "RMX3091", product = "RMX3091",
                hardware = "qcom", board = "atoll", bootloader = "unknown",
                fingerprint = "realme/RMX3091/RMX3091:11/RP1A.200720.011/1636363205855:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.AT.4.0-00050-SM7150_GEN_PACK-1",
                incremental = "1636363205855", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "realme/RMX3091/RMX3091:11/RP1A.200720.011/1636363205855:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "RMX3091-user 11 RP1A.200720.011 1636363205855 release-keys",
                buildFlavor = "RMX3091-user",
                buildHost = "oppo-build-01", buildUser = "oppo",
                buildDateUtc = "1635724800", securityPatch = "2021-11-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [38] Realme GT Master — SM7325 (SD778G) / holi
            "Realme GT Master" to DeviceFingerprint(
                manufacturer = "realme", brand = "realme", model = "RMX3363",
                device = "RE58B2L1", product = "RMX3363",
                hardware = "qcom", board = "holi", bootloader = "unknown",
                fingerprint = "realme/RMX3363/RE58B2L1:11/RP1A.200720.011/1638316800000:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.VT.5.2-00095-SM7325_GEN_PACK-1",
                incremental = "1638316800000", sdkInt = 30, release = "11",
                boardPlatform = "holi", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7325", zygote = "zygote64_32",
                vendorFingerprint = "realme/RMX3363/RE58B2L1:11/RP1A.200720.011/1638316800000:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "RMX3363-user 11 RP1A.200720.011 1638316800000 release-keys",
                buildFlavor = "RMX3363-user",
                buildHost = "oppo-build-02", buildUser = "oppo",
                buildDateUtc = "1638316800", securityPatch = "2021-12-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // =========================================================
            // GRUPO 8: OTROS  (2 dispositivos)
            // =========================================================

            // [39] ASUS ZenFone 7 — SM8250-AB (SD865+) / kona
            "ASUS ZenFone 7" to DeviceFingerprint(
                manufacturer = "asus", brand = "asus", model = "ASUS_I002D",
                device = "ASUS_I002D", product = "WW_I002D",
                hardware = "qcom", board = "kona", bootloader = "unknown",
                fingerprint = "asus/WW_I002D/ASUS_I002D:11/RKQ1.200826.002/18.0840.2101.26-0:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00125-SM8250_GEN_PACK-1",
                incremental = "18.0840.2101.26-0", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250-AB", zygote = "zygote64_32",
                vendorFingerprint = "asus/WW_I002D/ASUS_I002D:11/RKQ1.200826.002/18.0840.2101.26-0:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "WW_I002D-user 11 RKQ1.200826.002 18.0840.2101.26-0 release-keys",
                buildFlavor = "WW_I002D-user",
                buildHost = "ASUS_BUILD_SERVER_01", buildUser = "asus",
                buildDateUtc = "1617235200", securityPatch = "2021-04-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),

            // [40] Realme 8 — MT6785 (Helio G95) / mt6785
            "Realme 8" to DeviceFingerprint(
                manufacturer = "realme", brand = "realme", model = "RMX3085",
                device = "RMX3085", product = "RMX3085",
                hardware = "mt6785", board = "RMX3085", bootloader = "unknown",
                fingerprint = "realme/RMX3085/RMX3085:11/RP1A.200720.011/1630454400000:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V89.6",
                incremental = "1630454400000", sdkInt = 30, release = "11",
                boardPlatform = "mt6785", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6785", zygote = "zygote64_32",
                vendorFingerprint = "realme/RMX3085/RMX3085:11/RP1A.200720.011/1630454400000:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "RMX3085-user 11 RP1A.200720.011 1630454400000 release-keys",
                buildFlavor = "RMX3085-user",
                buildHost = "oppo-build-03", buildUser = "oppo",
                buildDateUtc = "1630454400", securityPatch = "2021-09-01",
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

            // Conditional Hooks (default false)
            if (getBool("hook_webview", false)) hookWebView(lpparam, fp)
            if (getBool("hook_packages", false)) hookPackageManager(lpparam)
            if (getBool("hook_hide_debug", false)) hookApplicationFlags(lpparam)

            if (getBool("hook_hide_root", false)) {
                 hookFile(lpparam)
                 hookProcessBuilderAndRuntime(lpparam)
            }

            hookSSLPinning(lpparam)
            hookSensors(lpparam, fp)

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
                val carrierName = getStr("carrier_name", "")
                val carrier = US_CARRIERS.find {
                    it.mccMnc == mccMnc && it.name.equals(carrierName, ignoreCase = true)
                } ?: US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS[0]
                SpoofingUtils.generatePhoneNumber(carrier.npas)
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

            // [FIX] Dual SIM hook
            try {
                XposedHelpers.findAndHookMethod(tm, "getSubscriberId",
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) {
                            p.result = cachedImsi
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

    private fun hookSensors(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
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
                            "getVendor" -> param.result = when (fp.brand.lowercase()) {
                                "xiaomi", "redmi", "poco" -> "STMicroelectronics"
                                "samsung"                 -> "Samsung Electronics"
                                "google"                  -> "Google"
                                "asus"                    -> "AsusTek"
                                "realme", "oppo", "vivo"  -> "Bosch Sensortec"
                                else                      -> "QTI"
                            }
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
