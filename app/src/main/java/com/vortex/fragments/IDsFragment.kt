package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
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
    private lateinit var tilImsi:      TextInputLayout;   private lateinit var etImsi:      TextInputEditText
    private lateinit var tilIccid:     TextInputLayout;   private lateinit var etIccid:     TextInputEditText
    private lateinit var tilPhone:     TextInputLayout;   private lateinit var etPhone:     TextInputEditText
    private lateinit var tilAndroidId: TextInputLayout;   private lateinit var etAndroidId: TextInputEditText
    private lateinit var tilSsaid:     TextInputLayout;   private lateinit var etSsaid:     TextInputEditText
    private lateinit var tilGaid:      TextInputLayout;   private lateinit var etGaid:      TextInputEditText
    private lateinit var tilGsfId:     TextInputLayout;   private lateinit var etGsfId:     TextInputEditText
    private lateinit var tilMediaDrm:  TextInputLayout;   private lateinit var etMediaDrm:  TextInputEditText
    private lateinit var tilSerial:    TextInputLayout;   private lateinit var etSerial:    TextInputEditText
    private lateinit var tilWifiMac:   TextInputLayout;   private lateinit var etWifiMac:   TextInputEditText
    private lateinit var tilBtMac:     TextInputLayout;   private lateinit var etBtMac:     TextInputEditText
    private lateinit var tilGmail:     TextInputLayout;   private lateinit var etGmail:     TextInputEditText
    private lateinit var btnRandomize: Button
    private lateinit var btnSave:      Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ids, container, false)

        tilImei      = view.findViewById(R.id.til_imei);       etImei      = view.findViewById(R.id.et_imei)
        tilImei2     = view.findViewById(R.id.til_imei2);      etImei2     = view.findViewById(R.id.et_imei2)
        tilImsi      = view.findViewById(R.id.til_imsi);       etImsi      = view.findViewById(R.id.et_imsi)
        tilIccid     = view.findViewById(R.id.til_iccid);      etIccid     = view.findViewById(R.id.et_iccid)
        tilPhone     = view.findViewById(R.id.til_phone);      etPhone     = view.findViewById(R.id.et_phone)
        tilAndroidId = view.findViewById(R.id.til_android_id); etAndroidId = view.findViewById(R.id.et_android_id)
        tilSsaid     = view.findViewById(R.id.til_ssaid);      etSsaid     = view.findViewById(R.id.et_ssaid)
        tilGaid      = view.findViewById(R.id.til_gaid);       etGaid      = view.findViewById(R.id.et_gaid)
        tilGsfId     = view.findViewById(R.id.til_gsf_id);     etGsfId     = view.findViewById(R.id.et_gsf_id)
        tilMediaDrm  = view.findViewById(R.id.til_media_drm);  etMediaDrm  = view.findViewById(R.id.et_media_drm)
        tilSerial    = view.findViewById(R.id.til_serial);     etSerial    = view.findViewById(R.id.et_serial)
        tilWifiMac   = view.findViewById(R.id.til_wifi_mac);   etWifiMac   = view.findViewById(R.id.et_wifi_mac)
        tilBtMac     = view.findViewById(R.id.til_bt_mac);     etBtMac     = view.findViewById(R.id.et_bt_mac)
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
        tilImsi.setEndIconOnClickListener {
            etImsi.setText(SpoofingUtils.generateValidImsi(PrefsManager.getString(ctx,"mcc_mnc","310260")))
        }
        tilIccid.setEndIconOnClickListener {
            etIccid.setText(SpoofingUtils.generateValidIccid(PrefsManager.getString(ctx,"mcc_mnc","310260")))
        }
        tilPhone.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(ctx, "mcc_mnc", "310260")
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == mcc }
            etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))
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
        tilWifiMac.setEndIconOnClickListener   { etWifiMac.setText(SpoofingUtils.generateRandomMac()) }
        tilBtMac.setEndIconOnClickListener     { etBtMac.setText(SpoofingUtils.generateRandomMac()) }
        tilGmail.setEndIconOnClickListener     { etGmail.setText(SpoofingUtils.generateRealisticGmail()) }

        return view
    }

    private fun loadData() {
        val ctx = requireContext()
        etImei.setText(PrefsManager.getString(ctx, "imei", ""))
        etImei2.setText(PrefsManager.getString(ctx, "imei2", ""))
        etImsi.setText(PrefsManager.getString(ctx, "imsi", ""))
        etIccid.setText(PrefsManager.getString(ctx, "iccid", ""))
        etPhone.setText(PrefsManager.getString(ctx, "phone_number", ""))
        etAndroidId.setText(PrefsManager.getString(ctx, "android_id", ""))
        etSsaid.setText(PrefsManager.getString(ctx, "ssaid_snapchat", ""))
        etGaid.setText(PrefsManager.getString(ctx, "gaid", ""))
        etGsfId.setText(PrefsManager.getString(ctx, "gsf_id", ""))
        etMediaDrm.setText(PrefsManager.getString(ctx, "media_drm_id", ""))
        etSerial.setText(PrefsManager.getString(ctx, "serial", ""))
        etWifiMac.setText(PrefsManager.getString(ctx, "wifi_mac", ""))
        etBtMac.setText(PrefsManager.getString(ctx, "bluetooth_mac", ""))
        etGmail.setText(PrefsManager.getString(ctx, "gmail", ""))
    }

    private fun randomizeAll() {
        val ctx = requireContext()
        val mcc = PrefsManager.getString(ctx, "mcc_mnc", "310260")
        val profile = PrefsManager.getString(ctx, "profile", "Redmi 9")
        val brand = MainHook.DEVICE_FINGERPRINTS[profile]?.brand ?: ""
        val carrier = MainHook.getUsCarriers().find { it.mccMnc == mcc }

        etImei.setText(SpoofingUtils.generateValidImei(profile))
        etImei2.setText(SpoofingUtils.generateValidImei(profile))
        etImsi.setText(SpoofingUtils.generateValidImsi(mcc))
        etIccid.setText(SpoofingUtils.generateValidIccid(mcc))
        etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))
        etAndroidId.setText(SpoofingUtils.generateRandomId(16))
        etSsaid.setText(SpoofingUtils.generateRandomId(16))
        etGaid.setText(SpoofingUtils.generateRandomGaid())
        etGsfId.setText(SpoofingUtils.generateRandomId(16))
        etMediaDrm.setText(SpoofingUtils.generateRandomId(32))
        etSerial.setText(SpoofingUtils.generateRandomSerial(brand))
        etWifiMac.setText(SpoofingUtils.generateRandomMac())
        etBtMac.setText(SpoofingUtils.generateRandomMac())
        etGmail.setText(SpoofingUtils.generateRealisticGmail())
    }

    private fun saveData(): Boolean {
        val ctx = requireContext()
        val imei1 = etImei.text.toString()
        val imei2 = etImei2.text.toString()
        val iccid = etIccid.text.toString()
        val imsi  = etImsi.text.toString()

        var ok = true

        if (imei1.isNotEmpty() && !ValidationUtils.isValidImei(imei1)) {
            tilImei.error = "Invalid IMEI (Luhn check failed)"; ok = false
        } else tilImei.error = null

        if (imei2.isNotEmpty() && !ValidationUtils.isValidImei(imei2)) {
            tilImei2.error = "Invalid IMEI (Luhn check failed)"; ok = false
        } else tilImei2.error = null

        if (iccid.isNotEmpty() && !ValidationUtils.isValidIccid(iccid)) {
            tilIccid.error = "Invalid ICCID (must be 19-20 digits starting with 89)"; ok = false
        } else tilIccid.error = null

        if (imsi.isNotEmpty() && !ValidationUtils.isValidImsi(imsi)) {
            tilImsi.error = "Invalid IMSI (must be 14-15 digits)"; ok = false
        } else tilImsi.error = null

        if (!ok) return false

        PrefsManager.saveString(ctx, "imei",          imei1)
        PrefsManager.saveString(ctx, "imei2",         imei2)
        PrefsManager.saveString(ctx, "imsi",          imsi)
        PrefsManager.saveString(ctx, "iccid",         iccid)
        PrefsManager.saveString(ctx, "phone_number",  etPhone.text.toString())
        PrefsManager.saveString(ctx, "android_id",    etAndroidId.text.toString())
        PrefsManager.saveString(ctx, "ssaid_snapchat", etSsaid.text.toString())
        PrefsManager.saveString(ctx, "gaid",          etGaid.text.toString())
        PrefsManager.saveString(ctx, "gsf_id",        etGsfId.text.toString())
        PrefsManager.saveString(ctx, "media_drm_id",  etMediaDrm.text.toString())
        PrefsManager.saveString(ctx, "serial",        etSerial.text.toString())
        PrefsManager.saveString(ctx, "wifi_mac",      etWifiMac.text.toString())
        PrefsManager.saveString(ctx, "bluetooth_mac", etBtMac.text.toString())
        PrefsManager.saveString(ctx, "gmail",         etGmail.text.toString())

        Toast.makeText(ctx, "IDs Saved Securely ✓", Toast.LENGTH_SHORT).show()
        return true
    }
}
