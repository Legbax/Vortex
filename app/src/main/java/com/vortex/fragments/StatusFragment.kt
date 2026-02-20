package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.R

class StatusFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val tvProfile = view.findViewById<TextView>(R.id.text_current_profile)
        val tvScore = view.findViewById<TextView>(R.id.text_score)
        val tvSim = view.findViewById<TextView>(R.id.text_sim_country)
        val tvLoc = view.findViewById<TextView>(R.id.text_location_status)

        val ctx = requireContext()
        val currentProfile = PrefsManager.getString(ctx, "profile", "Redmi 9")
        tvProfile.text = currentProfile

        val isMock = PrefsManager.getBoolean(ctx, "mock_location_enabled", false)
        tvLoc.text = if (isMock) "Spoofed" else "Real"

        tvSim.text = PrefsManager.getString(ctx, "carrier_name", "T-Mobile")

        // Simple Score Logic
        var score = 100
        if (!isMock) score -= 10
        if (currentProfile == "Redmi 9") score -= 5 // Default profile penalty
        tvScore.text = "$score%"

        MainHook.log("Status Check: Perfil=$currentProfile | Score=$score%")

        return view
    }
}
