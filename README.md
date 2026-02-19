# <img src="assets/logo.png" width="100" align="right" /> Vortex - Device Spoofer

**Advanced Android 11 Device Spoofing Module**

## Features

- ✅ **Complete Device Spoofing** - Build info, fingerprints, identifiers
- ✅ **40+ High-Fidelity Profiles** - Xiaomi, Samsung, OnePlus, Google Pixel, etc. (Android 11/10)
- ✅ **Hardware Consistency** - Hooks `ro.hardware.egl`, `ro.board.platform`, etc.
- ✅ **US Carrier Emulation** - Real MCC/MNCs, NPAs, and phone number generation
- ✅ **Manual Encryption** - AES-encrypted SharedPreferences for cross-process stealth
- ✅ **CI/CD Integration** - Automated APK builds via GitHub Actions
- ✅ **IMEI/IMSI with Luhn Algorithm** - Valid checksums, real TACs
- ✅ **Mock Location** - Advanced GPS spoofing (Bearing, Speed, Altitude)
- ✅ **Xposed Detection Evasion** - Stack trace filtering & package hiding

## Requirements

- Android 11 (API 30) - Recommended
- LSPosed or EdXposed
- Root access (Magisk or KernelSU)

## Installation

1. Download the latest APK from [Actions](../../actions) or Releases.
2. Install Vortex APK.
3. Enable module in LSPosed.
4. Select target apps (Snapchat, Instagram, etc.) in the scope.
5. Restart target apps.

## Usage

1. Open **Vortex** app.
2. Select a device profile from the list (e.g., "Redmi Note 9 Pro - Android 11").
3. Select a US Carrier (e.g., "T-Mobile", "Verizon").
4. Optionally enable **Mock Location** and set coordinates.
5. Click **"Randomize"** to generate new valid identifiers (IMEI, GAID, etc.).
6. Click **"Save Profile"** to apply changes.
7. Restart target apps to take effect.

## Profiles Available

Includes over 40 high-fidelity device fingerprints:

- **Xiaomi**: Redmi 9/Note 9 series, POCO X3/M3, Mi 10/11 series
- **Samsung**: Galaxy A52, A32, S20+, S10e, M12
- **Google**: Pixel 4a, Pixel 5, Pixel 4a 5G
- **OnePlus**: Nord, 8T, Nord CE
- **Motorola**: Moto G30, G Power, G50
- **Other**: Nokia, Vivo, Realme, Huawei, Honor, Asus

## Security Features

### 🔐 Encryption & Stealth
- **AES-256 Encryption**: All sensitive data in `shared_prefs` is encrypted to prevent plain-text analysis.
- **World-Readable Logic**: Uses manual encryption instead of `EncryptedSharedPreferences` to avoid permission crashes across UIDs.
- **Hiding**: Hooks `PackageManager` to hide Xposed and itself from detection.

### 🆔 Consistent Identifiers
All IDs are cached per session - no random changes between calls:
- **IMEI**: Valid TAC + Luhn Checksum
- **IMSI/ICCID**: Valid MCC/MNC + Carrier logic
- **Phone Number**: Real US Area Codes (NPA) + NXX validity
- **Hardware IDs**: Android ID, GSF ID, WiFi/BT MACs

### 📍 Advanced Location Spoofing
- Spoofs Latitude, Longitude, Altitude, Accuracy.
- Generates realistic random `Bearing` and `Speed` to simulate movement.
- Hooks `LocationManager` and `Location` class directly.

## Technical Details

### Build & Properties Hooked
- `Build.MANUFACTURER`, `MODEL`, `DEVICE`, `PRODUCT`, `HARDWARE`, `BOARD`
- `Build.VERSION.SDK_INT` (30), `RELEASE` (11), `SECURITY_PATCH`
- `SystemProperties`:
    - `ro.product.*` (system, vendor, odm, product partitions)
    - `ro.build.*`
    - `ro.board.platform`, `ro.hardware.egl`, `ro.opengles.version`
    - `gsm.version.baseband`

### CI/CD
This project uses GitHub Actions to automatically build the APK on every push to `main`.
Artifacts are available in the Actions tab.

## Disclaimer
Educational purposes only. Use responsibly and ethically.
