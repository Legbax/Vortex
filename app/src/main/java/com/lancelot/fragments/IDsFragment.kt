package com.lancelot.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lancelot.MainHook
import com.lancelot.PrefsManager
import com.lancelot.SpoofingUtils
import com.lancelot.utils.ValidationUtils
import com.lancelot.R

class IDsFragment : Fragment() {

    private lateinit var tilImei: TextInputLayout
    private lateinit var etImei: TextInputEditText
    private lateinit var tilImei2: TextInputLayout
    private lateinit var etImei2: TextInputEditText
    private lateinit var tilImsi: TextInputLayout
    private lateinit var etImsi: TextInputEditText
    private lateinit var tilIccid: TextInputLayout
    private lateinit var etIccid: TextInputEditText
    private lateinit var tilPhone: TextInputLayout
    private lateinit var etPhone: TextInputEditText
    private lateinit var tilAndroidId: TextInputLayout
    private lateinit var etAndroidId: TextInputEditText
    private lateinit var tilGaid: TextInputLayout
    private lateinit var etGaid: TextInputEditText
    private lateinit var tilGsfId: TextInputLayout
    private lateinit var etGsfId: TextInputEditText
    private lateinit var tilSerial: TextInputLayout
    private lateinit var etSerial: TextInputEditText
    private lateinit var tilWifiMac: TextInputLayout
    private lateinit var etWifiMac: TextInputEditText
    private lateinit var tilBtMac: TextInputLayout
    private lateinit var etBtMac: TextInputEditText
    private lateinit var tilGmail: TextInputLayout
    private lateinit var etGmail: TextInputEditText

