package com.vortex.utils

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
