package com.vortex

object DeviceData {

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

    data class GPUProfile(val renderer: String, val vendor: String)

    fun getGPUProfile(profileName: String): GPUProfile {
        val fp = DEVICE_FINGERPRINTS[profileName] ?: DEVICE_FINGERPRINTS["Redmi 9"]!!
        val platform = fp.boardPlatform.lowercase()
        val hardware = fp.hardware.lowercase()

        // Lógica replicada de GPUSpoofer.kt para la UI
        return when {
            platform == "lahaina" -> GPUProfile("Adreno (TM) 660", "Qualcomm")
            platform == "kona" -> GPUProfile("Adreno (TM) 650", "Qualcomm")
            platform == "msmnile" -> GPUProfile("Adreno (TM) 640", "Qualcomm")
            platform == "lito" || platform == "holi" -> GPUProfile("Adreno (TM) 620", "Qualcomm")
            platform == "atoll" || hardware.contains("sm7150") -> GPUProfile("Adreno (TM) 618", "Qualcomm")
            platform == "bengal" || platform == "trinket" -> GPUProfile("Adreno (TM) 610", "Qualcomm")
            platform.contains("mt6833") || hardware == "mt6833" -> GPUProfile("Mali-G57 MC2", "ARM")
            platform.contains("mt6768") || platform.contains("mt6769") || hardware == "mt6768" -> GPUProfile("Mali-G52 MC2", "ARM")
            platform.contains("mt6785") || hardware == "mt6785" -> GPUProfile("Mali-G76 MC4", "ARM")
            platform == "exynos9610" -> GPUProfile("Mali-G72 MP3", "ARM")
            platform == "exynos850" -> GPUProfile("Mali-G52", "ARM")
            platform == "exynos9825" -> GPUProfile("Mali-G76 MP12", "ARM")
            else -> GPUProfile("Adreno (TM) 660", "Qualcomm") // Default fallback
        }
    }

