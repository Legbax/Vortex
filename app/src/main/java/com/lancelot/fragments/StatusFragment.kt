package com.lancelot.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.lancelot.R

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

        view.findViewById<ImageView>(R.id.iv_status_icon).apply {
            setImageResource(if (active) R.drawable.ic_check_circle else R.drawable.ic_error_circle)
            setColorFilter(if (active) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }

        view.findViewById<TextView>(R.id.tv_status_title).apply {
            text = if (active) "Módulo Activo" else "Módulo Inactivo"
            setTextColor(if (active) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }

        view.findViewById<TextView>(R.id.tv_status_description).text = if (active) {
            "El módulo Lancelot está cargado correctamente por Xposed/LSPosed."
        } else {
            "El módulo no está activo. Asegúrate de haberlo habilitado en LSPosed " +
            "y de haber reiniciado el dispositivo."
        }

        return view
    }
}
