package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.vortex.MainHook
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

        loadData()

        btnRandomize.setOnClickListener { randomizeAll() }
        btnSave.setOnClickListener      { saveData() }

        // Botones de randomize individuales por campo
        val ctx = requireContext()
        tilImei.setEndIconOnClickListener {
            val p = PrefsManager.getString(ctx, "profile", "Redmi 9")
            etImei.setText(SpoofingUtils.generateValidImei(p))
        }
        tilImei2.setEndIconOnClickListener {
            val p = PrefsManager.getString(ctx, "profile", "Redmi 9")
            etImei2.setText(SpoofingUtils.generateValidImei(p))
        }
        tilAndroidId.setEndIconOnClickListener { etAndroidId.setText(SpoofingUtils.generateRandomId(16)) }
        tilSsaid.setEndIconOnClickListener     { etSsaid.setText(SpoofingUtils.generateRandomId(16)) }
        tilGaid.setEndIconOnClickListener      { etGaid.setText(SpoofingUtils.generateRandomGaid()) }
        tilGsfId.setEndIconOnClickListener     { etGsfId.setText(SpoofingUtils.generateRandomId(16)) }
        tilMediaDrm.setEndIconOnClickListener  { etMediaDrm.setText(SpoofingUtils.generateRandomId(32)) }
        tilSerial.setEndIconOnClickListener    {
            val brand = MainHook.DEVICE_FINGERPRINTS[PrefsManager.getString(ctx,"profile","")]?.brand ?: ""
            etSerial.setText(SpoofingUtils.generateRandomSerial(brand))
        }
        tilGmail.setEndIconOnClickListener     { etGmail.setText(SpoofingUtils.generateRealisticGmail()) }

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
    }

    private fun randomizeAll() {
        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "Redmi 9")
        val brand = MainHook.DEVICE_FINGERPRINTS[profile]?.brand ?: ""

        etImei.setText(SpoofingUtils.generateValidImei(profile))
        etImei2.setText(SpoofingUtils.generateValidImei(profile))
        etAndroidId.setText(SpoofingUtils.generateRandomId(16))
        etSsaid.setText(SpoofingUtils.generateRandomId(16))
        etGaid.setText(SpoofingUtils.generateRandomGaid())
        etGsfId.setText(SpoofingUtils.generateRandomId(16))
        etMediaDrm.setText(SpoofingUtils.generateRandomId(32))
        etSerial.setText(SpoofingUtils.generateRandomSerial(brand))
        etGmail.setText(SpoofingUtils.generateRealisticGmail())
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
