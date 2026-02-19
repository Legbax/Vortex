package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.R

class StatusFragment : Fragment() {

    private lateinit var tvScore: TextView
    private lateinit var progress: CircularProgressIndicator
    private lateinit var tvStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val tvProfile  = view.findViewById<TextView>(R.id.text_current_profile)
        val tvSim      = view.findViewById<TextView>(R.id.text_sim_country)
        val tvLoc      = view.findViewById<TextView>(R.id.text_location_status)
        tvScore        = view.findViewById(R.id.text_score)
        progress       = view.findViewById(R.id.progress_evasion)
        // Usamos el text_subtitle para indicar estado general, aunque el titulo ahora es "Developed by Legba"
        // Mostraremos errores en un Toast o Dialog al hacer clic.

        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "Not Set")
        val carrier = PrefsManager.getString(ctx, "carrier_name", "Not Set")
        val isMock  = PrefsManager.getBoolean(ctx, "mock_location_enabled", false)

        tvProfile.text = profile
        tvSim.text     = if (carrier != "Not Set") "United States ($carrier)" else "Not Set"
        tvLoc.text     = if (isMock) "Spoofed (US)" else "Real/Disabled"

        tvScore.text = "--"
        progress.progress = 0

        progress.setOnClickListener { checkDiscrepancies() }
        tvScore.setOnClickListener  { checkDiscrepancies() }

        return view
    }

    private fun checkDiscrepancies() {
        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "")
        if (profile.isEmpty()) {
            Toast.makeText(ctx, "No profile selected!", Toast.LENGTH_SHORT).show()
            return
        }
        val mccMnc = PrefsManager.getString(ctx, "mcc_mnc", "310260")

        val expected = SpoofingUtils.generateAllForProfile(profile, mccMnc)
        val discrepancies = mutableListOf<String>()
        var totalChecks = 0
        var passedChecks = 0

        val checks = mapOf(
            "imei"           to "IMEI",
            "imei2"          to "IMEI 2",
            "imsi"           to "IMSI",
            "iccid"          to "ICCID",
            "phone_number"   to "Phone",
            "android_id"     to "Android ID",
            "ssaid_snapchat" to "SSAID",
            "gaid"           to "GAID",
            "gsf_id"         to "GSF ID",
            "media_drm_id"   to "MediaDRM",
            "serial"         to "Serial",
            "wifi_mac"       to "WiFi MAC",
            "bluetooth_mac"  to "BT MAC",
            "gmail"          to "Gmail",
            "wifi_ssid"      to "SSID",
            "wifi_bssid"     to "BSSID"
        )

        for ((key, label) in checks) {
            totalChecks++
            val actual = PrefsManager.getString(ctx, key, "")
            val exp    = expected[key] ?: ""

            if (actual.isEmpty()) {
                discrepancies.add("$label missing")
            } else if (actual != exp) {
                discrepancies.add("$label mismatch")
            } else {
                passedChecks++
            }
        }

        val percentage = if (totalChecks > 0) (passedChecks * 100 / totalChecks) else 0
        progress.setProgressCompat(percentage, true)
        tvScore.text = "$percentage%"

        if (discrepancies.isEmpty()) {
            Toast.makeText(ctx, "✅ System Integrity 100%", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, "⚠️ Discrepancies: ${discrepancies.size}", Toast.LENGTH_SHORT).show()
        }
    }
}
