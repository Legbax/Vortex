package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.R

class NetworkFragment : Fragment() {

    private lateinit var rvCarriers: RecyclerView
    private lateinit var adapter: CarrierAdapter
    private lateinit var btnRandomAll: Button
    private lateinit var btnSave: Button

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
        adapter = CarrierAdapter(MainHook.getUsCarriers()) { carrier ->
            val ctx = requireContext()
            // Set context fields (read-only base)
            etSimOp.setText("${carrier.name} (${carrier.mccMnc})")
            etSimCountry.setText("us") // Always US for these carriers

            // Save context immediately
            PrefsManager.saveString(ctx, "mcc_mnc", carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name", carrier.name)
            PrefsManager.saveString(ctx, "sim_country", "us")

            Toast.makeText(ctx, "Carrier Context Set: ${carrier.name}", Toast.LENGTH_SHORT).show()
        }
        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter
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
    }

    private fun setupListeners() {
        // Individual Randomizers
        tilImsi.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            etImsi.setText(SpoofingUtils.generateValidImsi(mcc))
        }
        tilIccid.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            etIccid.setText(SpoofingUtils.generateValidIccid(mcc))
        }
        tilPhone.setEndIconOnClickListener {
            val mcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == mcc }
            etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier?.npas ?: emptyList()))
        }
        tilSsid.setEndIconOnClickListener { etSsid.setText(SpoofingUtils.generateRealisticSsid()) }
        tilBssid.setEndIconOnClickListener { etBssid.setText(SpoofingUtils.generateRandomMac()) }
        tilWifiMac.setEndIconOnClickListener { etWifiMac.setText(SpoofingUtils.generateRandomMac()) }
        tilBtMac.setEndIconOnClickListener { etBtMac.setText(SpoofingUtils.generateRandomMac()) }

        // Randomize All
        btnRandomAll.setOnClickListener {
            val ctx = requireContext()
            // 1. Pick random carrier
            val carrier = MainHook.getUsCarriers().random()
            adapter.setSelected(carrier.mccMnc)

            // 2. Set context
            etSimOp.setText("${carrier.name} (${carrier.mccMnc})")
            etSimCountry.setText("us")
            PrefsManager.saveString(ctx, "mcc_mnc", carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name", carrier.name)

            // 3. Generate all values
            etImsi.setText(SpoofingUtils.generateValidImsi(carrier.mccMnc))
            etIccid.setText(SpoofingUtils.generateValidIccid(carrier.mccMnc))
            etPhone.setText(SpoofingUtils.generatePhoneNumber(carrier.npas))
            etSsid.setText(SpoofingUtils.generateRealisticSsid())
            etBssid.setText(SpoofingUtils.generateRandomMac())
            etWifiMac.setText(SpoofingUtils.generateRandomMac())
            etBtMac.setText(SpoofingUtils.generateRandomMac())

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
}
