package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.content.res.ColorStateList
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton
import com.vortex.DeviceData
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.utils.ValidationUtils
import com.vortex.R

class IDsFragment : Fragment() {

    private lateinit var tilImei:      TextInputLayout;   private lateinit var etImei:      TextInputEditText
    private lateinit var tilImei2:     TextInputLayout;   private lateinit var etImei2:     TextInputEditText
    private lateinit var tilAndroidId: TextInputLayout;   private lateinit var etAndroidId: TextInputEditText
    private lateinit var tilSsaid:     TextInputLayout;   private lateinit var etSsaid:     TextInputEditText
    private lateinit var tilGaid:      TextInputLayout;   private lateinit var etGaid:      TextInputEditText
    private lateinit var tilGsfId:     TextInputLayout;   private lateinit var etGsfId:     TextInputEditText
    private lateinit var tilMediaDrm:  TextInputLayout;   private lateinit var etMediaDrm:  TextInputEditText
    private lateinit var tilSerial:    TextInputLayout;   private lateinit var etSerial:    TextInputEditText
    private lateinit var tilGmail:     TextInputLayout;   private lateinit var etGmail:     TextInputEditText
    private lateinit var btnRandomize: Button
    private lateinit var btnSave:      Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ids, container, false)

        tilImei      = view.findViewById(R.id.til_imei);       etImei      = view.findViewById(R.id.et_imei)
        tilImei2     = view.findViewById(R.id.til_imei2);      etImei2     = view.findViewById(R.id.et_imei2)
        tilAndroidId = view.findViewById(R.id.til_android_id); etAndroidId = view.findViewById(R.id.et_android_id)
        tilSsaid     = view.findViewById(R.id.til_ssaid);      etSsaid     = view.findViewById(R.id.et_ssaid)
        tilGaid      = view.findViewById(R.id.til_gaid);       etGaid      = view.findViewById(R.id.et_gaid)
        tilGsfId     = view.findViewById(R.id.til_gsf_id);     etGsfId     = view.findViewById(R.id.et_gsf_id)
        tilMediaDrm  = view.findViewById(R.id.til_media_drm);  etMediaDrm  = view.findViewById(R.id.et_media_drm)
        tilSerial    = view.findViewById(R.id.til_serial);     etSerial    = view.findViewById(R.id.et_serial)
        tilGmail     = view.findViewById(R.id.til_gmail);      etGmail     = view.findViewById(R.id.et_gmail)
        btnRandomize = view.findViewById(R.id.btn_generate_random)
        btnSave      = view.findViewById(R.id.btn_save)

        val swGPUSpoof = view.findViewById<SwitchMaterial>(R.id.sw_gpu_spoof)
        val swJA3 = view.findViewById<SwitchMaterial>(R.id.sw_ja3_randomizer)
        val btnRefreshGPU = view.findViewById<MaterialButton>(R.id.btn_refresh_gpu)
        val btnRefreshJA3 = view.findViewById<MaterialButton>(R.id.btn_refresh_ja3)

        val ctx = requireContext()

        swGPUSpoof.isChecked = PrefsManager.getBoolean(ctx, "gpu_spoof_enabled", false)
        swJA3.isChecked = PrefsManager.getBoolean(ctx, "ja3_randomizer_enabled", false)

        swGPUSpoof.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "gpu_spoof_enabled", isChecked) }
        swJA3.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "ja3_randomizer_enabled", isChecked) }

        btnRefreshGPU.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("⚠️ Force Refresh GPU Fingerprint")
                .setMessage("This will break the current fingerprint, You must re-open Snapchat afterwards.\n\nContinue?")
                .setPositiveButton("Yes, Refresh") { _, _ ->
                    val newSeed = java.util.UUID.randomUUID().toString()
                    PrefsManager.saveString(ctx, "gpu_seed", newSeed)
                    Toast.makeText(ctx, "New GPU generated. Re-open Snapchat.", Toast.LENGTH_LONG).show()
                }.setNegativeButton("Cancelar", null).show()
        }

        btnRefreshJA3.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("⚠️ Force Refresh JA3 Fingerprint")
                .setMessage("Esto generará una nueva semilla TLS determinística estable.\n\nContinue?")
                .setPositiveButton("Yes, Refresh") { _, _ ->
                    val newSeed = java.util.UUID.randomUUID().toString()
                    PrefsManager.saveString(ctx, "ja3_seed", newSeed)
                    Toast.makeText(ctx, "New JA3 seed generated. Re-open Snapchat.", Toast.LENGTH_LONG).show()
                }.setNegativeButton("Cancelar", null).show()
        }

        loadData()

        btnRandomize.setOnClickListener { randomizeAll() }
        btnSave.setOnClickListener      { saveData() }

        // Botones de randomize individuales por campo
        tilImei.setEndIconOnClickListener {
            val p = PrefsManager.getString(ctx, "profile", "Redmi 9")
            val v = SpoofingUtils.generateValidImei(p)
            etImei.setText(v)
            setValidationBadge(tilImei, ValidationUtils.isValidImei(v))
        }
        tilImei2.setEndIconOnClickListener {
            val p = PrefsManager.getString(ctx, "profile", "Redmi 9")
            val v = SpoofingUtils.generateValidImei(p)
            etImei2.setText(v)
            setValidationBadge(tilImei2, ValidationUtils.isValidImei(v))
        }
        tilAndroidId.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomId(16)
            etAndroidId.setText(v)
            setValidationBadge(tilAndroidId, ValidationUtils.isValidAndroidId(v))
        }
        tilSsaid.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomId(16)
            etSsaid.setText(v)
            setValidationBadge(tilSsaid, ValidationUtils.isValidAndroidId(v))
        }
        tilGaid.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomGaid()
            etGaid.setText(v)
            setValidationBadge(tilGaid, ValidationUtils.isValidGaid(v))
        }
        tilGsfId.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomId(16)
            etGsfId.setText(v)
            setValidationBadge(tilGsfId, ValidationUtils.isValidAndroidId(v))
        }
        tilMediaDrm.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomId(32)
            etMediaDrm.setText(v)
            setValidationBadge(tilMediaDrm, v.length == 32 && v.all { it.isLetterOrDigit() })
        }
        tilSerial.setEndIconOnClickListener {
            val brand = DeviceData.DEVICE_FINGERPRINTS[PrefsManager.getString(ctx, "profile", "")]?.brand ?: ""
            val v = SpoofingUtils.generateRandomSerial(brand)
            etSerial.setText(v)
            setValidationBadge(tilSerial, v.isNotEmpty())
        }
        tilGmail.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRealisticGmail()
            etGmail.setText(v)
            setValidationBadge(tilGmail, android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches())
        }

        return view
    }

    private fun loadData() {
        val ctx = requireContext()
        etImei.setText(PrefsManager.getString(ctx, "imei", ""))
        etImei2.setText(PrefsManager.getString(ctx, "imei2", ""))
        etAndroidId.setText(PrefsManager.getString(ctx, "android_id", ""))
        etSsaid.setText(PrefsManager.getString(ctx, "ssaid_snapchat", ""))
        etGaid.setText(PrefsManager.getString(ctx, "gaid", ""))
        etGsfId.setText(PrefsManager.getString(ctx, "gsf_id", ""))
        etMediaDrm.setText(PrefsManager.getString(ctx, "media_drm_id", ""))
        etSerial.setText(PrefsManager.getString(ctx, "serial", ""))
        etGmail.setText(PrefsManager.getString(ctx, "gmail", ""))
        validateAndBadgeAll()
    }

    private fun validateAndBadgeAll() {
        val imei = etImei.text.toString()
        val imei2 = etImei2.text.toString()
        val aid = etAndroidId.text.toString()
        val ssaid = etSsaid.text.toString()
        val gaid = etGaid.text.toString()
        val gsf = etGsfId.text.toString()
        val drm = etMediaDrm.text.toString()
        val serial = etSerial.text.toString()
        val gmail = etGmail.text.toString()
        if (imei.isNotEmpty()) setValidationBadge(tilImei, ValidationUtils.isValidImei(imei))
        if (imei2.isNotEmpty()) setValidationBadge(tilImei2, ValidationUtils.isValidImei(imei2))
        if (aid.isNotEmpty()) setValidationBadge(tilAndroidId, ValidationUtils.isValidAndroidId(aid))
        if (ssaid.isNotEmpty()) setValidationBadge(tilSsaid, ValidationUtils.isValidAndroidId(ssaid))
        if (gaid.isNotEmpty()) setValidationBadge(tilGaid, ValidationUtils.isValidGaid(gaid))
        if (gsf.isNotEmpty()) setValidationBadge(tilGsfId, ValidationUtils.isValidAndroidId(gsf))
        if (drm.isNotEmpty()) setValidationBadge(tilMediaDrm, drm.length == 32 && drm.all { it.isLetterOrDigit() })
        if (serial.isNotEmpty()) setValidationBadge(tilSerial, true)
        if (gmail.isNotEmpty()) setValidationBadge(tilGmail, android.util.Patterns.EMAIL_ADDRESS.matcher(gmail).matches())
    }

    private fun randomizeAll() {
        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "Redmi 9")
        val brand = DeviceData.DEVICE_FINGERPRINTS[profile]?.brand ?: ""

        etImei.setText(SpoofingUtils.generateValidImei(profile))
        .also { setValidationBadge(tilImei, true) }
        etImei2.setText(SpoofingUtils.generateValidImei(profile))
        .also { setValidationBadge(tilImei2, true) }
        etAndroidId.setText(SpoofingUtils.generateRandomId(16))
        .also { setValidationBadge(tilAndroidId, true) }
        etSsaid.setText(SpoofingUtils.generateRandomId(16))
        .also { setValidationBadge(tilSsaid, true) }
        etGaid.setText(SpoofingUtils.generateRandomGaid())
        .also { setValidationBadge(tilGaid, true) }
        etGsfId.setText(SpoofingUtils.generateRandomId(16))
        .also { setValidationBadge(tilGsfId, true) }
        etMediaDrm.setText(SpoofingUtils.generateRandomId(32))
        .also { setValidationBadge(tilMediaDrm, true) }
        etSerial.setText(SpoofingUtils.generateRandomSerial(brand))
        .also { setValidationBadge(tilSerial, true) }
        etGmail.setText(SpoofingUtils.generateRealisticGmail())
        .also { setValidationBadge(tilGmail, true) }
    }

    private fun setValidationBadge(til: TextInputLayout, isValid: Boolean) {
        val ctx = requireContext()
        val (text, color) = if (isValid)
            "Valid" to ContextCompat.getColor(ctx, R.color.vortex_success)
        else
            "Invalid" to ContextCompat.getColor(ctx, R.color.vortex_error)
        til.helperText = text
        til.setHelperTextColor(ColorStateList.valueOf(color))
        // Feedback visual en el ícono de validación
        setValidationIcon(til, isValid)
    }

    private fun setValidationIcon(til: TextInputLayout, isValid: Boolean) {
        val icon = if (isValid) R.drawable.ic_check_circle else R.drawable.ic_error_circle
        val color = if (isValid) R.color.vortex_success else R.color.vortex_error

        til.setStartIconDrawable(icon)
        til.setStartIconTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)))
    }

    private fun saveData(): Boolean {
        val ctx = requireContext()
        val imei1 = etImei.text.toString()
        val imei2 = etImei2.text.toString()

        var ok = true

        if (imei1.isNotEmpty() && !ValidationUtils.isValidImei(imei1)) {
            tilImei.error = "Invalid IMEI (Luhn check failed)"; ok = false
            setValidationIcon(tilImei, false)
        } else {
            tilImei.error = null
            if (imei1.isNotEmpty()) setValidationIcon(tilImei, true)
        }

        if (imei2.isNotEmpty() && !ValidationUtils.isValidImei(imei2)) {
            tilImei2.error = "Invalid IMEI (Luhn check failed)"; ok = false
            setValidationIcon(tilImei2, false)
        } else {
            tilImei2.error = null
            if (imei2.isNotEmpty()) setValidationIcon(tilImei2, true)
        }

        if (!ok) return false

        PrefsManager.saveString(ctx, "imei",          imei1)
        PrefsManager.saveString(ctx, "imei2",         imei2)
        PrefsManager.saveString(ctx, "android_id",    etAndroidId.text.toString())
        PrefsManager.saveString(ctx, "ssaid_snapchat", etSsaid.text.toString())
        PrefsManager.saveString(ctx, "gaid",          etGaid.text.toString())
        PrefsManager.saveString(ctx, "gsf_id",        etGsfId.text.toString())
        PrefsManager.saveString(ctx, "media_drm_id",  etMediaDrm.text.toString())
        PrefsManager.saveString(ctx, "serial",        etSerial.text.toString())
        PrefsManager.saveString(ctx, "gmail",         etGmail.text.toString())

        Toast.makeText(ctx, "IDs Saved Securely ✓", Toast.LENGTH_SHORT).show()
        return true
    }
}
