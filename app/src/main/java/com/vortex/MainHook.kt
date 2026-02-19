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
        private const val PREFS_NAME = "vortex_prefs"   // ← sin "lancelot"

        // ── Apps objetivo — sólo hookear estas, nunca el sistema entero
        private val TARGET_APPS = setOf(
            "com.snapchat.android",
            "com.instagram.android",
            "com.facebook.katana",
            "com.whatsapp",
            "com.google.android.gms",
            "com.google.android.gms.unstable",
            "com.android.vending"
        )

        // ── Cache @Volatile + synchronized para evitar race condition ──
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
        @Volatile private var cachedSsaidSnapchat: String? = null
        @Volatile private var cachedMediaDrmId: String? = null
        @Volatile private var cachedWifiSsid: String? = null
        @Volatile private var cachedWifiBssid: String? = null

        // ── Mock location ──
        @Volatile private var mockLatitude: Double = 0.0
        @Volatile private var mockLongitude: Double = 0.0
        @Volatile private var mockAltitude: Double = 0.0
        @Volatile private var mockAccuracy: Float = 10.0f
        @Volatile private var mockBearing: Float = 0.0f
        @Volatile private var mockSpeed: Float = 0.0f
        @Volatile private var mockLocationEnabled: Boolean = false

        // ── Carriers US con MCC/MNC únicos ──
        // NOTA: T-Mobile, Metro PCS y Google Fi comparten 310260.
        // Se diferencia por nombre (SPN). Metro PCS tiene su propio MNC real: 490.
        // Google Fi usa múltiples redes (T-Mobile+Sprint+US Cellular), se modela como T-Mobile.
        data class UsCarrier(
            val name: String,
            val mccMnc: String,     // lo que devuelve getNetworkOperator()
            val spn: String,        // Service Provider Name — lo que ve el usuario
            val npas: List<String>  // NPAs/area codes típicos de ese carrier
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

        // ── Device fingerprints ──
        // IMPORTANTE: Los valores de device/board "lancelot" en Redmi 9
        // son el CODENAME REAL del hardware. No confundir con el paquete de la app.
        val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9",
                device = "lancelot",                    // ← codename real del chip MT6768
                product = "lancelot_global",
                hardware = "mt6768", board = "lancelot", bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",  // ← una sola entrada (corrección A10)
                incremental = "V12.5.3.0.RJCMIXM",
                sdkInt = 30, release = "11", boardPlatform = "mt6768",
                eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6768",
                zygote = "zygote64_32",
                vendorFingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "lancelot_global-user 11 RP1A.200720.011 V12.5.3.0.RJCMIXM release-keys",
                buildFlavor = "lancelot_global-user",
                buildHost = "pangu-build-component-system-177793",
                buildUser = "builder",
                buildDateUtc = "1632960000",             // segundos unix (no ms)
                securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9",
                device = "merlin", product = "merlin_global",
                hardware = "mt6769", board = "merlin", bootloader = "unknown",
                fingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.2.0.RJOMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6769", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6769", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "merlin_global-user 11 RP1A.200720.011 V12.5.2.0.RJOMIXM release-keys",
                buildFlavor = "merlin_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi 9A" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9A",
                device = "dandelion", product = "dandelion_global",
                hardware = "mt6762", board = "dandelion", bootloader = "unknown",
                fingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.4.0.RCDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608",
                hardwareChipname = "MT6762", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "dandelion_global-user 11 RP1A.200720.011 V12.5.4.0.RCDMIXM release-keys",
                buildFlavor = "dandelion_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1633046400", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi 9C" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9C",
                device = "angelica", product = "angelica_global",
                hardware = "mt6762", board = "angelica", bootloader = "unknown",
                fingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.1.0.RCRMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608",
                hardwareChipname = "MT6762", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "angelica_global-user 11 RP1A.200720.011 V12.5.1.0.RCRMIXM release-keys",
                buildFlavor = "angelica_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9S" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9S",
                device = "curtana", product = "curtana_global",
                hardware = "qcom", board = "curtana", bootloader = "unknown",
                fingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.1.0.RJWMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "curtana_global-user 11 RKQ1.200826.002 V12.5.1.0.RJWMIXM release-keys",
                buildFlavor = "curtana_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9 Pro",
                device = "joyeuse", product = "joyeuse_global",
                hardware = "qcom", board = "joyeuse", bootloader = "unknown",
                fingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.3.0.RJZMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "joyeuse_global-user 11 RKQ1.200826.002 V12.5.3.0.RJZMIXM release-keys",
                buildFlavor = "joyeuse_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO X3 NFC" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO X3 NFC",
                device = "surya", product = "surya_global",
                hardware = "qcom", board = "surya", bootloader = "unknown",
                fingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.7.0.RJGMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150", zygote = "zygote64_32",
                vendorFingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "surya_global-user 11 RKQ1.200826.002 V12.5.7.0.RJGMIXM release-keys",
                buildFlavor = "surya_global-user",
                buildHost = "pangu-build-component-system-177793", buildUser = "builder",
                buildDateUtc = "1634860800", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO X3 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO X3 Pro",
                device = "vayu", product = "vayu_global",
                hardware = "qcom", board = "vayu", bootloader = "unknown",
                fingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.5.0.RJUMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.5.0.RJUMIXM", sdkInt = 30, release = "11",
                boardPlatform = "msmnile", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8150-AC", zygote = "zygote64_32",
                vendorFingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.5.0.RJUMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "vayu_global-user 11 RKQ1.200826.002 V12.5.5.0.RJUMIXM release-keys",
                buildFlavor = "vayu_global-user",
                buildHost = "cr3-buildbot-02", buildUser = "builder",
                buildDateUtc = "1634860800", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO M3" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO M3",
                device = "citrus", product = "citrus_global",
                hardware = "qcom", board = "citrus", bootloader = "unknown",
                fingerprint = "POCO/citrus_global/citrus:11/RKQ1.200826.002/V12.5.2.0.RJBMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "V12.5.2.0.RJBMIXM", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6115", zygote = "zygote64_32",
                vendorFingerprint = "POCO/citrus_global/citrus:11/RKQ1.200826.002/V12.5.2.0.RJBMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "citrus_global-user 11 RKQ1.200826.002 V12.5.2.0.RJBMIXM release-keys",
                buildFlavor = "citrus_global-user",
                buildHost = "cr3-buildbot-02", buildUser = "builder",
                buildDateUtc = "1632960000", securityPatch = "2021-09-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 10T" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "Mi 10T",
                device = "apollo", product = "apollo_global",
                hardware = "qcom", board = "apollo", bootloader = "unknown",
                fingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.11.0.RJDMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.11.0.RJDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.11.0.RJDMIXM:user/release-keys",
                display = "RKQ1.200826.002",
                buildDescription = "apollo_global-user 11 RKQ1.200826.002 V12.5.11.0.RJDMIXM release-keys",
                buildFlavor = "apollo_global-user",
                buildHost = "cr3-buildbot-02", buildUser = "builder",
                buildDateUtc = "1634860800", securityPatch = "2021-10-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A52" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A525F",
                device = "a52q", product = "a52qxx",
                hardware = "qcom", board = "a52q", bootloader = "A525FXXU4CVJB",
                fingerprint = "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "A525FXXU4CVJB", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7125", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                display = "RP1A.200720.012.A525FXXU4CVJB",
                buildDescription = "a52qxx-user 11 RP1A.200720.012 A525FXXU4CVJB release-keys",
                buildFlavor = "a52qxx-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO: "dpi" → "se.infra"
                buildDateUtc = "1639497600", securityPatch = "2021-12-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A32" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A325F",
                device = "a32", product = "a32xxx",
                hardware = "mt6853", board = "a32", bootloader = "A325FXXU2BUG1",
                fingerprint = "samsung/a32xxx/a32:11/RP1A.200720.012/A325FXXU2BUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR14A.R3.MP.V62.P4",
                incremental = "A325FXXU2BUG1", sdkInt = 30, release = "11",
                boardPlatform = "mt6853", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6853", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a32xxx/a32:11/RP1A.200720.012/A325FXXU2BUG1:user/release-keys",
                display = "RP1A.200720.012.A325FXXU2BUG1",
                buildDescription = "a32xxx-user 11 RP1A.200720.012 A325FXXU2BUG1 release-keys",
                buildFlavor = "a32xxx-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A51" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A515F",
                device = "a51", product = "a51xxx",
                hardware = "exynos9610", board = "a51", bootloader = "A515FXXU4CUG1",
                fingerprint = "samsung/a51xxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "A515FXXU4CUG1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9610", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "Exynos9611", zygote = "zygote64_32",
                vendorFingerprint = "samsung/a51xxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
                display = "RP1A.200720.012.A515FXXU4CUG1",
                buildDescription = "a51xxx-user 11 RP1A.200720.012 A515FXXU4CUG1 release-keys",
                buildFlavor = "a51xxx-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO
                buildDateUtc = "1625097600", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy M12" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-M127F",
                device = "m12", product = "m12xid",
                hardware = "exynos850", board = "m12", bootloader = "M127FXXU3BUK1",
                fingerprint = "samsung/m12xid/m12:11/RP1A.200720.012/M127FXXU3BUK1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "M127FXXU3BUK1", sdkInt = 30, release = "11",
                boardPlatform = "exynos850", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "S5E3830", zygote = "zygote64_32",
                vendorFingerprint = "samsung/m12xid/m12:11/RP1A.200720.012/M127FXXU3BUK1:user/release-keys",
                display = "RP1A.200720.012.M127FXXU3BUK1",
                buildDescription = "m12xid-user 11 RP1A.200720.012 M127FXXU3BUK1 release-keys",
                buildFlavor = "m12xid-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO
                buildDateUtc = "1636934400", securityPatch = "2021-11-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy S20+" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-G985F",
                device = "y2s", product = "y2sxx",
                hardware = "exynos990", board = "universal990", bootloader = "G985FXXSGHWA3",
                fingerprint = "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "G985FXXSGHWA3", sdkInt = 30, release = "11",
                boardPlatform = "exynos990", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "Exynos990", zygote = "zygote64_32",
                vendorFingerprint = "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys",
                display = "RP1A.200720.012.G985FXXSGHWA3",
                buildDescription = "y2sxx-user 11 RP1A.200720.012 G985FXXSGHWA3 release-keys",
                buildFlavor = "y2sxx-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO
                // buildDateUtc de Dic 2021 pero securityPatch 2022-01 es coherente para HWA3
                buildDateUtc = "1639497600", securityPatch = "2022-01-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy S10e" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-G970F",
                device = "beyond0", product = "beyond0ltexx",
                hardware = "exynos9820", board = "universal9820", bootloader = "G970FXXSGHWC1",
                fingerprint = "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "G970FXXSGHWC1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9820", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "Exynos9820", zygote = "zygote64_32",
                vendorFingerprint = "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys",
                display = "RP1A.200720.012.G970FXXSGHWC1",
                buildDescription = "beyond0ltexx-user 11 RP1A.200720.012 G970FXXSGHWC1 release-keys",
                buildFlavor = "beyond0ltexx-user",
                buildHost = "21R3NF12", buildUser = "se.infra",   // ← CORREGIDO
                buildDateUtc = "1646236800", securityPatch = "2022-03-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 4a" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a",
                device = "sunfish", product = "sunfish",
                hardware = "qcom", board = "sunfish", bootloader = "s5-0.5-9637172",
                fingerprint = "google/sunfish/sunfish:11/RQ3A.210705.001/7380771:user/release-keys",
                buildId = "RQ3A.210705.001", tags = "release-keys", type = "user",
                radioVersion = "g7150-00023-210610-B-7310372",
                incremental = "7380771", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7150", zygote = "zygote64_32",
                vendorFingerprint = "google/sunfish_vend/sunfish:11/RQ3A.210705.001/7380771:vendor/release-keys",
                display = "RQ3A.210705.001",
                buildDescription = "sunfish-user 11 RQ3A.210705.001 7380771 release-keys",
                buildFlavor = "sunfish-user",
                buildHost = "abfarm-release-rbe-64.hot.corp.google.com",
                buildUser = "android-build",
                buildDateUtc = "1625616000", securityPatch = "2021-07-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 5" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 5",
                device = "redfin", product = "redfin",
                hardware = "qcom", board = "redfin", bootloader = "r8-0.3-7219051",
                fingerprint = "google/redfin/redfin:11/RQ3A.210805.001.A1/7512229:user/release-keys",
                buildId = "RQ3A.210805.001.A1", tags = "release-keys", type = "user",
                radioVersion = "g725-00164-210812-B-7522969",
                incremental = "7512229", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "google/redfin_vend/redfin:11/RQ3A.210805.001.A1/7512229:vendor/release-keys",
                display = "RQ3A.210805.001.A1",
                buildDescription = "redfin-user 11 RQ3A.210805.001.A1 7512229 release-keys",
                buildFlavor = "redfin-user",
                buildHost = "abfarm-release-rbe-64.hot.corp.google.com",
                buildUser = "android-build",
                buildDateUtc = "1628100000", securityPatch = "2021-08-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 4a 5G" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a (5G)",
                device = "bramble", product = "bramble",
                hardware = "qcom", board = "bramble", bootloader = "b2-0.3-7214727",
                fingerprint = "google/bramble/bramble:11/RQ3A.210705.001/7380771:user/release-keys",
                buildId = "RQ3A.210705.001", tags = "release-keys", type = "user",
                radioVersion = "g7250-00195-210614-B-7352378",
                incremental = "7380771", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "google/bramble_vend/bramble:11/RQ3A.210705.001/7380771:vendor/release-keys",
                display = "RQ3A.210705.001",
                buildDescription = "bramble-user 11 RQ3A.210705.001 7380771 release-keys",
                buildFlavor = "bramble-user",
                buildHost = "abfarm-release-rbe-64.hot.corp.google.com",
                buildUser = "android-build",
                buildDateUtc = "1625616000", securityPatch = "2021-07-05",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "OnePlus Nord" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "OnePlus Nord",
                device = "avicii", product = "OnePlus Nord",
                hardware = "qcom", board = "avicii", bootloader = "avicii_11_A.11",
                fingerprint = "OnePlus/OnePlus Nord/avicii:11/RKQ1.201022.002/2107141742:user/release-keys",
                buildId = "RKQ1.201022.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "2107141742", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM7250", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/OnePlus Nord/avicii:11/RKQ1.201022.002/2107141742:user/release-keys",
                display = "RKQ1.201022.002.2107141742",
                buildDescription = "OnePlus Nord-user 11 RKQ1.201022.002 2107141742 release-keys",
                buildFlavor = "OnePlus Nord-user",
                buildHost = "buildserver12", buildUser = "jenkins",
                buildDateUtc = "1626307200", securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "OnePlus 8T" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "KB2005",
                device = "kebab", product = "OnePlus8T",
                hardware = "qcom", board = "kebab", bootloader = "kebab_21_A.14",
                fingerprint = "OnePlus/OnePlus8T/kebab:11/RKQ1.201022.002/2106051219:user/release-keys",
                buildId = "RKQ1.201022.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "2106051219", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8250", zygote = "zygote64_32",
                vendorFingerprint = "OnePlus/OnePlus8T/kebab:11/RKQ1.201022.002/2106051219:user/release-keys",
                display = "RKQ1.201022.002.2106051219",
                buildDescription = "OnePlus8T-user 11 RKQ1.201022.002 2106051219 release-keys",
                buildFlavor = "OnePlus8T-user",
                buildHost = "buildserver12", buildUser = "jenkins",
                buildDateUtc = "1622937600", securityPatch = "2021-06-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Moto G30" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g30",
                device = "caprip", product = "caprip",
                hardware = "qcom", board = "caprip", bootloader = "unknown",
                fingerprint = "motorola/caprip/caprip:11/RRHS31.Q3-47-32/a57fec:user/release-keys",
                buildId = "RRHS31.Q3-47-32", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "a57fec", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6115", zygote = "zygote64_32",
                vendorFingerprint = "motorola/caprip/caprip:11/RRHS31.Q3-47-32/a57fec:user/release-keys",
                display = "RRHS31.Q3-47-32",
                buildDescription = "caprip-user 11 RRHS31.Q3-47-32 a57fec release-keys",
                buildFlavor = "caprip-user",
                buildHost = "buildbot-motoauto06.mcd.mot.com", buildUser = "hudsoncm",
                buildDateUtc = "1630022400", securityPatch = "2021-08-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Moto G Power 2021" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g power (2021)",
                device = "borneo", product = "borneo",
                hardware = "qcom", board = "borneo", bootloader = "unknown",
                fingerprint = "motorola/borneo/borneo:11/RRQ31.Q3-47-22/2b4fae:user/release-keys",
                buildId = "RRQ31.Q3-47-22", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "2b4fae", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM6115", zygote = "zygote64_32",
                vendorFingerprint = "motorola/borneo/borneo:11/RRQ31.Q3-47-22/2b4fae:user/release-keys",
                display = "RRQ31.Q3-47-22",
                buildDescription = "borneo-user 11 RRQ31.Q3-47-22 2b4fae release-keys",
                buildFlavor = "borneo-user",
                buildHost = "buildbot-motoauto06.mcd.mot.com", buildUser = "hudsoncm",
                buildDateUtc = "1619827200", securityPatch = "2021-05-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Realme 7 5G" to DeviceFingerprint(
                manufacturer = "realme", brand = "realme", model = "RMX2111",
                device = "RMX2111", product = "RMX2111",
                hardware = "mt6853", board = "RMX2111", bootloader = "unknown",
                fingerprint = "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR14A.R3.MP.V62",
                incremental = "1626245367", sdkInt = 30, release = "11",
                boardPlatform = "mt6853", eglDriver = "mali", openGlEs = "196610",
                hardwareChipname = "MT6853", zygote = "zygote64_32",
                vendorFingerprint = "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367:user/release-keys",
                display = "RP1A.200720.011",
                buildDescription = "RMX2111-user 11 RP1A.200720.011 1626245367 release-keys",
                buildFlavor = "RMX2111-user",
                buildHost = "ubuntu-123", buildUser = "jenkins",
                buildDateUtc = "1626245367",      // ← CORREGIDO: era "1626245367375" (ms), ahora segundos
                securityPatch = "2021-07-01",
                buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Asus Zenfone 8" to DeviceFingerprint(
                manufacturer = "asus", brand = "asus", model = "ASUS_I006D",
                device = "ASUS_I006D", product = "WW_I006D",
                hardware = "qcom", board = "sake", bootloader = "unknown",
                fingerprint = "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                buildId = "RKQ1.201112.002", tags = "release-keys", type = "user",
                radioVersion = "M3.13.24.51-Sake_0000100",
                incremental = "30.11.51.115", sdkInt = 30, release = "11",
                boardPlatform = "lahaina", eglDriver = "adreno", openGlEs = "196610",
                hardwareChipname = "SM8350", zygote = "zygote64_32",
                vendorFingerprint = "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                display = "RKQ1.201112.002.30.11.51.115",
                buildDescription = "WW_I006D-user 11 RKQ1.201112.002 30.11.51.115 release-keys",
                buildFlavor = "WW_I006D-user",
                buildHost = "android-build", buildUser = "jenkins",
                buildDateUtc = "1629859200", securityPatch = "2021-08-01",
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

    // ─────────────────────────────────────────────────────────────
    //  HANDLE LOAD PACKAGE
    // ─────────────────────────────────────────────────────────────

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // [FIX L2] Solo hookear apps objetivo, nunca el sistema completo
        if (lpparam.packageName !in TARGET_APPS) return

        try {
            val prefs = XSharedPreferences("com.vortex", PREFS_NAME)   // ← paquete correcto
            prefs.reload()

            fun getStr(key: String, def: String): String {
                val raw = prefs.getString(key, null)
                return CryptoUtils.decrypt(raw) ?: def
            }
            // [FIX U3] También descifrar booleanos
            fun getBool(key: String, def: Boolean): Boolean {
                val v = getStr(key, "")
                return if (v.isEmpty()) def else v.toBooleanStrictOrNull() ?: def
            }

            val profileName = getStr("profile", "Redmi 9")
            val fp = DEVICE_FINGERPRINTS[profileName] ?: DEVICE_FINGERPRINTS["Redmi 9"]!!

            synchronized(this) { initializeCache(prefs, ::getStr) }

            hookBuildFields(lpparam, fp)
            hookSystemProperties(lpparam, fp)
            hookTelephonyManager(lpparam)
            hookSettingsSecure(lpparam)
            hookNetworkInterfaces(lpparam)
            hookWifiManager(lpparam)               // [NEW A14]
            hookBluetoothAdapter(lpparam)          // [NEW A15]
            hookLocation(lpparam, prefs, ::getStr)

            // Always hook webview and packages (removed conditional checks as per "Always On")
            hookWebView(lpparam, fp)
            hookAccountManager(lpparam)
            hookPackageManager(lpparam)
            hookApplicationFlags(lpparam)          // [FIX A6] versión correcta
            hookFingerprintedPartitions(lpparam, fp)  // [NEW A13]

            // Vortex Injections
            hookMediaDrm(lpparam)
            hookWifiInfo(lpparam)

        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("Vortex error: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  INIT CACHE  (llamar dentro de synchronized)
    // ─────────────────────────────────────────────────────────────

    private fun initializeCache(prefs: XSharedPreferences, getStr: (String, String) -> String) {
        val profileName = getStr("profile", "Redmi 9")
        val mccMnc = getStr("mcc_mnc", "310260")

        if (cachedMccMnc == null) cachedMccMnc = mccMnc

        if (cachedImei == null)        cachedImei        = getStr("imei",          SpoofingUtils.generateValidImei(profileName))
        if (cachedImei2 == null)       cachedImei2       = getStr("imei2",         SpoofingUtils.generateValidImei(profileName))
        if (cachedAndroidId == null)   cachedAndroidId   = getStr("android_id",    SpoofingUtils.generateRandomId(16))
        if (cachedGsfId == null)       cachedGsfId       = getStr("gsf_id",        SpoofingUtils.generateRandomId(16))
        if (cachedGaid == null)        cachedGaid        = getStr("gaid",          SpoofingUtils.generateRandomGaid())    // [FIX A2] UUID v4

        val brand = DEVICE_FINGERPRINTS[profileName]?.brand ?: ""
        if (cachedSerial == null)      cachedSerial      = getStr("serial",        SpoofingUtils.generateRandomSerial(brand))
        if (cachedWifiMac == null)     cachedWifiMac     = getStr("wifi_mac",      SpoofingUtils.generateRandomMac())
        if (cachedBtMac == null)       cachedBtMac       = getStr("bluetooth_mac", SpoofingUtils.generateRandomMac())
        if (cachedGmail == null)       cachedGmail       = getStr("gmail",         SpoofingUtils.generateRealisticGmail())  // [FIX A11]

        // Vortex Extra IDs
        if (cachedSsaidSnapchat == null) cachedSsaidSnapchat = getStr("ssaid_snapchat", SpoofingUtils.generateRandomId(16))
        if (cachedMediaDrmId == null)    cachedMediaDrmId    = getStr("media_drm_id",   SpoofingUtils.generateRandomId(32))
        if (cachedWifiSsid == null)      cachedWifiSsid      = getStr("wifi_ssid",      "Vortex-5G")
        if (cachedWifiBssid == null)     cachedWifiBssid     = getStr("wifi_bssid",     SpoofingUtils.generateRandomMac())

        // [FIX A1] IMSI = MCC+MNC(6) + MSIN(9) = 15 dígitos exactos
        if (cachedImsi == null)        cachedImsi        = getStr("imsi",          SpoofingUtils.generateValidImsi(mccMnc))
        if (cachedIccid == null)       cachedIccid       = getStr("iccid",         SpoofingUtils.generateValidIccid(mccMnc))

        // Número de teléfono usando NPAs del carrier correcto
        if (cachedPhoneNumber == null) {
            val savedPhone = getStr("phone_number", "")
            cachedPhoneNumber = if (savedPhone.isNotEmpty()) savedPhone else {
                val carrier = US_CARRIERS.find { it.mccMnc == mccMnc && it.name == getStr("carrier_name","") }
                    ?: US_CARRIERS.find { it.mccMnc == mccMnc }   // [FIX A3] fallback por nombre
                SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList())
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  BUILD FIELDS
    // ─────────────────────────────────────────────────────────────

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
            XposedHelpers.setStaticObjectField(b, "USER",         fp.buildUser)   // [FIX A8]
            XposedHelpers.setStaticObjectField(b, "SERIAL",       cachedSerial)
            // [FIX A9] buildDateUtc ya son segundos unix, multiplicar por 1000 da ms correcto
            XposedHelpers.setStaticLongField(b, "TIME", fp.buildDateUtc.toLong() * 1000L)

            val v = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(v,    "SDK_INT",        fp.sdkInt)
            XposedHelpers.setStaticObjectField(v, "RELEASE",        fp.release)
            XposedHelpers.setStaticObjectField(v, "INCREMENTAL",    fp.incremental)
            XposedHelpers.setStaticObjectField(v, "SECURITY_PATCH", fp.securityPatch)
            XposedHelpers.setStaticObjectField(v, "CODENAME",       fp.buildVersionCodename)
            XposedHelpers.setStaticIntField(v,    "PREVIEW_SDK_INT",fp.buildVersionPreviewSdk.toInt())

            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedSerial }
                    })
            } catch (_: NoSuchMethodError) {}
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  SYSTEM PROPERTIES
    // ─────────────────────────────────────────────────────────────

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
                        "ro.build.user"                   -> fp.buildUser     // [FIX A8]
                        "ro.build.date.utc"               -> fp.buildDateUtc
                        "ro.build.version.security_patch" -> fp.securityPatch
                        "ro.build.version.codename"       -> fp.buildVersionCodename
                        "ro.build.version.preview_sdk"    -> fp.buildVersionPreviewSdk
                        "ro.build.characteristics"        -> "default"
                        // particiones system/vendor/odm/product
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
                        // boot/security
                        "ro.debuggable"                   -> "0"
                        "ro.secure"                       -> "1"
                        "ro.kernel.qemu"                  -> "0"
                        "service.adb.root"                -> "0"
                        "ro.boot.serialno"                -> cachedSerial
                        "ro.boot.hardware"                -> fp.hardware
                        "ro.boot.bootloader"              -> fp.bootloader
                        "ro.boot.verifiedbootstate"       -> "green"
                        "ro.boot.flash.locked"            -> "1"
                        "ro.boot.vbmeta.device_state"     -> "locked"
                        "persist.sys.usb.config"          -> "none"
                        else -> null
                    }
                    if (res != null) param.result = res
                }
            }
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, hook)
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, String::class.java, hook)
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  TELEPHONY MANAGER
    // ─────────────────────────────────────────────────────────────

    private fun hookTelephonyManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val tm = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)
            val mccMnc = cachedMccMnc ?: "310260"
            // [FIX A3] buscar por nombre guardado + MCC/MNC
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

                // MEID Hooks (Vortex addition)
                XposedHelpers.findAndHookMethod(tm, "getMeid", after(cachedImei))
                XposedHelpers.findAndHookMethod(tm, "getMeid", Int::class.javaPrimitiveType, object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        p.result = if ((p.args[0] as Int) == 0) cachedImei else cachedImei2
                    }
                })
            } catch (_: NoSuchMethodError) {}
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  SETTINGS SECURE
    // ─────────────────────────────────────────────────────────────

    private fun hookSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getString",
                android.content.ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[1] as String) {
                            Settings.Secure.ANDROID_ID   -> {
                                if (lpparam.packageName == "com.snapchat.android" && cachedSsaidSnapchat != null) {
                                    param.result = cachedSsaidSnapchat
                                } else {
                                    param.result = cachedAndroidId
                                }
                            }
                            "advertising_id"             -> param.result = cachedGaid
                            "gsf_id", "android_id_gsf"  -> param.result = cachedGsfId
                        }
                    }
                })
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  NETWORK INTERFACES (WiFi + Bluetooth via NetworkInterface)
    // ─────────────────────────────────────────────────────────────

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

    // [FIX A14] WiFi MAC vía WifiManager.getConnectionInfo()
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

    // Vortex: WifiInfo SSID/BSSID
    private fun hookWifiInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = "\"$cachedWifiSsid\"" } })
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getBSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedWifiBssid } })
        } catch (e: Throwable) {}
    }

    // [FIX A15] Bluetooth MAC vía BluetoothAdapter.getAddress()
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

    // Vortex: MediaDrm
    private fun hookMediaDrm(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val drmClass = XposedHelpers.findClass("android.media.MediaDrm", lpparam.classLoader)

            // Hook getPropertyByteArray
            XposedHelpers.findAndHookMethod(drmClass, "getPropertyByteArray", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if ((key == "deviceUniqueId" || key == "deviceId") && cachedMediaDrmId != null) {
                         param.result = hexStringToByteArray(cachedMediaDrmId!!)
                    }
                }
            })

            // Hook getPropertyString (New)
            XposedHelpers.findAndHookMethod(drmClass, "getPropertyString", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if ((key == "deviceUniqueId" || key == "deviceId") && cachedMediaDrmId != null) {
                         param.result = cachedMediaDrmId
                    }
                }
            })
        } catch (e: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  LOCATION
    // ─────────────────────────────────────────────────────────────

    private fun hookLocation(lpparam: XC_LoadPackage.LoadPackageParam,
                             prefs: XSharedPreferences,
                             getStr: (String, String) -> String) {
        try {
            // [FIX A12] Guard PRIMERO, luego calcular jitter
            mockLocationEnabled = prefs.getBoolean("mock_location_enabled", false)
            if (!mockLocationEnabled) return   // ← salir antes de modificar variables

            mockLatitude  = getStr("mock_latitude",  "40.7128").toDoubleOrNull() ?: 40.7128
            mockLongitude = getStr("mock_longitude", "-74.0060").toDoubleOrNull() ?: -74.0060
            mockAltitude  = getStr("mock_altitude",  "10.0").toDoubleOrNull()  ?: 10.0
            mockAccuracy  = getStr("mock_accuracy",  "5.0").toFloatOrNull()    ?: 5.0f

            val rng = Random()
            mockBearing  = rng.nextFloat() * 360f
            mockSpeed    = rng.nextFloat() * 3f   // 0–3 m/s caminando
            // Jitter gaussiano pequeño — solo cuando mock está activo
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

    // ─────────────────────────────────────────────────────────────
    //  WEBVIEW
    // ─────────────────────────────────────────────────────────────

    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            val cls = XposedHelpers.findClass("android.webkit.WebSettings", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getUserAgentString", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ua = param.result as? String ?: return
                    // Reemplazar modelo Y versión Android en el UA
                    val patched = ua
                        .replace(Regex(";\\s+[^;]+?\\s+Build/[^)]+"), "; ${fp.model} Build/${fp.buildId}")
                        .replace(Regex("Android [0-9.]+"), "Android ${fp.release}")
                    param.result = patched
                }
            })
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  ACCOUNT MANAGER
    // ─────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────
    //  PACKAGE MANAGER  [FIX A7] también hookear getInstallSourceInfo (API 30)
    // ─────────────────────────────────────────────────────────────

    private fun hookPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader)

            // API antigua
            XposedHelpers.findAndHookMethod(cls, "getInstallerPackageName", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        p.result = "com.android.vending"
                    }
                })

            // [FIX A7] API 30: getInstallSourceInfo()
            try {
                XposedHelpers.findAndHookMethod(cls, "getInstallSourceInfo", String::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            // Crear un InstallSourceInfo simulando instalación desde Play Store
                            val infoClass = XposedHelpers.findClass(
                                "android.content.pm.InstallSourceInfo", lpparam.classLoader)
                            try {
                                val ctor = infoClass.getDeclaredConstructor(
                                    String::class.java, String::class.java,
                                    String::class.java, String::class.java
                                )
                                ctor.isAccessible = true
                                param.result = ctor.newInstance(
                                    "com.android.vending",  // initiatingPackageName
                                    null,                   // initiatingPackageSigningInfo
                                    "com.android.vending",  // originatingPackageName
                                    "com.android.vending"   // installingPackageName
                                )
                            } catch (_: Exception) {
                                // Constructor diferente según versión del sistema
                                // Si falla, el campo installerPackageName ya está hookeado arriba
                            }
                        }
                    })
            } catch (_: NoSuchMethodError) {}

        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  APPLICATION FLAGS  [FIX A6] hook correcto: campo, no método
    // ─────────────────────────────────────────────────────────────

    private fun hookApplicationFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getApplicationInfo",
                String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val appInfo = param.result as? android.content.pm.ApplicationInfo ?: return
                        // Limpiar FLAG_DEBUGGABLE (0x2) y FLAG_TEST_ONLY (0x100)
                        appInfo.flags = appInfo.flags and
                            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv() and
                            0x100.inv()
                    }
                })
        } catch (_: Throwable) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  FINGERPRINTED PARTITIONS  [FIX A13] API 30
    // ─────────────────────────────────────────────────────────────

    private fun hookFingerprintedPartitions(lpparam: XC_LoadPackage.LoadPackageParam, fp: DeviceFingerprint) {
        try {
            XposedHelpers.findAndHookMethod(Build::class.java, "getFingerprintedPartitions",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Devolver lista vacía hace que el método retorne sin revelar
                        // los fingerprints reales de las particiones del dispositivo físico
                        param.result = emptyList<Any>()
                    }
                })
        } catch (_: Throwable) {
            // API no disponible en esta versión, ignorar
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

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
}
