package com.vortex.fragments

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
import com.vortex.MainHook
import com.vortex.SpoofingUtils
import com.vortex.PrefsManager
import com.vortex.R
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

        // FIX #11: Save MCC/MNC and SIM data as PLAIN TEXT using standard SharedPreferences
        // This avoids issues where encryption overhead isn't needed for non-sensitive public data
        // and improves compatibility with MainHook reading logic that might expect standard prefs.
        val prefs = context.getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("mcc_mnc", carrier.mccMnc)
            putString("sim_operator", carrier.mccMnc)
            putString("sim_country", "us")
            apply()
        }

        Toast.makeText(context, "Carrier saved: ${carrier.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val context = requireContext()
        // Load from standard prefs (plain text)
        val prefs = context.getSharedPreferences("spoof_prefs", Context.MODE_PRIVATE)
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
}
