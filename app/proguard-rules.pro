# ================================================================
# Vortex Xposed Module — ProGuard Rules
# ================================================================

# --- Xposed Framework API ---
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# --- Punto de entrada del módulo ---
-keep class com.vortex.MainHook { *; }

# FIX #7: Companion object debe mantenerse por nombre; Xposed accede a campos
# estáticos del Companion via reflexión y el nombre original es obligatorio.
-keep class com.vortex.MainHook$Companion { *; }

# FIX #18: UsCarrier es accedido por nombre en tiempo de ejecución.
-keepclassmembers class com.vortex.MainHook$UsCarrier { *; }

# FIX #18 (Update): DeviceFingerprint moved to utils, must be kept for reflection.
-keep class com.vortex.utils.DeviceFingerprint { *; }

# Todas las inner classes de MainHook
-keep class com.vortex.MainHook$* { *; }

# --- Clases del módulo completas ---
-keep class com.vortex.** { *; }
-keepclassmembers class com.vortex.** { *; }

# --- Clases utilitarias accedidas por nombre ---
-keep class com.vortex.SpoofingUtils    { *; }
-keep class com.vortex.SpoofingUtils$*  { *; }
-keep class com.vortex.utils.**         { *; }
-keep class com.vortex.utils.CryptoUtils       { *; }
-keep class com.vortex.utils.OriginalBuildValues { *; }
-keep class com.vortex.BuildConfig      { *; }

# --- Metadatos Kotlin (necesario para data classes y reflexión) ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- Suppress warnings para APIs internas de Android ---
-dontwarn android.os.SystemProperties
-dontwarn android.app.ActivityThread
-dontwarn android.content.pm.InstallSourceInfo
-dontwarn android.telephony.**
-dontwarn kotlinx.**

# FIX #17: Una sola directiva -repackageclasses (el original tenía dos contradictorias).
# Nota: no reempaquetar clases que Xposed necesita encontrar por nombre canónico.
-repackageclasses 'com.vortex.r'
