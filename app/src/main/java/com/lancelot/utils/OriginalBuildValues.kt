package com.lancelot.utils

object OriginalBuildValues {
    val ORIGINAL_BUILD_TIME: Long by lazy {
        try {
            // Read the original value BEFORE any hooks modify it
            android.os.Build.TIME
        } catch (e: Exception) {
            0L // Safe fallback
        }
    }
}
