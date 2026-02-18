package com.lancelot.utils

object OriginalBuildValues {
    val ORIGINAL_BUILD_TIME: Long by lazy {
        try {
            // Capturamos el valor real ANTES de cualquier hook
            android.os.Build.TIME
        } catch (e: Exception) {
            System.currentTimeMillis() // fallback seguro
        }
    }
}
