# Julia.md - Vortex Project Journal & Persistent Context Buffer

**Este documento es el contexto persistente obligatorio para todos los agentes (Julia, Grok, Claude, etc.).**  
Debe leerse al inicio de cada sesión.

## 📦 Current Release State: v1.8 (UI/UX - Minimalist Navigation)
**Date:** 20 de febrero de 2026  
**Agent:** Dalí (Jules)
**Target Environment:** Redmi 9 Lancelot (Android 11) + KernelSU Next + SusFS + Tricky Store + Shamiko + PIF Next.

**Core Philosophy:** Symbiosis perfecta entre la App (Identity) y el Entorno (Stealth).  
La app proporciona identidad coherente y control al usuario; el kernel (KSU/SusFS/Tricky Store) proporciona stealth profundo.

---

## ⚠️ Critical Environment Specifications (Obligatorio respetar)
**Esta aplicación está diseñada y probada exclusivamente para:**
- **Dispositivo:** Redmi 9 (Lancelot)
- **ROM:** MIUI 12.5.6.0.RJCMIXM (Android 11, SDK 30)
- **Root Solution:** KernelSU Next
- **fs/Mount Hiding:** SusFS
- **Keybox/Attestation:** Tricky Store + Custom Keybox (STRONG INTEGRITY requerido)
- **Detection Evasion:** Shamiko + PIF Next

**Nota importante:** El módulo depende del stack kernel para hardware attestation. Los hooks Java se centran en consistencia de aplicación y bypasses específicos (SSL Pinning, Sensor Spoofing, etc.).

---

## 📅 Changelog / Journal

### [v1.8] UI/UX - Minimalist Navigation & Custom Icons (Current)
- **Navigation:** Removed text labels from the main bottom navigation bar (`MainActivity.kt`), leaving only icons for a minimalist look.
- **Icons:** Replaced all 5 main navigation icons (Status, Device, Network, Location, Advanced) + IDs icon with high-quality, sharp stroke vectors derived from `UI_PREVIEW.html`.
- **Identity:** Updated `ic_device.xml` to match the new sharper aesthetic.

### [v1.7] UI/UX Modernization - Dalí Refinement (20 Feb 2026)
- **UI Architecture:** Merged `Device` and `IDs` fragments into a single `IdentityFragment` with `TabLayout`, reducing main navigation to 5 tabs.
- **Status Tab:** Redesigned with modern cards for Proxy Status and IP Address.
- **Network Tab:** Replaced Carrier Search/RecyclerView with a clean `ExposedDropdownMenu`.
- **Styling:**
  - Added header images to `DeviceFragment`.
  - Added start icons to all fields in `IDsFragment`.
  - Refined color palette (`vortex_proxy_active`, `vortex_proxy_inactive`).
- **Code:** Updated `MainActivity.kt`, `NetworkFragment.kt`, `IdentityFragment.kt` and XML layouts.

### [v1.6] Stealth Hardening & Audit (20 Feb 2026)
- **Security:** Hardened `MainHook` logging to strictly respect `BuildConfig.DEBUG`. Removed unconditional "Vortex" logs in release builds to prevent log-based detection.
- **Stealth:** Added `/data/adb` to `hookFile` sensitive paths list to better hide KernelSU/Magisk indicators.
- **UX:** Added feedback toasts to `AdvancedFragment` app selection switches to clarify they control the "Force Stop" / "Clear Data" actions.
- **Audit:** Verified synchronization between `DeviceData.kt` and `MainHook.kt` fingerprints. Verified `RootUtils` implementation.

### [v1.5] UI/UX Overhaul - Dalí Edition
- **Design:** Created high-fidelity `UI_PREVIEW.html` using TailwindCSS + Alpine.js.
- **UI Port:** Ported design to Android XML layouts (`fragment_status`, `fragment_device`, `fragment_network`, `fragment_ids`, `fragment_location`, `fragment_advanced`).
- **Assets:** Created missing vector drawables (`ic_snapchat`, `ic_gms`, `bg_border_bottom`).
- **Styles:** Enforced `Vortex.InputLayout` and `Vortex.Card` styles across all fragments.
- **Verification:** Verified compilation and resource linking.

