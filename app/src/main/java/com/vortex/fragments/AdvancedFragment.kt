package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.PrefsManager
import com.vortex.R

class AdvancedFragment : Fragment() {

    private lateinit var swHideRoot: SwitchMaterial
    private lateinit var swHideDebug: SwitchMaterial
    private lateinit var swPackages: SwitchMaterial
    private lateinit var swWebView: SwitchMaterial
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)

        swHideRoot = view.findViewById(R.id.switch_hide_root)
        swHideDebug = view.findViewById(R.id.switch_hide_debug)
        swPackages = view.findViewById(R.id.switch_packages)
        swWebView = view.findViewById(R.id.switch_webview)
        btnSave = view.findViewById(R.id.btn_save_advanced)

        val ctx = requireContext()
        // Cargar estado guardado (default true para protección máxima)
        swHideRoot.isChecked = PrefsManager.getBoolean(ctx, "hide_root", true)
        swHideDebug.isChecked = PrefsManager.getBoolean(ctx, "hide_debug", true)
        swPackages.isChecked = PrefsManager.getBoolean(ctx, "hook_packages", true)
        swWebView.isChecked = PrefsManager.getBoolean(ctx, "hook_webview", true)

        btnSave.setOnClickListener {
            PrefsManager.saveBoolean(ctx, "hide_root", swHideRoot.isChecked)
            PrefsManager.saveBoolean(ctx, "hide_debug", swHideDebug.isChecked)
            PrefsManager.saveBoolean(ctx, "hook_packages", swPackages.isChecked)
            PrefsManager.saveBoolean(ctx, "hook_webview", swWebView.isChecked)

            Toast.makeText(ctx, "Advanced settings saved. Reboot required.", Toast.LENGTH_LONG).show()
        }

        return view
    }
}
