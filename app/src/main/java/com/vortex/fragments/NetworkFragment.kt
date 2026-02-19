package com.vortex.fragments

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
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.R

class NetworkFragment : Fragment() {

    private lateinit var rvCarriers: RecyclerView
    private lateinit var tvInfo: TextView
    private lateinit var adapter: CarrierAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        rvCarriers = view.findViewById(R.id.rv_carriers)
        tvInfo     = view.findViewById(R.id.tv_generated_info)

        adapter = CarrierAdapter(MainHook.getUsCarriers()) { carrier ->
            val ctx = requireContext()
            // Guardar MCC/MNC y nombre del carrier (Contexto de red)
            PrefsManager.saveString(ctx, "mcc_mnc",       carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name",  carrier.name)
            PrefsManager.saveString(ctx, "sim_country",   "us")

            // [LOGIC FIX] NO generar ni guardar IDs automáticamente.
            // El usuario debe ir a IDsFragment para generarlos si lo desea.

            updateInfoDisplay()
            Toast.makeText(ctx, "Carrier Context Set: ${carrier.name}", Toast.LENGTH_SHORT).show()
        }

        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter

        val savedMcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
        adapter.setSelected(savedMcc)
        updateInfoDisplay()

        return view
    }

    private fun updateInfoDisplay() {
        val ctx = requireContext()
        val carrierName = PrefsManager.getString(ctx, "carrier_name", "Unknown")
        val mccMnc      = PrefsManager.getString(ctx, "mcc_mnc", "—")
        // Mostrar valores GUARDADOS, no generados al vuelo
        val imsi        = PrefsManager.getString(ctx, "imsi", "Not generated")
        val phone       = PrefsManager.getString(ctx, "phone_number", "Not generated")

        tvInfo.text = getString(R.string.network_info_format, carrierName, mccMnc, carrierName, imsi, phone)
    }
}
