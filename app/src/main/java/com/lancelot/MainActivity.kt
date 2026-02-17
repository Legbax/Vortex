package com.lancelot

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import androidx.core.content.edit
import java.io.File

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE) }

    // FIX #6: Solo perfiles Android 11 disponibles
    private val profiles = listOf(
        "Redmi 9 (Original)",
        "Redmi Note 9",
        "Redmi 9A",
        "Redmi 9C",
        "Redmi Note 9S",
        "Redmi Note 9 Pro",
        "Redmi Note 8",
        "Redmi Note 8 Pro",
        "Redmi Note 10",
        "Redmi Note 10S",
        "Redmi 10",
        "POCO X3 NFC",
        "POCO X3 Pro",
        "POCO M3",
        "POCO M3 Pro 5G",
        "Mi 10T",
        "Mi 10T Pro",
        "Mi 11 Lite",
        "Mi 11i",
        "Samsung Galaxy S10",
        "Samsung Galaxy S10+",
        "Samsung Galaxy S10e",
        "Samsung Galaxy S20",
        "Samsung Galaxy S20+",
        "Samsung Galaxy S20 FE",
        "Samsung Galaxy Note 10",
        "Samsung Galaxy Note 10+",
        "Samsung Galaxy A51",
        "Samsung Galaxy A71",
        "Samsung Galaxy A52",
        "Samsung Galaxy A72",
        "Google Pixel 3",
        "Google Pixel 3 XL",
        "Google Pixel 4",
        "Google Pixel 4 XL",
        "Google Pixel 4a",
        "Google Pixel 4a 5G",
        "Google Pixel 5",
        "OnePlus 7",
        "OnePlus 7 Pro",
        "OnePlus 7T",
        "OnePlus 8",
        "OnePlus 8 Pro",
        "OnePlus Nord",
        "Motorola Moto G9 Plus",
        "Motorola Moto G Power 2021",
        "Motorola Edge 20",
        "Nokia 5.4",
        "Nokia 8.3 5G",
        "Sony Xperia 1 II"
    )

    // FIX #7: NO guardamos model/brand/version separados, solo el perfil
    private lateinit var spProfile: Spinner
    private lateinit var etAndroidId: EditText
    private lateinit var etImei: EditText
    private lateinit var etImei2: EditText
    private lateinit var etSerial: EditText
    private lateinit var etGaid: EditText
    private lateinit var etSsaid: EditText
    private lateinit var etSimOperator: EditText
    private lateinit var etSimCountry: EditText
    private lateinit var etMccMnc: EditText
    private lateinit var etWifiMac: EditText
    private lateinit var etBluetoothMac: EditText
    private lateinit var etGsfId: EditText

    // Mock Location
    private lateinit var swMockLocation: Switch
    private lateinit var etMockLat: EditText
    private lateinit var etMockLon: EditText
    private lateinit var etMockAlt: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupProfileSpinner()
        loadSavedValues()
        setupButtons()
    }

    private fun initializeViews() {
        spProfile = findViewById(R.id.sp_profile)
        etAndroidId = findViewById(R.id.et_android_id)
        etImei = findViewById(R.id.et_imei)
        etImei2 = findViewById(R.id.et_imei2)
        etSerial = findViewById(R.id.et_serial)
        etGaid = findViewById(R.id.et_gaid)
        etSsaid = findViewById(R.id.et_ssaid)
        etSimOperator = findViewById(R.id.et_sim_operator)
        etSimCountry = findViewById(R.id.et_sim_country)
        etMccMnc = findViewById(R.id.et_mcc_mnc)
        etWifiMac = findViewById(R.id.et_wifi_mac)
        etBluetoothMac = findViewById(R.id.et_bluetooth_mac)
        etGsfId = findViewById(R.id.et_gsf_id)

        swMockLocation = findViewById(R.id.sw_mock_location)
        etMockLat = findViewById(R.id.et_mock_lat)
        etMockLon = findViewById(R.id.et_mock_lon)
        etMockAlt = findViewById(R.id.et_mock_alt)
    }

    private fun setupProfileSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profiles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProfile.adapter = adapter

        val savedProfile = prefs.getString("profile", "Redmi 9 (Original)")
        spProfile.setSelection(profiles.indexOf(savedProfile).coerceAtLeast(0))
    }

    private fun loadSavedValues() {
        etAndroidId.setText(prefs.getString("android_id", generateRandomId(16)))
        etImei.setText(prefs.getString("imei", generateValidImei()))
        etImei2.setText(prefs.getString("imei2", generateValidImei()))
        etSerial.setText(prefs.getString("serial", generateRandomSerial()))
        etGaid.setText(prefs.getString("gaid", generateRandomGaid()))
        etSsaid.setText(prefs.getString("ssaid", generateRandomId(16)))
        etSimOperator.setText(prefs.getString("sim_operator", "310260"))
        etSimCountry.setText(prefs.getString("sim_country", "us"))
        etMccMnc.setText(prefs.getString("mcc_mnc", "310260"))
        etWifiMac.setText(prefs.getString("wifi_mac", generateRandomMac()))
        etBluetoothMac.setText(prefs.getString("bluetooth_mac", generateRandomMac()))
        etGsfId.setText(prefs.getString("gsf_id", generateRandomId(16)))

        swMockLocation.isChecked = prefs.getBoolean("mock_location_enabled", false)
        etMockLat.setText(prefs.getString("mock_latitude", "0.0"))
        etMockLon.setText(prefs.getString("mock_longitude", "0.0"))
        etMockAlt.setText(prefs.getString("mock_altitude", "0.0"))
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_apply).setOnClickListener {
            if (validateInputs()) {
                saveValues()
                // FIX #2: Hacer archivo world-readable para Xposed
                makePrefsWorldReadable()
                Toast.makeText(this, "Settings applied! Restart target apps.", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btn_randomize).setOnClickListener {
            randomizeValues()
        }
    }

    private fun validateInputs(): Boolean {
        // Validar IMEI
        if (etImei.text.toString().length != 15) {
            Toast.makeText(this, "IMEI must be 15 digits", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etImei2.text.toString().length != 15) {
            Toast.makeText(this, "IMEI 2 must be 15 digits", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar MAC
        val macRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        if (!etWifiMac.text.toString().matches(macRegex)) {
            Toast.makeText(this, "Invalid WiFi MAC format", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!etBluetoothMac.text.toString().matches(macRegex)) {
            Toast.makeText(this, "Invalid Bluetooth MAC format", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveValues() {
        prefs.edit {
            putString("profile", spProfile.selectedItem.toString())
            putString("android_id", etAndroidId.text.toString())
            putString("imei", etImei.text.toString())
            putString("imei2", etImei2.text.toString())
            putString("serial", etSerial.text.toString())
            putString("gaid", etGaid.text.toString())
            putString("ssaid", etSsaid.text.toString())
            putString("sim_operator", etSimOperator.text.toString())
            putString("sim_country", etSimCountry.text.toString())
            putString("mcc_mnc", etMccMnc.text.toString())
            putString("wifi_mac", etWifiMac.text.toString())
            putString("bluetooth_mac", etBluetoothMac.text.toString())
            putString("gsf_id", etGsfId.text.toString())

            putBoolean("mock_location_enabled", swMockLocation.isChecked)
            putString("mock_latitude", etMockLat.text.toString())
            putString("mock_longitude", etMockLon.text.toString())
            putString("mock_altitude", etMockAlt.text.toString())
        }
    }

    // FIX #2: Hacer preferencias world-readable para Xposed
    private fun makePrefsWorldReadable() {
        try {
            val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "spoof_prefs.xml")

            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
                prefsDir.setExecutable(true, false)

                // Cambiar permisos usando shell root
                val commands = arrayOf(
                    "chmod 644 ${prefsFile.absolutePath}",
                    "chmod 755 ${prefsDir.absolutePath}"
                )

                Runtime.getRuntime().exec(arrayOf("su", "-c", commands.joinToString(" && "))).waitFor()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Warning: Could not set world-readable. Root may be required.", Toast.LENGTH_LONG).show()
        }
    }

    private fun randomizeValues() {
        etAndroidId.setText(generateRandomId(16))
        etImei.setText(generateValidImei())
        etImei2.setText(generateValidImei())
        etSerial.setText(generateRandomSerial())
        etGaid.setText(generateRandomGaid())
        etSsaid.setText(generateRandomId(16))
        etWifiMac.setText(generateRandomMac())
        etBluetoothMac.setText(generateRandomMac())
        etGsfId.setText(generateRandomId(16))

        Toast.makeText(this, "Values randomized", Toast.LENGTH_SHORT).show()
    }

    // ========== GENERADORES ==========

    private fun generateRandomId(length: Int): String =
        (1..length).map { "0123456789abcdef".random() }.joinToString("")

    private fun generateValidImei(): String {
        val validTacs = listOf(
            "35891603",  // Xiaomi
            "35328708",  // Samsung
            "35404907"   // Motorola
        )
        val tac = validTacs.random()
        val serial = (1..6).map { (0..9).random() }.joinToString("")
        val base = tac + serial
        return base + luhnChecksum(base)
    }

    private fun luhnChecksum(number: String): Int {
        var sum = 0
        for (i in number.indices.reversed()) {
            var digit = number[i].digitToInt()
            if ((number.length - i) % 2 == 0) digit *= 2
            if (digit > 9) digit -= 9
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun generateRandomSerial(): String =
        (1..12).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

    private fun generateRandomGaid(): String =
        "${generateRandomId(8)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(4)}-${generateRandomId(12)}"

    private fun generateRandomMac(): String {
        val firstByte = (0x02 or (kotlin.random.Random.nextInt(256) and 0xFC))
            .toString(16).padStart(2, '0').uppercase()

        val rest = (1..5).map {
            kotlin.random.Random.nextInt(256).toString(16)
                .padStart(2, '0').uppercase()
        }.joinToString(":")

        return "$firstByte:$rest"
    }
}
