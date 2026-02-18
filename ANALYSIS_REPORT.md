# Reporte de Análisis: Lancelot vs. Snapchat Detection & UI

## 1. Análisis de Detección (Snapchat/SafetyNet)

### 🔴 Riesgos Críticos Detectados
1.  **Falta de Hooks en File System (`/system/xbin/su`, `/sbin/su`):**
    *   Snapchat y SafetyNet verifican la existencia de binarios `su`. Actualmente `MainHook.kt` **no intercepta** `File.exists()` ni `Runtime.exec()`.
    *   **Impacto:** Detección inmediata de Root si el usuario no tiene MagiskHide/Zygisk configurado perfectamente.
2.  **`sys.usb.config` incompleto:**
    *   Se hookea `persist.sys.usb.config` -> "none", pero falta `sys.usb.config` (propiedad activa) y `sys.usb.state`.
    *   Si `sys.usb.state` contiene "adb", es bandera roja.
3.  **Tags de Build:**
    *   Los perfiles tienen `tags = "release-keys"`, lo cual es correcto.
    *   Sin embargo, si el sistema real reporta `test-keys` en otros lugares no hookeados (ej. `ro.build.tags` en `/default.prop`), puede haber mismatch.
    *   *Nota:* Nuestro hook de `SystemProperties` cubre `ro.build.tags`, lo cual mitiga esto.

### 🟡 Riesgos Medios
1.  **Inconsistencia de Bootloader:**
    *   Hookeamos `ro.boot.verifiedbootstate` -> "green", pero si una app lee `/proc/cmdline` directamente, podría ver el estado real. Xposed no puede hookear `/proc` fácilmente sin ser detectado.

## 2. Análisis de UI (Discrepancia VortexUI)

### 🔴 Problemas de Estructura
1.  **Layout Plano vs Tabs:**
    *   **Actual:** `LinearLayout` vertical simple con todos los campos amontonados.
    *   **Requerido:** División en pestañas "Status", "Device", "Network", "IDs", "Location", "Advanced" como en el diseño VortexUI aprobado.
    *   **Impacto:** Mala UX y no cumple con la "división" solicitada explícitamente.

### 🔴 Problemas de Randomización
1.  **Botón Único "Randomize Field":**
    *   Actualmente hay un `Spinner` + `Button` ("Randomize Field").
    *   **Requerimiento:** "Un botón para randomizar cada valor por separado".
    *   **Falta:** Botones pequeños (ícono de dado/refresh) al lado de **cada** campo (IMEI, ICCID, Gmail, etc.) para acceso rápido, no un dropdown lento.

## 3. Plan de Acción Propuesto

### A. Mejoras de Stealth (MainHook.kt)
1.  **Agregar Hooks de File:** Interceptar `File.exists` para ocultar `/system/bin/su`, `/system/xbin/su`, `/sbin/su`.
2.  **Reforzar Props USB:** Hookear `sys.usb.config` y `sys.usb.state` -> "mtp" o "none".

### B. Reescritura Total de UI (VortexUI Nativo)
1.  **Navegación:** Implementar `BottomNavigationView` con los 5 fragmentos/vistas (Status, Device, Network, IDs, Location).
2.  **Diseño:** Implementar el estilo "Cyberpunk/Glass" definido en `UI_REDESIGN_PREVIEW.md`.
3.  **Botones Individuales:**
    *   En la vista "IDs", poner un botón `[Random]` al lado del `EditText` de IMEI.
    *   En la vista "IDs", poner un botón `[Random]` al lado del `EditText` de ICCID.
    *   Etc.

¿Apruebas la ejecución de este plan (A y B)?
