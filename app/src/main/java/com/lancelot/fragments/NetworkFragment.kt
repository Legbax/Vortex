package com.lancelot.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.lancelot.MainHook
import com.lancelot.PrefsManager
import com.lancelot.SpoofingUtils
import com.lancelot.R

class NetworkFragment : Fragment() {

    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var btnGenerate: Button
    private lateinit var tvCurrentCarrier: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        etPhoneNumber = view.findViewById(R.id.et_phone_number)
        btnGenerate = view.findViewById(R.id.btn_generate_phone)
        tvCurrentCarrier = view.findViewById(R.id.tv_current_carrier)

        setupListeners()
        loadData()
        return view
    }

    private fun setupListeners() {
        btnGenerate.setOnClickListener {
            val mccMnc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
            generateAndSavePhoneNumber(carrier?.npas ?: emptyList())
        }
    }

    private fun loadData() {
        val mccMnc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
        val carrier = MainHook.getUsCarriers().find { it.mccMnc == mccMnc }
        tvCurrentCarrier.text = "Current Carrier: ${carrier?.name ?: "Unknown"}"

        // Load saved phone number
        etPhoneNumber.setText(PrefsManager.getString(requireContext(), "phone_number", ""))
    }

    private fun generateAndSavePhoneNumber(npas: List<String>) {
        val number = SpoofingUtils.generatePhoneNumber(npas)

        // FIX #27: Persist generated phone number
        PrefsManager.saveString(requireContext(), "phone_number", number)

        // Update UI
        etPhoneNumber.setText(number)
    }
}
