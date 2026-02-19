package com.vortex

import android.accounts.Account
import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.net.NetworkInterface
import java.util.Random
import com.vortex.utils.CryptoUtils
import com.vortex.utils.OriginalBuildValues
import com.vortex.SpoofingUtils
import com.vortex.BuildConfig

class MainHook : IXposedHookLoadPackage {

    companion object {
        private const val PREFS_NAME = "spoof_prefs"

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
        private var cachedSsaidSnapchat: String? = null
        private var cachedMediaDrmId: String? = null
        private var cachedWifiSsid: String? = null
        private var cachedWifiBssid: String? = null

        private var mockLatitude: Double = 0.0
        private var mockLongitude: Double = 0.0
        private var mockAltitude: Double = 0.0
        private var mockAccuracy: Float = 10.0f
        private var mockBearing: Float = 0.0f
        private var mockSpeed: Float = 0.0f
        private var mockLocationEnabled: Boolean = false

        data class UsCarrier(
            val name: String,
            val mccMnc: String,
            val npas: List<String>
        )

        private val US_CARRIERS = listOf(
            UsCarrier("T-Mobile",        "310260", listOf("206","253","360","425","509","564","347","646","917","929","718","212")),
            UsCarrier("AT&T",            "310410", listOf("214","469","972","817","682","940","404","678","770","470","312","872")),
            UsCarrier("Verizon",         "310012", listOf("201","551","732","848","908","973","609","856","862","732","908","862")),
            UsCarrier("Sprint (legacy)", "310120", listOf("312","773","847","224","331","630","708","872","779","815","618","309")),
            UsCarrier("US Cellular",     "311580", listOf("217","309","618","815","319","563","920","414","262","608","715","906")),
            UsCarrier("Cricket",         "310150", listOf("832","713","281","346","979","936","210","726","830","956","361","430")),
            UsCarrier("Metro PCS",       "310030", listOf("305","786","954","754","561","407","321","689","386","352","904","727")),
            UsCarrier("Boost Mobile",    "311870", listOf("323","213","310","424","818","747","626","562","661","760","442","951")),
            UsCarrier("Google Fi",       "310260", listOf("202","703","571","240","301","410","443","667","301","202","571","703")),
            UsCarrier("Tracfone",        "310410", listOf("786","305","954","561","407","321","352","863","941","239","850","904"))
        )

        fun getUsCarriers(): List<UsCarrier> = US_CARRIERS

        // Expanded DEVICE_FINGERPRINTS (40 Android 11 profiles)
        val DEVICE_FINGERPRINTS = mapOf(
            // --- XIAOMI / POCO ---
            "Redmi 9" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi 9","lancelot","lancelot_global","mt6768","lancelot","unknown",
                "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.3.0.RJCMIXM",
                30,"11","mt6768","mali","196610","MT6768","zygote64_32",
                "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                "V12.5.3.0.RJCMIXM","lancelot_global-user","lancelot_global-user",
                "pangu","builder","1632960000","2021-09-01","REL","0"),
            "Redmi Note 9" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi Note 9","merlin","merlin_global","mt6769","merlin","unknown",
                "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.2.0.RJOMIXM",
                30,"11","mt6769","mali","196610","MT6769","zygote64_32",
                "Redmi/merlin_global/merlin:11/RP1A.200720.011/V12.5.2.0.RJOMIXM:user/release-keys",
                "V12.5.2.0.RJOMIXM","merlin_global-user","merlin_global-user","pangu","builder","1632960000","2021-09-01","REL","0"),
            "Redmi 9A" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi 9A","dandelion","dandelion_global","mt6762","dandelion","unknown",
                "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.4.0.RCDMIXM",
                30,"11","mt6762","pvrsrvkm","196608","MT6762","zygote64_32",
                "Redmi/dandelion_global/dandelion:11/RP1A.200720.011/V12.5.4.0.RCDMIXM:user/release-keys",
                "V12.5.4.0.RCDMIXM","dandelion_global-user","dandelion_global-user","pangu","builder","1633046400","2021-10-01","REL","0"),
            "Redmi 9C" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi 9C","angelica","angelica_global","mt6762","angelica","unknown",
                "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.1.0.RCRMIXM",
                30,"11","mt6762","pvrsrvkm","196608","MT6762","zygote64_32",
                "Redmi/angelica_global/angelica:11/RP1A.200720.011/V12.5.1.0.RCRMIXM:user/release-keys",
                "V12.5.1.0.RCRMIXM","angelica_global-user","angelica_global-user","pangu","builder","1632960000","2021-09-01","REL","0"),
            "POCO X3 NFC" to DeviceFingerprint(
                "Xiaomi","POCO","POCO X3 NFC","surya","surya_global","qcom","surya","unknown",
                "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                "RKQ1.200826.002","release-keys","user","MPSS.HI.3.1.c3-00186-SM7150_GEN_PACK-1","V12.5.7.0.RJGMIXM",
                30,"11","atoll","adreno","196610","SM7150","zygote64_32",
                "POCO/surya_global/surya:11/RKQ1.200826.002/V12.5.7.0.RJGMIXM:user/release-keys",
                "V12.5.7.0.RJGMIXM","surya_global-user","surya_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),
            "POCO F3" to DeviceFingerprint(
                "Xiaomi","POCO","POCO F3","alioth","alioth_global","qcom","alioth","unknown",
                "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.6.0.RKHMIXM:user/release-keys",
                "RKQ1.200826.002","release-keys","user","MPSS.HI.2.0.c4-00165","V12.5.6.0.RKHMIXM",
                30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.6.0.RKHMIXM:user/release-keys",
                "V12.5.6.0.RKHMIXM","alioth_global-user","alioth_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),
            "Mi 10T" to DeviceFingerprint(
                "Xiaomi","Xiaomi","Mi 10T","apollo","apollo_global","qcom","apollo","unknown",
                "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.10.0.RJDMIXM:user/release-keys",
                "RKQ1.200826.002","release-keys","user","MPSS.HI.2.0.c4-00165","V12.5.10.0.RJDMIXM",
                30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "Xiaomi/apollo_global/apollo:11/RKQ1.200826.002/V12.5.10.0.RJDMIXM:user/release-keys",
                "V12.5.10.0.RJDMIXM","apollo_global-user","apollo_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),
            "Redmi Note 10 Pro" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi Note 10 Pro","sweet","sweet_global","qcom","sweet","unknown",
                "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.9.0.RKFMIXM:user/release-keys",
                "RKQ1.200826.002","release-keys","user","MPSS.HI.3.0.c1-00072","V12.5.9.0.RKFMIXM",
                30,"11","sm6150","adreno","196610","SM7150","zygote64_32",
                "Redmi/sweet_global/sweet:11/RKQ1.200826.002/V12.5.9.0.RKFMIXM:user/release-keys",
                "V12.5.9.0.RKFMIXM","sweet_global-user","sweet_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),
            "Mi 11" to DeviceFingerprint(
                "Xiaomi","Xiaomi","Mi 11","venus","venus_global","qcom","venus","unknown",
                "Xiaomi/venus_global/venus:11/RKQ1.201112.002/V12.5.7.0.RKBMIXM:user/release-keys",
                "RKQ1.201112.002","release-keys","user","MPSS.HI.2.0.c4-00165","V12.5.7.0.RKBMIXM",
                30,"11","lahaina","adreno","196610","SM8350","zygote64_32",
                "Xiaomi/venus_global/venus:11/RKQ1.201112.002/V12.5.7.0.RKBMIXM:user/release-keys",
                "V12.5.7.0.RKBMIXM","venus_global-user","venus_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),
            "Redmi 10" to DeviceFingerprint(
                "Xiaomi","Redmi","Redmi 10","selene","selene_global","mt6768","selene","unknown",
                "Redmi/selene_global/selene:11/RP1A.200720.011/V12.5.9.0.RKUMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.9.0.RKUMIXM",
                30,"11","mt6768","mali","196610","MT6768","zygote64_32",
                "Redmi/selene_global/selene:11/RP1A.200720.011/V12.5.9.0.RKUMIXM:user/release-keys",
                "V12.5.9.0.RKUMIXM","selene_global-user","selene_global-user","pangu","builder","1634860800","2021-10-01","REL","0"),

            // --- GOOGLE PIXEL (Android 11) ---
            "Google Pixel 5" to DeviceFingerprint(
                "Google","google","Pixel 5","redfin","redfin","qcom","redfin","r8-0.3-7219051",
                "google/redfin/redfin:11/RQ3A.210805.001.A1/7512229:user/release-keys",
                "RQ3A.210805.001.A1","release-keys","user","g725-00164-210812-B-7522969","7512229",
                30,"11","lito","adreno","196610","SM7250","zygote64_32",
                "google/redfin_vend/redfin:11/RQ3A.210805.001.A1/7512229:vendor/release-keys",
                "RQ3A.210805.001.A1","redfin-user","redfin-user","abfarm","android-build","1628100000","2021-08-05","REL","0"),
            "Google Pixel 4a 5G" to DeviceFingerprint(
                "Google","google","Pixel 4a (5G)","bramble","bramble","qcom","bramble","b2-0.3-7214727",
                "google/bramble/bramble:11/RQ3A.210705.001/7380771:user/release-keys",
                "RQ3A.210705.001","release-keys","user","g7250-00195-210614-B-7352378","7380771",
                30,"11","lito","adreno","196610","SM7250","zygote64_32",
                "google/bramble_vend/bramble:11/RQ3A.210705.001/7380771:vendor/release-keys",
                "RQ3A.210705.001","bramble-user","bramble-user","abfarm","android-build","1625616000","2021-07-05","REL","0"),
            "Google Pixel 4a" to DeviceFingerprint(
                "Google","google","Pixel 4a","sunfish","sunfish","qcom","sunfish","b2-0.3-7214727",
                "google/sunfish/sunfish:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","sm7150","adreno","196610","SM7150","zygote64_32",
                "google/sunfish_vend/sunfish:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","sunfish-user","sunfish-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 4 XL" to DeviceFingerprint(
                "Google","google","Pixel 4 XL","coral","coral","qcom","coral","b2-0.3-7214727",
                "google/coral/coral:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","msmnile","adreno","196610","SM8150","zygote64_32",
                "google/coral_vend/coral:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","coral-user","coral-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 4" to DeviceFingerprint(
                "Google","google","Pixel 4","flame","flame","qcom","flame","b2-0.3-7214727",
                "google/flame/flame:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","msmnile","adreno","196610","SM8150","zygote64_32",
                "google/flame_vend/flame:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","flame-user","flame-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 3a XL" to DeviceFingerprint(
                "Google","google","Pixel 3a XL","bonito","bonito","qcom","bonito","b2-0.3-7214727",
                "google/bonito/bonito:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","sdm670","adreno","196610","SDM670","zygote64_32",
                "google/bonito_vend/bonito:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","bonito-user","bonito-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 3a" to DeviceFingerprint(
                "Google","google","Pixel 3a","sargo","sargo","qcom","sargo","b2-0.3-7214727",
                "google/sargo/sargo:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","sdm670","adreno","196610","SDM670","zygote64_32",
                "google/sargo_vend/sargo:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","sargo-user","sargo-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 3 XL" to DeviceFingerprint(
                "Google","google","Pixel 3 XL","crosshatch","crosshatch","qcom","crosshatch","b2-0.3-7214727",
                "google/crosshatch/crosshatch:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","sdm845","adreno","196610","SDM845","zygote64_32",
                "google/crosshatch_vend/crosshatch:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","crosshatch-user","crosshatch-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 3" to DeviceFingerprint(
                "Google","google","Pixel 3","blueline","blueline","qcom","blueline","b2-0.3-7214727",
                "google/blueline/blueline:11/RQ3A.210605.005/7349438:user/release-keys",
                "RQ3A.210605.005","release-keys","user","g7250-00195-210614-B-7352378","7349438",
                30,"11","sdm845","adreno","196610","SDM845","zygote64_32",
                "google/blueline_vend/blueline:11/RQ3A.210605.005/7349438:vendor/release-keys",
                "RQ3A.210605.005","blueline-user","blueline-user","abfarm","android-build","1622851200","2021-06-05","REL","0"),
            "Google Pixel 2 XL" to DeviceFingerprint(
                "Google","google","Pixel 2 XL","taimen","taimen","qcom","taimen","b2-0.3-7214727",
                "google/taimen/taimen:11/RP1A.201005.004.A1/6934943:user/release-keys",
                "RP1A.201005.004.A1","release-keys","user","g7250-00195-210614-B-7352378","6934943",
                30,"11","msm8998","adreno","196610","MSM8998","zygote64_32",
                "google/taimen_vend/taimen:11/RP1A.201005.004.A1/6934943:vendor/release-keys",
                "RP1A.201005.004.A1","taimen-user","taimen-user","abfarm","android-build","1601932800","2020-10-05","REL","0"),

            // --- SAMSUNG (Android 11) ---
            "Samsung Galaxy S21" to DeviceFingerprint(
                "samsung","samsung","SM-G991B","o1s","o1sxx","exynos2100","o1s","G991BXXU3AUIE",
                "samsung/o1sxx/o1s:11/RP1A.200720.012/G991BXXU3AUIE:user/release-keys",
                "RP1A.200720.012","release-keys","user","","G991BXXU3AUIE",30,"11","exynos2100","mali","196610","Exynos2100","zygote64_32",
                "samsung/o1sxx/o1s:11/RP1A.200720.012/G991BXXU3AUIE:user/release-keys","RP1A.200720.012.G991BXXU3AUIE",
                "o1sxx-user","o1sxx-user","21R3NF12","se.infra","1632873600","2021-09-01","REL","0"),
            "Samsung Galaxy S21 Ultra" to DeviceFingerprint(
                "samsung","samsung","SM-G998B","p3s","p3sxx","exynos2100","p3s","G998BXXU3AUIE",
                "samsung/p3sxx/p3s:11/RP1A.200720.012/G998BXXU3AUIE:user/release-keys",
                "RP1A.200720.012","release-keys","user","","G998BXXU3AUIE",30,"11","exynos2100","mali","196610","Exynos2100","zygote64_32",
                "samsung/p3sxx/p3s:11/RP1A.200720.012/G998BXXU3AUIE:user/release-keys","RP1A.200720.012.G998BXXU3AUIE",
                "p3sxx-user","p3sxx-user","21R3NF12","se.infra","1632873600","2021-09-01","REL","0"),
            "Samsung Galaxy S20 FE" to DeviceFingerprint(
                "samsung","samsung","SM-G780F","r8q","r8qxx","exynos990","r8q","G780FXXS7CUI1",
                "samsung/r8qxx/r8q:11/RP1A.200720.012/G780FXXS7CUI1:user/release-keys",
                "RP1A.200720.012","release-keys","user","","G780FXXS7CUI1",30,"11","exynos990","mali","196610","Exynos990","zygote64_32",
                "samsung/r8qxx/r8q:11/RP1A.200720.012/G780FXXS7CUI1:user/release-keys","RP1A.200720.012.G780FXXS7CUI1",
                "r8qxx-user","r8qxx-user","21R3NF12","se.infra","1632873600","2021-09-01","REL","0"),
            "Samsung Galaxy Note 20 Ultra" to DeviceFingerprint(
                "samsung","samsung","SM-N986B","c2s","c2sxx","exynos990","c2s","N986BXXS4DUI1",
                "samsung/c2sxx/c2s:11/RP1A.200720.012/N986BXXS4DUI1:user/release-keys",
                "RP1A.200720.012","release-keys","user","","N986BXXS4DUI1",30,"11","exynos990","mali","196610","Exynos990","zygote64_32",
                "samsung/c2sxx/c2s:11/RP1A.200720.012/N986BXXS4DUI1:user/release-keys","RP1A.200720.012.N986BXXS4DUI1",
                "c2sxx-user","c2sxx-user","21R3NF12","se.infra","1632873600","2021-09-01","REL","0"),
            "Samsung Galaxy A52" to DeviceFingerprint(
                "samsung","samsung","SM-A525F","a52q","a52qxx","qcom","a52q","A525FXXU4CVJB",
                "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys",
                "RP1A.200720.012","release-keys","user","MPSS.HI.3.0.c1-00072","A525FXXU4CVJB",30,"11","trinket","adreno","196610","SM7125","zygote64_32",
                "samsung/a52qxx/a52q:11/RP1A.200720.012/A525FXXU4CVJB:user/release-keys","RP1A.200720.012.A525FXXU4CVJB",
                "a52qxx-user","a52qxx-user","21R3NF12","se.infra","1639497600","2021-12-01","REL","0"),
            "Samsung Galaxy A72" to DeviceFingerprint(
                "samsung","samsung","SM-A725F","a72q","a72qxx","qcom","a72q","A725FXXU4AUH2",
                "samsung/a72qxx/a72q:11/RP1A.200720.012/A725FXXU4AUH2:user/release-keys",
                "RP1A.200720.012","release-keys","user","MPSS.HI.3.0.c1-00072","A725FXXU4AUH2",30,"11","atoll","adreno","196610","SM7225","zygote64_32",
                "samsung/a72qxx/a72q:11/RP1A.200720.012/A725FXXU4AUH2:user/release-keys","RP1A.200720.012.A725FXXU4AUH2",
                "a72qxx-user","a72qxx-user","21R3NF12","se.infra","1629849600","2021-08-01","REL","0"),
            "Samsung Galaxy S10+" to DeviceFingerprint(
                "samsung","samsung","SM-G975F","beyond2","beyond2ltexx","exynos9820","beyond2","G975FXXSGHWC1",
                "samsung/beyond2ltexx/beyond2:11/RP1A.200720.012/G975FXXSGHWC1:user/release-keys",
                "RP1A.200720.012","release-keys","user","","G975FXXSGHWC1",30,"11","exynos9820","mali","196610","Exynos9820","zygote64_32",
                "samsung/beyond2ltexx/beyond2:11/RP1A.200720.012/G975FXXSGHWC1:user/release-keys","RP1A.200720.012.G975FXXSGHWC1",
                "beyond2ltexx-user","beyond2ltexx-user","21R3NF12","se.infra","1646236800","2022-03-01","REL","0"),
            "Samsung Galaxy S10" to DeviceFingerprint(
                "samsung","samsung","SM-G973F","beyond1","beyond1ltexx","exynos9820","beyond1","G973FXXSGHWC1",
                "samsung/beyond1ltexx/beyond1:11/RP1A.200720.012/G973FXXSGHWC1:user/release-keys",
                "RP1A.200720.012","release-keys","user","","G973FXXSGHWC1",30,"11","exynos9820","mali","196610","Exynos9820","zygote64_32",
                "samsung/beyond1ltexx/beyond1:11/RP1A.200720.012/G973FXXSGHWC1:user/release-keys","RP1A.200720.012.G973FXXSGHWC1",
                "beyond1ltexx-user","beyond1ltexx-user","21R3NF12","se.infra","1646236800","2022-03-01","REL","0"),
            "Samsung Galaxy Note 10+" to DeviceFingerprint(
                "samsung","samsung","SM-N975F","d2s","d2sxx","exynos9825","d2s","N975FXXS7FUH3",
                "samsung/d2sxx/d2s:11/RP1A.200720.012/N975FXXS7FUH3:user/release-keys",
                "RP1A.200720.012","release-keys","user","","N975FXXS7FUH3",30,"11","exynos9825","mali","196610","Exynos9825","zygote64_32",
                "samsung/d2sxx/d2s:11/RP1A.200720.012/N975FXXS7FUH3:user/release-keys","RP1A.200720.012.N975FXXS7FUH3",
                "d2sxx-user","d2sxx-user","21R3NF12","se.infra","1629849600","2021-08-01","REL","0"),
            "Samsung Galaxy M31" to DeviceFingerprint(
                "samsung","samsung","SM-M315F","m31","m31nsxx","exynos9611","m31","M315FXXU2BUH1",
                "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU2BUH1:user/release-keys",
                "RP1A.200720.012","release-keys","user","","M315FXXU2BUH1",30,"11","exynos9611","mali","196610","Exynos9611","zygote64_32",
                "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU2BUH1:user/release-keys","RP1A.200720.012.M315FXXU2BUH1",
                "m31nsxx-user","m31nsxx-user","21R3NF12","se.infra","1629849600","2021-08-01","REL","0"),

            // --- ONEPLUS (Android 11) ---
            "OnePlus 9 Pro" to DeviceFingerprint(
                "OnePlus","OnePlus","LE2123","OnePlus9Pro","OnePlus9Pro_EEA","qcom","OnePlus9Pro","2109102035",
                "OnePlus/OnePlus9Pro_EEA/OnePlus9Pro:11/RKQ1.201105.002/2109102035:user/release-keys",
                "RKQ1.201105.002","release-keys","user","","2109102035",30,"11","lahaina","adreno","196610","SM8350","zygote64_32",
                "OnePlus/OnePlus9Pro_EEA/OnePlus9Pro:11/RKQ1.201105.002/2109102035:user/release-keys","RKQ1.201105.002",
                "OnePlus9Pro_EEA-user","OnePlus9Pro_EEA-user","OnePlus","OnePlus","1631278800","2021-09-01","REL","0"),
            "OnePlus 9" to DeviceFingerprint(
                "OnePlus","OnePlus","LE2113","OnePlus9","OnePlus9_EEA","qcom","OnePlus9","2109102035",
                "OnePlus/OnePlus9_EEA/OnePlus9:11/RKQ1.201105.002/2109102035:user/release-keys",
                "RKQ1.201105.002","release-keys","user","","2109102035",30,"11","lahaina","adreno","196610","SM8350","zygote64_32",
                "OnePlus/OnePlus9_EEA/OnePlus9:11/RKQ1.201105.002/2109102035:user/release-keys","RKQ1.201105.002",
                "OnePlus9_EEA-user","OnePlus9_EEA-user","OnePlus","OnePlus","1631278800","2021-09-01","REL","0"),
            "OnePlus 8T" to DeviceFingerprint(
                "OnePlus","OnePlus","KB2003","OnePlus8T","OnePlus8T_EEA","qcom","OnePlus8T","2110091915",
                "OnePlus/OnePlus8T_EEA/OnePlus8T:11/RP1A.201005.001/2110091915:user/release-keys",
                "RP1A.201005.001","release-keys","user","","2110091915",30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "OnePlus/OnePlus8T_EEA/OnePlus8T:11/RP1A.201005.001/2110091915:user/release-keys","RP1A.201005.001",
                "OnePlus8T_EEA-user","OnePlus8T_EEA-user","OnePlus","OnePlus","1633784400","2021-10-01","REL","0"),
            "OnePlus 8 Pro" to DeviceFingerprint(
                "OnePlus","OnePlus","IN2023","OnePlus8Pro","OnePlus8Pro_EEA","qcom","OnePlus8Pro","2110091915",
                "OnePlus/OnePlus8Pro_EEA/OnePlus8Pro:11/RP1A.201005.001/2110091915:user/release-keys",
                "RP1A.201005.001","release-keys","user","","2110091915",30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "OnePlus/OnePlus8Pro_EEA/OnePlus8Pro:11/RP1A.201005.001/2110091915:user/release-keys","RP1A.201005.001",
                "OnePlus8Pro_EEA-user","OnePlus8Pro_EEA-user","OnePlus","OnePlus","1633784400","2021-10-01","REL","0"),
            "OnePlus Nord" to DeviceFingerprint(
                "OnePlus","OnePlus","AC2003","Nord","Nord_EEA","qcom","Nord","2108231920",
                "OnePlus/Nord_EEA/Nord:11/RP1A.201005.001/2108231920:user/release-keys",
                "RP1A.201005.001","release-keys","user","","2108231920",30,"11","lito","adreno","196610","SM7250","zygote64_32",
                "OnePlus/Nord_EEA/Nord:11/RP1A.201005.001/2108231920:user/release-keys","RP1A.201005.001",
                "Nord_EEA-user","Nord_EEA-user","OnePlus","OnePlus","1629723600","2021-08-01","REL","0"),

            // --- OTHERS (Android 11) ---
            "Xperia 5 II" to DeviceFingerprint(
                "Sony","Sony","XQ-AS52","XQ-AS52","XQ-AS52_EEA","qcom","XQ-AS52","unknown",
                "Sony/XQ-AS52_EEA/XQ-AS52:11/58.1.A.5.441/058001A005044102927236206:user/release-keys",
                "58.1.A.5.441","release-keys","user","","058001A005044102927236206",30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "Sony/XQ-AS52_EEA/XQ-AS52:11/58.1.A.5.441/058001A005044102927236206:user/release-keys","58.1.A.5.441",
                "XQ-AS52_EEA-user","XQ-AS52_EEA-user","Sony","Sony","1632747600","2021-09-01","REL","0"),
            "Vivo Y53s" to DeviceFingerprint(
                "vivo","vivo","V2058","V2058","V2058","mt6769","V2058","unknown",
                "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V98","compiler08272021",
                30,"11","mt6769","mali","196610","MT6769","zygote64_32",
                "vivo/V2058/V2058:11/RP1A.200720.011/compiler08272021:user/release-keys",
                "RP1A.200720.011","V2058-user","V2058-user","compiler","build","1630022400","2021-08-01","REL","0"),
            "Realme 7 5G" to DeviceFingerprint(
                "realme","realme","RMX2111","RMX2111","RMX2111","mt6853","RMX2111","unknown",
                "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR14A.R3.MP.V62","1626245367375",
                30,"11","mt6853","mali","196610","MT6853","zygote64_32",
                "realme/RMX2111/RMX2111:11/RP1A.200720.011/1626245367375:user/release-keys",
                "RP1A.200720.011","RMX2111-user","RMX2111-user","ubuntu-123","jenkins","1626245367","2021-07-01","REL","0"),
            "Oppo Reno5" to DeviceFingerprint(
                "OPPO","OPPO","CPH2159","OP4E75L1","CPH2159","qcom","OP4E75L1","unknown",
                "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys",
                "RP1A.200720.011","release-keys","user","MPSS.HI.2.0.c4-00165","1622635284",
                30,"11","atoll","adreno","196610","SM7225","zygote64_32",
                "OPPO/CPH2159/OP4E75L1:11/RP1A.200720.011/1622635284:user/release-keys",
                "RP1A.200720.011","CPH2159-user","CPH2159-user","ubuntu","jenkins","1622635284","2021-06-01","REL","0"),
            "Vivo X60" to DeviceFingerprint(
                "vivo","vivo","V2046","V2046","V2046","qcom","V2046","unknown",
                "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys",
                "RP1A.200720.012","release-keys","user","MPSS.HI.2.0.c4-00165","compiler05211516",
                30,"11","kona","adreno","196610","SM8250","zygote64_32",
                "vivo/V2046/V2046:11/RP1A.200720.012/compiler05211516:user/release-keys",
                "RP1A.200720.012","V2046-user","V2046-user","compiler","build","1621584000","2021-05-01","REL","0"),
            "Vivo X70 Pro" to DeviceFingerprint(
                "vivo","vivo","V2105","V2105","V2105","mt6893","V2105","unknown",
                "vivo/V2105/V2105:11/RP1A.200720.012/compiler10212015:user/release-keys",
                "RP1A.200720.012","release-keys","user","MOLY.LR12A.R3.MP.V98","compiler10212015",
                30,"11","mt6893","mali","196610","MT6893","zygote64_32",
                "vivo/V2105/V2105:11/RP1A.200720.012/compiler10212015:user/release-keys",
                "RP1A.200720.012","V2105-user","V2105-user","compiler","build","1634860800","2021-10-01","REL","0"),
            "Realme 8 Pro" to DeviceFingerprint(
                "realme","realme","RMX3081","RMX3081","RMX3081","qcom","RMX3081","unknown",
                "realme/RMX3081/RMX3081:11/RP1A.200720.011/1626245367375:user/release-keys",
                "RP1A.200720.011","release-keys","user","MPSS.HI.2.0.c4-00165","1626245367375",
                30,"11","atoll","adreno","196610","SM7125","zygote64_32",
                "realme/RMX3081/RMX3081:11/RP1A.200720.011/1626245367375:user/release-keys",
                "RP1A.200720.011","RMX3081-user","RMX3081-user","ubuntu-123","jenkins","1626245367","2021-07-01","REL","0"),
            "Asus Zenfone 8" to DeviceFingerprint(
                "asus","asus","ASUS_I006D","ASUS_I006D","WW_I006D","qcom","sake","unknown",
                "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                "RKQ1.201112.002","release-keys","user","M3.13.24.51-Sake_0000100","30.11.51.115",
                30,"11","lahaina","adreno","196610","SM8350","zygote64_32",
                "asus/WW_I006D/ASUS_I006D:11/RKQ1.201112.002/30.11.51.115:user/release-keys",
                "RKQ1.201112.002.30.11.51.115","WW_I006D-user","WW_I006D-user","android-build","jenkins","1629859200","2021-08-01","REL","0")
        )
    }

    data class DeviceFingerprint(
        val manufacturer: String, val brand: String, val model: String, val device: String,
        val product: String, val hardware: String, val board: String, val bootloader: String,
        val fingerprint: String, val buildId: String, val tags: String, val type: String,
        val radioVersion: String, val incremental: String, val sdkInt: Int, val release: String,
        val boardPlatform: String, val eglDriver: String, val openGlEs: String,
        val hardwareChipname: String, val zygote: String, val vendorFingerprint: String,
        val display: String, val buildDescription: String, val buildFlavor: String,
        val buildHost: String, val buildUser: String, val buildDateUtc: String,
        val securityPatch: String, val buildVersionCodename: String,
        val buildVersionPreviewSdk: String
    )

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.vortex") {
            hookModuleStatus(lpparam)
            return
        }

        if (lpparam.packageName == "android" ||
            lpparam.packageName.startsWith("com.android.")) {
            return
        }

        try {
            val originalTime = OriginalBuildValues.ORIGINAL_BUILD_TIME

            val prefs = XSharedPreferences("com.vortex", PREFS_NAME)
            prefs.reload()

            fun getEncryptedString(key: String, def: String): String {
                val raw = prefs.getString(key, null)
                return CryptoUtils.decrypt(raw) ?: def
            }

            val profileName = getEncryptedString("profile", "Redmi 9")
            val fingerprint = getDeviceFingerprint(profileName)

            initializeCache(prefs, ::getEncryptedString, fingerprint.model)

            hookBuildFields(lpparam, fingerprint)
            hookSystemProperties(lpparam, fingerprint)
            hookTelephonyManager(lpparam)
            hookUnifiedSettingsSecure(lpparam)
            hookNetworkInterfaces(lpparam)
            hookWifiInfo(lpparam)
            hookMediaDrm(lpparam)
            hookLocation(lpparam, prefs, ::getEncryptedString)
            hookWebView(lpparam, fingerprint)
            hookAccountManager(lpparam)

            hookPackageManager(lpparam)
            hookApplicationFlags(lpparam)
            hookFile()
            hookProcessBuilderAndRuntime()

        } catch (e: Throwable) {
            try {
                if (BuildConfig.DEBUG) XposedBridge.log("Vortex Error: ${e.message}")
            } catch (ex: Throwable) {}
        }
    }

    private fun hookModuleStatus(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.vortex.StatusFragment",
                lpparam.classLoader,
                "isModuleActive",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
        } catch (e: Throwable) {}
    }

    private fun hookFile() {
        try {
            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result == false) return
                    val file = param.thisObject as File
                    val path = file.absolutePath
                    if (path.length < 5) return

                    if (!path.contains("su")        && !path.contains("magisk")    &&
                        !path.contains("xposed")    && !path.contains("busybox")   &&
                        !path.contains("supersu")   && !path.contains("frida")     &&
                        !path.contains("substrate") && !path.contains("riru")      &&
                        !path.contains("zygisk"))    return
                    if (SpoofingUtils.isSensitivePath(path)) {
                        param.result = false
                    }
                }
            }
            XposedHelpers.findAndHookMethod(File::class.java, "exists",      hook)
            XposedHelpers.findAndHookMethod(File::class.java, "canRead",     hook)
            XposedHelpers.findAndHookMethod(File::class.java, "canExecute",  hook)
            XposedHelpers.findAndHookMethod(File::class.java, "isFile",      hook)
            XposedHelpers.findAndHookMethod(File::class.java, "isDirectory", hook)
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("File hook error: ${e.message}")
        }
    }

    private fun createDummyProcess(): Process = object : Process() {
        override fun getOutputStream() = object : java.io.OutputStream() {
            override fun write(b: Int) = Unit
        }
        override fun getInputStream()  = java.io.ByteArrayInputStream(ByteArray(0))
        override fun getErrorStream()  = java.io.ByteArrayInputStream(ByteArray(0))
        override fun waitFor()         = 1
        override fun exitValue()       = 1
        override fun destroy()         = Unit
    }

    private fun shouldBlockCommand(cmd: String): Boolean =
        cmd.contains("magisk",              ignoreCase = true)  ||
        cmd.contains("busybox",             ignoreCase = true)  ||
        cmd.contains("frida-server",        ignoreCase = true)  ||
        cmd.contains("zygisk",              ignoreCase = true)  ||
        cmd.contains("getenforce")                              ||
        cmd.contains("/system/xbin/su")                         ||
        cmd.contains("/sbin/su")                                ||
        cmd.contains("/system/bin/su")                          ||
        cmd.contains("getprop ro.build.tags")                   ||
        cmd.contains("getprop ro.debuggable")                   ||
        cmd.contains("getprop ro.secure")                       ||
        cmd.trim() == "su"                                      ||
        cmd.startsWith("su ")

    private fun hookProcessBuilderAndRuntime() {
        try {
            val runtimeHook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val cmd = when (val arg = param.args[0]) {
                        is String     -> arg
                        is Array<*>   -> arg.joinToString(" ")
                        else          -> return
                    }
                    if (shouldBlockCommand(cmd)) {
                        param.result = createDummyProcess()
                        if (BuildConfig.DEBUG) XposedBridge.log("Vortex: Blocked Runtime.exec: $cmd")
                    }
                }
            }

            XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", String::class.java, runtimeHook)
            try { XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", Array<String>::class.java, runtimeHook) } catch (e: Throwable) {}
            try { XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", String::class.java, Array<String>::class.java, runtimeHook) } catch (e: Throwable) {}
            try { XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", String::class.java, Array<String>::class.java, File::class.java, runtimeHook) } catch (e: Throwable) {}
            try { XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", Array<String>::class.java, Array<String>::class.java, File::class.java, runtimeHook) } catch (e: Throwable) {}

            XposedHelpers.findAndHookMethod(
                ProcessBuilder::class.java, "start",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val command = (param.thisObject as ProcessBuilder).command().joinToString(" ")
                        if (shouldBlockCommand(command)) {
                            param.result = createDummyProcess()
                            if (BuildConfig.DEBUG) XposedBridge.log("Vortex: Blocked ProcessBuilder: $command")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("ProcessBuilder/Runtime hook error: ${e.message}")
        }
    }

    private fun initializeCache(
        prefs: XSharedPreferences,
        getString: (String, String) -> String,
        modelName: String
    ) {
        if (cachedAndroidId == null) cachedAndroidId = getString("android_id", SpoofingUtils.generateRandomId(16))

        val seed = (cachedAndroidId ?: "default").hashCode().toLong()
        val seededRandom = Random(seed)

        val rawMccMnc = prefs.getString("mcc_mnc", null)
        val mccMnc: String = if (rawMccMnc != null) {
            CryptoUtils.decrypt(rawMccMnc) ?: rawMccMnc
        } else {
            US_CARRIERS[Math.abs(seededRandom.nextInt()) % US_CARRIERS.size].mccMnc
        }

        if (cachedImei  == null) cachedImei  = getString("imei",  SpoofingUtils.generateValidImei(modelName))
        if (cachedImei2 == null) cachedImei2 = getString("imei2", SpoofingUtils.generateValidImei(modelName))

        if (cachedImsi == null) cachedImsi = getString("imsi", SpoofingUtils.generateValidImsi(mccMnc))

        if (cachedIccid       == null) cachedIccid       = getString("iccid",          SpoofingUtils.generateValidIccid(mccMnc))
        if (cachedGsfId       == null) cachedGsfId       = getString("gsf_id",        SpoofingUtils.generateRandomId(16))
        if (cachedGaid        == null) cachedGaid        = getString("gaid",           SpoofingUtils.generateRandomGaid())
        if (cachedWifiMac     == null) cachedWifiMac     = getString("wifi_mac",       SpoofingUtils.generateRandomMac())
        if (cachedBtMac       == null) cachedBtMac       = getString("bluetooth_mac",  SpoofingUtils.generateRandomMac())
        if (cachedGmail       == null) cachedGmail       = getString("gmail",          SpoofingUtils.generateRealisticGmail())
        if (cachedSerial      == null) cachedSerial      = getString("serial",         SpoofingUtils.generateRandomSerial())

        if (cachedSsaidSnapchat == null) cachedSsaidSnapchat = getString("ssaid_snapchat", SpoofingUtils.generateRandomId(16))
        if (cachedMediaDrmId    == null) cachedMediaDrmId    = getString("media_drm_id",   SpoofingUtils.generateRandomId(32))
        if (cachedWifiSsid      == null) cachedWifiSsid      = getString("wifi_ssid",      "Vortex-5G")
        if (cachedWifiBssid     == null) cachedWifiBssid     = getString("wifi_bssid",     SpoofingUtils.generateRandomMac())

        if (cachedPhoneNumber == null) {
            val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS.first()
            cachedPhoneNumber = SpoofingUtils.generatePhoneNumber(carrier.npas)
        }
    }

    private fun extractMccMnc(imsi: String): String {
        if (imsi.length < 5) return "310260"
        val mccInt = imsi.take(3).toIntOrNull() ?: return imsi.take(6)
        val mncLength = when (mccInt) {
            in 310..316 -> 3
            302         -> 3
            else        -> 2
        }
        return imsi.take(3 + mncLength)
    }

    private fun getDeviceFingerprint(profileName: String): DeviceFingerprint {
        val cleanName = profileName.replace(Regex(" - Android \\d+"), "").trim()
        val fp = DEVICE_FINGERPRINTS.entries.find { it.key == cleanName }?.value
        if (fp == null && BuildConfig.DEBUG) {
            XposedBridge.log("Vortex: Perfil desconocido '$profileName', fallback Redmi 9")
        }
        return fp
            ?: DEVICE_FINGERPRINTS.values.firstOrNull()
            ?: DeviceFingerprint(
                "Xiaomi","Redmi","Redmi 9","lancelot","lancelot_global","mt6768","lancelot","unknown",
                "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                "RP1A.200720.011","release-keys","user","MOLY.LR12A.R3.MP.V84.P47","V12.5.3.0.RJCMIXM",
                30,"11","mt6768","mali","196610","MT6768","zygote64_32",
                "Redmi/lancelot_global/lancelot:11/RP1A.200720.011/V12.5.3.0.RJCMIXM:user/release-keys",
                "V12.5.3.0.RJCMIXM","lancelot_global-user","lancelot_global-user",
                "pangu","builder","1632960000","2021-09-01","REL","0")
    }

    private fun hookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
        try {
            val buildClass = Build::class.java
            XposedHelpers.setStaticObjectField(buildClass, "MANUFACTURER", fingerprint.manufacturer)
            XposedHelpers.setStaticObjectField(buildClass, "BRAND",        fingerprint.brand)
            XposedHelpers.setStaticObjectField(buildClass, "MODEL",        fingerprint.model)
            XposedHelpers.setStaticObjectField(buildClass, "DEVICE",       fingerprint.device)
            XposedHelpers.setStaticObjectField(buildClass, "PRODUCT",      fingerprint.product)
            XposedHelpers.setStaticObjectField(buildClass, "HARDWARE",     fingerprint.hardware)
            XposedHelpers.setStaticObjectField(buildClass, "BOARD",        fingerprint.board)
            XposedHelpers.setStaticObjectField(buildClass, "BOOTLOADER",   fingerprint.bootloader)
            XposedHelpers.setStaticObjectField(buildClass, "FINGERPRINT",  fingerprint.fingerprint)
            XposedHelpers.setStaticObjectField(buildClass, "ID",           fingerprint.buildId)
            XposedHelpers.setStaticObjectField(buildClass, "TAGS",         fingerprint.tags)
            XposedHelpers.setStaticObjectField(buildClass, "TYPE",         fingerprint.type)
            XposedHelpers.setStaticObjectField(buildClass, "DISPLAY",      fingerprint.display)
            XposedHelpers.setStaticObjectField(buildClass, "HOST",         fingerprint.buildHost)
            XposedHelpers.setStaticObjectField(buildClass, "USER",         fingerprint.buildUser)
            XposedHelpers.setStaticLongField(  buildClass, "TIME",         fingerprint.buildDateUtc.toLong() * 1000)

            if (lpparam.appInfo != null && lpparam.appInfo.targetSdkVersion < 29) {
                XposedHelpers.setStaticObjectField(buildClass, "SERIAL", cachedSerial)
            }

            val versionClass = Build.VERSION::class.java
            XposedHelpers.setStaticIntField(   versionClass, "SDK_INT",       fingerprint.sdkInt)
            XposedHelpers.setStaticObjectField(versionClass, "RELEASE",       fingerprint.release)
            XposedHelpers.setStaticObjectField(versionClass, "INCREMENTAL",   fingerprint.incremental)
            XposedHelpers.setStaticObjectField(versionClass, "SECURITY_PATCH",fingerprint.securityPatch)
            XposedHelpers.setStaticObjectField(versionClass, "CODENAME",      fingerprint.buildVersionCodename)
            XposedHelpers.setStaticIntField(   versionClass, "PREVIEW_SDK_INT",fingerprint.buildVersionPreviewSdk.toInt())

            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) { param.result = cachedSerial }
                    }
                )
            } catch (e: NoSuchMethodError) {}

            // FIX #21: Hook Build.getFingerprintedPartitions (Android 11+)
            try {
                XposedHelpers.findAndHookMethod(Build::class.java, "getFingerprintedPartitions",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val partitions = param.result as? List<*> ?: return
                            val newPartitions = ArrayList<Any>()
                            for (p in partitions) {
                                // Clone partition object if possible or just modify field?
                                // Partition is hidden class. Easier to set fields?
                                // We cannot easily clone hidden classes without reflection instantiation.
                                // Instead, we iterate and set "mFingerprint" field on existing objects.
                                try {
                                    XposedHelpers.setObjectField(p, "mFingerprint", fingerprint.fingerprint)
                                } catch (e: Throwable) {}
                                newPartitions.add(p!!)
                            }
                            param.result = newPartitions
                        }
                    }
                )
            } catch (e: Throwable) {}

        } catch (e: Throwable) {}
    }

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
        try {
            val sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader)
            val hook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    val res = when (key) {
                        "ro.product.manufacturer"              -> fingerprint.manufacturer
                        "ro.product.brand"                     -> fingerprint.brand
                        "ro.product.model"                     -> fingerprint.model
                        "ro.product.device"                    -> fingerprint.device
                        "ro.product.name"                      -> fingerprint.product
                        "ro.hardware"                          -> fingerprint.hardware
                        "ro.product.board"                     -> fingerprint.board
                        "ro.bootloader"                        -> fingerprint.bootloader
                        "ro.build.fingerprint"                 -> fingerprint.fingerprint
                        "ro.build.id"                          -> fingerprint.buildId
                        "ro.build.tags"                        -> fingerprint.tags
                        "ro.build.type"                        -> fingerprint.type
                        "gsm.version.baseband"                 -> fingerprint.radioVersion
                        "ro.serialno"                          -> cachedSerial
                        "ro.build.version.release"             -> fingerprint.release
                        "ro.build.version.sdk"                 -> fingerprint.sdkInt.toString()
                        "ro.debuggable"                        -> "0"
                        "ro.secure"                            -> "1"
                        "ro.build.display.id"                  -> fingerprint.display
                        "ro.build.description"                 -> fingerprint.buildDescription
                        "ro.build.characteristics"             -> "default"
                        "ro.build.flavor"                      -> fingerprint.buildFlavor
                        "ro.vendor.build.fingerprint"          -> fingerprint.vendorFingerprint
                        "ro.board.platform"                    -> fingerprint.boardPlatform
                        "ro.hardware.egl"                      -> fingerprint.eglDriver
                        "ro.opengles.version"                  -> fingerprint.openGlEs
                        "ro.hardware.chipname"                 -> fingerprint.hardwareChipname
                        "ro.zygote"                            -> fingerprint.zygote
                        "ro.build.host"                        -> fingerprint.buildHost
                        "ro.build.user"                        -> fingerprint.buildUser
                        "ro.build.date.utc"                    -> fingerprint.buildDateUtc
                        "ro.build.version.security_patch"      -> fingerprint.securityPatch
                        "ro.build.version.codename"            -> fingerprint.buildVersionCodename
                        "ro.build.version.preview_sdk"         -> fingerprint.buildVersionPreviewSdk
                        "ro.product.system.manufacturer"       -> fingerprint.manufacturer
                        "ro.product.system.brand"              -> fingerprint.brand
                        "ro.product.system.model"              -> fingerprint.model
                        "ro.product.system.device"             -> fingerprint.device
                        "ro.product.system.name"               -> fingerprint.product
                        "ro.product.vendor.manufacturer"       -> fingerprint.manufacturer
                        "ro.product.vendor.brand"              -> fingerprint.brand
                        "ro.product.vendor.model"              -> fingerprint.model
                        "ro.product.vendor.device"             -> fingerprint.device
                        "ro.product.vendor.name"               -> fingerprint.product
                        "ro.product.odm.manufacturer"          -> fingerprint.manufacturer
                        "ro.product.odm.brand"                 -> fingerprint.brand
                        "ro.product.odm.model"                 -> fingerprint.model
                        "ro.product.odm.device"                -> fingerprint.device
                        "ro.product.odm.name"                  -> fingerprint.product
                        "ro.product.product.manufacturer"      -> fingerprint.manufacturer
                        "ro.product.product.brand"             -> fingerprint.brand
                        "ro.product.product.model"             -> fingerprint.model
                        "ro.product.product.device"            -> fingerprint.device
                        "ro.product.product.name"              -> fingerprint.product
                        "ro.build.version.all_codenames"       -> fingerprint.buildVersionCodename
                        "ro.build.version.min_supported_target_sdk" -> "23"
                        "ro.kernel.qemu"                       -> "0"
                        "persist.sys.usb.config"               -> "none"
                        "service.adb.root"                     -> "0"
                        "ro.boot.serialno"                     -> cachedSerial
                        "ro.boot.hardware"                     -> fingerprint.hardware
                        "ro.boot.bootloader"                   -> fingerprint.bootloader
                        "ro.boot.verifiedbootstate"            -> "green"
                        "ro.boot.flash.locked"                 -> "1"
                        "ro.boot.vbmeta.device_state"          -> "locked"
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

            XposedHelpers.findAndHookMethod(tmClass, "getDeviceId", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedImei } })
            try { XposedHelpers.findAndHookMethod(tmClass, "getDeviceId", Int::class.javaPrimitiveType, object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = if ((p.args[0] as Int) == 0) cachedImei else cachedImei2 } }) } catch (e: NoSuchMethodError) {}
            try {
                XposedHelpers.findAndHookMethod(tmClass, "getImei", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedImei } })
                XposedHelpers.findAndHookMethod(tmClass, "getImei", Int::class.javaPrimitiveType, object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = if ((p.args[0] as Int) == 0) cachedImei else cachedImei2 } })
            } catch (e: NoSuchMethodError) {}

            XposedHelpers.findAndHookMethod(tmClass, "getSubscriberId",  object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedImsi } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimSerialNumber", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedIccid } })
            XposedHelpers.findAndHookMethod(tmClass, "getLine1Number",   object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedPhoneNumber } })

            val mccMnc  = if (cachedImsi != null) extractMccMnc(cachedImsi!!) else "310260"
            val carrier = US_CARRIERS.find { it.mccMnc == mccMnc } ?: US_CARRIERS.first()

            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperator",     object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperator",         object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mccMnc } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkOperatorName", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimOperatorName",     object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = carrier.name } })
            XposedHelpers.findAndHookMethod(tmClass, "getNetworkCountryIso",   object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = "us" } })
            XposedHelpers.findAndHookMethod(tmClass, "getSimCountryIso",       object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = "us" } })
        } catch (e: Throwable) {}
    }

    private fun hookUnifiedSettingsSecure(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cls = XposedHelpers.findClass("android.provider.Settings\$Secure", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(cls, "getString",
                ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[1] as String) {
                            Settings.Secure.ANDROID_ID  -> {
                                if (lpparam.packageName == "com.snapchat.android" && cachedSsaidSnapchat != null) {
                                    param.result = cachedSsaidSnapchat
                                } else {
                                    param.result = cachedAndroidId
                                }
                            }
                            "advertising_id"            -> param.result = cachedGaid
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
                        val ni   = param.thisObject as NetworkInterface
                        val name = ni.name ?: return
                        when {
                            name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("swlan") ->
                                param.result = macStringToBytes(cachedWifiMac ?: SpoofingUtils.generateRandomMac())
                            name.startsWith("bt") || name.startsWith("bluetooth") ->
                                param.result = macStringToBytes(cachedBtMac ?: SpoofingUtils.generateRandomMac())
                            name.startsWith("p2p") || name.startsWith("eth") ->
                                param.result = macStringToBytes(SpoofingUtils.generateRandomMac())
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) XposedBridge.log("Vortex: Error spoofing network interface: ${e.message}")
                    }
                }
            })
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("Vortex: Error hooking NetworkInterface: ${e.message}")
        }
    }

    private fun hookLocation(
        lpparam: XC_LoadPackage.LoadPackageParam,
        prefs: XSharedPreferences,
        getString: (String, String) -> String
    ) {
        try {
            mockLocationEnabled = prefs.getBoolean("mock_location_enabled", false)
            mockLatitude  = (getString("mock_latitude",  "0.0")).toDoubleOrNull() ?: 0.0
            mockLongitude = (getString("mock_longitude", "0.0")).toDoubleOrNull() ?: 0.0
            mockAltitude  = (getString("mock_altitude",  "0.0")).toDoubleOrNull() ?: 0.0
            mockAccuracy  = (getString("mock_accuracy",  "10.0")).toFloatOrNull() ?: 10.0f

            if (!mockLocationEnabled) return

            val random      = Random()
            val applyJitter = prefs.getBoolean("location_jitter_enabled", true)
            val isMoving    = prefs.getBoolean("location_is_moving", false)

            if (applyJitter) {
                mockAccuracy   = (mockAccuracy + (random.nextGaussian() * 2.0)).toFloat().coerceAtLeast(1.0f)
                mockAltitude  += random.nextGaussian() * 0.5
                mockLatitude  += random.nextGaussian() * 0.00001
                mockLongitude += random.nextGaussian() * 0.00001
            }

            mockBearing = if (isMoving) random.nextFloat() * 360.0f else 0.0f
            mockSpeed   = if (isMoving) random.nextFloat() * 5.0f   else 0.0f

            val locationClass = XposedHelpers.findClass("android.location.Location", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(locationClass, "getLatitude",  object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockLatitude  } })
            XposedHelpers.findAndHookMethod(locationClass, "getLongitude", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockLongitude } })
            XposedHelpers.findAndHookMethod(locationClass, "getAltitude",  object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockAltitude  } })
            XposedHelpers.findAndHookMethod(locationClass, "hasAltitude",  object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = true } })
            XposedHelpers.findAndHookMethod(locationClass, "getAccuracy",  object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockAccuracy  } })
            XposedHelpers.findAndHookMethod(locationClass, "getBearing",   object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockBearing   } })
            XposedHelpers.findAndHookMethod(locationClass, "hasBearing",   object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = true } })
            XposedHelpers.findAndHookMethod(locationClass, "getSpeed",     object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = mockSpeed     } })
            XposedHelpers.findAndHookMethod(locationClass, "hasSpeed",     object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = true } })

            try {
                XposedHelpers.findAndHookMethod(locationClass, "isFromMockProvider",
                    object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = false } })
            } catch (e: NoSuchMethodError) {}

            val lmClass = XposedHelpers.findClass("android.location.LocationManager", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(lmClass, "getLastKnownLocation", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val loc = Location(param.args[0] as String).apply {
                            latitude              = mockLatitude
                            longitude             = mockLongitude
                            altitude              = mockAltitude
                            accuracy              = mockAccuracy
                            bearing               = mockBearing
                            speed                 = mockSpeed
                            time                  = System.currentTimeMillis()
                            elapsedRealtimeNanos  = SystemClock.elapsedRealtimeNanos()
                        }
                        try {
                            Location::class.java.getDeclaredMethod(
                                "setIsFromMockProvider", Boolean::class.javaPrimitiveType
                            ).apply { isAccessible = true }.invoke(loc, false)
                        } catch (e: Exception) {}
                        param.result = loc
                    }
                }
            )
        } catch (e: Throwable) {}
    }

    private fun hookWebView(lpparam: XC_LoadPackage.LoadPackageParam, fingerprint: DeviceFingerprint) {
        try {
            val wsClass = XposedHelpers.findClass("android.webkit.WebSettings", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(wsClass, "getUserAgentString", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val original = param.result as? String ?: return
                    var ua = original.replace(
                        Regex(";\\s+[^;]+?\\s+Build/"),
                        "; ${fingerprint.model} Build/"
                    )
                    ua = ua.replace(Regex("Android\\s+[\\d.]+"), "Android ${fingerprint.release}")
                    param.result = ua
                }
            })
        } catch (e: Throwable) {}
    }

    private fun hookAccountManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val amClass = XposedHelpers.findClass("android.accounts.AccountManager", lpparam.classLoader)

            XposedHelpers.findAndHookMethod(amClass, "getAccounts", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val gmail = cachedGmail ?: return
                    val accounts = param.result as Array<Account>
                    val result = accounts.filter { it.type != "com.google" }.toMutableList()
                    result.add(Account(gmail, "com.google"))
                    param.result = result.toTypedArray()
                }
            })

            XposedHelpers.findAndHookMethod(amClass, "getAccountsByType", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if ((param.args[0] as String) != "com.google") return
                        val gmail = cachedGmail
                        param.result = if (gmail != null) arrayOf(Account(gmail, "com.google"))
                                       else emptyArray()
                    }
                }
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("AccountManager hook error: ${e.message}")
        }
    }

    private fun hookPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", lpparam.classLoader,
                "getInstallerPackageName", String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = "com.android.vending"
                    }
                }
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("PackageManager legacy hook error: ${e.message}")
        }

        try {
            val isiClass = XposedHelpers.findClass("android.content.pm.InstallSourceInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(isiClass, "getInstallingPackageName",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) { p.result = "com.android.vending" }
                }
            )
            try {
                XposedHelpers.findAndHookMethod(isiClass, "getInitiatingPackageName",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) { p.result = "com.android.vending" }
                    }
                )
            } catch (e: Throwable) {}
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("PackageManager getInstallSourceInfo hook error: ${e.message}")
        }
    }

    private fun hookApplicationFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val appInfoHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val ai = param.result as? ApplicationInfo ?: return
                    ai.flags = ai.flags and 0x100.inv() and 0x2.inv()
                }
            }

            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", lpparam.classLoader,
                "getApplicationInfo", String::class.java, Int::class.javaPrimitiveType,
                appInfoHook
            )

            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", lpparam.classLoader,
                "getPackageInfo", String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val pi = param.result as? PackageInfo ?: return
                        pi.applicationInfo?.let { ai ->
                            ai.flags = ai.flags and 0x100.inv() and 0x2.inv()
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) XposedBridge.log("ApplicationFlags hook error: ${e.message}")
        }
    }

    private fun hookWifiInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wifiInfoClass = XposedHelpers.findClass("android.net.wifi.WifiInfo", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = "\"$cachedWifiSsid\"" } })
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getBSSID", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedWifiBssid } })
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getMacAddress", object : XC_MethodHook() { override fun afterHookedMethod(p: MethodHookParam) { p.result = cachedWifiMac } })
        } catch (e: Throwable) {}
    }

    private fun hookMediaDrm(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val drmClass = XposedHelpers.findClass("android.media.MediaDrm", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(drmClass, "getPropertyByteArray", String::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.args[0] == "deviceUniqueId" && cachedMediaDrmId != null) {
                         param.result = hexStringToByteArray(cachedMediaDrmId!!)
                    }
                }
            })
        } catch (e: Throwable) {}
    }

    private fun macStringToBytes(mac: String): ByteArray {
        return try {
            val parts = mac.split(":")
            if (parts.size != 6) throw IllegalArgumentException()
            ByteArray(6) { i -> parts[i].toInt(16).toByte() }
        } catch (e: Exception) { byteArrayOf(0x02, 0x00, 0x00, 0x00, 0x00, 0x00) }
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
