# Análisis: Lancelot vs Play Integrity Device Recall

## ¿Qué es Device Recall?
**Play Integrity Device Recall** es una nueva función (actualmente en beta) que permite a los desarrolladores almacenar "bits" de información (etiquetas o banderas) asociados a un dispositivo físico en los servidores de Google.

**Características clave:**
*   **Persistencia Extrema:** La información persiste incluso si el usuario desinstala la app o restablece los datos de fábrica del dispositivo (factory reset), siempre que el usuario vuelva a iniciar sesión con la misma cuenta de Google o el dispositivo mantenga sus identificadores de hardware confiables.
*   **Privacidad:** La app no recibe un ID único del dispositivo, sino que "recupera" los bits que guardó anteriormente para ese entorno.
*   **Uso:** Se usa para detectar abusadores reincidentes (e.g., usuarios que crean cuentas masivas para pruebas gratuitas).

## ¿Cómo funciona Lancelot ante esto?

### 1. El conflicto de identidad
Lancelot funciona falsificando (spoofing) los identificadores que las aplicaciones leen localmente (`Build.MODEL`, `Settings.Secure.ANDROID_ID`, `TelephonyManager.getImei`).

Sin embargo, **Device Recall no depende de lo que la app lee**, sino de lo que **Google Play Services** reporta al servidor de Play Integrity.

### 2. Puntos de Fuga (Vulnerabilidades)
Si Google Play Services determina la identidad del dispositivo para "Device Recall" basándose en:
*   **Atestación de Hardware (Hardware-backed Keystore):** Lancelot (al ser un módulo Xposed Java) **NO** puede falsificar la atestación de hardware. El chip de seguridad (TEE/StrongBox) reportará la clave criptográfica real del dispositivo. Si Google liga los datos de "Recall" a esta clave de hardware, Lancelot no podrá evitar que se recuperen los datos originales.
*   **Cuenta de Google:** Si los datos están ligados a la cuenta de usuario, cambiar los IDs del dispositivo no servirá si el usuario inicia sesión con la misma cuenta de Gmail.

### 3. ¿Lancelot protege contra Device Recall?
**Parcialmente / Probablemente NO por sí solo.**

*   **Escenario Exitoso:** Si Device Recall depende puramente de identificadores de software que Lancelot ya falsifica (como GSF ID o Android ID), y Lancelot logra engañar al proceso de Google Play Services (`com.google.android.gms`), entonces podría funcionar. Lancelot incluye un hook para `gsf_id` y `android_id`.
*   **Escenario de Fallo (Más probable):** Play Integrity está diseñado para ser robusto. Utiliza chequeos nativos y de hardware que Lancelot no intercepta. Es muy probable que Google Play Services ignore los hooks de Lancelot al generar el token de integridad, enviando la identidad real al servidor, lo que permitiría a la app recuperar los datos de "abuso" previos.

## Recomendación para Evasión Total
Para evadir Device Recall, no basta con spoofing de aplicación. Se requiere:
1.  **Spoofing de GMS (Google Play Services):** El módulo debe inyectarse no solo en la app objetivo (Snapchat), sino también en `com.google.android.gms` para falsificar los datos que este envía a Google. (Lancelot actualmente *excluye* hooking del sistema para estabilidad, pero podría necesitarse aquí).
2.  **Play Integrity Fix (Fingerprint):** Usar módulos como *Play Integrity Fix* (chiteroman) que manipulan la atestación para forzar un nivel de integridad "Basic" en lugar de "Hardware", obligando al servidor a confiar en datos de software (que sí podemos falsificar).
3.  **Nuevas Cuentas:** No reutilizar cuentas de Google marcadas.

**Conclusión:** Lancelot por sí solo es una herramienta de "Device Fingerprinting" local. Device Recall es una herramienta de "Server-side Reputation". Lancelot ayuda, pero no garantiza evasión contra sistemas que validan integridad a nivel de servidor/hardware.
