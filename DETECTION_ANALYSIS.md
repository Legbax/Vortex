# Análisis de Vectores de Detección en Lancelot

A continuación se detallan los posibles vectores de detección encontrados en el código actual (`MainHook.kt` y configuración general), que aplicaciones como Snapchat o Instagram podrían utilizar para detectar el módulo o el entorno modificado.

## 1. Detección de Propiedades del Sistema (System Properties)

### Incompletitud en las Propiedades de Particiones
El código actual intercepta propiedades genéricas como `ro.product.model`, `ro.product.manufacturer`, etc. Sin embargo, en Android 10+ (y especialmente 11/12+), los fabricantes definen propiedades específicas para cada partición (`vendor`, `odm`, `product`, `system_ext`).

**Riesgo:**
Apps sofisticadas verifican la consistencia entre:
*   `ro.product.model`
*   `ro.product.system.model`
*   `ro.product.vendor.model`
*   `ro.product.odm.model`

Actualmente, Lancelot **no** está cubriendo las propiedades de particiones (`ro.product.system.*`, `ro.product.vendor.*`, etc.). Si un fingerprint de "Redmi 9" se inyecta en `ro.product.model` pero `ro.product.vendor.model` sigue diciendo "Pixel 4" (o el dispositivo real), la discrepancia es una bandera roja inmediata.

### Acceso Nativo (JNI/C++)
El hook implementado utiliza `XposedHelpers.findAndHookMethod` sobre `android.os.SystemProperties`. Esto solo afecta a las llamadas hechas desde código Java/Kotlin.
Muchas apps de alta seguridad (Snapchat, juegos, banca) leen las propiedades del sistema directamente desde código nativo (C/C++) usando la función `__system_property_get` de `libc`.

**Riesgo:**
Los hooks de Xposed en Java son **invisibles** para estas lecturas nativas. La app verá los valores reales del dispositivo a nivel nativo y los valores falsos a nivel Java. Esta discrepancia es 100% detectable.

## 2. Detección de Ubicación Falsa (Mock Location)

### Flags de Mock Provider
El hook intercepta `getLatitude`, `getLongitude`, etc., y devuelve valores fijos. También intercepta `getLastKnownLocation` y devuelve un objeto `Location` creado manualmente.

**Riesgo:**
*   El objeto `Location` tiene un método `isFromMockProvider()` (en versiones antiguas) y flags internos que indican si fue generado por un proveedor de prueba.
*   Aunque el código intenta ocultar la ubicación, si la app solicita actualizaciones de ubicación (`requestLocationUpdates`) y el sistema entrega una ubicación marcada como "Mock" (porque se está usando una app de Mock Location subyacente o el propio sistema lo marca), el hook actual sobre los *getters* podría no ser suficiente si la app inspecciona los extras del objeto `Location` original antes de llamar a los métodos hookeados.
*   En Android 11+, la API `Location.isMock()` es la estándar. El hook no parece estar interceptando explícitamente esta llamada en la instancia de `Location` devuelta, aunque al crear un `new Location` en `getLastKnownLocation` este flag suele estar en falso por defecto, lo cual es bueno. Pero para `requestLocationUpdates` (GPS activo), el hook solo modifica los valores de retorno, no el objeto en sí.

## 3. Detección de Archivos y Root

### Archivos World-Readable
El módulo utiliza `chmod 604` (o similar) mediante `su` para hacer legible el archivo `/data/data/com.lancelot/shared_prefs/spoof_prefs.xml`.

**Riesgo:**
*   Cualquier app puede intentar leer ese archivo específico. Si el archivo existe y es legible, es una confirmación inmediata de que Lancelot está instalado.
*   Snapchat verifica la existencia de paquetes conocidos y archivos de configuración de herramientas de hooking.

### Detección de Binarios SU
Lancelot requiere Root para funcionar (para los permisos de archivos). No oculta el root. Si el usuario no usa MagiskHide / Zygisk DenyList correctamente, la presencia del binario `su` o la app de gestión de root (Magisk app) será detectada.

## 4. Detección de Xposed/LSPosed

### Stack Trace Filtering
El código tiene una función `hookXposedDetection` que filtra `getStackTrace` para eliminar referencias a "xposed", "lsposed", etc.

**Riesgo:**
*   Esto es efectivo para detecciones básicas (Java).
*   No protege contra inspección de memoria nativa (buscar mapas de memoria `/proc/self/maps` que contengan `xposed` o `lsposed` en sus rutas).
*   No protege contra la detección de métodos nativos que no deberían serlo (Xposed cambia métodos Java a nativos para hookearlos).

## 5. Detección de WebView (User-Agent)

**Riesgo:**
Si la app (Instagram/Snapchat) abre un `WebView` interno, el `User-Agent` del navegador enviará el modelo *real* del dispositivo a menos que también se hookee `WebSettings.getUserAgentString()`. Lancelot actualmente no hookea WebViews.

## Resumen de Vulnerabilidades Críticas

1.  **Falta de Propiedades de Partición:** Alta probabilidad de detección por inconsistencia de propiedades (`vendor`, `system`).
2.  **Fuga por Código Nativo:** Los hooks Java no afectan lecturas C++, revelando el dispositivo real.
3.  **Archivos Detectables:** El archivo de preferencias es un indicador obvio si se busca explícitamente.
4.  **User-Agent de WebView:** Fuga del modelo real en navegación web interna.

Se recomienda solucionar al menos las propiedades de partición y considerar hooks para WebView. El problema nativo requiere soluciones más complejas (como Zygisk modules o Riru) que escapan al alcance de un módulo Xposed Java tradicional.
