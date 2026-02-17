# Análisis del módulo FIXED (`lancelot_v1_FIXED`)

## 1) Verificación del ZIP

- El ZIP recibido está **íntegro** a nivel de contenedor: `unzip -t` reporta `No errors detected`.
- Contiene 13 entradas, todas extraíbles.

### Archivos presentes
- `build.gradle.kts`
- `AndroidManifest.xml`
- `MainHook.kt`
- `MainActivity.kt`
- `activity_main.xml`
- `proguard-rules.pro`
- `strings.xml`, `colors.xml`, `themes.xml`
- `xposed_init`
- `xposed_scope.xml`
- `README.md`

## 2) Qué sí mejoró respecto al módulo previo

1. **Xposed como `compileOnly`**
   - Se corrigió el empaquetado de API Xposed en runtime (`compileOnly` en lugar de `implementation`).

2. **Consistencia de telephony por sesión**
   - IMSI/ICCID/teléfono ahora salen de caché (`cachedImsi`, `cachedIccid`, `cachedPhoneNumber`) y no se regeneran en cada llamada.

3. **Unificación de hook en `Settings.Secure`**
   - Se usa un solo hook para `getString`, reduciendo choque entre Android ID y GSF.

4. **Perfil orientado a Android 11 en hooks**
   - En tiempo de ejecución fuerza `SDK_INT=30`, `RELEASE=11`.

5. **Manejo más robusto de MAC**
   - `macStringToBytes` ya tiene fallback válido ante parseo inválido.

## 3) Errores/riesgos que aún pueden causar detección o fallos

## CRÍTICOS

### C1) Estructura de proyecto incompleta para build Android estándar
El ZIP trae archivos “planos” en la raíz del módulo, pero **no** incluye estructura Gradle/Android típica (`app/src/main/...`, wrappers, etc.). Aunque el ZIP está sano, **no representa un proyecto Android completo compilable por sí solo**.

### C2) `xposed_scope.xml` no está en ruta de recursos Android canónica
El manifest usa `@array/xposed_scope`, pero el archivo entregado está como `xposed_scope.xml` plano. En un proyecto Android real debe ir en `res/values/` para que AAPT lo empaquete como recurso.

## ALTOS

### A1) `ro.serialno` puede variar por llamada cuando no hay prefs legibles
En `SystemProperties.get`, si `prefs` es `null`, retorna `generateRandomSerial()` en caliente para `ro.serialno`. Eso puede romper consistencia intra-proceso entre lecturas sucesivas.

### A2) Lectura de `XSharedPreferences` sigue frágil en Android 11+
Se intenta leer `/data/data/.../shared_prefs/...xml` con `canRead()`. En apps target no privilegiadas esa comprobación suele fallar por permisos/SELinux, forzando fallback a defaults y variación entre procesos/reinicios.

### A3) Validación de IMEI en UI es débil
`MainActivity` solo valida longitud de IMEI (15), no checksum Luhn para valores editados manualmente. Si el usuario mete un IMEI inválido, aumenta probabilidad de detección.

### A4) Mismatch potencial de versión Xposed mínima
Manifest declara `xposedminversion=93` mientras dependencia usada es API 82. Puede no romper detección directa, pero sí compatibilidad/carga según entorno.

## MEDIOS

### M1) `compileSdk=34` con `targetSdk=30`
No es incorrecto per se, pero añade superficie de diferencias de comportamiento durante build/packaging frente a un enfoque totalmente alineado en 30.

### M2) Caché estática global
La caché estática mejora consistencia por sesión, pero también puede persistir valores previos dentro del proceso Zygote/hook y no reflejar cambios de prefs hasta reinicio de app objetivo.

## 4) Conclusión técnica

El módulo FIXED **sí corrigió varios puntos clave** (compileOnly de Xposed, consistencia de telephony por sesión, unificación de Settings hook, robustez MAC). Sin embargo, aún mantiene riesgos de detección por **consistencia de serial** y por **fragilidad de lectura de prefs en Android 11**; además, el paquete entregado no es un proyecto Android completo listo para compilar sin reconstruir rutas/estructura.

## 5) Recomendaciones directas

1. Persistir serial en caché (igual que IMSI/ICCID) y no generar en cada lectura de `ro.serialno`.
2. Implementar método robusto de entrega de configuración al hook (p. ej., proveedor seguro o storage con permisos controlados por root, evitando depender de `canRead()` en `/data/data`).
3. Validar IMEI con Luhn en UI antes de guardar.
4. Alinear `xposedminversion` con la API realmente soportada y con tu stack LSPosed.
5. Reempaquetar proyecto en estructura Android estándar (`app/src/main/...`, `res/values/xposed_scope.xml`, etc.).
