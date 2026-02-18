# ================================================================
# Lancelot Xposed Module — ProGuard Rules
# ================================================================

# --- Xposed Framework API ---
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# --- Punto de entrada del módulo ---
-keep class com.lancelot.MainHook { *; }

# FIX #7: Companion object debe mantenerse por nombre; Xposed accede a campos
# estáticos del Companion via reflexión y el nombre original es obligatorio.
-keep class com.lancelot.MainHook$Companion { *; }

# FIX #18: Los campos de DeviceFingerprint y UsCarrier son accedidos por nombre
# en tiempo de ejecución (data class Kotlin). Sin esto ProGuard los renombra
# y los accesos via reflexión fallan silenciosamente.
-keepclassmembers class com.lancelot.MainHook$DeviceFingerprint { *; }
-keepclassmembers class com.lancelot.MainHook$UsCarrier { *; }

# Todas las inner classes de MainHook
-keep class com.lancelot.MainHook$* { *; }

# --- Clases del módulo completas ---
-keep class com.lancelot.** { *; }
-keepclassmembers class com.lancelot.** { *; }

# --- Clases utilitarias accedidas por nombre ---
-keep class com.lancelot.SpoofingUtils    { *; }
-keep class com.lancelot.SpoofingUtils$*  { *; }
-keep class com.lancelot.utils.**         { *; }
-keep class com.lancelot.utils.CryptoUtils       { *; }
-keep class com.lancelot.utils.OriginalBuildValues { *; }
-keep class com.lancelot.BuildConfig      { *; }

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
-repackageclasses 'com.lancelot.r'
