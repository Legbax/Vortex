# Julia.md - Agent Context & Changelog

## Entry 001 - Initial Context & Architecture Analysis
**Date:** 2025-05-22
**Agent:** Jules

### Project Overview
Vortex is an advanced Android 11 (API 30) Device Spoofing Xposed module written in Kotlin. It is designed to pass high-security checks (Snapchat, Banking apps) by spoofing device fingerprints, identifiers, and network information.

### ⚠️ Critical Environment Specifications
**This application is strictly designed for and tested on the following environment:**
*   **Device:** Redmi 9 (Lancelot)
*   **ROM:** MIUI 12.5.6.0.RJCMIXM (Android 11)
*   **Root Solution:** KernelSU Next
*   **fs/Mount Hiding:** SusFS
*   **Keybox/Attestation:** Tricky Store + Custom Keybox (STRONG INTEGRITY required)
*   **Detection Evasion:** Shamiko + PIF Next

**Note:** The module relies on this kernel-level stack for hardware attestation (Play Integrity). The Java-level hooks in this module focus on application-layer consistency and specific bypasses (SSL Pinning).

### Architecture
*   **Core:** `MainHook.kt` is the entry point (implements `IXposedHookLoadPackage`).
*   **Spoofing:**
    *   `DeviceFingerprint.kt`: Contains ~40 high-fidelity device profiles.
    *   `SpoofingUtils.kt`: Generates valid IDs (Luhn-valid IMEI, real OUIs, carrier-specific IMSI).
*   **Security:**
    *   `CryptoUtils.kt`: AES-GCM encryption for `SharedPreferences` to prevent forensic analysis.
    *   `PrefsManager.kt`: Manages encrypted preferences.
*   **Bypasses:**
    *   **SSL Pinning:** Specific hooks for `com.snapchat.android` targeting `CertificatePinner`, `X509TrustManager`, and `SSLContext`.
    *   **Root Hiding:** Blocks `su` execution and hides sensitive paths (`/data/adb`) via `File` and `Runtime` hooks.

### External Resources & Research
*   SSL Pinning & Argos Analysis: Based on research by [Riyad Mondol](https://github.com/riyadmondol2006).
*   The module implements logic similar to known Frida scripts for SSL bypassing.

### Next Steps for Future Agents
*   Maintain the strict environment constraints when testing or debugging.
*   Ensure any new hooks do not conflict with the KernelSU/SusFS stack.
*   Updates to `MainHook.kt` should preserve the encryption mechanism for preferences.
