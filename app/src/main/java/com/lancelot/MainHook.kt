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
import com.lancelot.utils.CryptoUtils
import com.lancelot.SpoofingUtils
import com.lancelot.BuildConfig

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "spoof_prefs"

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
            "Google Pixel 5" to DeviceFingerprint("Google", "google", "Pixel 5", "redfin", "redfin", "qcom", "redfin", "r8-0.3-7219051", "google/redfin/redfin:11/RQ3A.210805.001.A1/7512229:user/release-keys", "RQ3A.210805.001.A1", "release-keys", "user", "g725-00164-210812-B-7522969", "7512229", 30, "11", "lito", "adreno", "196610", "SM7250", "zygote64_32", "google/redfin_vend/redfin:11/RQ3A.210805.001.A1/7512229:vendor/release-keys", "RQ3A.210805.001.A1", "redfin-user 11 RQ3A.210805.001.A1 7512229 release-keys", "redfin-user", "abfarm-release-rbe-64.hot.corp.google.com", "android-build", "1628100000", "2021-08-05", "REL", "0"),
            "Google Pixel 4a 5G" to DeviceFingerprint("Google", "google", "Pixel 4a (5G)", "bramble", "bramble", "qcom", "bramble", "b2-0.3-7214727", "google/bramble/bramble:11/RQ3A.210705.001/7380771:user/release-keys", "RQ3A.210705.001", "release-keys", "user", "g7250-00195-210614-B-7352378", "7380771", 30, "11", "lito", "adreno", "196610", "SM7250", "zygote64_32", "google/bramble_vend/bramble:11/RQ3A.210705.001/7380771:vendor/release-keys", "RQ3A.210705.001", "bramble-user 11 RQ3A.210705.001 7380771 release-keys", "bramble-user", "abfarm-release-rbe-64.hot.corp.google.com", "android-build", "1625616000", "2021-07-05", "REL", "0"),
            "Samsung Galaxy S20+" to DeviceFingerprint("samsung", "samsung", "SM-G985F", "y2s", "y2sxx", "exynos990", "universal990", "G985FXXSGHWA3", "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys", "RP1A.200720.012", "release-keys", "user", "", "G985FXXSGHWA3", 30, "11", "exynos990", "mali", "196610", "Exynos990", "zygote64_32", "samsung/y2sxx/y2s:11/RP1A.200720.012/G985FXXSGHWA3:user/release-keys", "RP1A.200720.012.G985FXXSGHWA3", "y2sxx-user 11 RP1A.200720.012 G985FXXSGHWA3 release-keys", "y2sxx-user", "21R3NF12", "dpi", "1639497600", "2022-01-01", "REL", "0"),
            "Samsung Galaxy S10e" to DeviceFingerprint("samsung", "samsung", "SM-G970F", "beyond0", "beyond0ltexx", "exynos9820", "universal9820", "G970FXXSGHWC1", "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys", "RP1A.200720.012", "release-keys", "user", "", "G970FXXSGHWC1", 30, "11", "exynos9820", "mali", "196610", "Exynos9820", "zygote64_32", "samsung/beyond0ltexx/beyond0:11/RP1A.200720.012/G970FXXSGHWC1:user/release-keys", "RP1A.200720.012.G970FXXSGHWC1", "beyond0ltexx-user 11 RP1A.200720.012 G970FXXSGHWC1 release-keys", "beyond0ltexx-user", "21R3NF12", "dpi", "1646236800", "2022-03-01", "REL", "0"),
            "Vivo Y53s" to DeviceFingerprint("vivo", "vivo", "V2058", "V2058", "V2058", "mt6769", "V2058", "unknown", "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V98", "compiler08272021", 30, "11", "mt6769", "mali", "196610", "MT6769", "zygote64_32", "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys", "RP1A.200720.011", "V2058-user 11 RP1A.200720.011 compiler08272021 release-keys", "V2058-user", "compiler", "build", "1630022400", "2021-08-01", "REL", "0"),
            "Realme 7 5G" to DeviceFingerprint("realme", "realme", "RMX2111", "RMX2111", "RMX2111", "mt6853", "RMX2111", "unknown", "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR14A.R3.MP.V62", "1626245367375", 30, "11", "mt6853", "mali", "196610", "MT6853", "zygote64_32", "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys", "RP1A.200720.011", "RMX2111-user 11 RP1A.200720.011 1626245367375 release-keys", "RMX2111-user", "ubuntu-123", "jenkins", "1626245367", "2021-07-01", "REL", "0"),
            "Oppo Reno5" to DeviceFingerprint("OPPO", "OPPO", "CPH2159", "OP4E75L1", "CPH2159", "qcom", "OP4E75L1", "unknown", "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MPSS.HI.2.0.c4-00165", "1622635284", 30, "11", "atoll", "adreno", "196610", "SM7225", "zygote64_32", "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys", "RP1A.200720.011", "CPH2159-user 11 RP1A.200720.011 1622635284 release-keys", "CPH2159-user", "ubuntu", "jenkins", "1622635284", "2021-06-01", "REL", "0"),
            "Vivo X60" to DeviceFingerprint("vivo", "vivo", "V2046", "V2046", "V2046", "qcom", "V2046", "unknown", "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys", "RP1A.200720.012", "release-keys", "user", "MPSS.HI.2.0.c4-00165", "compiler05211516", 30, "11", "kona", "adreno", "196610", "SM8250", "zygote64_32", "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys", "RP1A.200720.012", "V2046-user 11 RP1A.200720.012 compiler05211516 release-keys", "V2046-user", "compiler", "build", "1621584000", "2021-05-01", "REL", "0"),
            "Vivo X70 Pro" to DeviceFingerprint("vivo", "vivo", "V2105", "V2105", "V2105", "mt6893", "V2105", "unknown", "vivo/V2105/V2105:11/RP1A.200720.012/compiler10212015:user/release-keys", "RP1A.200720.012", "release-keys", "user", "MOLY.LR12A.R3.MP.V98", "compiler10212015", 30, "11", "mt6893", "mali", "196610", "MT6893", "zygote64_32", "vivo/V2105/V2105:11/RP1A.200720.012/compiler10212015:user/release-keys", "RP1A.200720.012", "V2105-user 11 RP1A.200720.012 compiler10212015 release-keys", "V2105-user", "compiler", "build", "1634860800", "2021-10-01", "REL", "0"),
            "Realme 8 Pro" to DeviceFingerprint("realme", "realme", "RMX3081", "RMX3081", "RMX3081", "qcom", "RMX3081", "unknown", "realme/RMX3081/RMX3081:11/RP1A.200720.011/1626245367375:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MPSS.HI.2.0.c4-00165", "1626245367375", 30, "11", "atoll", "adreno", "196610", "SM7125", "zygote64_32", "realme/RMX3081/RMX3081:11/RP1A.200720.011/1626245367375:user/release-keys", "RP1A.200720.011", "RMX3081-user 11 RP1A.200720.011 1626245367375 release-keys", "RMX3081-user", "ubuntu-123", "jenkins", "1626245367", "2021-07-01", "REL", "0"),
            "Asus Zenfone 8" to DeviceFingerprint("asus", "asus", "ASUS_I006D", "ASUS_I006D", "WW_I006D", "qcom", "sake", "unknown", "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys", "RKQ1.201112.002", "release-keys", "user", "M3.13.24.51-Sake_0000100", "30.11.51.115", 30, "11", "lahaina", "adreno", "196610", "SM8350", "zygote64_32", "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys", "RKQ1.201112.002.30.11.51.115", "WW_I006D-user 11 RKQ1.201112.002 30.11.51.115 release-keys", "WW_I006D-user", "android-build", "jenkins", "1629859200", "2021-08-01", "REL", "0")
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
                return CryptoUtils.decrypt(raw) ?: def
            }

            val profileName = getEncryptedString("profile", "Redmi 9")
            val fingerprint = getDeviceFingerprint(profileName)

            initializeCache(prefs, ::getEncryptedString)

            hookBuildFields(lpparam, fingerprint)
            hookSystemProperties(lpparam, fingerprint)
            hookTelephonyManager(lpparam)
            hookUnifiedSettingsSecure(lpparam)
            hookNetworkInterfaces(lpparam)
            hookLocation(lpparam, prefs, ::getEncryptedString)
            hookWebView(lpparam, fingerprint)
            hookAccountManager(lpparam)
            hookXposedDetection(lpparam)
            hookPackageManager(lpparam)
            hookApplicationFlags(lpparam)

            // Activate File protection hook
            hookFile()

        } catch (e: Throwable) {
            try {
                if (BuildConfig.DEBUG) XposedBridge.log("Lancelot Error: ${e.message}")
            } catch (ex: Throwable) {}
        }
    }

    private fun hookFile() {
        try {
            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    val path = file.absolutePath

                    if (SpoofingUtils.isSensitivePath(path)) {
                        param.result = false
                    }
                }
            }

            // Apply hook to all File check methods
            XposedHelpers.findAndHookMethod(File::class.java, "exists", hook)
            XposedHelpers.findAndHookMethod(File::class.java, "canRead", hook)
            XposedHelpers.findAndHookMethod(File::class.java, "canExecute", hook)
            XposedHelpers.findAndHookMethod(File::class.java, "isFile", hook)
            XposedHelpers.findAndHookMethod(File::class.java, "isDirectory", hook)

        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("File hook error: ${e.message}")
        }
    }

    private fun initializeCache(prefs: XSharedPreferences, getString: (String, String) -> String) {
        if (cachedImei == null) cachedImei = getString("imei", SpoofingUtils.generateValidImei())
        if (cachedImei2 == null) cachedImei2 = getString("imei2", SpoofingUtils.generateValidImei())
        if (cachedAndroidId == null) cachedAndroidId = getString("android_id", SpoofingUtils.generateRandomId(16))
        if (cachedGsfId == null) cachedGsfId = getString("gsf_id", SpoofingUtils.generateRandomId(16))
        if (cachedGaid == null) cachedGaid = getString("gaid", SpoofingUtils.generateRandomGaid())
        if (cachedWifiMac == null) cachedWifiMac = getString("wifi_mac", SpoofingUtils.generateRandomMac())
        if (cachedBtMac == null) cachedBtMac = getString("bluetooth_mac", SpoofingUtils.generateRandomMac())
        if (cachedGmail == null) cachedGmail = getString("gmail", SpoofingUtils.generateRealisticGmail())
        if (cachedSerial == null) cachedSerial = getString("serial", SpoofingUtils.generateRandomSerial())

        val defaultMccMnc = US_CARRIERS.random().mccMnc
        val mccMnc = getString("mcc_mnc", defaultMccMnc)

        if (cachedImsi == null) cachedImsi = mccMnc + (1..10).map { (0..9).random() }.joinToString("")
        if (cachedIccid == null) cachedIccid = SpoofingUtils.generateValidIccid(mccMnc)
        if (cachedPhoneNumber == null) cachedPhoneNumber = SpoofingUtils.generatePhoneNumber(emptyList())
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        val cleanName = profileName.replace(" - Android 11", "").trim()
        return DEVICE_FINGERPRINTS.entries.find { it.key == cleanName }?.value
            ?: DEVICE_FINGERPRINTS.values.firstOrNull()
            ?: DeviceFingerprint("Xiaomi", "Redmi", "Redmi 9", "lancelot", "lancelot_global", "mt6768", "lancelot", "unknown", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "RP1A.200720.011", "release-keys", "user", "MOLY.LR12A.R3.MP.V84.P47", "V12.5.3.0.RJCMIXM", 30, "11", "mt6768", "mali", "196610", "MT6768", "zygote64_32", "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys", "V12.5.3.0.RJCMIXM", "lancelot_global-user", "lancelot_global-user", "pangu", "builder", "1632960000", "2021-09-01", "REL", "0")
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
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

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
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

    private fun hookTelephonyManager(lpparam: XC_LoadPackage.LoadPackageParam) {
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

    private fun hookUnifiedSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam) {
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

    private fun hookNetworkInterfaces(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val niClass = XposedHelpers.findClass("java.net.NetworkInterface", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(niClass, "getHardwareAddress", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val ni = param.thisObject as NetworkInterface
                        val name = ni.name ?: return // 1. Avoid error if name is null
                        when {
                            // 2. If cache fails, generate random instead of failing/leaking real
                            name.startsWith("wlan") -> param.result = macStringToBytes(cachedWifiMac ?: SpoofingUtils.generateRandomMac())
                            name.startsWith("bt") || name.startsWith("bluetooth") -> param.result = macStringToBytes(cachedBtMac ?: SpoofingUtils.generateRandomMac())
                        }
                    } catch (e: Exception) {
                        // 3. Log errors in debug
                        if (BuildConfig.DEBUG) XposedBridge.log("Lancelot: Error spoofing network interface: ${e.message}")
                    }
                }
            })
        } catch (e: Throwable) {
             if (BuildConfig.DEBUG) XposedBridge.log("Lancelot: Error hooking NetworkInterface: ${e.message}")
        }
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
            // Fix #8: Make jitter optional
            val applyJitter = prefs.getBoolean("location_jitter_enabled", true)
            // Fix #9: Respect static location
            val isMoving = prefs.getBoolean("location_is_moving", false)

            if (applyJitter) {
                // Gaussian Noise
                mockAccuracy = (mockAccuracy + (random.nextGaussian() * 2.0)).toFloat().coerceAtLeast(1.0f)
                mockAltitude = mockAltitude + (random.nextGaussian() * 0.5)

                // Small jitter for location
                val jitterDeg = 0.00001
                mockLatitude += random.nextGaussian() * jitterDeg
                mockLongitude += random.nextGaussian() * jitterDeg
            }

            mockBearing = if (isMoving) random.nextFloat() * 360.0f else 0.0f
            mockSpeed = if (isMoving) random.nextFloat() * 5.0f else 0.0f

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

    private fun macStringToBytes(mac: String): ByteArray {
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) throw IllegalArgumentException()
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (e: Exception) { byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00) }
    }
}
