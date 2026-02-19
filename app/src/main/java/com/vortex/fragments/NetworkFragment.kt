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
            // Guardar MCC/MNC y nombre del carrier (para diferenciar T-Mobile vs Metro vs Google Fi)
            PrefsManager.saveString(ctx, "mcc_mnc",       carrier.mccMnc)
            PrefsManager.saveString(ctx, "carrier_name",  carrier.name)
            PrefsManager.saveString(ctx, "sim_country",   "us")
            // [FIX U4] Regenerar IDs dependientes automáticamente
            PrefsManager.saveString(ctx, "imsi",          SpoofingUtils.generateValidImsi(carrier.mccMnc))
            PrefsManager.saveString(ctx, "iccid",         SpoofingUtils.generateValidIccid(carrier.mccMnc))
            PrefsManager.saveString(ctx, "phone_number",  SpoofingUtils.generatePhoneNumber(carrier.npas))
            showInfo(carrier)
            Toast.makeText(ctx, "Carrier saved: ${carrier.name}. SIM IDs regenerated.", Toast.LENGTH_LONG).show()
        }

        rvCarriers.layoutManager = LinearLayoutManager(context)
        rvCarriers.adapter = adapter

        val savedMcc = PrefsManager.getString(requireContext(), "mcc_mnc", "310260")
        adapter.setSelected(savedMcc)
        MainHook.getUsCarriers().find { it.mccMnc == savedMcc }?.let { showInfo(it) }

        return view
    }

    private fun showInfo(c: MainHook.Companion.UsCarrier) {
        val imsi  = PrefsManager.getString(requireContext(), "imsi", SpoofingUtils.generateValidImsi(c.mccMnc))
        val phone = PrefsManager.getString(requireContext(), "phone_number", SpoofingUtils.generatePhoneNumber(c.npas))
        tvInfo.text = "Carrier: ${c.name}\nMCC/MNC: ${c.mccMnc}\nSPN: ${c.spn}\nIMSI: $imsi\nPhone: $phone"
    }
}
