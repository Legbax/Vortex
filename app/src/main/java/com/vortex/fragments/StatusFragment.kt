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
    private lateinit var tvWarnings: TextView // Asumo que existe o mostraré en Toast/Dialog si no hay TextView dedicado en XML.
    // El XML tiene `text_subtitle` que puedo usar para warnings o el `text_score`.
    // Pero el informe 1 menciona `tvWarnings.text`. Revisaré si el XML lo tiene.
    // Si no, usaré `text_subtitle`.

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val tvProfile  = view.findViewById<TextView>(R.id.text_current_profile)
        val tvCarrier  = view.findViewById<TextView>(R.id.text_current_carrier)
        tvScore        = view.findViewById(R.id.text_score)
        progress       = view.findViewById(R.id.progress_evasion)
        // Usaré text_subtitle para mostrar estado/warnings si es breve, o un diálogo.
        // El XML no tenía `tvWarnings` explícito en mi lectura anterior, pero tenía `text_subtitle`.
        val tvSubtitle = view.findViewById<TextView>(R.id.text_subtitle)

        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "Not Set")
        val carrier = PrefsManager.getString(ctx, "carrier_name", "Not Set")

        tvProfile.text = profile
        tvCarrier.text = carrier

        // [LOGIC FIX] NO calcular score automáticamente al iniciar.
        tvScore.text = "--"
        progress.progress = 0
        tvSubtitle.text = getString(R.string.nav_status_subtitle) // "System Integrity Check"

        // [LOGIC FIX] Calcular SOLO al hacer click
        progress.setOnClickListener { checkDiscrepancies(tvSubtitle) }
        tvScore.setOnClickListener  { checkDiscrepancies(tvSubtitle) }

        return view
    }

    private fun checkDiscrepancies(tvStatus: TextView) {
        val ctx = requireContext()
        val profile = PrefsManager.getString(ctx, "profile", "")
        if (profile.isEmpty()) {
            Toast.makeText(ctx, "No profile selected!", Toast.LENGTH_SHORT).show()
            return
        }
        val mccMnc = PrefsManager.getString(ctx, "mcc_mnc", "310260")

        // Generar valores esperados (Deterministas para este perfil)
        val expected = SpoofingUtils.generateAllForProfile(profile, mccMnc)
        val discrepancies = mutableListOf<String>()
        var totalChecks = 0
        var passedChecks = 0

        // Lista de claves a verificar y sus nombres legibles
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
            "gmail"          to "Gmail"
        )

        for ((key, label) in checks) {
            totalChecks++
            val actual = PrefsManager.getString(ctx, key, "")
            val exp    = expected[key] ?: ""

            if (actual.isEmpty()) {
                discrepancies.add("$label missing")
            } else if (actual != exp) {
                // Si son diferentes, discrepancia (ya que usamos semilla determinista)
                // Nota: El usuario puede haber generado valores random manualmente.
                // En ese caso, serán diferentes. El "Status" marca esto como "Desincronizado"
                // respecto al perfil base ideal.
                discrepancies.add("$label mismatch")
            } else {
                passedChecks++
            }
        }

        val percentage = if (totalChecks > 0) (passedChecks * 100 / totalChecks) else 0

        // Animación de progreso
        progress.setProgressCompat(percentage, true)
        tvScore.text = "$percentage%"

        if (discrepancies.isEmpty()) {
            tvStatus.text = "✅ 100% Synced with $profile"
            tvStatus.setTextColor(resources.getColor(R.color.vortex_success, null))
        } else {
            tvStatus.text = "⚠️ Discrepancies: ${discrepancies.size} items\n(${discrepancies.take(3).joinToString()})"
            tvStatus.setTextColor(resources.getColor(R.color.vortex_error, null))

            // Mostrar detalle completo en Toast o Dialog si son muchos
            if (discrepancies.size > 0) {
                 Toast.makeText(ctx, "Mismatch: ${discrepancies.joinToString()}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
