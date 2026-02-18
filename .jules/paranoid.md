## 2024-05-22 - Command Evasion Bypass
**Learning:** `SpoofingUtils.isSensitiveCommand` previously only checked for exact matches of commands in `SENSITIVE_COMMANDS` (Set). Commands wrapped in `sh -c` or with arguments were bypassing the check if they weren't in the specific `contains` block.
**Action:** Always verify command matching logic handles substrings and wrapped commands. Added comprehensive `contains` checks for sensitive properties.

## 2024-05-22 - Lancelot Bootloader Leak
**Learning:** Lancelot devices with unlocked bootloaders leak `androidboot.verifiedbootstate=orange` in `/proc/cmdline`. This is readable by apps even if `Build` props are spoofed.
**Action:** Blocked access to `/proc/cmdline` and `getprop ro.boot.verifiedbootstate`.

## 2024-05-22 - Build.SERIAL Behavior Leak
**Learning:** Forcing `Build.SERIAL` field to a value on apps targeting Android 10+ (API 29+) is a major detection vector. The system framework guarantees this field is "unknown" for privacy. A value other than "unknown" indicates tampering or an old environment, causing a behavior mismatch with the spoofed `SDK_INT=30`.
**Action:** Modified `MainHook` to only set `Build.SERIAL` for apps with `targetSdkVersion < 29`. Modern apps will see the expected "unknown" (or system default) and must use `getSerial()` (which is hooked).

## 2024-05-22 - GMS Self-Check & Stability
**Learning:** Hooking stack traces or detection methods inside `com.google.android.gms` (Google Play Services) is a high-risk activity. GMS often performs internal integrity self-checks or sensitive operations that fail when hooks introduce overhead or stack anomalies. This can trigger SafetyNet/Play Integrity failures even if the device is otherwise clean.
**Action:** Exempted `com.google.android.gms` from Xposed detection hooks in `MainHook.kt` to improve system stability and reduce self-incrimination risk.
