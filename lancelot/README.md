# Lancelot - Device Spoofer

**Advanced Android 11 Device Spoofing Module**

## Features

- ✅ **Complete Device Spoofing** - Build info, fingerprints, identifiers
- ✅ **IMEI/IMSI with Luhn Algorithm** - Valid checksums, real TACs
- ✅ **Mock Location** - GPS spoofing support
- ✅ **Xposed Detection Evasion** - Stack trace filtering
- ✅ **Consistent IDs** - No random changes between calls
- ✅ **Android 11 Only** - Optimized for API 30 (Redmi 9 compatible)

## Requirements

- Android 11 (API 30)
- LSPosed or EdXposed
- Root access (Magisk or KernelSU)

## Installation

1. Install LSPosed framework
2. Install Lancelot APK
3. Enable module in LSPosed
4. Select target apps (Snapchat, Instagram, etc.)
5. Restart target apps

## Usage

1. Open Lancelot app
2. Select a device profile (all Android 11)
3. Optionally customize identifiers
4. Click "Randomize" for random values
5. Click "Apply" to save
6. Restart target apps

## Profiles Available

All profiles use Android 11 (API 30) to match device framework:

- Redmi 9 (Original)
- Redmi Note 9
- Redmi 9A
- Redmi 9C
- Redmi Note 9S
- Redmi Note 9 Pro

## Security Features

### Consistent Identifiers
All IDs are cached per session - no random changes between calls:
- IMEI 1 & 2
- IMSI (Subscriber ID)
- ICCID (SIM Serial)
- Phone Number
- Android ID
- GSF ID
- MAC Addresses

### Valid Generators
- **IMEI**: Real TACs + Luhn checksum
- **ICCID**: ISO/IEC 7812 format + Luhn
- **MAC**: IEEE 802 compliant (locally administered bit)

### Anti-Detection
- Stack trace filtering (hides Xposed)
- World-readable prefs (no random fallbacks)
- Framework version matches reported version

## Important Notes

⚠️ **Android 11 Only**: Do not use profiles from other Android versions. Framework mismatch causes detection.

⚠️ **Consistent Profile**: Once applied, use same profile for 30+ days minimum.

⚠️ **Root Hiding**: Combine with Magisk Hide or KernelSU Hide.

## Troubleshooting

### Module not working
- Check LSPosed is active
- Verify target app is selected in LSPosed scope
- Restart target app completely
- Check logs in LSPosed

### Settings not applying
- Ensure root access for world-readable prefs
- Check file permissions: `/data/data/com.lancelot/shared_prefs/spoof_prefs.xml`
- Should be readable by all (chmod 644)

### Detection still occurs
- Verify you're using Android 11 profile only
- Don't change profiles frequently
- Check SafetyNet/Play Integrity status
- Ensure no other Xposed modules conflict

## Technical Details

### What is Spoofed

**Build Fields:**
- MANUFACTURER, BRAND, MODEL
- DEVICE, PRODUCT, HARDWARE
- BOARD, BOOTLOADER, FINGERPRINT
- SDK_INT (always 30), RELEASE (always "11")

**System Properties:**
- ro.product.*
- ro.build.*
- ro.hardware
- gsm.version.baseband

**Telephony:**
- IMEI/MEID
- IMSI (Subscriber ID)
- ICCID (SIM Serial)
- Phone Number
- Network Operator
- SIM Operator

**Identifiers:**
- Settings.Secure.ANDROID_ID
- GSF ID (Google Services Framework)
- GAID (Advertising ID)
- SSAID

**Network:**
- WiFi MAC address
- Bluetooth MAC address

**Location (optional):**
- Latitude, Longitude, Altitude
- GPS accuracy

### Algorithms Used

**Luhn Checksum:**
```
Used for IMEI and ICCID validation
Ensures mathematically valid identifiers
```

**MAC IEEE 802:**
```
First byte: 0x02 (locally administered)
Universally/locally administered bit correctly set
```

## License

Educational purposes only. Use responsibly.

## Credits

Created for Redmi 9 (Android 11)
Optimized for LSPosed compatibility