    val DEVICE_FINGERPRINTS = mapOf(

        // =========================================================
        // GRUPO 1: XIAOMI / POCO / REDMI (12 dispositivos)
        // =========================================================
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

        "Redmi 10X 4G" to DeviceFingerprint(
            manufacturer = "Xiaomi", brand = "Redmi", model = "M2004J7AC",
            device = "merlin", product = "merlin",
            hardware = "mt6769", board = "merlin", bootloader = "unknown",
            fingerprint = "Redmi/merlin/merlin:11/RP1A.200720.011/V12.5.3.0.QJJCNXM:user/release-keys",
            buildId = "RP1A.200720.011", tags = "release-keys", type = "user",
            radioVersion = "MOLY.LR12A.R3.MP.V110.6",
            incremental = "V12.5.3.0.QJJCNXM", sdkInt = 30, release = "11",
            boardPlatform = "mt6769", eglDriver = "mali", openGlEs = "196610",
            hardwareChipname = "MT6769", zygote = "zygote64_32",
            vendorFingerprint = "Redmi/merlin/merlin:11/RP1A.200720.011/V12.5.3.0.QJJCNXM:user/release-keys",
            display = "RP1A.200720.011",
            buildDescription = "merlin-user 11 RP1A.200720.011 V12.5.3.0.QJJCNXM release-keys",
            buildFlavor = "merlin-user",
            buildHost = "pangu-build-component-system-175411", buildUser = "builder",
            buildDateUtc = "1622505600", securityPatch = "2021-06-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        "POCO F3" to DeviceFingerprint(
            manufacturer = "Xiaomi", brand = "POCO", model = "M2012K11AG",
            device = "alioth", product = "alioth_global",
            hardware = "qcom", board = "alioth", bootloader = "unknown",
            fingerprint = "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.5.0.RKHMIXM:user/release-keys",
            buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
            radioVersion = "MPSS.HI.3.2.c1.1-00085-SM8250_GEN_PACK-1",
            incremental = "V12.5.5.0.RKHMIXM", sdkInt = 30, release = "11",
            boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
            hardwareChipname = "SM8250", zygote = "zygote64_32",
            vendorFingerprint = "POCO/alioth_global/alioth:11/RKQ1.200826.002/V12.5.5.0.RKHMIXM:user/release-keys",
            display = "RKQ1.200826.002",
            buildDescription = "alioth_global-user 11 RKQ1.200826.002 V12.5.5.0.RKHMIXM release-keys",
            buildFlavor = "alioth_global-user",
            buildHost = "c3-miui-ota-bd88", buildUser = "builder",
            buildDateUtc = "1630454400", securityPatch = "2021-09-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        // =========================================================
        // GRUPO 2: SAMSUNG (10 dispositivos) - CORREGIDOS
        // =========================================================
        "Galaxy A52" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A525F",
            device = "a52x", product = "a52xnsxx",
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

        "Galaxy A72" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A725F",
            device = "a72", product = "a72nsxx",
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

        "Galaxy A32 5G" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A326B",
            device = "a32x", product = "a32xnsxx",
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

        "Galaxy A51" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A515F",
            device = "a51", product = "a51nsxx",
            hardware = "exynos9610", board = "exynos9610", bootloader = "unknown",
            fingerprint = "samsung/a51nsxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
            buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
            radioVersion = "",
            incremental = "A515FXXU4CUG1", sdkInt = 30, release = "11",
            boardPlatform = "exynos9610", eglDriver = "mali", openGlEs = "196610",
            hardwareChipname = "Exynos9611", zygote = "zygote64_32",
            vendorFingerprint = "samsung/a51nsxx/a51:11/RP1A.200720.012/A515FXXU4CUG1:user/release-keys",
            display = "RP1A.200720.012",
            buildDescription = "a51nsxx-user 11 RP1A.200720.012 A515FXXU4CUG1 release-keys",
            buildFlavor = "a51nsxx-user",
            buildHost = "21R3NF12", buildUser = "dpi",
            buildDateUtc = "1625097600", securityPatch = "2021-07-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        "Galaxy M31" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-M315F",
            device = "m31", product = "m31nsxx",
            hardware = "exynos850", board = "m31", bootloader = "unknown",
            fingerprint = "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU4CUG1:user/release-keys",
            buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
            radioVersion = "",
            incremental = "M315FXXU4CUG1", sdkInt = 30, release = "11",
            boardPlatform = "exynos850", eglDriver = "mali", openGlEs = "196610",
            hardwareChipname = "S5E3830", zygote = "zygote64_32",
            vendorFingerprint = "samsung/m31nsxx/m31:11/RP1A.200720.012/M315FXXU4CUG1:user/release-keys",
            display = "RP1A.200720.012",
            buildDescription = "m31nsxx-user 11 RP1A.200720.012 M315FXXU4CUG1 release-keys",
            buildFlavor = "m31nsxx-user",
            buildHost = "21R3NF12", buildUser = "dpi",
            buildDateUtc = "1636934400", securityPatch = "2021-11-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        "Galaxy A12" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A125F",
            device = "a12", product = "a12nsxx",
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

        "Galaxy A21s" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A217F",
            device = "a21s", product = "a21snsxx",
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
            buildDateUtc = "1625097600", securityPatch = "2021-07-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        "Galaxy A31" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-A315F",
            device = "a31", product = "a31nsxx",
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

        "Galaxy F62" to DeviceFingerprint(
            manufacturer = "samsung", brand = "samsung", model = "SM-E625F",
            device = "e1q", product = "e1qnsxx",
            hardware = "exynos9825", board = "exynos9825", bootloader = "unknown",
            fingerprint = "samsung/e1qnsxx/e1q:11/RP1A.200720.012/E625FXXU2BUG1:user/release-keys",
            buildId = "RP1A.200720.012", tags = "release-keys", type = "user",
            radioVersion = "",
            incremental = "E625FXXU2BUG1", sdkInt = 30, release = "11",
            boardPlatform = "exynos9825", eglDriver = "mali", openGlEs = "196610",
            hardwareChipname = "exynos9825", zygote = "zygote64_32",
            vendorFingerprint = "samsung/e1qnsxx/e1q:11/RP1A.200720.012/E625FXXU2BUG1:user/release-keys",
            display = "RP1A.200720.012",
            buildDescription = "e1qnsxx-user 11 RP1A.200720.012 E625FXXU2BUG1 release-keys",
            buildFlavor = "e1qnsxx-user",
            buildHost = "SWDD5830", buildUser = "dpi",
            buildDateUtc = "1625097600", securityPatch = "2021-07-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

        // =========================================================
        // GRUPO 3: ONEPLUS (4 dispositivos)
        // =========================================================
        "OnePlus 8T" to DeviceFingerprint(
            manufacturer = "OnePlus", brand = "OnePlus", model = "KB2001",
            device = "kebab", product = "kebab_EEA",
            hardware = "qcom", board = "kona", bootloader = "unknown",
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

        "OnePlus 8" to DeviceFingerprint(
            manufacturer = "OnePlus", brand = "OnePlus", model = "IN2013",
            device = "instantnoodle", product = "instantnoodle_EEA",
            hardware = "qcom", board = "kona", bootloader = "unknown",
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
        // GRUPO 4: GOOGLE PIXEL (4 dispositivos)
        // =========================================================
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

        "Pixel 4a 5G" to DeviceFingerprint(
            manufacturer = "Google", brand = "google", model = "Pixel 4a (5G)",
            device = "bramble", product = "bramble",
            hardware = "bramble", board = "bramble", bootloader = "b2-0.3-7214727",
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
        // GRUPO 5: MOTOROLA (4 dispositivos)
        // =========================================================
        "Moto G Power 2021" to DeviceFingerprint(
            manufacturer = "motorola", brand = "motorola", model = "moto g power (2021)",
            device = "borneo", product = "borneo",
            hardware = "qcom", board = "bengal", bootloader = "unknown",
            fingerprint = "motorola/borneo/borneo:11/RRQ31.Q3-47-22/2b4fae:user/release-keys",
            buildId = "RRQ31.Q3-47-22", tags = "release-keys", type = "user",
            radioVersion = "MPSS.AT.4.0-00055-SM6115_GEN_PACK-1",
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
        // GRUPO 6: NOKIA (2 dispositivos)
        // =========================================================
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
        // GRUPO 7: REALME (2 dispositivos)
        // =========================================================
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
        // GRUPO 8: OTROS (2 dispositivos)
        // =========================================================
        "ASUS ZenFone 7" to DeviceFingerprint(
            manufacturer = "asus", brand = "asus", model = "ASUS_I002D",
            device = "ASUS_I002D", product = "WW_I002D",
            hardware = "qcom", board = "kona", bootloader = "unknown",
            fingerprint = "asus/WW_I002D/ASUS_I002D:11/RKQ1.200826.002/18.0840.2101.26-0:user/release-keys",
            buildId = "RKQ1.200826.002", tags = "release-keys", type = "user",
            radioVersion = "M3.13.24.51-Sake_0000100",
            incremental = "18.0840.2101.26-0", sdkInt = 30, release = "11",
            boardPlatform = "kona", eglDriver = "adreno", openGlEs = "196610",
            hardwareChipname = "SM8250-AB", zygote = "zygote64_32",
            vendorFingerprint = "asus/WW_I002D/ASUS_I002D:11/RKQ1.200826.002/18.0840.2101.26-0:user/release-keys",
            display = "RKQ1.200826.002",
            buildDescription = "WW_I002D-user 11 RKQ1.200826.002 18.0840.2101.26-0 release-keys",
            buildFlavor = "WW_I002D-user",
            buildHost = "android-build", buildUser = "jenkins",
            buildDateUtc = "1629859200", securityPatch = "2021-08-01",
            buildVersionCodename = "REL", buildVersionPreviewSdk = "0"
        ),

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

    // TARGET_APPS puro (Objetivos principales de Vortex)
    val TARGET_APPS = listOf(
        "com.snapchat.android",
        "com.snapchat.android.beta"
    )

    // WEBVIEW_PACKAGES (Proveedores del sistema para interceptar renderers aislados u0_iXXX)
    val WEBVIEW_PACKAGES = listOf(
        "com.google.android.webview",
        "com.android.chrome",
        "com.android.webview"
    )
}