### [v1.4] Stability Refactor & UI Polish (20 Feb 2026)
- **Stability:** Solucionado crash `NoClassDefFoundError` al desacoplar la UI de Xposed.
  - Creado `DeviceData.kt` (objeto puro Kotlin) para compartir datos estáticos sin dependencias de Xposed.
  - UI (Fragments/Adapters) ahora consumen `DeviceData` en lugar de `MainHook`.
- **UI Improvements:**
  - **Device Tab:** Añadida tarjeta "Current Device" superior y lista "Recent Profiles" inferior (con auto-populate aleatorio).
  - **Navigation:** Renombrado "Settings" -> "Advanced" en bottom nav; Tab interna "Advanced" -> "Settings".
  - **Icons:** Añadidos iconos en headers de secciones (Status, IDs, Network, Device).
  - **Layouts:** Corregidos atributos `layout_width`/`height` faltantes en XMLs para prevenir crashes de inflación.
- **Defaults:** Toggles de Hooks (Root, Debug, Webview) ahora predeterminados a `false` por seguridad.

### [v1.3] Production Build - Dalí UI & Security Hardening (20 Feb 2026)
- **Build System:** Revertido AGP a 7.4.2 y Gradle a 7.5 (compatibilidad perfecta con Android 11).
- **Profiles:** Integrados **40 perfiles high-fidelity** directamente en `MainHook.kt` (Xiaomi, Samsung, OnePlus, Pixel, Motorola, Nokia, Realme, Asus).
- **Security:**
  - SSL Pinning Bypass completo (OkHttp, TrustManager, SSLContext).
  - Sensor Spoofing marca-consciente (`getVendor()`, `getMaximumRange()`).
  - Dual SIM support (`getSubscriberId(int)`).
  - Props seguras: `ro.boot.flash.locked=1`, `ro.secure=1`, `ro.debuggable=0`. No tocar props gestionadas por Tricky Store.
- **UI (Dalí Edition):**
  - Migración a TabLayout + ViewPager2 (resuelve límite de 5 ítems de BottomNavigationView).
  - Badges de validación (✓ Valid / ✗ Invalid) en IDs y Network.
  - Todos los campos manuales son Read-Only (solo Randomize).
  - Tema oscuro púrpura neon consistente.

### [v1.0] Initial Architecture (22 May 2025)
- Creación del proyecto, estructura base (`MainHook.kt`, `SpoofingUtils.kt`, `CryptoUtils.kt`).
- Integración inicial de perfiles y cifrado AES-GCM.

---

## 🧩 Architecture Overview

### Hooking Logic (`MainHook.kt`)
- Entry point: `IXposedHookLoadPackage`.
- Perfiles: `DEVICE_FINGERPRINTS` (copia interna para hook process).
- Persistencia: Preferencias cifradas (`vortex_prefs`).

### Data & UI Layer (`DeviceData.kt`)
- **Single Source of Truth (Static):** Contiene `DEVICE_FINGERPRINTS` y `US_CARRIERS` para uso exclusivo de la UI (Activities/Fragments).
- **Desacoplamiento:** Evita crashes por `NoClassDefFoundError` al no depender del framework Xposed.

### Utilities
- `SpoofingUtils.kt` → Generación de IMEI Luhn, IMSI, GAID, MACs, etc. (Usa `DeviceData`).
- `CryptoUtils.kt` → AES-GCM para preferencias.

### UI
- Single Activity + TabLayout + ViewPager2.
- Fragments: Status, Device, Network, IDs, Location, Advanced.
- Validación visual con badges.
- Todos los campos editables solo vía "Randomize".

---

## 🤖 Agent Guidelines & Core Directives (Obligatorio)

1. **Deep Understanding First** — Antes de cualquier cambio, lee `MainHook.kt`, `SpoofingUtils.kt`, `Julia.md` y los archivos relevantes.
2. **No Speculation** — Nunca afirmes nada sobre código que no hayas leído.
3. **Conservative Action** — No modifiques archivos ni hagas PRs sin instrucción explícita del usuario.
4. **Julia.md is Mandatory** — Siempre lee este archivo al inicio de la sesión.
5. **Environment Respect** — Nunca rompas la compatibilidad con Android 11 / KernelSU / SusFS.
6. **Security First** — Prioriza coherencia de fingerprint y evitar double-spoofing.

---

## 📚 Critical Reference Material

