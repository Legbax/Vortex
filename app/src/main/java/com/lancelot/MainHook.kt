package com.lancelot

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.net.NetworkInterface
import java.util.*

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "spoof_prefs"

        // Cache para valores consistentes por sesión
        private var cachedImei: String? = null
        private var cachedImei2: String? = null
        private var cachedImsi: String? = null
        private var cachedIccid: String? = null
        private var cachedPhoneNumber: String? = null
        private var cachedAndroidId: String? = null
        private var cachedGsfId: String? = null
        private var cachedGaid: String? = null
        private var cachedSsaid: String? = null
        private var cachedWifiMac: String? = null
        private var cachedBtMac: String? = null

        // Fingerprints REALES - Android 11 (API 30)
        private val DEVICE_FINGERPRINTS = mapOf(
            "Redmi 9 (Original)" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi 9",
                device = "lancelot",
                product = "lancelot_global",
                hardware = "mt6768",
                board = "lancelot",
                bootloader = "unknown",
                fingerprint = "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.3.0.RJCMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 9" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 9",
                device = "merlin",
                product = "merlin_global",
                hardware = "mt6768",
                board = "merlin",
                bootloader = "unknown",
                fingerprint = "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.2.0.RJOMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi 9A" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi 9A",
                device = "dandelion",
                product = "dandelion_global",
                hardware = "mt6762",
                board = "dandelion",
                bootloader = "unknown",
                fingerprint = "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.4.0.RCDMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi 9C" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi 9C",
                device = "angelica",
                product = "angelica_global",
                hardware = "mt6762",
                board = "angelica",
                bootloader = "unknown",
                fingerprint = "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.1.0.RCRMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 9S" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 9S",
                device = "curtana",
                product = "curtana_global",
                hardware = "qcom",
                board = "curtana",
                bootloader = "unknown",
                fingerprint = "Redmi/curtana_global/curtana:11/RKQ1.200826.002/V12.5.1.0.RJWMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.1.0.RJWMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 9 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 9 Pro",
                device = "joyeuse",
                product = "joyeuse_global",
                hardware = "qcom",
                board = "joyeuse",
                bootloader = "unknown",
                fingerprint = "Redmi/joyeuse_global/joyeuse:11/RKQ1.200826.002/V12.5.3.0.RJZMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.3.0.RJZMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 8" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 8",
                device = "ginkgo",
                product = "ginkgo_global",
                hardware = "qcom",
                board = "ginkgo",
                bootloader = "unknown",
                fingerprint = "Redmi/ginkgo_global/ginkgo:11/RKQ1.201004.002/V12.5.2.0.RCOMIXM:user/release-keys",
                buildId = "RKQ1.201004.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.2.0.RCOMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 8 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 8 Pro",
                device = "begonia",
                product = "begonia_global",
                hardware = "mt6785",
                board = "begonia",
                bootloader = "unknown",
                fingerprint = "Redmi/begonia_global/begonia:11/RP1A.200720.011/V12.5.6.0.RGGMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.6.0.RGGMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 10" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 10",
                device = "mojito",
                product = "mojito_global",
                hardware = "qcom",
                board = "mojito",
                bootloader = "unknown",
                fingerprint = "Redmi/mojito_global/mojito:11/RKQ1.201022.002/V12.5.2.0.RKGMIXM:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.2.0.RKGMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi Note 10S" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi Note 10S",
                device = "rosemary",
                product = "rosemary_global",
                hardware = "mt6785",
                board = "rosemary",
                bootloader = "unknown",
                fingerprint = "Redmi/rosemary_global/rosemary:11/RP1A.200720.011/V12.5.11.0.RKLMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.11.0.RKLMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Redmi 10" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Redmi",
                model = "Redmi 10",
                device = "selene",
                product = "selene_global",
                hardware = "mt6769",
                board = "selene",
                bootloader = "unknown",
                fingerprint = "Redmi/selene_global/selene:11/RP1A.200720.011/V12.5.12.0.RKUMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.12.0.RKUMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "POCO X3 NFC" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "POCO",
                model = "POCO X3 NFC",
                device = "surya",
                product = "surya_global",
                hardware = "qcom",
                board = "surya",
                bootloader = "unknown",
                fingerprint = "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.7.0.RJGMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "POCO X3 Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "POCO",
                model = "POCO X3 Pro",
                device = "vayu",
                product = "vayu_global",
                hardware = "qcom",
                board = "vayu",
                bootloader = "unknown",
                fingerprint = "POCO/vayu_global/vayu:11/RKQ1.200826.002/V12.5.5.0.RJUMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.5.0.RJUMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "POCO M3" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "POCO",
                model = "POCO M3",
                device = "citrus",
                product = "citrus_global",
                hardware = "qcom",
                board = "citrus",
                bootloader = "unknown",
                fingerprint = "POCO/citrus_global/citrus:11/RKQ1.200826.002/V12.5.8.0.RJFMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.8.0.RJFMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "POCO M3 Pro 5G" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "POCO",
                model = "POCO M3 Pro 5G",
                device = "camellia",
                product = "camellia_global",
                hardware = "mt6833",
                board = "camellia",
                bootloader = "unknown",
                fingerprint = "POCO/camellia_global/camellia:11/RP1A.200720.011/V12.5.3.0.RKSMIXM:user/release-keys",
                buildId = "RP1A.200720.011",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.3.0.RKSMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Mi 10T" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Xiaomi",
                model = "Mi 10T",
                device = "apollo",
                product = "apollo_global",
                hardware = "qcom",
                board = "apollo",
                bootloader = "unknown",
                fingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.11.0.RJDMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.11.0.RJDMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Mi 10T Pro" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Xiaomi",
                model = "Mi 10T Pro",
                device = "apollo",
                product = "apollo_global",
                hardware = "qcom",
                board = "apollo",
                bootloader = "unknown",
                fingerprint = "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.9.0.RJDMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.9.0.RJDMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Mi 11 Lite" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Xiaomi",
                model = "Mi 11 Lite",
                device = "courbet",
                product = "courbet_global",
                hardware = "qcom",
                board = "courbet",
                bootloader = "unknown",
                fingerprint = "Xiaomi/courbet_global/courbet:11/RKQ1.200826.002/V12.5.5.0.RKQMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.5.0.RKQMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Mi 11i" to DeviceFingerprint(
                manufacturer = "Xiaomi",
                brand = "Xiaomi",
                model = "Mi 11i",
                device = "haydn",
                product = "haydn_global",
                hardware = "qcom",
                board = "haydn",
                bootloader = "unknown",
                fingerprint = "Xiaomi/haydn_global/haydn:11/RKQ1.200826.002/V12.5.7.0.RKKMIXM:user/release-keys",
                buildId = "RKQ1.200826.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "V12.5.7.0.RKKMIXM",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S10" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G973F",
                device = "beyond1",
                product = "beyond1ltexx",
                hardware = "exynos9820",
                board = "universal9820",
                bootloader = "G973FXXUHFVG4",
                fingerprint = "samsung/beyond1ltexx/beyond1:11/RP1A.200720.012/G973FXXUHFVG4:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G973FXXUHFVG4",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S10+" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G975F",
                device = "beyond2",
                product = "beyond2ltexx",
                hardware = "exynos9820",
                board = "universal9820",
                bootloader = "G975FXXSGHWC1",
                fingerprint = "samsung/beyond2ltexx/beyond2:11/RP1A.200720.012/G975FXXSGHWC1:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G975FXXSGHWC1",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S10e" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G970F",
                device = "beyond0",
                product = "beyond0ltexx",
                hardware = "exynos9820",
                board = "universal9820",
                bootloader = "G970FXXSGHWC1",
                fingerprint = "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G970FXXSGHWC1",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S20" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G980F",
                device = "x1s",
                product = "x1sxx",
                hardware = "exynos990",
                board = "universal990",
                bootloader = "G980FXXSGHWA3",
                fingerprint = "samsung/x1sxx/x1s:11/RP1A.200720.012/G980FXXSGHWA3:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G980FXXSGHWA3",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S20+" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G985F",
                device = "y2s",
                product = "y2sxx",
                hardware = "exynos990",
                board = "universal990",
                bootloader = "G985FXXSGHWA3",
                fingerprint = "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G985FXXSGHWA3",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy S20 FE" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-G780G",
                device = "r8s",
                product = "r8sxx",
                hardware = "qcom",
                board = "kona",
                bootloader = "G780GXXU3CVD2",
                fingerprint = "samsung/r8sxx/r8s:11/RP1A.200720.012/G780GXXU3CVD2:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "G780GXXU3CVD2",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy Note 10" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-N970F",
                device = "d1",
                product = "d1xxx",
                hardware = "exynos9825",
                board = "universal9825",
                bootloader = "N970FXXU8HVG3",
                fingerprint = "samsung/d1xxx/d1:11/RP1A.200720.012/N970FXXU8HVG3:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "N970FXXU8HVG3",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy Note 10+" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-N975F",
                device = "d2s",
                product = "d2sxxx",
                hardware = "exynos9825",
                board = "universal9825",
                bootloader = "N975FXXS8HVG1",
                fingerprint = "samsung/d2sxxx/d2s:11/RP1A.200720.012/N975FXXS8HVG1:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "N975FXXS8HVG1",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy A51" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-A515F",
                device = "a51",
                product = "a51xx",
                hardware = "exynos9611",
                board = "universal9611",
                bootloader = "A515FXXU5FVG4",
                fingerprint = "samsung/a51xx/a51:11/RP1A.200720.012/A515FXXU5FVG4:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "A515FXXU5FVG4",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy A71" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-A715F",
                device = "a71",
                product = "a71xx",
                hardware = "qcom",
                board = "kona",
                bootloader = "A715FXXU8DVG1",
                fingerprint = "samsung/a71xx/a71:11/RP1A.200720.012/A715FXXU8DVG1:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "A715FXXU8DVG1",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy A52" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-A525F",
                device = "a52q",
                product = "a52qxx",
                hardware = "qcom",
                board = "lito",
                bootloader = "A525FXXU4CVJB",
                fingerprint = "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "A525FXXU4CVJB",
                sdkInt = 30,
                release = "11"
            ),
            "Samsung Galaxy A72" to DeviceFingerprint(
                manufacturer = "Samsung",
                brand = "samsung",
                model = "SM-A725F",
                device = "a72q",
                product = "a72qxx",
                hardware = "qcom",
                board = "lito",
                bootloader = "A725FXXS4CVG1",
                fingerprint = "samsung/a72qxx/a72q:11/RP1A.200720.012/A725FXXS4CVG1:user/release-keys",
                buildId = "RP1A.200720.012",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "A725FXXS4CVG1",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 3" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 3",
                device = "blueline",
                product = "blueline",
                hardware = "qcom",
                board = "blueline",
                bootloader = "b1c1-0.1-6788728",
                fingerprint = "google/blueline/blueline:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 3 XL" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 3 XL",
                device = "crosshatch",
                product = "crosshatch",
                hardware = "qcom",
                board = "crosshatch",
                bootloader = "b1c1-0.1-6788728",
                fingerprint = "google/crosshatch/crosshatch:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 4" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 4",
                device = "flame",
                product = "flame",
                hardware = "qcom",
                board = "flame",
                bootloader = "c2f2-0.2-7349499",
                fingerprint = "google/flame/flame:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 4 XL" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 4 XL",
                device = "coral",
                product = "coral",
                hardware = "qcom",
                board = "coral",
                bootloader = "c2f2-0.2-7349499",
                fingerprint = "google/coral/coral:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 4a" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 4a",
                device = "sunfish",
                product = "sunfish",
                hardware = "qcom",
                board = "sunfish",
                bootloader = "s1f4-0.3-7349499",
                fingerprint = "google/sunfish/sunfish:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 4a 5G" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 4a (5G)",
                device = "bramble",
                product = "bramble",
                hardware = "qcom",
                board = "bramble",
                bootloader = "b1f6-0.3-7349499",
                fingerprint = "google/bramble/bramble:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "Google Pixel 5" to DeviceFingerprint(
                manufacturer = "Google",
                brand = "google",
                model = "Pixel 5",
                device = "redfin",
                product = "redfin",
                hardware = "qcom",
                board = "redfin",
                bootloader = "r1f7-0.2-7349499",
                fingerprint = "google/redfin/redfin:11/RQ3A.211001.001/7641976:user/release-keys",
                buildId = "RQ3A.211001.001",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "7641976",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus 7" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "GM1903",
                device = "OnePlus7",
                product = "OnePlus7_EEA",
                hardware = "qcom",
                board = "msmnile",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlus7_EEA/OnePlus7:11/RKQ1.201022.002/2201102330:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201102330",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus 7 Pro" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "GM1913",
                device = "OnePlus7Pro",
                product = "OnePlus7Pro_EEA",
                hardware = "qcom",
                board = "msmnile",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlus7Pro_EEA/OnePlus7Pro:11/RKQ1.201022.002/2201140132:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201140132",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus 7T" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "HD1903",
                device = "OnePlus7T",
                product = "OnePlus7T_EEA",
                hardware = "qcom",
                board = "msmnile",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlus7T_EEA/OnePlus7T:11/RKQ1.201022.002/2201132109:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201132109",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus 8" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "IN2013",
                device = "OnePlus8",
                product = "OnePlus8_EEA",
                hardware = "qcom",
                board = "kona",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlus8_EEA/OnePlus8:11/RKQ1.201022.002/2201131524:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201131524",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus 8 Pro" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "IN2023",
                device = "OnePlus8Pro",
                product = "OnePlus8Pro_EEA",
                hardware = "qcom",
                board = "kona",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlus8Pro_EEA/OnePlus8Pro:11/RKQ1.201022.002/2201132019:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201132019",
                sdkInt = 30,
                release = "11"
            ),
            "OnePlus Nord" to DeviceFingerprint(
                manufacturer = "OnePlus",
                brand = "OnePlus",
                model = "AC2003",
                device = "OnePlusNord",
                product = "OnePlusNord_EEA",
                hardware = "qcom",
                board = "avicii",
                bootloader = "unknown",
                fingerprint = "OnePlus/OnePlusNord_EEA/OnePlusNord:11/RKQ1.201022.002/2201131914:user/release-keys",
                buildId = "RKQ1.201022.002",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "2201131914",
                sdkInt = 30,
                release = "11"
            ),
            "Motorola Moto G9 Plus" to DeviceFingerprint(
                manufacturer = "motorola",
                brand = "motorola",
                model = "moto g(9) plus",
                device = "odessa",
                product = "odessa_retail",
                hardware = "qcom",
                board = "odessa",
                bootloader = "MBM-3.0-odessa_retail",
                fingerprint = "motorola/odessa_retail/odessa:11/RPAS31.Q4U-39-26-10/5f8b6:user/release-keys",
                buildId = "RPAS31.Q4U-39-26-10",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "5f8b6",
                sdkInt = 30,
                release = "11"
            ),
            "Motorola Moto G Power 2021" to DeviceFingerprint(
                manufacturer = "motorola",
                brand = "motorola",
                model = "moto g power (2021)",
                device = "borneo",
                product = "borneo_retail",
                hardware = "qcom",
                board = "borneo",
                bootloader = "MBM-2.0-borneo_retail",
                fingerprint = "motorola/borneo_retail/borneo:11/RZB31.Q2-143-27/1e12a:user/release-keys",
                buildId = "RZB31.Q2-143-27",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "1e12a",
                sdkInt = 30,
                release = "11"
            ),
            "Motorola Edge 20" to DeviceFingerprint(
                manufacturer = "motorola",
                brand = "motorola",
                model = "motorola edge 20",
                device = "berlin",
                product = "berlin_retail",
                hardware = "qcom",
                board = "berlin",
                bootloader = "MBM-3.0-berlin_retail",
                fingerprint = "motorola/berlin_retail/berlin:11/R1RG31.Q1-56-9-14/9f32b:user/release-keys",
                buildId = "R1RG31.Q1-56-9-14",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "9f32b",
                sdkInt = 30,
                release = "11"
            ),
            "Nokia 5.4" to DeviceFingerprint(
                manufacturer = "HMD Global",
                brand = "Nokia",
                model = "Nokia 5.4",
                device = "captainamerica",
                product = "CaptainAmerica_00WW",
                hardware = "qcom",
                board = "captainamerica",
                bootloader = "unknown",
                fingerprint = "Nokia/CaptainAmerica_00WW/CaptainAmerica:11/RRFS31.Q1-59-40-9-01:user/release-keys",
                buildId = "RRFS31.Q1-59-40-9",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "01",
                sdkInt = 30,
                release = "11"
            ),
            "Nokia 8.3 5G" to DeviceFingerprint(
                manufacturer = "HMD Global",
                brand = "Nokia",
                model = "Nokia 8.3 5G",
                device = "daredevil",
                product = "Daredevil_00WW",
                hardware = "qcom",
                board = "daredevil",
                bootloader = "unknown",
                fingerprint = "Nokia/Daredevil_00WW/Daredevil:11/RRFS31.Q1-59-40-9-01:user/release-keys",
                buildId = "RRFS31.Q1-59-40-9",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "01",
                sdkInt = 30,
                release = "11"
            ),
            "Sony Xperia 1 II" to DeviceFingerprint(
                manufacturer = "Sony",
                brand = "Sony",
                model = "XQ-AT51",
                device = "pdx203",
                product = "pdx203",
                hardware = "qcom",
                board = "kona",
                bootloader = "unknown",
                fingerprint = "Sony/pdx203/pdx203:11/58.1.A.5.530/058001A0050530:user/release-keys",
                buildId = "58.1.A.5.530",
                tags = "release-keys",
                type = "user",
                radioVersion = "",
                incremental = "058001A0050530",
                sdkInt = 30,
                release = "11"
            )
        )

        // Mock Location settings
        private var mockLatitude: Double = 0.0
        private var mockLongitude: Double = 0.0
        private var mockAltitude: Double = 0.0
        private var mockLocationEnabled: Boolean = false
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
        val release: String
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Solo hookear apps que no sean del sistema
        if (lpparam.packageName == "android" ||
            lpparam.packageName.startsWith("com.android.") ||
            lpparam.packageName == "com.lancelot") {
            return
        }

        try {
            // FIX #2: Leer preferencias desde archivo world-readable
            val prefsFile = File("/data/data/com.lancelot/shared_prefs/$PREFS_NAME.xml")
            val prefs = if (prefsFile.exists() && prefsFile.canRead()) {
                XSharedPreferences("com.lancelot", PREFS_NAME).apply {
                    reload()
                }
            } else {
                // Fallback a valores por defecto
                null
            }

            // Cargar configuración con defaults seguros
            val profileName = prefs?.getString("profile", "Redmi 9 (Original)") ?: "Redmi 9 (Original)"
            val fingerprint = getDeviceFingerprint(profileName)

            // Inicializar cache de valores consistentes
            initializeCache(prefs)

            // Hook Build fields
            hookBuildFields(lpparam, fingerprint, prefs)

            // Hook System Properties (Addresses Problem 1 & mitigates Problem 2 for Java readers)
            hookSystemProperties(lpparam, fingerprint, prefs)

            // Hook TelephonyManager
            hookTelephonyManager(lpparam, prefs)

            // Hook Settings.Secure (FIX #4: un solo hook)
            hookSettingsSecure(lpparam, prefs)

            // Hook WiFi/Bluetooth MAC
            hookNetworkInterfaces(lpparam, prefs)

            // Hook Mock Location
            hookLocation(lpparam, prefs)

            // Hook WebView User-Agent (FIX #4)
            hookWebView(lpparam, fingerprint)

            // Anti-detección adicional
            hookXposedDetection(lpparam)

        } catch (e: Throwable) {
            XposedBridge.log("Lancelot Error: ${e.message}")
        }
    }

    private fun initializeCache(prefs: XSharedPreferences?) {
        // FIX #3: Inicializar valores una sola vez por sesión
        if (cachedImei == null) cachedImei = prefs?.getString("imei", null) ?: generateValidImei()
        if (cachedImei2 == null) cachedImei2 = prefs?.getString("imei2", null) ?: generateValidImei()
        if (cachedAndroidId == null) cachedAndroidId = prefs?.getString("android_id", null) ?: generateRandomId(16)
        if (cachedGsfId == null) cachedGsfId = prefs?.getString("gsf_id", null) ?: generateRandomId(16)
        if (cachedGaid == null) cachedGaid = prefs?.getString("gaid", null) ?: generateRandomGaid()
        if (cachedSsaid == null) cachedSsaid = prefs?.getString("ssaid", null) ?: generateRandomId(16)
        if (cachedWifiMac == null) cachedWifiMac = prefs?.getString("wifi_mac", null) ?: generateRandomMac()
        if (cachedBtMac == null) cachedBtMac = prefs?.getString("bluetooth_mac", null) ?: generateRandomMac()

        // FIX #3: Generar una vez valores de telefonía
        val mccMnc = prefs?.getString("mcc_mnc", "310260") ?: "310260"
        if (cachedImsi == null) cachedImsi = mccMnc + (1..10).map { (0..9).random() }.joinToString("")
        if (cachedIccid == null) cachedIccid = generateValidIccid()

        val simCountry = prefs?.getString("sim_country", "us") ?: "us"
        if (cachedPhoneNumber == null) cachedPhoneNumber = generatePhoneNumber(simCountry)
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        // FIX #6: Buscar fingerprint, fallback seguro
        val cleanProfileName = profileName.replace(Regex("\\s*\\(.*\\)\\s*"), "").trim()

        DEVICE_FINGERPRINTS[profileName]?.let { return it }
        DEVICE_FINGERPRINTS.entries.find {
            it.key.replace(Regex("\\s*\\(.*\\)\\s*"), "").trim() == cleanProfileName
        }?.value?.let { return it }

        // Fallback seguro
        return DEVICE_FINGERPRINTS["Redmi 9 (Original)"]!!
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam,
                                 fingerprint: DeviceFingerprint,
                                 prefs: XSharedPreferences?) {
        try {
            val buildClass = Build::class.java

            // FIX #5: SOLO usar Android 11 (API 30)
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

            val customSerial = prefs?.getString("serial", null) ?: generateRandomSerial()
            XposedHelpers.setStaticObjectField(buildClass, "SERIAL", customSerial)

            // Hook Build.VERSION - SIEMPRE API 30
            val versionClass = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(versionClass, "SDK_INT", 30)
            XposedHelpers.setStaticObjectField(versionClass, "RELEASE", "11")
            XposedHelpers.setStaticObjectField(versionClass, "INCREMENTAL", fingerprint.incremental)

            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = customSerial
                        }
                    }
                )
            } catch (e: NoSuchMethodError) {
                // API < 26
            }

        } catch (e: Throwable) {
            XposedBridge.log("Build hook error: ${e.message}")
        }
    }

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam,
                                      fingerprint: DeviceFingerprint,
                                      prefs: XSharedPreferences?) {
        try {
            val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        when (key) {
                            "ro.product.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.brand" -> param.result = fingerprint.brand
                            "ro.product.model" -> param.result = fingerprint.model
                            "ro.product.device" -> param.result = fingerprint.device
                            "ro.product.name" -> param.result = fingerprint.product
                            "ro.hardware" -> param.result = fingerprint.hardware
                            "ro.product.board" -> param.result = fingerprint.board
                            "ro.bootloader" -> param.result = fingerprint.bootloader
                            "ro.build.fingerprint" -> param.result = fingerprint.fingerprint
                            "ro.build.id" -> param.result = fingerprint.buildId
                            "ro.build.tags" -> param.result = fingerprint.tags
                            "ro.build.type" -> param.result = fingerprint.type
                            "gsm.version.baseband" -> param.result = fingerprint.radioVersion
                            "ro.serialno" -> param.result = prefs?.getString("serial", null) ?: generateRandomSerial()
                            "ro.build.version.release" -> param.result = "11"  // FIX #5: Siempre 11
                            "ro.build.version.sdk" -> param.result = "30"  // FIX #5: Siempre 30

                            // Partition specific properties
                            "ro.product.system.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.system.brand" -> param.result = fingerprint.brand
                            "ro.product.system.model" -> param.result = fingerprint.model
                            "ro.product.system.device" -> param.result = fingerprint.device
                            "ro.product.system.name" -> param.result = fingerprint.product

                            "ro.product.vendor.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.vendor.brand" -> param.result = fingerprint.brand
                            "ro.product.vendor.model" -> param.result = fingerprint.model
                            "ro.product.vendor.device" -> param.result = fingerprint.device
                            "ro.product.vendor.name" -> param.result = fingerprint.product

                            "ro.product.odm.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.odm.brand" -> param.result = fingerprint.brand
                            "ro.product.odm.model" -> param.result = fingerprint.model
                            "ro.product.odm.device" -> param.result = fingerprint.device
                            "ro.product.odm.name" -> param.result = fingerprint.product

                            "ro.product.product.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.product.brand" -> param.result = fingerprint.brand
                            "ro.product.product.model" -> param.result = fingerprint.model
                            "ro.product.product.device" -> param.result = fingerprint.device
                            "ro.product.product.name" -> param.result = fingerprint.product
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(sysPropClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        when (key) {
                            "ro.product.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.brand" -> param.result = fingerprint.brand
                            "ro.product.model" -> param.result = fingerprint.model
                            "ro.product.device" -> param.result = fingerprint.device
                            "ro.product.name" -> param.result = fingerprint.product
                            "ro.hardware" -> param.result = fingerprint.hardware
                            "ro.product.board" -> param.result = fingerprint.board
                            "ro.bootloader" -> param.result = fingerprint.bootloader
                            "ro.build.fingerprint" -> param.result = fingerprint.fingerprint
                            "ro.build.id" -> param.result = fingerprint.buildId
                            "ro.build.tags" -> param.result = fingerprint.tags
                            "ro.build.type" -> param.result = fingerprint.type
                            "gsm.version.baseband" -> param.result = fingerprint.radioVersion
                            "ro.serialno" -> param.result = prefs?.getString("serial", null) ?: generateRandomSerial()
                            "ro.build.version.release" -> param.result = "11"
                            "ro.build.version.sdk" -> param.result = "30"

                            // Partition specific properties
                            "ro.product.system.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.system.brand" -> param.result = fingerprint.brand
                            "ro.product.system.model" -> param.result = fingerprint.model
                            "ro.product.system.device" -> param.result = fingerprint.device
                            "ro.product.system.name" -> param.result = fingerprint.product

                            "ro.product.vendor.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.vendor.brand" -> param.result = fingerprint.brand
                            "ro.product.vendor.model" -> param.result = fingerprint.model
                            "ro.product.vendor.device" -> param.result = fingerprint.device
                            "ro.product.vendor.name" -> param.result = fingerprint.product

                            "ro.product.odm.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.odm.brand" -> param.result = fingerprint.brand
                            "ro.product.odm.model" -> param.result = fingerprint.model
                            "ro.product.odm.device" -> param.result = fingerprint.device
                            "ro.product.odm.name" -> param.result = fingerprint.product

                            "ro.product.product.manufacturer" -> param.result = fingerprint.manufacturer
                            "ro.product.product.brand" -> param.result = fingerprint.brand
                            "ro.product.product.model" -> param.result = fingerprint.model
                            "ro.product.product.device" -> param.result = fingerprint.device
                            "ro.product.product.name" -> param.result = fingerprint.product
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("SystemProperties hook error: ${e.message}")
        }
    }

    private fun hookTelephonyManager(lpparam: XC_LoadPackage.LoadPackageParam,
                                      prefs: XSharedPreferences?) {
        try {
            val tmClass = XposedHelpers.findClass("android.telephony.TelephonyManager", lpparam.classLoader)

            // FIX #3: Usar valores cacheados
            XposedHelpers.findAndHookMethod(tmClass, "getDeviceId",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = cachedImei
                    }
                }
            )

            try {
                XposedHelpers.findAndHookMethod(tmClass, "getDeviceId", Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val slot = param.args[0] as Int
                            param.result = if (slot == 0) cachedImei else cachedImei2
                        }
                    }
                )
            } catch (e: NoSuchMethodError) {}

            try {
                XposedHelpers.findAndHookMethod(tmClass, "getImei",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = cachedImei
                        }
                    }
                )

                XposedHelpers.findAndHookMethod(tmClass, "getImei", Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val slot = param.args[0] as Int
                            param.result = if (slot == 0) cachedImei else cachedImei2
                        }
                    }
                )
            } catch (e: NoSuchMethodError) {}

            // FIX #3: IMSI consistente
            XposedHelpers.findAndHookMethod(tmClass, "getSubscriberId",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = cachedImsi
                    }
                }
            )

            // FIX #3: ICCID consistente
            XposedHelpers.findAndHookMethod(tmClass, "getSimSerialNumber",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = cachedIccid
                    }
                }
            )

            // FIX #3: Número telefónico consistente
            XposedHelpers.findAndHookMethod(tmClass, "getLine1Number",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = cachedPhoneNumber
                    }
                }
            )

            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperator",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = prefs?.getString("mcc_mnc", "310260") ?: "310260"
                    }
                }
            )

            XposedHelpers.findAndHookMethod(tmClass, "getSimOperator",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = prefs?.getString("sim_operator", "310260") ?: "310260"
                    }
                }
            )

            XposedHelpers.findAndHookMethod(tmClass, "getNetworkCountryIso",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = prefs?.getString("sim_country", "us") ?: "us"
                    }
                }
            )

            XposedHelpers.findAndHookMethod(tmClass, "getSimCountryIso",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = prefs?.getString("sim_country", "us") ?: "us"
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("TelephonyManager hook error: ${e.message}")
        }
    }

    // FIX #4: Un solo hook para Settings.Secure
    private fun hookSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam,
                                    prefs: XSharedPreferences?) {
        try {
            val settingsSecureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(settingsSecureClass, "getString",
                android.content.ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.args[1] as String
                        when (name) {
                            Settings.Secure.ANDROID_ID -> param.result = cachedAndroidId
                            "advertising_id" -> param.result = cachedGaid
                            // FIX #4: GSF ID separado de ANDROID_ID
                            "gsf_id", "android_id_gsf" -> param.result = cachedGsfId
                        }
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("Settings.Secure hook error: ${e.message}")
        }
    }

    private fun hookNetworkInterfaces(lpparam: XC_LoadPackage.LoadPackageParam,
                                       prefs: XSharedPreferences?) {
        try {
            val niClass = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(niClass, "getHardwareAddress",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val ni = param.thisObject as NetworkInterface
                            val name = ni.name

                            when {
                                name.startsWith("wlan") -> {
                                    param.result = macStringToBytes(cachedWifiMac!!)
                                }
                                name.startsWith("bt") || name.startsWith("bluetooth") -> {
                                    param.result = macStringToBytes(cachedBtMac!!)
                                }
                            }
                        } catch (e: Exception) {
                            // FIX #8: Silenciar errores de conversión
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("NetworkInterface hook error: ${e.message}")
        }
    }

    private fun hookLocation(lpparam: XC_LoadPackage.LoadPackageParam,
                            prefs: XSharedPreferences?) {
        try {
            mockLocationEnabled = prefs?.getBoolean("mock_location_enabled", false) ?: false
            val latStr = prefs?.getString("mock_latitude", "0.0") ?: "0.0"
            val lonStr = prefs?.getString("mock_longitude", "0.0") ?: "0.0"
            val altStr = prefs?.getString("mock_altitude", "0.0") ?: "0.0"

            mockLatitude = latStr.toDoubleOrNull() ?: 0.0
            mockLongitude = lonStr.toDoubleOrNull() ?: 0.0
            mockAltitude = altStr.toDoubleOrNull() ?: 0.0

            if (!mockLocationEnabled) return

            val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(locationClass, "getLatitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // FIX #9: Permitir coordenada (0,0)
                        if (mockLocationEnabled) {
                            param.result = mockLatitude
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(locationClass, "getLongitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) {
                            param.result = mockLongitude
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(locationClass, "getAltitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) {
                            param.result = mockAltitude
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(locationClass, "hasAltitude",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) {
                            param.result = true
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(locationClass, "getAccuracy",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) {
                            param.result = 10.0f
                        }
                    }
                }
            )

            val locationManagerClass = XposedHelpers.findClass("android.location.LocationManager", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(locationManagerClass, "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (mockLocationEnabled) {
                            val location = Location(param.args[0] as String)
                            location.latitude = mockLatitude
                            location.longitude = mockLongitude
                            location.altitude = mockAltitude
                            location.accuracy = 10.0f
                            location.time = System.currentTimeMillis()
                            location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                            param.result = location
                        }
                    }
                }
            )

        } catch (e: Throwable) {
            XposedBridge.log("Location hook error: ${e.message}")
        }
    }

    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
        try {
            val webSettingsClass = XposedHelpers.findClass("android.webkit.WebSettings", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(webSettingsClass, "getUserAgentString",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originalUserAgent = param.result as String
                        // Replace the device model in the User-Agent
                        // Pattern matches typical Android User-Agents: ...; Model Build/...
                        val newUserAgent = originalUserAgent.replace(
                            Regex(";\\s+[^;]+?\\s+Build/"),
                            "; ${fingerprint.model} Build/"
                        )
                        param.result = newUserAgent
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("WebView hook error: ${e.message}")
        }
    }

    private fun hookXposedDetection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(Throwable::class.java, "getStackTrace",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val stackTrace = param.result as Array<StackTraceElement>
                        val filtered = stackTrace.filterNot {
                            it.className.contains("xposed", ignoreCase = true) ||
                            it.className.contains("edxposed", ignoreCase = true) ||
                            it.className.contains("lsposed", ignoreCase = true) ||
                            it.className.contains("de.robv", ignoreCase = true)
                        }
                        param.result = filtered.toTypedArray()
                    }
                }
            )

        } catch (e: Throwable) {
            // No crítico
        }
    }

    // ========== GENERADORES ==========

    private fun generateRandomId(length: Int): String =
        (1..length).map { "0123456789abcdef".random() }.joinToString("")

    private fun generateValidImei(): String {
        val validTacs = listOf(
            "35891603",  // Xiaomi
            "35328708",  // Samsung
            "35404907"   // Motorola
        )
        val tac = validTacs.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    private fun generateValidIccid(): String {
        val prefix = "89"
        val country = listOf("01", "52").random()
        val issuer = (1..2).map { (0..9).random() }.joinToString("")
        val account = (1..13).map { (0..9).random() }.joinToString("")
        val base = prefix + country + issuer + account
        return base + luhnChecksum(base)
    }

    private fun generatePhoneNumber(countryCode: String): String {
        return when (countryCode.lowercase()) {
            "us" -> "+1" + (2..9).random().toString() + (0..9).random().toString() + (0..9).random().toString() +
                    (1..7).map { (0..9).random() }.joinToString("")
            "mx" -> "+52" + listOf("55", "33", "81").random() + (1..7).map { (0..9).random() }.joinToString("")
            else -> "+1" + (2..9).random().toString() + (1..9).map { (0..9).random() }.joinToString("")
        }
    }

    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var digit = number[i].digitToInt()
            if ((number.length - i) % 2 == 0) digit *= 2
            if (digit > 9) digit -= 9
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun generateRandomSerial(): String =
        (1..12).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

    private fun generateRandomGaid(): String =
        "${generateRandomId(8)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(12)}"

    private fun generateRandomMac(): String {
        val firstByte = (0x02 or (kotlin.random.Random.nextInt(256) and 0xFC))
            .toString(16).padStart(2, '0').uppercase()

        val rest = (1..5).map {
            kotlin.random.Random.nextInt(256).toString(16)
                .padStart(2, '0').uppercase()
        }.joinToString(":")

        return "$firstByte:$rest"
    }

    private fun macStringToBytes(mac: String): ByteArray {
        // FIX #8: Validación robusta
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) throw IllegalArgumentException()
            ByteArray(6) { i ->
                parts[i].toInt(16).toByte()
            }
        } catch (e: Exception) {
            // Fallback a MAC válida
            byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00)
        }
    }
}
