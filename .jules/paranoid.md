# Paranoid Journal 🥷

This journal documents critical learnings about detection evasion, specifically tailored for the Redmi 9 Lancelot device.

## 2024-05-23 - File Existence Hook Missing
**Learning:** The application was missing hooks for `java.io.File.exists()` and related methods. While system properties and package manager were hooked, direct file system checks for root binaries (e.g., `/system/bin/su`, `/sbin/su`) and Xposed/Magisk artifacts were not intercepted. This is a primary detection vector for anti-cheat systems like Argos.
**Action:** Implement a comprehensive `hookFile` function in `MainHook.kt` that intercepts `exists`, `canRead`, `canExecute`, `isFile`, and `isDirectory` for `java.io.File`, returning `false` for a curated list of sensitive paths and keywords (su, magisk, xposed).