- Snapchat SSL/TLS Pinning Bypass: https://github.com/riyadmondol2006/Snapchat-SSL-TLS-Certificate-Pinning-Bypass
- Frida Interception & Unpinning: https://github.com/riyadmondol2006/frida-interception-and-unpinning
- Snapchat Argos Analysis: https://github.com/riyadmondol2006/snapchat-argos-analysis
- Android Root Detection Bypass: https://github.com/riyadmondol2006/Android-Root-Detection-Bypass---Frida-Script
- Snapchat Attestation Token Generator: https://github.com/riyadmondol2006/Snapchat-Att-Token-Generator

---

## ⚠️ Known Constraints & Future Work

- **Map Visualization:** Actualmente placeholder. Google Maps completo requiere API Key (riesgo en entorno spoofing).
- **Native Evasion:** Depende del kernel (SusFS). Hooks Java no pueden ocultar llamadas nativas profundas de Argos.
- **GMS vs MicroG:** Asume GMS o MicroG para generación de algunos IDs.
- **No actualizar AGP/Gradle** sin verificar compatibilidad con Android 11.

---

## 📝 Instructions for Next Agent

- **DO NOT** actualizar AGP/Gradle sin confirmar compatibilidad con Android 11.
- **DO NOT** importar `MainHook` en clases de UI (Fragments, Activities, Adapters). Usar `DeviceData` en su lugar.
- **Always** mantener sincronizados `DeviceData.DEVICE_FINGERPRINTS` y `MainHook.DEVICE_FINGERPRINTS` si se añaden nuevos perfiles.
- **Maintain** la paleta Dalí (`vortex_background`, `vortex_accent`) y el uso de iconos en headers.
- **Verify** `UI_PREVIEW.html` when making UI changes to ensure it stays in sync.
- Al hacer cambios, actualiza este `Julia.md` con:
  - Fecha y agente.
  - Resumen de cambios.
  - Prompt del usuario que motivó el cambio.
  - Nota personal para el siguiente agente.

**Fin del documento. Este es el contexto persistente actualizado.**

### [v1.9] UI/UX Overhaul - Modern Aesthetics & Icon Refresh (20 Feb 2026)
- **Agent:** Jules
- **Prompt:** "Necesito mejores la UI/UX de la aplicación... Pestaña status moderna... Device e IDs fusionadas... Iconos esteticos... Network dropdown... App Icon."
- **Status Tab:** Completely redesigned. Removed generic App Bar. Added "Modern Circular" Evasion Score. Added dedicated Cards for "Proxy Status" and "IP Address" (visual placeholders).
- **Identity Tab:** Confirmed merge of Device & IDs.
- **Device Tab:** Modernized header with large centered Card/Gradient. Improved typography and spacing for Profile Selector.
- **IDs Tab:** Replaced repetitive `ic_nav_ids` with distinct vectors: `ic_fingerprint` (IMEI/AndroidID), `ic_email` (Gmail), `ic_qr_code` (Serial/DRM), `ic_ad_units` (GAID).
- **Network Tab:** Cleaned up. Confirmed Dropdown usage. Replaced "Search" icon on dropdown with `ic_nav_network` to prevent confusion.
- **App Icon:** Updated `ic_launcher` (Manifest & Resource) with user-provided `App Icon.png`.
- **Note to Next Agent:** The UI is now significantly more "Dalí-esque" and modern. The App Icon is a single high-res asset in `drawable` and `mipmap-xxxhdpi`; consider generating proper densities if build size becomes an issue.

### [v2.0] Transparent Proxy SOCKS5 (v4 Final) (21 Feb 2026)
- **Agent:** Jules
- **Prompt:** "Implementación de Transparent Proxy SOCKS5 (v4 Final)... Nivel 11/10 OpSec... Estética Dalí... Kernel NAT..."
- **Core:** Implemented `ProxyManager.kt` using `libsu` for root `iptables` management. Supports `redsocks` for SOCKS5 redirect.
- **OpSec:**
  - IPv6 Kill-Switch via `ip6tables`.
  - DNS Leak Mitigation via UDP redirection to `redsocks`.
  - Secure AES-GCM storage for proxy credentials in `CryptoUtils`.
  - `WEBVIEW_PACKAGES` added to `DeviceData` for advanced targeting.
