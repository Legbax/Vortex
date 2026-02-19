# Vortex UI Preview (Lancelot_v1_FIXED)

## 1. Status Tab
**Header:** "VORTEX" (Large, Center)
**Subtitle:** "Developed by Legba" (Small, Center)

**Integrity Indicator:**
- Circular Progress Bar (0-100%).
- Text: "Integrity Score".
- Action: Click to verify system coherence.

**Active Configuration Card:**
- **Fingerprint:** [Redmi 9 / Pixel 5 / ...]
- **SIM Country:** [United States (T-Mobile)]
- **Location:** [Spoofed (US) / Real]

---

## 2. Device Tab
**Header:** "Select Device Profile"

**Action:** [ SELECT RANDOM PROFILE ] (Button)

**List:** Scrollable list of 40+ Fingerprints (e.g., Redmi 9, Pixel 5, Samsung A52).
- Click item to select.

---

## 3. Network Tab
**Header:** "Network Identity"
**Action:** [ RANDOMIZE ALL ] (Button - Top Right)

**Carrier Context:**
- Scrollable list of US Carriers (T-Mobile, AT&T, Verizon...).
- Sets MCC/MNC/Country base.

**Cellular Identity (Read-Only Fields):**
- **SIM Operator:** [T-Mobile (310260)]
- **SIM Country:** [us]
- **IMSI:** [310260xxxxxxxxx] (Random Btn)
- **ICCID:** [89310xxxxxxxxx] (Random Btn)
- **Mobile Number:** [+1202xxxxxxx] (Random Btn)

**WiFi & Bluetooth (Read-Only Fields):**
- **WiFi SSID:** [TP-Link_1234] (Random Btn)
- **WiFi BSSID:** [xx:xx:xx:xx:xx:xx] (Random Btn)
- **WiFi MAC:** [xx:xx:xx:xx:xx:xx] (Random Btn)
- **Bluetooth MAC:** [xx:xx:xx:xx:xx:xx] (Random Btn)

**Action:** [ SAVE CONFIGURATION ] (Button - Bottom)

---

## 4. Identity (IDs) Tab
**Header:** "Device Identifiers"
**Action:** [ RANDOMIZE ALL ] (Button - Top Right)

**Hardware IDs (Read-Only Fields):**
- **IMEI (Slot 1):** [86xxxxxxxxxxxxx] (Random Btn)
- **IMEI (Slot 2):** [86xxxxxxxxxxxxx] (Random Btn)
- **Android ID:** [xxxxxxxxxxxxxxxx] (Random Btn)
- **GSF ID:** [xxxxxxxxxxxxxxxx] (Random Btn)
- **SSAID (Snapchat):** [xxxxxxxxxxxxxxxx] (Random Btn)
- **MediaDRM ID:** [xxxxxxxxxxxxxxxx] (Random Btn)
- **Advertising ID:** [uuid-v4] (Random Btn)
- **Serial Number:** [xxxxxxxx] (Random Btn)

**Accounts (Read-Only Fields):**
- **Gmail Account:** [user@gmail.com] (Random Btn)

**Action:** [ SAVE CONFIGURATION ] (Button - Bottom)

---

## 5. Location Tab
**Header:** "Location Spoofing"

**Map Interface:** Interactive Map (Leaflet/OSM) to view/set location.

**Controls:**
- **Enable Mock Location:** [Switch]
- **Latitude:** [40.7128]
- **Longitude:** [-74.0060]
- **Altitude:** [10.0]
- **Accuracy:** [5.0]

**Actions:**
- [ RANDOM US CITY ]
- [ SAVE CONFIGURATION ]

---

## 6. Advanced Tab
**Header:** "Advanced Hooks"

**Toggles:**
- **Root Hiding:** [Switch] (Prevents root detection)
- **Debug Hiding:** [Switch] (Prevents debug detection)
- **Package Manager Hook:** [Switch] (Spoofs install source to Play Store)
- **WebView User-Agent:** [Switch] (Spoofs Browser UA)

**Action:** [ SAVE CONFIGURATION ]
