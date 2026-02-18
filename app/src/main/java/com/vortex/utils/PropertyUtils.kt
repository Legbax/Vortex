package com.vortex.utils

object PropertyUtils {

    fun getSpoofedProperty(key: String, fingerprint: DeviceFingerprint, serial: String?): String? {
        return when (key) {
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
            "ro.serialno"                          -> serial
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

            // System Partition
            "ro.product.system.manufacturer"       -> fingerprint.manufacturer
            "ro.product.system.brand"              -> fingerprint.brand
            "ro.product.system.model"              -> fingerprint.model
            "ro.product.system.device"             -> fingerprint.device
            "ro.product.system.name"               -> fingerprint.product

            // Vendor Partition
            "ro.product.vendor.manufacturer"       -> fingerprint.manufacturer
            "ro.product.vendor.brand"              -> fingerprint.brand
            "ro.product.vendor.model"              -> fingerprint.model
            "ro.product.vendor.device"             -> fingerprint.device
            "ro.product.vendor.name"               -> fingerprint.product

            // ODM Partition
            "ro.product.odm.manufacturer"          -> fingerprint.manufacturer
            "ro.product.odm.brand"                 -> fingerprint.brand
            "ro.product.odm.model"                 -> fingerprint.model
            "ro.product.odm.device"                -> fingerprint.device
            "ro.product.odm.name"                  -> fingerprint.product

            // Product Partition
            "ro.product.product.manufacturer"      -> fingerprint.manufacturer
            "ro.product.product.brand"             -> fingerprint.brand
            "ro.product.product.model"             -> fingerprint.model
            "ro.product.product.device"            -> fingerprint.device
            "ro.product.product.name"              -> fingerprint.product

            // System Ext Partition (Added for Lancelot/Android 11 consistency)
            "ro.product.system_ext.manufacturer"   -> fingerprint.manufacturer
            "ro.product.system_ext.brand"          -> fingerprint.brand
            "ro.product.system_ext.model"          -> fingerprint.model
            "ro.product.system_ext.device"         -> fingerprint.device
            "ro.product.system_ext.name"           -> fingerprint.product

            // Additional Consistency Checks
            "ro.build.version.incremental"         -> fingerprint.incremental
            "ro.bootimage.build.fingerprint"       -> fingerprint.vendorFingerprint
            "ro.odm.build.fingerprint"             -> fingerprint.vendorFingerprint
            "ro.system_ext.build.fingerprint"      -> fingerprint.fingerprint

            "ro.build.version.all_codenames"       -> fingerprint.buildVersionCodename
            "ro.build.version.min_supported_target_sdk" -> "23"
            "ro.kernel.qemu"                       -> "0"
            "persist.sys.usb.config"               -> "none"
            "service.adb.root"                     -> "0"
            "ro.boot.serialno"                     -> serial
            "ro.boot.hardware"                     -> fingerprint.hardware
            "ro.boot.bootloader"                   -> fingerprint.bootloader
            "ro.boot.verifiedbootstate"            -> "green"
            "ro.boot.flash.locked"                 -> "1"
            "ro.boot.vbmeta.device_state"          -> "locked"
            else -> null
        }
    }
}
