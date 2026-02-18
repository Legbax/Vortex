package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

// FIX #28: AdvancedFragment ya no está completamente vacío.
// Muestra un placeholder claro en lugar de una pantalla en blanco
// que confunde al usuario y deja la tab visible sin funcionalidad.
class AdvancedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Opciones avanzadas — Próximamente"
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }
    }
}
