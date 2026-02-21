package com.vortex.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.PrefsManager
import com.vortex.R
import com.vortex.utils.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedFragment : Fragment() {

    private val PKG_SNAPCHAT = "com.snapchat.android"
    private val PKG_PLAY_STORE = "com.android.vending"
    private val PKG_GMS = "com.google.android.gms"

    // Toggles
    private lateinit var swHideRoot: SwitchMaterial
    private lateinit var swHideDebug: SwitchMaterial
    private lateinit var swPackages: SwitchMaterial
    private lateinit var swWebView: SwitchMaterial

    // App Management (Switches)
    private lateinit var cbSnapchat: SwitchMaterial
    private lateinit var cbPlayStore: SwitchMaterial
    private lateinit var cbGms: SwitchMaterial

    private lateinit var btnForceStop: Button
    private lateinit var btnClearData: Button
    private lateinit var btnReboot: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)

        // Bind Toggles
        swHideRoot = view.findViewById(R.id.sw_hide_root)
        swHideDebug = view.findViewById(R.id.sw_hide_debug)
        swPackages = view.findViewById(R.id.sw_packages)
        swWebView = view.findViewById(R.id.sw_webview)

        // Bind Root Utils (Now SwitchMaterial)
        cbSnapchat = view.findViewById(R.id.cb_snapchat)
        cbPlayStore = view.findViewById(R.id.cb_play_store)
        cbGms = view.findViewById(R.id.cb_gms)

        btnForceStop = view.findViewById(R.id.btn_force_stop)
        btnClearData = view.findViewById(R.id.btn_clear_data)
        btnReboot = view.findViewById(R.id.btn_reboot)

        setupState()
        setupListeners()

        return view
    }

    private fun setupState() {
        val ctx = requireContext()
        swHideRoot.isChecked = PrefsManager.getBoolean(ctx, "hook_hide_root", false)
        swHideDebug.isChecked = PrefsManager.getBoolean(ctx, "hook_hide_debug", false)
        swPackages.isChecked = PrefsManager.getBoolean(ctx, "hook_packages", false)
        swWebView.isChecked = PrefsManager.getBoolean(ctx, "hook_webview", false)

        // App selection state
        cbSnapchat.isChecked = PrefsManager.getBoolean(ctx, "target_snapchat", true)
        cbPlayStore.isChecked = PrefsManager.getBoolean(ctx, "target_play_store", false)
        cbGms.isChecked = PrefsManager.getBoolean(ctx, "target_gms", false)
    }

    private fun setupListeners() {
        val ctx = requireContext()

        // Toggles Listeners
        swHideRoot.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "hook_hide_root", isChecked) }
        swHideDebug.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "hook_hide_debug", isChecked) }
        swPackages.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "hook_packages", isChecked) }
        swWebView.setOnCheckedChangeListener { _, isChecked -> PrefsManager.saveBoolean(ctx, "hook_webview", isChecked) }

        // App Selection Feedback
        val selectionListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) Toast.makeText(ctx, "Selected for Force Stop / Clear Data", Toast.LENGTH_SHORT).show()

            // Save state [FIX #3]
            val key = when (buttonView.id) {
                R.id.cb_snapchat -> "target_snapchat"
                R.id.cb_play_store -> "target_play_store"
                R.id.cb_gms -> "target_gms"
                else -> return@OnCheckedChangeListener
            }
            PrefsManager.saveBoolean(ctx, key, isChecked)
        }
        cbSnapchat.setOnCheckedChangeListener(selectionListener)
        cbPlayStore.setOnCheckedChangeListener(selectionListener)
        cbGms.setOnCheckedChangeListener(selectionListener)

        // Root Utils Listeners
        btnForceStop.setOnClickListener {
            val apps = getSelectedApps()
            if (apps.isEmpty()) {
                Toast.makeText(context, "Select at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                var success = true
                apps.forEach { if (!RootUtils.forceStop(it)) success = false }

                withContext(Dispatchers.Main) {
                    val msg = if (success) "Force Stop executed" else "Error executing Force Stop (Root?)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnClearData.setOnClickListener {
            val apps = getSelectedApps()
            if (apps.isEmpty()) {
                Toast.makeText(context, "Select at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(context)
                .setTitle("Clear Data?")
                .setMessage("Irreversible action. App data will be wiped.")
                .setPositiveButton("WIPE") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        var success = true
                        apps.forEach { if (!RootUtils.clearData(it)) success = false }

                        withContext(Dispatchers.Main) {
                            val msg = if (success) "Data cleared" else "Error clearing data (Root?)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnReboot.setOnClickListener {
             AlertDialog.Builder(context)
                .setTitle("System Reboot")
                .setPositiveButton("REBOOT") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        RootUtils.rebootDevice()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getSelectedApps(): List<String> {
        val list = mutableListOf<String>()
        if (cbSnapchat.isChecked) list.add(PKG_SNAPCHAT)
        if (cbPlayStore.isChecked) list.add(PKG_PLAY_STORE)
        if (cbGms.isChecked) list.add(PKG_GMS)
        return list
    }
}
