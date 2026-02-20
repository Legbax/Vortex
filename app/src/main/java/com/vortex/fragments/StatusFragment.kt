package com.vortex.fragments

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.R
import com.vortex.utils.ValidationUtils

class StatusFragment : Fragment() {

    // -------------------------------------------------------------------------
    // Cada check tiene un nombre, su peso en puntos, y un lambda que devuelve
    // true si el valor guardado supera la validación.
    // Total posible: 100 pts.
    // -------------------------------------------------------------------------
    private data class ScoreCheck(val label: String, val points: Int, val passes: () -> Boolean)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)

        val tvProfile = view.findViewById<TextView>(R.id.text_current_profile)
        val tvScore   = view.findViewById<TextView>(R.id.text_score)
        val tvSim     = view.findViewById<TextView>(R.id.text_sim_country)
        val tvLoc     = view.findViewById<TextView>(R.id.text_location_status)
        val progress  = view.findViewById<CircularProgressIndicator>(R.id.progress_evasion)

        val ctx = requireContext()

        // --- Leer valores guardados ------------------------------------------
        val profile   = PrefsManager.getString(ctx, "profile",      "")
        val imei      = PrefsManager.getString(ctx, "imei",         "")
        val imei2     = PrefsManager.getString(ctx, "imei2",        "")
        val gaid      = PrefsManager.getString(ctx, "gaid",         "")
        val androidId = PrefsManager.getString(ctx, "android_id",   "")
        val imsi      = PrefsManager.getString(ctx, "imsi",         "")
        val iccid     = PrefsManager.getString(ctx, "iccid",        "")
        val carrier   = PrefsManager.getString(ctx, "carrier_name", "")
        val wifiMac   = PrefsManager.getString(ctx, "wifi_mac",     "")
        val isMock    = PrefsManager.getBoolean(ctx, "mock_location_enabled", false)

        // --- Poblar la tarjeta de estado -------------------------------------
        tvProfile.text = profile.ifEmpty { "Not Set" }
        tvSim.text     = carrier.ifEmpty { "Not Set" }
        tvLoc.text     = if (isMock) "Spoofed (US)" else "Real / Disabled"

        // --- Calcular score --------------------------------------------------
        val checks = listOf(
            ScoreCheck("Device Profile",  15) { profile.isNotEmpty() },
            ScoreCheck("IMEI Slot 1",     15) { ValidationUtils.isValidImei(imei) },
            ScoreCheck("IMEI Slot 2",     10) { ValidationUtils.isValidImei(imei2) },
            ScoreCheck("GAID",            10) { ValidationUtils.isValidGaid(gaid) },
            ScoreCheck("Android ID",      10) { ValidationUtils.isValidAndroidId(androidId) },
            ScoreCheck("Carrier / IMSI",  10) { carrier.isNotEmpty() && ValidationUtils.isValidImsi(imsi) },
            ScoreCheck("ICCID",           10) { ValidationUtils.isValidIccid(iccid) },
            ScoreCheck("WiFi MAC",        10) { ValidationUtils.isValidMac(wifiMac) },
            ScoreCheck("Location Spoof",  10) { isMock }
        )
        val score = checks.filter { it.passes() }.sumOf { it.points }

        MainHook.log("StatusFragment: score=$score/100 | profile=$profile | mock=$isMock")

        // --- Color del indicador según rango ---------------------------------
        // >=80 verde éxito | 50-79 naranja advertencia | <50 rojo error
        val indicatorColor = when {
            score >= 80 -> ContextCompat.getColor(ctx, R.color.vortex_success)
            score >= 50 -> ContextCompat.getColor(ctx, R.color.vortex_warning)
            else        -> ContextCompat.getColor(ctx, R.color.vortex_error)
        }
        progress.setIndicatorColor(indicatorColor)

        // Texto inicial en "--" hasta que arranque la animación
        tvScore.text = "--"

        // --- Animar el indicador + el contador de texto ----------------------
        // 300ms de delay para que el fragment esté visible antes de animar
        view.postDelayed({
            if (!isAdded) return@postDelayed  // guard si el fragment ya fue detached

            ValueAnimator.ofInt(0, score).apply {
                duration = 900
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    val v = anim.animatedValue as Int
                    progress.setProgressCompat(v, false)  // false = sin animación interna duplicada
                    tvScore.text = "$v%"
                }
            }.start()
        }, 300)

        return view
    }
}
