package com.vortex.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.vortex.DeviceData
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.utils.ValidationUtils
import com.vortex.R
import com.vortex.adapters.CarrierAdapter

class NetworkFragment : Fragment() {

    private lateinit var rvCarriers: RecyclerView
    private lateinit var adapter: CarrierAdapter
    private lateinit var btnRandomAll: Button
    private lateinit var btnSave: Button

    // Search
    private lateinit var etSearch: TextInputEditText

    // Campos
    private lateinit var etSimOp: TextInputEditText; private lateinit var tilSimOp: TextInputLayout
    private lateinit var etSimCountry: TextInputEditText; private lateinit var tilSimCountry: TextInputLayout
    private lateinit var etImsi: TextInputEditText; private lateinit var tilImsi: TextInputLayout
    private lateinit var etIccid: TextInputEditText; private lateinit var tilIccid: TextInputLayout
    private lateinit var etPhone: TextInputEditText; private lateinit var tilPhone: TextInputLayout
    private lateinit var etSsid: TextInputEditText; private lateinit var tilSsid: TextInputLayout
    private lateinit var etBssid: TextInputEditText; private lateinit var tilBssid: TextInputLayout
    private lateinit var etWifiMac: TextInputEditText; private lateinit var tilWifiMac: TextInputLayout
    private lateinit var etBtMac: TextInputEditText; private lateinit var tilBtMac: TextInputLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)

        btnRandomAll = view.findViewById(R.id.btn_random_network)
        btnSave = view.findViewById(R.id.btn_save_network)
        rvCarriers = view.findViewById(R.id.rv_carriers)
        etSearch = view.findViewById(R.id.et_search_carrier)

        // Bind fields
        etSimOp = view.findViewById(R.id.et_sim_operator); tilSimOp = view.findViewById(R.id.til_sim_operator)
        etSimCountry = view.findViewById(R.id.et_sim_country); tilSimCountry = view.findViewById(R.id.til_sim_country)
        etImsi = view.findViewById(R.id.et_imsi); tilImsi = view.findViewById(R.id.til_imsi)
        etIccid = view.findViewById(R.id.et_iccid); tilIccid = view.findViewById(R.id.til_iccid)
        etPhone = view.findViewById(R.id.et_phone); tilPhone = view.findViewById(R.id.til_phone)
        etSsid = view.findViewById(R.id.et_ssid); tilSsid = view.findViewById(R.id.til_ssid)
        etBssid = view.findViewById(R.id.et_bssid); tilBssid = view.findViewById(R.id.til_bssid)
        etWifiMac = view.findViewById(R.id.et_wifi_mac); tilWifiMac = view.findViewById(R.id.til_wifi_mac)
        etBtMac = view.findViewById(R.id.et_bt_mac); tilBtMac = view.findViewById(R.id.til_bt_mac)

        setupCarrierList()
        setupListeners()
        loadData()

        return view
    }

    private fun setupCarrierList() {
        adapter = CarrierAdapter(DeviceData.getUsCarriers()) { carrier ->
            val ctx = requireContext()
            // Set context fields (read-only base)
            etSimOp.setText("${carrier.name} (${carrier.mccMnc})")
            etSimCountry.setText("us") // Always US for these carriers

            // Save context immediately
            PrefsManager.saveString(ctx, "mcc_mnc", carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name", carrier.name)
            PrefsManager.saveString(ctx, "sim_country", "us")

            Toast.makeText(ctx, "Carrier Context Set: ${carrier.name}. Now use Randomize.", Toast.LENGTH_SHORT).show()
        }
        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        val ctx = requireContext()
        val carrierName = PrefsManager.getString(ctx, "carrier_name", "Unknown")
        val mccMnc = PrefsManager.getString(ctx, "mcc_mnc", "—")

        etSimOp.setText("$carrierName ($mccMnc)")
        etSimCountry.setText(PrefsManager.getString(ctx, "sim_country", "us"))

        etImsi.setText(PrefsManager.getString(ctx, "imsi", ""))
        etIccid.setText(PrefsManager.getString(ctx, "iccid", ""))
        etPhone.setText(PrefsManager.getString(ctx, "phone_number", ""))
        etSsid.setText(PrefsManager.getString(ctx, "wifi_ssid", ""))
        etBssid.setText(PrefsManager.getString(ctx, "wifi_bssid", ""))
        etWifiMac.setText(PrefsManager.getString(ctx, "wifi_mac", ""))
        etBtMac.setText(PrefsManager.getString(ctx, "bluetooth_mac", ""))

        // Restore selected carrier in list
        val savedMcc = PrefsManager.getString(ctx, "mcc_mnc", "310260")
        adapter.setSelected(savedMcc)

        val imsi = etImsi.text.toString()
        val iccid = etIccid.text.toString()
        val bssid = etBssid.text.toString()
        val wMac = etWifiMac.text.toString()
        val bMac = etBtMac.text.toString()
        if (imsi.isNotEmpty()) setValidationBadge(tilImsi, ValidationUtils.isValidImsi(imsi))
        if (iccid.isNotEmpty()) setValidationBadge(tilIccid, ValidationUtils.isValidIccid(iccid))
        if (bssid.isNotEmpty()) setValidationBadge(tilBssid, ValidationUtils.isValidMac(bssid))
        if (wMac.isNotEmpty()) setValidationBadge(tilWifiMac, ValidationUtils.isValidMac(wMac))
        if (bMac.isNotEmpty()) setValidationBadge(tilBtMac, ValidationUtils.isValidMac(bMac))
    }

    private fun setupListeners() {
        // Individual Randomizers
        tilImsi.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val v = SpoofingUtils.generateValidImsi(mcc)
            etImsi.setText(v)
            setValidationBadge(tilImsi, ValidationUtils.isValidImsi(v))
        }
        tilIccid.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val v = SpoofingUtils.generateValidIccid(mcc)
            etIccid.setText(v)
            setValidationBadge(tilIccid, ValidationUtils.isValidIccid(v))
        }
        tilPhone.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val carrier = DeviceData.getUsCarriers().find { it.mccMnc == mcc }
            val v = SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList())
            etPhone.setText(v)
            setValidationBadge(tilPhone, v.startsWith("+1") && v.length >= 12)
        }
        tilSsid.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRealisticSsid()
            etSsid.setText(v)
            setValidationBadge(tilSsid, v.isNotEmpty())
        }
        tilBssid.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomMac()
            etBssid.setText(v)
            setValidationBadge(tilBssid, ValidationUtils.isValidMac(v))
        }
        tilWifiMac.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomMac()
            etWifiMac.setText(v)
            setValidationBadge(tilWifiMac, ValidationUtils.isValidMac(v))
        }
        tilBtMac.setEndIconOnClickListener {
            val v = SpoofingUtils.generateRandomMac()
            etBtMac.setText(v)
            setValidationBadge(tilBtMac, ValidationUtils.isValidMac(v))
        }

        // Randomize All
        btnRandomAll.setOnClickListener {
            val ctx = requireContext()
            // 1. Pick random carrier
            val carrier = DeviceData.getUsCarriers().random()
            adapter.setSelected(carrier.mccMnc)

            // 2. Set context
            etSimOp.setText("${carrier.name} (${carrier.mccMnc})")
            etSimCountry.setText("us")
            PrefsManager.saveString(ctx, "mcc_mnc", carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name", carrier.name)

            // 3. Generate all values
            val imsi = SpoofingUtils.generateValidImsi(carrier.mccMnc)
            val iccid = SpoofingUtils.generateValidIccid(carrier.mccMnc)
            val phone = SpoofingUtils.generatePhoneNumber(carrier.npas)
            val ssid = SpoofingUtils.generateRealisticSsid()
            val bssid = SpoofingUtils.generateRandomMac()
            val wMac = SpoofingUtils.generateRandomMac()
            val bMac = SpoofingUtils.generateRandomMac()

            etImsi.setText(imsi); setValidationBadge(tilImsi, ValidationUtils.isValidImsi(imsi))
            etIccid.setText(iccid); setValidationBadge(tilIccid, ValidationUtils.isValidIccid(iccid))
            etPhone.setText(phone); setValidationBadge(tilPhone, phone.startsWith("+1") && phone.length >= 12)
            etSsid.setText(ssid); setValidationBadge(tilSsid, ssid.isNotEmpty())
            etBssid.setText(bssid); setValidationBadge(tilBssid, ValidationUtils.isValidMac(bssid))
            etWifiMac.setText(wMac); setValidationBadge(tilWifiMac, ValidationUtils.isValidMac(wMac))
            etBtMac.setText(bMac); setValidationBadge(tilBtMac, ValidationUtils.isValidMac(bMac))

            Toast.makeText(ctx, "Randomized Network Identity", Toast.LENGTH_SHORT).show()
        }

        // Save
        btnSave.setOnClickListener {
            val ctx = requireContext()
            PrefsManager.saveString(ctx, "imsi", etImsi.text.toString())
            PrefsManager.saveString(ctx, "iccid", etIccid.text.toString())
            PrefsManager.saveString(ctx, "phone_number", etPhone.text.toString())
            PrefsManager.saveString(ctx, "wifi_ssid", etSsid.text.toString())
            PrefsManager.saveString(ctx, "wifi_bssid", etBssid.text.toString())
            PrefsManager.saveString(ctx, "wifi_mac", etWifiMac.text.toString())
            PrefsManager.saveString(ctx, "bluetooth_mac", etBtMac.text.toString())

            Toast.makeText(ctx, "Network Identity Saved ✓", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setValidationBadge(til: TextInputLayout, isValid: Boolean) {
        val ctx = requireContext()
        val (text, color) = if (isValid)
            "Valid" to ContextCompat.getColor(ctx, R.color.vortex_success)
        else
            "Invalid" to ContextCompat.getColor(ctx, R.color.vortex_error)
        til.helperText = text
        til.setHelperTextColor(ColorStateList.valueOf(color))
    }
}
