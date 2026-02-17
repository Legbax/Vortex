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

        fun getUsCarriers(): List<UsCarrier> = US_CARRIERS

        private val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9", device = "lancelot", product = "lancelot_global",
                hardware = "mt6768", board = "lancelot", bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.3.0.RJCMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6768", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6768", zygote = "zygote64_32", vendorFingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                display = "V12.5.3.0.RJCMIXM", buildDescription = "lancelot_global-user 11 RP1A.200720.011 V12.5.3.0.RJCMIXM release-keys", buildFlavor = "lancelot_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9", device = "merlin", product = "merlin_global",
                hardware = "mt6769", board = "merlin", bootloader = "unknown",
                fingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.2.0.RJOMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6769", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6769", zygote = "zygote64_32", vendorFingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                display = "V12.5.2.0.RJOMIXM", buildDescription = "merlin_global-user 11 RP1A.200720.011 V12.5.2.0.RJOMIXM release-keys", buildFlavor = "merlin_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi 9A" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9A", device = "dandelion", product = "dandelion_global",
                hardware = "mt6762", board = "dandelion", bootloader = "unknown",
                fingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.4.0.RCDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608", hardwareChipname = "MT6762", zygote = "zygote64_32", vendorFingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                display = "V12.5.4.0.RCDMIXM", buildDescription = "dandelion_global-user 11 RP1A.200720.011 V12.5.4.0.RCDMIXM release-keys", buildFlavor = "dandelion_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1633046400", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi 9C" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi 9C", device = "angelica", product = "angelica_global",
                hardware = "mt6762", board = "angelica", bootloader = "unknown",
                fingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47,MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.1.0.RCRMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6762", eglDriver = "pvrsrvkm", openGlEs = "196608", hardwareChipname = "MT6762", zygote = "zygote64_32", vendorFingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                display = "V12.5.1.0.RCRMIXM", buildDescription = "angelica_global-user 11 RP1A.200720.011 V12.5.1.0.RCRMIXM release-keys", buildFlavor = "angelica_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9S" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9S", device = "curtana", product = "curtana_global",
                hardware = "qcom", board = "curtana", bootloader = "unknown",
                fingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.1.0.RJWMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7125", zygote = "zygote64_32", vendorFingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                display = "V12.5.1.0.RJWMIXM", buildDescription = "curtana_global-user 11 RKQ1.200826.002 V12.5.1.0.RJWMIXM release-keys", buildFlavor = "curtana_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 9 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 9 Pro", device = "joyeuse", product = "joyeuse_global",
                hardware = "qcom", board = "joyeuse", bootloader = "unknown",
                fingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.3.0.RJZMIXM", sdkInt = 30, release = "11",
                boardPlatform = "trinket", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7125", zygote = "zygote64_32", vendorFingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                display = "V12.5.3.0.RJZMIXM", buildDescription = "joyeuse_global-user 11 RKQ1.200826.002 V12.5.3.0.RJZMIXM release-keys", buildFlavor = "joyeuse_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO X3 NFC" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO X3 NFC", device = "surya", product = "surya_global",
                hardware = "qcom", board = "surya", bootloader = "unknown",
                fingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.7.0.RJGMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7150", zygote = "zygote64_32", vendorFingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                display = "V12.5.7.0.RJGMIXM", buildDescription = "surya_global-user 11 RKQ1.200826.002 V12.5.7.0.RJGMIXM release-keys", buildFlavor = "surya_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1634860800", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO X3 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO X3 Pro", device = "vayu", product = "vayu_global",
                hardware = "qcom", board = "vayu", bootloader = "unknown",
                fingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.5.0.RJUMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.5.0.RJUMIXM", sdkInt = 30, release = "11",
                boardPlatform = "vayu", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM8150", zygote = "zygote64_32", vendorFingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.5.0.RJUMIXM:user/release-keys",
                display = "V12.5.5.0.RJUMIXM", buildDescription = "vayu_global-user 11 RKQ1.200826.002 V12.5.5.0.RJUMIXM release-keys", buildFlavor = "vayu_global-user", buildHost = "cr3-buildbot-02", buildUser = "builder", buildDateUtc = "1634860800", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO M3" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO M3", device = "citrus", product = "citrus_global",
                hardware = "qcom", board = "citrus", bootloader = "unknown",
                fingerprint = "POCO/citrus_global/citrus:11/RKQ1.200826.002/V12.5.2.0.RJBMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "V12.5.2.0.RJBMIXM", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM6115", zygote = "zygote64_32", vendorFingerprint = "POCO/citrus_global/citrus:11/RKQ1.200826.002/V12.5.2.0.RJBMIXM:user/release-keys",
                display = "V12.5.2.0.RJBMIXM", buildDescription = "citrus_global-user 11 RKQ1.200826.002 V12.5.2.0.RJBMIXM release-keys", buildFlavor = "citrus_global-user", buildHost = "cr3-buildbot-02", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "POCO M3 Pro 5G" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "POCO", model = "POCO M3 Pro 5G", device = "camellia", product = "camellia_global",
                hardware = "mt6833", board = "camellia", bootloader = "unknown",
                fingerprint = "POCO/camellia_global/camellia:11/RP1A.200720.011/V12.5.3.0.RKSMIXM:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V84.P47",
                incremental = "V12.5.3.0.RKSMIXM", sdkInt = 30, release = "11",
                boardPlatform = "mt6833", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6833", zygote = "zygote64_32", vendorFingerprint = "POCO/camellia_global/camellia:11/RP1A.200720.011/V12.5.3.0.RKSMIXM:user/release-keys",
                display = "V12.5.3.0.RKSMIXM", buildDescription = "camellia_global-user 11 RP1A.200720.011 V12.5.3.0.RKSMIXM release-keys", buildFlavor = "camellia_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 10T" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "Mi 10T", device = "apollo", product = "apollo_global",
                hardware = "qcom", board = "apollo", bootloader = "unknown",
                fingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.11.0.RJDMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.11.0.RJDMIXM", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM8250", zygote = "zygote64_32", vendorFingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.11.0.RJDMIXM:user/release-keys",
                display = "V12.5.11.0.RJDMIXM", buildDescription = "apollo_global-user 11 RKQ1.200826.002 V12.5.11.0.RJDMIXM release-keys", buildFlavor = "apollo_global-user", buildHost = "cr3-buildbot-02", buildUser = "builder", buildDateUtc = "1634860800", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 11 Lite" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "Mi 11 Lite", device = "courbet", product = "courbet_global",
                hardware = "qcom", board = "courbet", bootloader = "unknown",
                fingerprint = "Xiaomi/courbet_global/courbet:11/RKQ1.200826.002/V12.5.5.0.RKQMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.5.0.RKQMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7150", zygote = "zygote64_32", vendorFingerprint = "Xiaomi/courbet_global/courbet:11/RKQ1.200826.002/V12.5.5.0.RKQMIXM:user/release-keys",
                display = "V12.5.5.0.RKQMIXM", buildDescription = "courbet_global-user 11 RKQ1.200826.002 V12.5.5.0.RKQMIXM release-keys", buildFlavor = "courbet_global-user", buildHost = "cr3-buildbot-02", buildUser = "builder", buildDateUtc = "1634860800", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Redmi Note 10 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Redmi", model = "Redmi Note 10 Pro", device = "sweet", product = "sweet_global",
                hardware = "qcom", board = "sweet", bootloader = "unknown",
                fingerprint = "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.3.0.RKIMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.3.0.RKIMIXM", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7150", zygote = "zygote64_32", vendorFingerprint = "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.3.0.RKIMIXM:user/release-keys",
                display = "V12.5.3.0.RKIMIXM", buildDescription = "sweet_global-user 11 RKQ1.200826.002 V12.5.3.0.RKIMIXM release-keys", buildFlavor = "sweet_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 10 Lite" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "Mi 10 Lite", device = "vangogh", product = "vangogh_global",
                hardware = "qcom", board = "vangogh", bootloader = "unknown",
                fingerprint = "Xiaomi/vangogh_global/vangogh:11/RKQ1.200826.002/V12.5.2.0.RJEMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "V12.5.2.0.RJEMIXM", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7250", zygote = "zygote64_32", vendorFingerprint = "Xiaomi/vangogh_global/vangogh:11/RKQ1.200826.002/V12.5.2.0.RJEMIXM:user/release-keys",
                display = "V12.5.2.0.RJEMIXM", buildDescription = "vangogh_global-user 11 RKQ1.200826.002 V12.5.2.0.RJEMIXM release-keys", buildFlavor = "vangogh_global-user", buildHost = "pangu-build-component-system-177793", buildUser = "builder", buildDateUtc = "1632960000", securityPatch = "2021-09-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Mi 11i" to DeviceFingerprint(
                manufacturer = "Xiaomi", brand = "Xiaomi", model = "Mi 11i", device = "haydn", product = "haydn_global",
                hardware = "qcom", board = "haydn", bootloader = "unknown",
                fingerprint = "Xiaomi/haydn_global/haydn:11/RKQ1.200826.002/V12.5.7.0.RKKMIXM:user/release-keys",
                buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "V12.5.7.0.RKKMIXM", sdkInt = 30, release = "11",
                boardPlatform = "laaayna", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM888", zygote = "zygote64_32", vendorFingerprint = "Xiaomi/haydn_global/haydn:11/RKQ1.200826.002/V12.5.7.0.RKKMIXM:user/release-keys",
                display = "V12.5.7.0.RKKMIXM", buildDescription = "haydn_global-user 11 RKQ1.200826.002 V12.5.7.0.RKKMIXM release-keys", buildFlavor = "haydn_global-user", buildHost = "cr3-buildbot-02", buildUser = "builder", buildDateUtc = "1634860800", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A52" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A525F", device = "a52q", product = "a52qxx",
                hardware = "qcom", board = "a52q", bootloader = "A525FXXU4CVJB",
                fingerprint = "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "A525FXXU4CVJB", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7125", zygote = "zygote64_32", vendorFingerprint = "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                display = "RP1A.200720.012.A525FXXU4CVJB", buildDescription = "a52qxx-user 11 RP1A.200720.012 A525FXXU4CVJB release-keys", buildFlavor = "a52qxx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1639497600", securityPatch = "2021-12-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A32" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A325F", device = "a32", product = "a32xxx",
                hardware = "mt6853", board = "a32", bootloader = "A325FXXU2BUG1",
                fingerprint = "samsung/a32xxx/a32:11/RP1A.200720.012/A325FXXU2BUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR14A.R3.MP.V62.P4,MOLY.LR14A.R3.MP.V62.P4",
                incremental = "A325FXXU2BUG1", sdkInt = 30, release = "11",
                boardPlatform = "mt6853", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6853", zygote = "zygote64_32", vendorFingerprint = "samsung/a32xxx/a32:11/RP1A.200720.012/A325FXXU2BUG1:user/release-keys",
                display = "RP1A.200720.012.A325FXXU2BUG1", buildDescription = "a32xxx-user 11 RP1A.200720.012 A325FXXU2BUG1 release-keys", buildFlavor = "a32xxx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1625097600", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A12" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A125F", device = "a12", product = "a12xxx",
                hardware = "exynos850", board = "a12", bootloader = "A125FXXU4BUL1",
                fingerprint = "samsung/a12xxx/a12:11/RP1A.200720.012/A125FXXU4BUL1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "A125FXXU4BUL1", sdkInt = 30, release = "11",
                boardPlatform = "exynos850", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "S5E3830", zygote = "zygote64_32", vendorFingerprint = "samsung/a12xxx/a12:11/RP1A.200720.012/A125FXXU4BUL1:user/release-keys",
                display = "RP1A.200720.012.A125FXXU4BUL1", buildDescription = "a12xxx-user 11 RP1A.200720.012 A125FXXU4BUL1 release-keys", buildFlavor = "a12xxx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1639497600", securityPatch = "2021-12-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy A51" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-A515F", device = "a51", product = "a51xxx",
                hardware = "exynos9610", board = "a51", bootloader = "A515FXXU4CUG1",
                fingerprint = "samsung/a51xxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "A515FXXU4CUG1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9610", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "Exynos9611", zygote = "zygote64_32", vendorFingerprint = "samsung/a51xxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
                display = "RP1A.200720.012.A515FXXU4CUG1", buildDescription = "a51xxx-user 11 RP1A.200720.012 A515FXXU4CUG1 release-keys", buildFlavor = "a51xxx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1625097600", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy M12" to DeviceFingerprint(
                manufacturer = "samsung", brand = "samsung", model = "SM-M127F", device = "m12", product = "m12xid",
                hardware = "exynos850", board = "m12", bootloader = "M127FXXU3BUK1",
                fingerprint = "samsung/m12xid/m12:11/RP1A.200720.012/M127FXXU3BUK1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "M127FXXU3BUK1", sdkInt = 30, release = "11",
                boardPlatform = "exynos850", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "S5E3830", zygote = "zygote64_32", vendorFingerprint = "samsung/m12xid/m12:11/RP1A.200720.012/M127FXXU3BUK1:user/release-keys",
                display = "RP1A.200720.012.M127FXXU3BUK1", buildDescription = "m12xid-user 11 RP1A.200720.012 M127FXXU3BUK1 release-keys", buildFlavor = "m12xid-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1636934400", securityPatch = "2021-11-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "OnePlus Nord" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "OnePlus Nord", device = "avicii", product = "OnePlus Nord",
                hardware = "qcom", board = "avicii", bootloader = "avicii_11_A.11",
                fingerprint = "OnePlus/OnePlus Nord/avicii:11/RKQ1.201022.002/2107141742:user/release-keys",
                buildId = "RKQ1.201022.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.0.c1-00072-SM7250_GEN_PACK-1",
                incremental = "2107141742", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7250", zygote = "zygote64_32", vendorFingerprint = "OnePlus/OnePlus Nord/avicii:11/RKQ1.201022.002/2107141742:user/release-keys",
                display = "RKQ1.201022.002.2107141742", buildDescription = "OnePlus Nord-user 11 RKQ1.201022.002 2107141742 release-keys", buildFlavor = "OnePlus Nord-user", buildHost = "buildserver12", buildUser = "jenkins", buildDateUtc = "1626307200", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "OnePlus 8T" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "KB2005", device = "kebab", product = "OnePlus8T",
                hardware = "qcom", board = "kebab", bootloader = "kebab_21_A.14",
                fingerprint = "OnePlus/OnePlus8T/kebab:11/RKQ1.201022.002/2106051219:user/release-keys",
                buildId = "RKQ1.201022.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
                incremental = "2106051219", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM8250", zygote = "zygote64_32", vendorFingerprint = "OnePlus/OnePlus8T/kebab:11/RKQ1.201022.002/2106051219:user/release-keys",
                display = "RKQ1.201022.002.2106051219", buildDescription = "OnePlus8T-user 11 RKQ1.201022.002 2106051219 release-keys", buildFlavor = "OnePlus8T-user", buildHost = "buildserver12", buildUser = "jenkins", buildDateUtc = "1622937600", securityPatch = "2021-06-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "OnePlus Nord CE" to DeviceFingerprint(
                manufacturer = "OnePlus", brand = "OnePlus", model = "EB2101", device = "ebba", product = "OnePlus Nord CE 5G",
                hardware = "qcom", board = "ebba", bootloader = "ebba_11_A.06",
                fingerprint = "OnePlus/OnePlus Nord CE 5G/ebba:11/RKQ1.201217.002/2106171000:user/release-keys",
                buildId = "RKQ1.201217.002", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1",
                incremental = "2106171000", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7225", zygote = "zygote64_32", vendorFingerprint = "OnePlus/OnePlus Nord CE 5G/ebba:11/RKQ1.201217.002/2106171000:user/release-keys",
                display = "RKQ1.201217.002.2106171000", buildDescription = "OnePlus Nord CE 5G-user 11 RKQ1.201217.002 2106171000 release-keys", buildFlavor = "OnePlus Nord CE 5G-user", buildHost = "buildserver12", buildUser = "jenkins", buildDateUtc = "1624060800", securityPatch = "2021-06-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Moto G30" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g30", device = "caprip", product = "caprip",
                hardware = "qcom", board = "caprip", bootloader = "unknown",
                fingerprint = "motorola/caprip/caprip:11/RRHS31.Q3-47-32/a57fec:user/release-keys",
                buildId = "RRHS31.Q3-47-32", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "a57fec", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM6115", zygote = "zygote64_32", vendorFingerprint = "motorola/caprip/caprip:11/RRHS31.Q3-47-32/a57fec:user/release-keys",
                display = "RRHS31.Q3-47-32", buildDescription = "caprip-user 11 RRHS31.Q3-47-32 a57fec release-keys", buildFlavor = "caprip-user", buildHost = "buildbot-motoauto06.mcd.mot.com", buildUser = "hudsoncm", buildDateUtc = "1630022400", securityPatch = "2021-08-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Moto G Power 2021" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g power (2021)", device = "borneo", product = "borneo",
                hardware = "qcom", board = "borneo", bootloader = "unknown",
                fingerprint = "motorola/borneo/borneo:11/RRQ31.Q3-47-22/2b4fae:user/release-keys",
                buildId = "RRQ31.Q3-47-22", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "2b4fae", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM6115", zygote = "zygote64_32", vendorFingerprint = "motorola/borneo/borneo:11/RRQ31.Q3-47-22/2b4fae:user/release-keys",
                display = "RRQ31.Q3-47-22", buildDescription = "borneo-user 11 RRQ31.Q3-47-22 2b4fae release-keys", buildFlavor = "borneo-user", buildHost = "buildbot-motoauto06.mcd.mot.com", buildUser = "hudsoncm", buildDateUtc = "1619827200", securityPatch = "2021-05-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Moto G50" to DeviceFingerprint(
                manufacturer = "motorola", brand = "motorola", model = "moto g50", device = "ibis", product = "ibis",
                hardware = "qcom", board = "ibis", bootloader = "unknown",
                fingerprint = "motorola/ibis/ibis:11/RRQS31.Q3-47-18/a7d2a1:user/release-keys",
                buildId = "RRQS31.Q3-47-18", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "a7d2a1", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM4350", zygote = "zygote64_32", vendorFingerprint = "motorola/ibis/ibis:11/RRQS31.Q3-47-18/a7d2a1:user/release-keys",
                display = "RRQS31.Q3-47-18", buildDescription = "ibis-user 11 RRQS31.Q3-47-18 a7d2a1 release-keys", buildFlavor = "ibis-user", buildHost = "buildbot-motoauto06.mcd.mot.com", buildUser = "hudsoncm", buildDateUtc = "1627344000", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Nokia 5.4" to DeviceFingerprint(
                manufacturer = "Nokia", brand = "Nokia", model = "Nokia 5.4", device = "DPL-N05", product = "DPL_sprout",
                hardware = "qcom", board = "DPL-N05", bootloader = "unknown",
                fingerprint = "Nokia/DPL_sprout/DPL-N05:11/00WW_4_350/00WW350_360_131:user/release-keys",
                buildId = "00WW_4_350", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "00WW350_360_131", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM6115", zygote = "zygote64_32", vendorFingerprint = "Nokia/DPL_sprout/DPL-N05:11/00WW_4_350/00WW350_360_131:user/release-keys",
                display = "00WW_4_350", buildDescription = "DPL_sprout-user 11 00WW_4_350 00WW350_360_131 release-keys", buildFlavor = "DPL_sprout-user", buildHost = "android-build", buildUser = "android-build", buildDateUtc = "1627430400", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Nokia X10" to DeviceFingerprint(
                manufacturer = "Nokia", brand = "Nokia", model = "Nokia X10", device = "CAP-N05", product = "CAP_sprout",
                hardware = "qcom", board = "CAP-N05", bootloader = "unknown",
                fingerprint = "Nokia/CAP_sprout/CAP-N05:11/00WW_2_390/00WW390_390_141:user/release-keys",
                buildId = "00WW_2_390", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.8.1.c3-00026-SM6115_GEN_PACK-1",
                incremental = "00WW390_390_141", sdkInt = 30, release = "11",
                boardPlatform = "bengal", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM4350", zygote = "zygote64_32", vendorFingerprint = "Nokia/CAP_sprout/CAP-N05:11/00WW_2_390/00WW390_390_141:user/release-keys",
                display = "00WW_2_390", buildDescription = "CAP_sprout-user 11 00WW_2_390 00WW390_390_141 release-keys", buildFlavor = "CAP_sprout-user", buildHost = "android-build", buildUser = "android-build", buildDateUtc = "1633046400", securityPatch = "2021-10-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 4a" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a", device = "sunfish", product = "sunfish",
                hardware = "qcom", board = "sunfish", bootloader = "s5-0.5-9637172",
                fingerprint = "google/sunfish/sunfish:11/RQ3A.210705.001/7380771:user/release-keys",
                buildId = "RQ3A.210705.001", tags = "release-keys", type = "user",
                radioVersion = "g7150-00023-210610-B-7310372",
                incremental = "7380771", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7150", zygote = "zygote64_32", vendorFingerprint = "google/sunfish_vend/sunfish:11/RQ3A.210705.001/7380771:vendor/release-keys",
                display = "RQ3A.210705.001", buildDescription = "sunfish-user 11 RQ3A.210705.001 7380771 release-keys", buildFlavor = "sunfish-user", buildHost = "abfarm-release-rbe-64.hot.corp.google.com", buildUser = "android-build", buildDateUtc = "1625616000", securityPatch = "2021-07-05", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 5" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 5", device = "redfin", product = "redfin",
                hardware = "qcom", board = "redfin", bootloader = "r8-0.3-7219051",
                fingerprint = "google/redfin/redfin:11/RQ3A.210805.001.A1/7512229:user/release-keys",
                buildId = "RQ3A.210805.001.A1", tags = "release-keys", type = "user",
                radioVersion = "g725-00164-210812-B-7522969",
                incremental = "7512229", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7250", zygote = "zygote64_32", vendorFingerprint = "google/redfin_vend/redfin:11/RQ3A.210805.001.A1/7512229:vendor/release-keys",
                display = "RQ3A.210805.001.A1", buildDescription = "redfin-user 11 RQ3A.210805.001.A1 7512229 release-keys", buildFlavor = "redfin-user", buildHost = "abfarm-release-rbe-64.hot.corp.google.com", buildUser = "android-build", buildDateUtc = "1628100000", securityPatch = "2021-08-05", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Google Pixel 4a 5G" to DeviceFingerprint(
                manufacturer = "Google", brand = "google", model = "Pixel 4a (5G)", device = "bramble", product = "bramble",
                hardware = "qcom", board = "bramble", bootloader = "b2-0.3-7214727",
                fingerprint = "google/bramble/bramble:11/RQ3A.210705.001/7380771:user/release-keys",
                buildId = "RQ3A.210705.001", tags = "release-keys", type = "user",
                radioVersion = "g7250-00195-210614-B-7352378",
                incremental = "7380771", sdkInt = 30, release = "11",
                boardPlatform = "lito", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7250", zygote = "zygote64_32", vendorFingerprint = "google/bramble_vend/bramble:11/RQ3A.210705.001/7380771:vendor/release-keys",
                display = "RQ3A.210705.001", buildDescription = "bramble-user 11 RQ3A.210705.001 7380771 release-keys", buildFlavor = "bramble-user", buildHost = "abfarm-release-rbe-64.hot.corp.google.com", buildUser = "android-build", buildDateUtc = "1625616000", securityPatch = "2021-07-05", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy S20+" to DeviceFingerprint(
                manufacturer = "Samsung", brand = "samsung", model = "SM-G985F", device = "y2s", product = "y2sxx",
                hardware = "exynos990", board = "universal990", bootloader = "G985FXXSGHWA3",
                fingerprint = "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "G985FXXSGHWA3", sdkInt = 30, release = "11",
                boardPlatform = "exynos990", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "Exynos990", zygote = "zygote64_32", vendorFingerprint = "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys",
                display = "RP1A.200720.012.G985FXXSGHWA3", buildDescription = "y2sxx-user 11 RP1A.200720.012 G985FXXSGHWA3 release-keys", buildFlavor = "y2sxx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1639497600", securityPatch = "2022-01-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Samsung Galaxy S10e" to DeviceFingerprint(
                manufacturer = "Samsung", brand = "samsung", model = "SM-G970F", device = "beyond0", product = "beyond0ltexx",
                hardware = "exynos9820", board = "universal9820", bootloader = "G970FXXSGHWC1",
                fingerprint = "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "",
                incremental = "G970FXXSGHWC1", sdkInt = 30, release = "11",
                boardPlatform = "exynos9820", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "Exynos9820", zygote = "zygote64_32", vendorFingerprint = "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys",
                display = "RP1A.200720.012.G970FXXSGHWC1", buildDescription = "beyond0ltexx-user 11 RP1A.200720.012 G970FXXSGHWC1 release-keys", buildFlavor = "beyond0ltexx-user", buildHost = "21R3NF12", buildUser = "dpi", buildDateUtc = "1646236800", securityPatch = "2022-03-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Vivo Y53s" to DeviceFingerprint(
                manufacturer = "vivo", brand = "vivo", model = "V2058", device = "V2058", product = "V2058",
                hardware = "mt6769", board = "V2058", bootloader = "unknown",
                fingerprint = "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR12A.R3.MP.V98",
                incremental = "compiler08272021", sdkInt = 30, release = "11",
                boardPlatform = "mt6769", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6769", zygote = "zygote64_32", vendorFingerprint = "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys",
                display = "RP1A.200720.011", buildDescription = "V2058-user 11 RP1A.200720.011 compiler08272021 release-keys", buildFlavor = "V2058-user", buildHost = "compiler", buildUser = "build", buildDateUtc = "1630022400", securityPatch = "2021-08-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Realme 7 5G" to DeviceFingerprint(
                manufacturer = "realme", brand = "realme", model = "RMX2111", device = "RMX2111", product = "RMX2111",
                hardware = "mt6853", board = "RMX2111", bootloader = "unknown",
                fingerprint = "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MOLY.LR14A.R3.MP.V62",
                incremental = "1626245367375", sdkInt = 30, release = "11",
                boardPlatform = "mt6853", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "MT6853", zygote = "zygote64_32", vendorFingerprint = "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys",
                display = "RP1A.200720.011", buildDescription = "RMX2111-user 11 RP1A.200720.011 1626245367375 release-keys", buildFlavor = "RMX2111-user", buildHost = "ubuntu-123", buildUser = "jenkins", buildDateUtc = "1626245367", securityPatch = "2021-07-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Oppo Reno5" to DeviceFingerprint(
                manufacturer = "OPPO", brand = "OPPO", model = "CPH2159", device = "OP4E75L1", product = "CPH2159",
                hardware = "qcom", board = "OP4E75L1", bootloader = "unknown",
                fingerprint = "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys",
                buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.0.c4-00165",
                incremental = "1622635284", sdkInt = 30, release = "11",
                boardPlatform = "atoll", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM7225", zygote = "zygote64_32", vendorFingerprint = "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys",
                display = "RP1A.200720.011", buildDescription = "CPH2159-user 11 RP1A.200720.011 1622635284 release-keys", buildFlavor = "CPH2159-user", buildHost = "ubuntu", buildUser = "jenkins", buildDateUtc = "1622635284", securityPatch = "2021-06-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Vivo X60" to DeviceFingerprint(
                manufacturer = "vivo", brand = "vivo", model = "V2046", device = "V2046", product = "V2046",
                hardware = "qcom", board = "V2046", bootloader = "unknown",
                fingerprint = "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys",
                buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
                radioVersion = "MPSS.HI.2.0.c4-00165",
                incremental = "compiler05211516", sdkInt = 30, release = "11",
                boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM8250", zygote = "zygote64_32", vendorFingerprint = "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys",
                display = "RP1A.200720.012", buildDescription = "V2046-user 11 RP1A.200720.012 compiler05211516 release-keys", buildFlavor = "V2046-user", buildHost = "compiler", buildUser = "build", buildDateUtc = "1621584000", securityPatch = "2021-05-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Huawei P40" to DeviceFingerprint(
                manufacturer = "HUAWEI", brand = "HUAWEI", model = "ANA-NX9", device = "HWANA", product = "ANA-NX9",
                hardware = "kirin990", board = "ANA", bootloader = "unknown",
                fingerprint = "HUAWEI/ANA-NX9/HWANA:10/HUAWEIANA-NX9/10.1.0.131C432:user/release-keys",
                buildId = "HUAWEIANA-NX9", tags = "release-keys", type = "user",
                radioVersion = "21C20B369S000C000",
                incremental = "10.1.0.131C432", sdkInt = 29, release = "10",
                boardPlatform = "kirin990", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "HiSilicon Kirin 990", zygote = "zygote64_32", vendorFingerprint = "HUAWEI/ANA-NX9/HWANA:10/HUAWEIANA-NX9/10.1.0.131C432:user/release-keys",
                display = "ANA-NX9 10.1.0.131(C432E8R4P1)", buildDescription = "ANA-NX9-user 10 HUAWEIANA-NX9 10.1.0.131C432 release-keys", buildFlavor = "ANA-NX9-user", buildHost = "hur-sz-12", buildUser = "jenkins", buildDateUtc = "1590480000", securityPatch = "2020-05-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Honor 30" to DeviceFingerprint(
                manufacturer = "HONOR", brand = "HONOR", model = "BMH-AN10", device = "BMH", product = "BMH-AN10",
                hardware = "kirin985", board = "BMH", bootloader = "unknown",
                fingerprint = "HONOR/BMH-AN10/BMH:10/HUAWEIBMH-AN10/3.1.1.162C00:user/release-keys",
                buildId = "HUAWEIBMH-AN10", tags = "release-keys", type = "user",
                radioVersion = "21C20B369S000C000",
                incremental = "3.1.1.162C00", sdkInt = 29, release = "10",
                boardPlatform = "kirin985", eglDriver = "mali", openGlEs = "196610", hardwareChipname = "HiSilicon Kirin 985", zygote = "zygote64_32", vendorFingerprint = "HONOR/BMH-AN10/BMH:10/HUAWEIBMH-AN10/3.1.1.162C00:user/release-keys",
                display = "BMH-AN10 3.1.1.162(C00E160R7P2)", buildDescription = "BMH-AN10-user 10 HUAWEIBMH-AN10 3.1.1.162C00 release-keys", buildFlavor = "BMH-AN10-user", buildHost = "hur-sz-12", buildUser = "jenkins", buildDateUtc = "1598480000", securityPatch = "2020-08-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
            ),
            "Asus Zenfone 8" to DeviceFingerprint(
                manufacturer = "asus", brand = "asus", model = "ASUS_I006D", device = "ASUS_I006D", product = "WW_I006D",
                hardware = "qcom", board = "sake", bootloader = "unknown",
                fingerprint = "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                buildId = "RKQ1.201112.002", tags = "release-keys", type = "user",
                radioVersion = "M3.13.24.51-Sake_0000100",
                incremental = "30.11.51.115", sdkInt = 30, release = "11",
                boardPlatform = "lahaina", eglDriver = "adreno", openGlEs = "196610", hardwareChipname = "SM8350", zygote = "zygote64_32", vendorFingerprint = "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                display = "RKQ1.201112.002.30.11.51.115", buildDescription = "WW_I006D-user 11 RKQ1.201112.002 30.11.51.115 release-keys", buildFlavor = "WW_I006D-user", buildHost = "android-build", buildUser = "jenkins", buildDateUtc = "1629859200", securityPatch = "2021-08-01", buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
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
        val boardPlatform: String,
        val eglDriver: String,
        val openGlEs: String,
        val hardwareChipname: String,
        val zygote: String,
        val vendorFingerprint: String,
        val display: String,
        val buildDescription: String,
        val buildFlavor: String,
        val buildHost: String,
        val buildUser: String,
        val buildDateUtc: String,
        val securityPatch: String,
        val buildVersionCodename: String,
        val buildVersionPreviewSdk: String
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

            fun getEncryptedString(key: String, def: String): String {
                val raw = prefs?.getString(key, null)
                return if (raw != null && raw.startsWith("ENC:")) {
                    decrypt(raw.substring(4)) ?: def
                } else {
                    raw ?: def
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

        val carrier = US_CARRIERS.random()

        val mccMnc = getString("mcc_mnc", carrier.mccMnc)
        if (cachedImsi == null) cachedImsi = mccMnc + (1..10).map { (0..9).random() }.joinToString("")

        if (cachedIccid == null) cachedIccid = generateValidIccid(mccMnc)

        val simCountry = "us"
        if (cachedPhoneNumber == null) cachedPhoneNumber = generatePhoneNumber(mccMnc)
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        val cleanName = profileName.replace(" - Android 11", "").trim()
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
                        "ro.build.version.release" -> fingerprint.release
                        "ro.build.version.sdk" -> fingerprint.sdkInt.toString()

                        "ro.debuggable"             -> "0"
                        "ro.secure"                 -> "1"
                        "ro.build.display.id"       -> fingerprint.display
                        "ro.build.description"      -> fingerprint.buildDescription
                        "ro.build.characteristics"  -> "default"
                        "ro.build.flavor"           -> fingerprint.buildFlavor
                        "ro.vendor.build.fingerprint" -> fingerprint.vendorFingerprint

                        // Hardware specific properties
                        "ro.board.platform"         -> fingerprint.boardPlatform
                        "ro.hardware.egl"           -> fingerprint.eglDriver
                        "ro.opengles.version"       -> fingerprint.openGlEs
                        "ro.hardware.chipname"      -> fingerprint.hardwareChipname
                        "ro.zygote"                 -> fingerprint.zygote

                        // Build info
                        "ro.build.host"             -> fingerprint.buildHost
                        "ro.build.user"             -> fingerprint.buildUser
                        "ro.build.date.utc"         -> fingerprint.buildDateUtc
                        "ro.build.version.security_patch" -> fingerprint.securityPatch
                        "ro.build.version.codename" -> fingerprint.buildVersionCodename
                        "ro.build.version.preview_sdk" -> fingerprint.buildVersionPreviewSdk

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
            mockLocationEnabled = prefs?.getBoolean("mock_location_enabled", false) ?: false
            val latStr = getString("mock_latitude", "0.0")
            val lonStr = getString("mock_longitude", "0.0")
            val altStr = getString("mock_altitude", "0.0")
            val accStr = getString("mock_accuracy", "10.0")

            mockLatitude = latStr.toDoubleOrNull() ?: 0.0
            mockLongitude = lonStr.toDoubleOrNull() ?: 0.0
            mockAltitude = altStr.toDoubleOrNull() ?: 0.0
            mockAccuracy = accStr.toFloatOrNull() ?: 10.0f

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
