-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

-keep class com.vortex.MainHook { *; }
-keep class com.vortex.MainHook$Companion { *; }
-keepclassmembers class com.vortex.MainHook$DeviceFingerprint { *; }
-keepclassmembers class com.vortex.MainHook$UsCarrier { *; }
-keep class com.vortex.MainHook$* { *; }
-keep class com.vortex.** { *; }
-keepclassmembers class com.vortex.** { *; }

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-dontwarn android.os.SystemProperties
-dontwarn android.app.ActivityThread
-dontwarn android.content.pm.InstallSourceInfo
-dontwarn android.telephony.**
-dontwarn kotlinx.**

-repackageclasses 'v'
