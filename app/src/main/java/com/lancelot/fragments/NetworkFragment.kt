package com.lancelot.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lancelot.MainHook
import com.lancelot.SpoofingUtils
import com.lancelot.R
import java.io.File

class NetworkFragment : Fragment() {

    private lateinit var rvCarriers: RecyclerView
    private lateinit var tvGeneratedInfo: TextView
    private lateinit var adapter: CarrierAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)

        rvCarriers = view.findViewById(R.id.rv_carriers)
        tvGeneratedInfo = view.findViewById(R.id.tv_generated_info)

        setupRecyclerView()
        loadSavedData()

        return view
    }

    private fun setupRecyclerView() {
        val carriers = MainHook.getUsCarriers()
        // Convert to list of CarrierInfo needed for adapter.
        // Wait, MainHook.UsCarrier is what we have.
        // My CarrierAdapter expects CarrierInfo? I named it CarrierInfo in Adapter but UsCarrier in MainHook.
        // I need to fix CarrierAdapter to use MainHook.UsCarrier or rename.
        // I'll update CarrierAdapter to use MainHook.UsCarrier.

        adapter = CarrierAdapter(carriers) { selectedCarrier ->
            saveCarrierPreference(selectedCarrier)
            generateAndDisplayInfo(selectedCarrier)
        }

        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter
    }

    private fun saveCarrierPreference(carrier: MainHook.Companion.UsCarrier) {
        val prefs = requireContext().getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("mcc_mnc", carrier.mccMnc)
            putString("sim_operator", carrier.mccMnc)
            putString("sim_country", "us")
            apply()
        }
        makePrefsWorldReadable()
    }

    private fun loadSavedData() {
        val prefs = requireContext().getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        val savedMccMnc = prefs.getString("mcc_mnc", "310260")

        if (savedMccMnc != null) {
            adapter.setSelected(savedMccMnc)

            // Also display info for this carrier
            val carrier = MainHook.getUsCarriers().find { it.mccMnc == savedMccMnc }
            if (carrier != null) {
                generateAndDisplayInfo(carrier)
            }
        }
    }

    private fun generateAndDisplayInfo(carrier: MainHook.Companion.UsCarrier) {
        val phoneNumber = SpoofingUtils.generatePhoneNumber(carrier.npas)
        val info = "Carrier: ${carrier.name}\n" +
                  "MCC/MNC: ${carrier.mccMnc}\n" +
                  "Country: US\n" +
                  "Generated Number: $phoneNumber"
        tvGeneratedInfo.text = info
    }

    private fun makePrefsWorldReadable() {
        try {
            val prefsDir = File(requireContext().applicationInfo.dataDir, "shared_prefs")
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
}
