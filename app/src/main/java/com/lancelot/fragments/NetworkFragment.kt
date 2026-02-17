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
import com.lancelot.PrefsManager
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

        adapter = CarrierAdapter(carriers) { selectedCarrier ->
            saveCarrierPreference(selectedCarrier)
            generateAndDisplayInfo(selectedCarrier)
        }

        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter
    }

    private fun saveCarrierPreference(carrier: MainHook.Companion.UsCarrier) {
        val context = requireContext()
        // Save encrypted preferences without world-readable permissions
        PrefsManager.saveString(context, "mcc_mnc", carrier.mccMnc)
        PrefsManager.saveString(context, "sim_operator", carrier.mccMnc)
        PrefsManager.saveString(context, "sim_country", "us")

        Toast.makeText(context, "Carrier saved: ${carrier.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val context = requireContext()
        val savedMccMnc = PrefsManager.getString(context, "mcc_mnc", "310260")

        if (savedMccMnc.isNotEmpty()) {
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
}
