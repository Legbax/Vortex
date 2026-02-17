package com.lancelot

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var spProfile: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnRandom: Button
    private lateinit var etImei: EditText
    private lateinit var etGmail: EditText
    private lateinit var spCarrier: Spinner
    private lateinit var swMockLocation: Switch
    private lateinit var etMockLat: EditText
    private lateinit var etMockLon: EditText
    private lateinit var etMockAlt: EditText
    private lateinit var etMockAcc: EditText
    private lateinit var spRandomField: Spinner
    private lateinit var btnRandomSelected: Button

    // Clave estática ofuscada (debe coincidir con MainHook)
    private val KEY_BYTES = byteArrayOf(
        0x4c, 0x61, 0x6e, 0x63, 0x65, 0x6c, 0x6f, 0x74,
        0x53, 0x74, 0x65, 0x61, 0x6c, 0x74, 0x68, 0x31
    )
    private val ALGO = "AES"

    private fun encrypt(value: String): String {
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGO)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            "ENC:" + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            value // Fallback inseguro si falla
        }
    }

    private fun decrypt(encrypted: String?): String? {
        if (encrypted.isNullOrEmpty()) return null
        if (!encrypted.startsWith("ENC:")) return encrypted // Compatibilidad
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGO)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = Base64.decode(encrypted.substring(4), Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    // Perfiles específicos con alta fidelidad
    private val profiles = listOf(
        "Redmi 9 - Android 11",
        "Redmi Note 9 - Android 11",
        "Redmi 9A - Android 11",
        "Redmi 9C - Android 11",
        "Redmi Note 9S - Android 11",
        "Redmi Note 9 Pro - Android 11",
        "POCO X3 NFC - Android 11",
        "POCO X3 Pro - Android 11",
        "POCO M3 - Android 11",
        "POCO M3 Pro 5G - Android 11",
        "Mi 10T - Android 11",
        "Mi 11 Lite - Android 11",
        "Redmi Note 10 Pro - Android 11",
        "Mi 10 Lite - Android 11",
        "Mi 11i - Android 11",
        "Samsung Galaxy A52 - Android 11",
        "Samsung Galaxy A32 - Android 11",
        "Samsung Galaxy A12 - Android 11",
        "Samsung Galaxy A51 - Android 11",
        "Samsung Galaxy M12 - Android 11",
        "OnePlus Nord - Android 11",
        "OnePlus 8T - Android 11",
        "OnePlus Nord CE - Android 11",
        "Moto G30 - Android 11",
        "Moto G Power 2021 - Android 11",
        "Moto G50 - Android 11",
        "Nokia 5.4 - Android 11",
        "Nokia X10 - Android 11",
        "Google Pixel 4a - Android 11",
        "Google Pixel 5 - Android 11",
        "Google Pixel 4a 5G - Android 11",
        "Samsung Galaxy S20+ - Android 11",
        "Samsung Galaxy S10e - Android 11",
        "Vivo Y53s - Android 11",
        "Realme 7 5G - Android 11",
        "Oppo Reno5 - Android 11",
        "Vivo X60 - Android 11",
        "Vivo X70 Pro - Android 11",
        "Realme 8 Pro - Android 11",
        "Asus Zenfone 8 - Android 11"
    )

    private val randomFields = listOf(
        "IMEI", "IMEI2", "Serial", "Android ID", "GAID",
        "WiFi MAC", "Bluetooth MAC", "GSF ID", "Gmail"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spProfile = findViewById(R.id.sp_profile)
        spCarrier = findViewById(R.id.sp_carrier)
        btnSave = findViewById(R.id.btn_save)
        btnRandom = findViewById(R.id.btn_random)
        etImei = findViewById(R.id.et_imei)
        etGmail = findViewById(R.id.et_gmail)

        swMockLocation = findViewById(R.id.sw_mock_location)
        etMockLat = findViewById(R.id.et_mock_lat)
        etMockLon = findViewById(R.id.et_mock_lon)
        etMockAlt = findViewById(R.id.et_mock_alt)
        etMockAcc = findViewById(R.id.et_mock_acc)

        spRandomField = findViewById(R.id.sp_random_field)
        btnRandomSelected = findViewById(R.id.btn_random_selected)

        setupSpinners()
        loadPrefs()

        btnSave.setOnClickListener {
            if (!isValidImei(etImei.text.toString())) {
                Toast.makeText(this, "IMEI inválido (15 dígitos o Checksum incorrecto)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            savePrefs()
        }

        btnRandom.setOnClickListener {
            randomizeAll()
        }

        btnRandomSelected.setOnClickListener {
            randomizeSelected()
        }
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profiles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spProfile.adapter = adapter

        val fieldAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, randomFields)
        fieldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRandomField.adapter = fieldAdapter

        val usCarriers = MainHook.getUsCarriers()
        val carrierNames = usCarriers.map { "${it.name} (${it.mccMnc})" }
        val carrierAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, carrierNames)
        carrierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCarrier.adapter = carrierAdapter
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        val encryptedProfile = prefs.getString("profile", null)
        val savedProfile = decrypt(encryptedProfile) ?: "Redmi 9"

        // Find the index, ignoring the suffix if necessary or matching start
        val index = profiles.indexOfFirst { it.startsWith(savedProfile) }.coerceAtLeast(0)
        spProfile.setSelection(index)

        val usCarriers = MainHook.getUsCarriers()
        val savedMccMnc = prefs.getString("mcc_mnc", "310260")
        val carrierIdx = usCarriers.indexOfFirst { it.mccMnc == savedMccMnc }.coerceAtLeast(0)
        spCarrier.setSelection(carrierIdx)

        val encryptedImei = prefs.getString("imei", "")
        etImei.setText(decrypt(encryptedImei))

        val encryptedGmail = prefs.getString("gmail", "")
        etGmail.setText(decrypt(encryptedGmail))

        swMockLocation.isChecked = prefs.getBoolean("mock_location_enabled", false)
        etMockLat.setText(decrypt(prefs.getString("mock_latitude", "0.0")))
        etMockLon.setText(decrypt(prefs.getString("mock_longitude", "0.0")))
        etMockAlt.setText(decrypt(prefs.getString("mock_altitude", "0.0")))
        etMockAcc.setText(decrypt(prefs.getString("mock_accuracy", "10.0")))
    }

    private fun savePrefs() {
        val prefs = getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)

        val usCarriers = MainHook.getUsCarriers()
        val selectedCarrier = usCarriers[spCarrier.selectedItemPosition]

        prefs.edit().apply {
            // Save the full string from spinner (e.g. "Redmi 9 - Android 11")
            // MainHook will strip the suffix.
            putString("profile", encrypt(spProfile.selectedItem.toString()))
            putString("imei", encrypt(etImei.text.toString()))
            putString("gmail", encrypt(etGmail.text.toString()))

            putString("mcc_mnc", selectedCarrier.mccMnc)
            putString("sim_operator", selectedCarrier.mccMnc)
            putString("sim_country", "us")

            putBoolean("mock_location_enabled", swMockLocation.isChecked)
            putString("mock_latitude", encrypt(etMockLat.text.toString()))
            putString("mock_longitude", encrypt(etMockLon.text.toString()))
            putString("mock_altitude", encrypt(etMockAlt.text.toString()))
            putString("mock_accuracy", encrypt(etMockAcc.text.toString()))

            apply()
        }
        makePrefsWorldReadable()
        Toast.makeText(this, "Guardado y Encriptado", Toast.LENGTH_SHORT).show()
    }

    private fun randomizeAll() {
        val randomImei = generateValidImei()
        etImei.setText(randomImei)

        val randomGmail = generateRealisticGmail()
        etGmail.setText(randomGmail)

        val randomProfileIndex = (0 until profiles.size).random()
        spProfile.setSelection(randomProfileIndex)

        val prefs = getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("gmail", encrypt(randomGmail))
            putString("imei2", encrypt(generateValidImei()))
            putString("android_id", encrypt(generateRandomId(16)))
            putString("gsf_id", encrypt(generateRandomId(16)))
            putString("gaid", encrypt(UUID.randomUUID().toString()))
            putString("wifi_mac", encrypt(generateRandomMac()))
            putString("bluetooth_mac", encrypt(generateRandomMac()))
            apply()
        }
    }

    private fun randomizeSelected() {
        val field = spRandomField.selectedItem.toString()
        val prefs = getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        var newValue = ""
        val key = when (field) {
            "IMEI" -> "imei"
            "IMEI2" -> "imei2"
            "Serial" -> "serial"
            "Android ID" -> "android_id"
            "GAID" -> "gaid"
            "WiFi MAC" -> "wifi_mac"
            "Bluetooth MAC" -> "bluetooth_mac"
            "GSF ID" -> "gsf_id"
            "Gmail" -> "gmail"
            else -> ""
        }

        if (key.isNotEmpty()) {
            newValue = when (field) {
                "IMEI", "IMEI2" -> generateValidImei()
                "Serial" -> generateRandomId(12).uppercase()
                "Android ID", "GSF ID" -> generateRandomId(16)
                "GAID" -> UUID.randomUUID().toString()
                "WiFi MAC", "Bluetooth MAC" -> generateRandomMac()
                "Gmail" -> generateRealisticGmail()
                else -> ""
            }
            editor.putString(key, encrypt(newValue))
            editor.apply()

            if (field == "IMEI") etImei.setText(newValue)
            if (field == "Gmail") etGmail.setText(newValue)

            Toast.makeText(this, "$field actualizado: $newValue", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePrefsWorldReadable() {
        try {
            val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "spoof_prefs.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
                prefsDir.setReadable(true, false)
                Runtime.getRuntime().exec("su -c chmod 644 ${prefsFile.absolutePath}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isValidImei(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) return false
        val number = imei.substring(0, 14)
        val checkDigit = imei.last().digitToInt()
        return luhnChecksum(number) == checkDigit
    }

    private fun generateValidImei(): String {
        val tac = "35" + (100000..999999).random()
        val serial = (100000..999999).random().toString()
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

    private fun generateRandomId(len: Int) = (1..len).map { "0123456789abcdef".random() }.joinToString("")

    private fun generateRandomMac(): String {
        val bytes = ByteArray(6)
        java.util.Random().nextBytes(bytes)
        bytes[0] = (bytes[0].toInt() and 0xFC or 0x02).toByte()
        return bytes.joinToString(":") { "%02X".format(it) }
    }

    private fun generateRealisticGmail(): String {
        val names = listOf(
            "juan", "jose", "luis", "carlos", "francisco", "antonio", "jorge", "miguel", "manuel", "pedro",
            "jesus", "alejandro", "david", "daniel", "ricardo", "fernando", "eduardo", "javier", "raul", "roberto"
        )
        val surnames = listOf(
            "rossi", "russo", "ferrari", "esposito", "bianchi", "romano", "colombo", "ricci", "marino", "greco"
        )
        val name = names.random()
        val surname = surnames.random()
        val randomNum = (1..9999).random().toString()

        return "$name$surname$randomNum@gmail.com"
    }
}
