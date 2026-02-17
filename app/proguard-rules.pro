# FIX #12: ProGuard mejorado para anti-análisis

# Ofuscación agresiva
-dontskipnonpubliclibraryclassmembers
-overloadaggressively
-repackageclasses ''
-allowaccessmodification

# NO conservar nombres (FIX #12)
-dontkeepnames

# Mantener solo lo esencial para Xposed
-keep class de.robv.android.xposed.** { *; }
-keep class com.lancelot.MainHook { *; }
-keep class com.lancelot.MainActivity { *; }

# Ofuscar campos de DeviceFingerprint
-keepclassmembers class com.lancelot.MainHook$DeviceFingerprint {
    <init>(...);
}

# Optimizaciones
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# Remover logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Ofuscar strings sensibles
-adaptclassstrings com.lancelot.**

# Renombrar paquetes
-repackageclasses 'o'

# Mantener ViewBinding
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static ** inflate(android.view.LayoutInflater);
}

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Kotlin
-keep class kotlin.** { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Metadata { *; }
