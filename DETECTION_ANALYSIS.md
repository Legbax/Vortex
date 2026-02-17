# Análisis de Vectores de Detección en Lancelot

A continuación se detallan los posibles vectores de detección encontrados en el código actual (`MainHook.kt` y configuración general), que aplicaciones como Snapchat o Instagram podrían utilizar para detectar el módulo o el entorno modificado.

## 1. Detección de Propiedades del Sistema (System Properties)

### Incompletitud en las Propiedades de Particiones
**Estado: Solucionado en código.**
El código ahora intercepta propiedades específicas de particiones (`ro.product.system.*`, `ro.product.vendor.*`, etc.) para asegurar consistencia con el modelo falsificado.

### Acceso Nativo (JNI/C++)
**Estado: Mitigado en Java.**
El hook implementado utiliza `XposedHelpers.findAndHookMethod` sobre `android.os.SystemProperties`.
Aunque los hooks Java no afectan lecturas nativas directas (`__system_property_get` en C++), la consistencia extendida en la capa Java reduce la superficie de ataque para detecciones híbridas. Para una solución completa a nivel nativo, se requeriría un módulo Zygisk, lo cual excede el alcance de este módulo Xposed estándar.

## 2. Detección de Ubicación Falsa (Mock Location)

### Flags de Mock Provider
El hook intercepta `getLatitude`, `getLongitude`, etc., y devuelve valores fijos. También intercepta `getLastKnownLocation` y devuelve un objeto `Location` creado manualmente.

**Riesgo:**
*   El objeto `Location` tiene un método `isFromMockProvider()` (en versiones antiguas) y flags internos que indican si fue generado por un proveedor de prueba.
*   En Android 11+, la API `Location.isMock()` es la estándar. El hook actual mitiga esto devolviendo objetos `Location` limpios o interceptando los valores, pero el uso de un proveedor de mock location a nivel sistema sigue siendo un vector si no se oculta con herramientas externas (como Mock Mock Locations o LSposed modules específicos).

## 3. Detección de Archivos y Root

### Archivos World-Readable
El módulo utiliza `chmod 604` (o similar) mediante `su` para hacer legible el archivo `/data/data/com.lancelot/shared_prefs/spoof_prefs.xml`.

**Solución Recomendada:**
La ocultación efectiva de Root y archivos del módulo **no debe hacerse dentro de la propia APK** para ser robusta. Se recomienda encarecidamente utilizar:
1.  **KernelSU** o **Magisk** (con Zygisk habilitado).
2.  **Zygisk - DenyList**: Añadir Snapchat e Instagram a la lista de denegación para ocultar el framework de root.
3.  **Shamiko** (opcional): Para ocultar el propio Zygisk/Magisk si la DenyList no es suficiente.
4.  LSPosed se encarga de inyectar el módulo de manera "stealth" si está configurado correctamente.

Con esta combinación externa, el "Problema 3" se soluciona de manera mucho más efectiva que intentando ocultar binarios `su` desde código Java dentro del módulo.

## 4. Detección de WebView (User-Agent)

### Detección de User-Agent
**Estado: Solucionado en código.**
Se ha implementado un hook en `android.webkit.WebSettings.getUserAgentString` que reemplaza dinámicamente el modelo del dispositivo en la cadena del User-Agent. Esto evita que la navegación web interna (Login con Google/Facebook, visualización de enlaces) filtre el modelo real del dispositivo.

## Resumen de Estado Actual

1.  **Propiedades de Partición:** ✅ Solucionado (Hooks extendidos).
2.  **Fuga por Código Nativo:** ⚠️ Parcialmente mitigado en Java. Solución total requiere Zygisk.
3.  **Archivos/Root:** 🛡️ Requiere configuración externa (KernelSU/Magisk) para solución robusta.
4.  **User-Agent de WebView:** ✅ Solucionado (Hook implementado).
