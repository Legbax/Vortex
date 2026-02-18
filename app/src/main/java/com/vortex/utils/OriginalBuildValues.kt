package com.vortex.utils

object OriginalBuildValues {
    val ORIGINAL_BUILD_TIME: Long by lazy {
        try {
            android.os.Build.TIME
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
