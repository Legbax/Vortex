package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.vortex.PrefsManager
import com.vortex.R

class StatusFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val tvProfile  = view.findViewById<TextView>(R.id.text_current_profile)
        val tvCarrier  = view.findViewById<TextView>(R.id.text_current_carrier)
        val tvScore    = view.findViewById<TextView>(R.id.text_score)
        val progress   = view.findViewById<CircularProgressIndicator>(R.id.progress_evasion)

        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "—")
        val carrier = PrefsManager.getString(ctx, "mcc_mnc", "—")

        tvProfile.text = if (profile.isEmpty()) "Not set" else profile
        tvCarrier.text = if (carrier.isEmpty()) "Not set" else carrier

        // Score calculado: +20 si tiene perfil, +20 carrier, +20 IMEI, +20 GAID, +20 location
        var score = 0
        if (PrefsManager.getString(ctx, "profile", "").isNotEmpty())     score += 20
        if (PrefsManager.getString(ctx, "mcc_mnc", "").isNotEmpty())     score += 20
        if (PrefsManager.getString(ctx, "imei", "").isNotEmpty())        score += 20
        if (PrefsManager.getString(ctx, "gaid", "").isNotEmpty())        score += 20
        if (PrefsManager.getBoolean(ctx, "mock_location_enabled", false)) score += 20

        tvScore.text = score.toString()
        progress.progress = score

        return view
    }
}