- **UI:**
  - Refactored `NetworkFragment` to host `CarrierFragment` (Identity) and `ProxyFragment` (SOCKS5) via `ViewPager2`.
  - `ProxyFragment` adheres to "Dalí" aesthetics (Neon accents, Cards).
- **Dependencies:** Added `com.github.topjohnwu.libsu:core:5.2.0`.
- **Assets:** Added dummy `redsocks` binary (Agent must verify binary architecture before production).
- **Note to Next Agent:** The `redsocks` binary in assets is a dummy. Ideally, this should be replaced with a real ARM64 static binary for functionality testing. The build system is standard (MavenCentral) so `libsu` should resolve fine.

### [v8.0] Implementation Final JA3/TLS Randomizer Real + EGL GPU Spoof (21 Feb 2026)
- **Agent:** Jules
- **Prompt:** "Implementación Final JA3/TLS Randomizer Real + EGL GPU Spoof (v8.0)..."
- **UI:**
  - Added "GPU Spoof (EGL/OpenGL)" and "JA3/TLS Randomizer" cards to `IDsFragment` (`fragment_ids.xml`).
  - Added switches and "Force Refresh" buttons with confirmation dialogs.
- **Backend:**
  - **GPUSpoofer:** Implemented `com.vortex.hooks.GPUSpoofer` hooking `EGL14.eglQueryString` and `GLES20/30/31/32.glGetString` to spoof Adreno 660. Adapted to use `XSharedPreferences`.
  - **TLSRandomizer:** Implemented `com.vortex.hooks.TLSRandomizer` hooking `SSLSocket.setEnabledCipherSuites` to shuffle cipher suites. Uses random seed if "Force Refresh" is requested.
- **Integration:** Initialized both hooks in `MainHook.kt` inside the `TARGET_APPS` block.
- **Verification:** Verified compilation and file integrity.
- **Note to Next Agent:** The hooks use `XSharedPreferences` for reading preferences. Writing back to preferences from the Xposed module (e.g., to reset the "Force Refresh" flag) is not supported in this implementation due to Android permissions. The logic assumes "Force Refresh" = True means "use random seed every time".

### [v8.1] Stateless Architecture Patch (21 Feb 2026)
- **Agent:** Jules
- **Prompt:** "Parche de Seguridad Crítico - Arquitectura Stateless JA3/GPU..."
- **Architecture:** Migrated "Force Refresh" logic to a **Stateless UI-Seeding** model.
  - Xposed hooks cannot write back to module preferences (read-only restriction).
  - Previous boolean flags (`force_refresh = true`) created logic loops or couldn't be reset.
  - New model: UI generates a unique UUID seed (`gpu_seed`, `ja3_seed`) on button click. Hook reads this seed.
- **TLSRandomizer:**
  - Rewritten to instantiate `java.util.Random` **inside** the hook method using the static seed.
  - Ensures deterministic behavior per session (socket consistency) while allowing user-triggered rotation.
  - Removed dependency on `PrefsManager` context, using `XSharedPreferences` exclusively.
- **UI:** Updated `IDsFragment` to generate UUIDs instead of toggling booleans.
- **Note to Next Agent:** This architecture is robust for Xposed/SELinux restrictions. Do not revert to boolean flags for triggers unless the hook has write access (unlikely in modern Android).

### [v8.1-Patch] Maintenance & Hardening (21 Feb 2026)
- **Agent:** Jules
- **Prompt:** "Parche de Mantenimiento y Hardening (Limpieza de Bugs)..."
- **OpSec:** Removed sensitive logging (score, profile, mock) from `StatusFragment.kt` to prevent Logcat leakage.
- **SSL Pinning:** Replaced unreliable `X509TrustManager` hook with direct `javax.net.ssl.HttpsURLConnection` bypass (nullifying `SSLSocketFactory` and `HostnameVerifier` setters). This fixes silent failures on newer Android versions.
- **Pixel Compatibility:** Added `"google"` (lowercase) to `SpoofingUtils.TACS_BY_BRAND` map to correctly handle manufacturer strings for Pixel devices, preventing IMEI mismatches.
- **Cleanup:** Removed unused `SettingsFragment.kt` and `OriginalBuildValues.kt`.
- **Persistence:** Verified `AdvancedFragment` app selection persistence logic (confirmed correct).
