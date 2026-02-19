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

class NetworkFragment : Fragment() {

    private lateinit var tilSimOperator: TextInputLayout
    private lateinit var etSimOperator: TextInputEditText
    private lateinit var tilMccMnc: TextInputLayout
    private lateinit var etMccMnc: TextInputEditText
    private lateinit var tilIccid: TextInputLayout
    private lateinit var etIccid: TextInputEditText
    private lateinit var tilImsi: TextInputLayout
    private lateinit var etImsi: TextInputEditText
    private lateinit var tilSimCountry: TextInputLayout
    private lateinit var etSimCountry: TextInputEditText
    private lateinit var tilPhoneNumber: TextInputLayout
    private lateinit var etPhoneNumber: TextInputEditText

    private lateinit var tilWifiSsid: TextInputLayout
    private lateinit var etWifiSsid: TextInputEditText
    private lateinit var tilWifiBssid: TextInputLayout
    private lateinit var etWifiBssid: TextInputEditText
    private lateinit var tilWifiMac: TextInputLayout
    private lateinit var etWifiMac: TextInputEditText
    private lateinit var tilBtMac: TextInputLayout
    private lateinit var etBtMac: TextInputEditText

    private lateinit var btnRandomize: Button
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        bindViews(view)
        setupListeners()
        loadData()
        return view
    }

    private fun bindViews(view: View) {
        tilSimOperator = view.findViewById(R.id.til_sim_operator)
        etSimOperator = view.findViewById(R.id.et_sim_operator)
        tilMccMnc = view.findViewById(R.id.til_mcc_mnc)
        etMccMnc = view.findViewById(R.id.et_mcc_mnc)
        tilIccid = view.findViewById(R.id.til_iccid)
        etIccid = view.findViewById(R.id.et_iccid)
        tilImsi = view.findViewById(R.id.til_imsi)
        etImsi = view.findViewById(R.id.et_imsi)
        tilSimCountry = view.findViewById(R.id.til_sim_country)
        etSimCountry = view.findViewById(R.id.et_sim_country)
        tilPhoneNumber = view.findViewById(R.id.til_phone_number)
        etPhoneNumber = view.findViewById(R.id.et_phone_number)

        tilWifiSsid = view.findViewById(R.id.til_wifi_ssid)
        etWifiSsid = view.findViewById(R.id.et_wifi_ssid)
        tilWifiBssid = view.findViewById(R.id.til_wifi_bssid)
        etWifiBssid = view.findViewById(R.id.et_wifi_bssid)
        tilWifiMac = view.findViewById(R.id.til_wifi_mac)
        etWifiMac = view.findViewById(R.id.et_wifi_mac)
        tilBtMac = view.findViewById(R.id.til_bt_mac)
        etBtMac = view.findViewById(R.id.et_bt_mac)

        btnRandomize = view.findViewById(R.id.btn_randomize_network)
        btnSave = view.findViewById(R.id.btn_save)
    }

    private fun setupListeners() {
        btnRandomize.setOnClickListener { randomizeNetwork() }
        btnSave.setOnClickListener { saveData() }

        // Individual Randomizers
        tilMccMnc.setEndIconOnClickListener {
            // Pick random US carrier
            val carrier = MainHook.getUsCarriers().random()
            etMccMnc.setText(carrier.mccMnc)
            etSimOperator.setText(carrier.name)
        }

        tilIccid.setEndIconOnClickListener {
            val mccMnc = etMccMnc.text.toString().takeIf { it.isNotEmpty() } ?: "310260"
            etIccid.setText(SpoofingUtils.generateValidIccid(mccMnc))
        }

        tilImsi.setEndIconOnClickListener {
            val mccMnc = etMccMnc.text.toString().takeIf { it.isNotEmpty() } ?: "310260"
            etImsi.setText(SpoofingUtils.generateValidImsi(mccMnc))
        }

        tilPhoneNumber.setEndIconOnClickListener {
            val mccMnc = etMccMnc.text.toString().takeIf { it.isNotEmpty() } ?: "310260"
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
            etPhoneNumber.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))
        }

        tilWifiSsid.setEndIconOnClickListener { etWifiSsid.setText("Vortex-${(100..999).random()}") }
        tilWifiBssid.setEndIconOnClickListener { etWifiBssid.setText(SpoofingUtils.generateRandomMac()) }
        tilWifiMac.setEndIconOnClickListener { etWifiMac.setText(SpoofingUtils.generateRandomMac()) }
        tilBtMac.setEndIconOnClickListener { etBtMac.setText(SpoofingUtils.generateRandomMac()) }

        tilSimCountry.setEndIconOnClickListener { etSimCountry.setText("us") }
    }

    private fun loadData() {
        val context = requireContext()
        val mccMnc = PrefsManager.getString(context, "mcc_mnc", "310260")

        etMccMnc.setText(mccMnc)

        // Derive operator name from mccMnc
        val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
        etSimOperator.setText(carrier?.name ?: "Unknown")

        etIccid.setText(PrefsManager.getString(context, "iccid", ""))
        etImsi.setText(PrefsManager.getString(context, "imsi", ""))
        etSimCountry.setText("us") // Fixed for now as per hooks
        etPhoneNumber.setText(PrefsManager.getString(context, "phone_number", ""))

        etWifiSsid.setText(PrefsManager.getString(context, "wifi_ssid", ""))
        etWifiBssid.setText(PrefsManager.getString(context, "wifi_bssid", ""))
        etWifiMac.setText(PrefsManager.getString(context, "wifi_mac", ""))
        etBtMac.setText(PrefsManager.getString(context, "bluetooth_mac", ""))
    }

    private fun randomizeNetwork() {
        val carrier = MainHook.getUsCarriers().random()
        val mccMnc = carrier.mccMnc

        etMccMnc.setText(mccMnc)
        etSimOperator.setText(carrier.name)
        etIccid.setText(SpoofingUtils.generateValidIccid(mccMnc))
        etImsi.setText(SpoofingUtils.generateValidImsi(mccMnc))
        etSimCountry.setText("us")
        etPhoneNumber.setText(SpoofingUtils.generatePhoneNumber(carrier.npas))

        etWifiSsid.setText("Vortex-${(100..999).random()}")
        etWifiBssid.setText(SpoofingUtils.generateRandomMac())
        etWifiMac.setText(SpoofingUtils.generateRandomMac())
        etBtMac.setText(SpoofingUtils.generateRandomMac())
    }

    private fun saveData() {
        val context = requireContext()

        val mccMnc = etMccMnc.text.toString()
        val imsi = etImsi.text.toString()
        val iccid = etIccid.text.toString()

        // Basic validation
        if (iccid.isNotEmpty() && !ValidationUtils.isValidIccid(iccid)) {
            tilIccid.error = "Invalid ICCID Checksum"
            return
        } else {
            tilIccid.error = null
        }

        PrefsManager.saveString(context, "mcc_mnc", mccMnc)
        PrefsManager.saveString(context, "imsi", imsi)
        PrefsManager.saveString(context, "iccid", iccid)
        PrefsManager.saveString(context, "phone_number", etPhoneNumber.text.toString())

        PrefsManager.saveString(context, "wifi_ssid", etWifiSsid.text.toString())
        PrefsManager.saveString(context, "wifi_bssid", etWifiBssid.text.toString())
        PrefsManager.saveString(context, "wifi_mac", etWifiMac.text.toString())
        PrefsManager.saveString(context, "bluetooth_mac", etBtMac.text.toString())

        Toast.makeText(context, "Network Identity Saved", Toast.LENGTH_SHORT).show()
    }
}