    private lateinit var btnRandomize: Button
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ids, container, false)

        bindViews(view)
        setupListeners()
        loadData()

        return view
    }

    private fun bindViews(view: View) {
        tilImei = view.findViewById(R.id.til_imei)
        etImei = view.findViewById(R.id.et_imei)
        tilImei2 = view.findViewById(R.id.til_imei2)
        etImei2 = view.findViewById(R.id.et_imei2)
        tilImsi = view.findViewById(R.id.til_imsi)
        etImsi = view.findViewById(R.id.et_imsi)
        tilIccid = view.findViewById(R.id.til_iccid)
        etIccid = view.findViewById(R.id.et_iccid)
        tilPhone = view.findViewById(R.id.til_phone)
        etPhone = view.findViewById(R.id.et_phone)

        tilAndroidId = view.findViewById(R.id.til_android_id)
        etAndroidId = view.findViewById(R.id.et_android_id)
        tilGaid = view.findViewById(R.id.til_gaid)
        etGaid = view.findViewById(R.id.et_gaid)
        tilGsfId = view.findViewById(R.id.til_gsf_id)
        etGsfId = view.findViewById(R.id.et_gsf_id)
        tilSerial = view.findViewById(R.id.til_serial)
        etSerial = view.findViewById(R.id.et_serial)
        tilWifiMac = view.findViewById(R.id.til_wifi_mac)
        etWifiMac = view.findViewById(R.id.et_wifi_mac)
        tilBtMac = view.findViewById(R.id.til_bt_mac)
        etBtMac = view.findViewById(R.id.et_bt_mac)
        tilGmail = view.findViewById(R.id.til_gmail)
        etGmail = view.findViewById(R.id.et_gmail)

        btnRandomize = view.findViewById(R.id.btn_generate_random)
        btnSave = view.findViewById(R.id.btn_save)
    }

    private fun setupListeners() {
        btnRandomize.setOnClickListener { randomizeAll() }
        btnSave.setOnClickListener { saveData() }

        // Individual Randomization
        tilImei.setEndIconOnClickListener {
            val profile = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
            etImei.setText(SpoofingUtils.generateValidImei(profile))
        }
        tilImei2.setEndIconOnClickListener {
            val profile = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
            etImei2.setText(SpoofingUtils.generateValidImei(profile))
        }

        tilImsi.setEndIconOnClickListener {
            val mccMnc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            etImsi.setText(SpoofingUtils.generateValidImsi(mccMnc))
        }

        tilIccid.setEndIconOnClickListener {
            val mccMnc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            etIccid.setText(SpoofingUtils.generateValidIccid(mccMnc))
        }

        tilPhone.setEndIconOnClickListener {
            val mccMnc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
            etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))
        }

        tilAndroidId.setEndIconOnClickListener { etAndroidId.setText(SpoofingUtils.generateRandomId(16)) }
        tilGaid.setEndIconOnClickListener { etGaid.setText(SpoofingUtils.generateRandomGaid()) }
        tilGsfId.setEndIconOnClickListener { etGsfId.setText(SpoofingUtils.generateRandomId(16)) }
        tilSerial.setEndIconOnClickListener { etSerial.setText(SpoofingUtils.generateRandomSerial()) }
        tilWifiMac.setEndIconOnClickListener { etWifiMac.setText(SpoofingUtils.generateRandomMac()) }
        tilBtMac.setEndIconOnClickListener { etBtMac.setText(SpoofingUtils.generateRandomMac()) }
        tilGmail.setEndIconOnClickListener { etGmail.setText(SpoofingUtils.generateRealisticGmail()) }
    }

    private fun loadData() {
        val context = requireContext()
        etImei.setText(PrefsManager.getString(context, "imei", ""))
        etImei2.setText(PrefsManager.getString(context, "imei2", ""))
        etImsi.setText(PrefsManager.getString(context, "imsi", ""))
        etIccid.setText(PrefsManager.getString(context, "iccid", ""))
        etPhone.setText(PrefsManager.getString(context, "phone_number", ""))
        etAndroidId.setText(PrefsManager.getString(context, "android_id", ""))
        etGaid.setText(PrefsManager.getString(context, "gaid", ""))
        etGsfId.setText(PrefsManager.getString(context, "gsf_id", ""))
        etSerial.setText(PrefsManager.getString(context, "serial", ""))
        etWifiMac.setText(PrefsManager.getString(context, "wifi_mac", ""))
        etBtMac.setText(PrefsManager.getString(context, "bluetooth_mac", ""))
        etGmail.setText(PrefsManager.getString(context, "gmail", ""))
    }

    private fun randomizeAll() {
        val context = requireContext()
        val mccMnc = PrefsManager.getString(context, "mcc_mnc", "310260")
        val profile = PrefsManager.getString(context, "profile", "Redmi 9")

        etImei.setText(SpoofingUtils.generateValidImei(profile))
        etImei2.setText(SpoofingUtils.generateValidImei(profile))
        etImsi.setText(SpoofingUtils.generateValidImsi(mccMnc))
        etIccid.setText(SpoofingUtils.generateValidIccid(mccMnc))

        // Use carrier for phone number
        val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
        etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))

        etAndroidId.setText(SpoofingUtils.generateRandomId(16))
        etGaid.setText(SpoofingUtils.generateRandomGaid())
        etGsfId.setText(SpoofingUtils.generateRandomId(16))
        etSerial.setText(SpoofingUtils.generateRandomSerial())
        etWifiMac.setText(SpoofingUtils.generateRandomMac())
        etBtMac.setText(SpoofingUtils.generateRandomMac())
        etGmail.setText(SpoofingUtils.generateRealisticGmail())
    }

    private fun saveData() {
        val imei1 = etImei.text.toString()
        val imei2 = etImei2.text.toString()
        val iccid = etIccid.text.toString()

        if (imei1.isNotEmpty() && !ValidationUtils.isValidImei(imei1)) {
            tilImei.error = "Invalid IMEI (Luhn check failed)"
            return
        } else {
            tilImei.error = null
        }

        if (imei2.isNotEmpty() && !ValidationUtils.isValidImei(imei2)) {
            tilImei2.error = "Invalid IMEI (Luhn check failed)"
            return
        } else {
            tilImei2.error = null
        }

        if (iccid.isNotEmpty() && !ValidationUtils.isValidIccid(iccid)) {
            tilIccid.error = "Invalid ICCID (checksum incorrect)"
            return
        } else {
            tilIccid.error = null
        }

        val context = requireContext()

        PrefsManager.saveString(context, "imei", imei1)
        PrefsManager.saveString(context, "imei2", imei2)
        PrefsManager.saveString(context, "imsi", etImsi.text.toString())
        PrefsManager.saveString(context, "iccid", iccid)
        PrefsManager.saveString(context, "phone_number", etPhone.text.toString())
        PrefsManager.saveString(context, "android_id", etAndroidId.text.toString())
        PrefsManager.saveString(context, "gaid", etGaid.text.toString())
        PrefsManager.saveString(context, "gsf_id", etGsfId.text.toString())
        PrefsManager.saveString(context, "serial", etSerial.text.toString())
        PrefsManager.saveString(context, "wifi_mac", etWifiMac.text.toString())
        PrefsManager.saveString(context, "bluetooth_mac", etBtMac.text.toString())
        PrefsManager.saveString(context, "gmail", etGmail.text.toString())

        Toast.makeText(context, "IDs Saved Securely", Toast.LENGTH_SHORT).show()
    }
}
