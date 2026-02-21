# Julia.md - Vortex Project Journal & Persistent Context Buffer

**Este documento es el contexto persistente obligatorio para todos los agentes (Julia, Grok, Claude, etc.).**  
Debe leerse al inicio de cada sesión.

## 📦 Current Release State: v1.7 (UI/UX Modernization)
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

### [v1.7] UI/UX Modernization - Dalí Refinement (Current)
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
