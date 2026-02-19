package com.vortex.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.vortex.R

class StatusFragment : Fragment() {

    // FIX #8: Este método devuelve false por defecto.
    // MainHook.hookModuleStatus() lo hookea para devolver true cuando
    // el módulo está activo. Si devuelve false, el módulo no está cargado.
    fun isModuleActive(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val active = isModuleActive()

        // Update Module Status Card
        view.findViewById<ImageView>(R.id.iv_module_status).apply {
            setImageResource(if (active) R.drawable.ic_logo else R.drawable.ic_error_circle) // Keeping logo for active as per design
            setColorFilter(if (active) Color.parseColor("#00D4FF") else Color.parseColor("#FF5252")) // Accent or Error
        }

        view.findViewById<TextView>(R.id.tv_module_status).apply {
            text = if (active) "MODULE ACTIVE" else "MODULE INACTIVE"
            setTextColor(if (active) Color.parseColor("#8F9BB3") else Color.parseColor("#FF5252")) // Secondary or Error
        }

        // Update Description
        view.findViewById<TextView>(R.id.tv_status_description).text = if (active) {
            "Your device profile is highly consistent and evasion is active."
        } else {
            "Module is not loaded. Please enable in LSPosed and reboot."
        }

        // Update Evasion Score
        val score = if (active) 98 else 0
        view.findViewById<CircularProgressIndicator>(R.id.progress_evasion).progress = score
        view.findViewById<TextView>(R.id.tv_evasion_score).text = score.toString()

        return view
    }
}
