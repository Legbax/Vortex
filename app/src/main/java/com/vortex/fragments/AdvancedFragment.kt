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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.PrefsManager
import com.vortex.R
import com.vortex.utils.RootUtils

class AdvancedFragment : Fragment() {

    private val PKG_SNAPCHAT = "com.snapchat.android"
    private val PKG_PLAY_STORE = "com.android.vending"
    private val PKG_GMS = "com.google.android.gms"

    // Toggles
    private lateinit var swHideRoot: SwitchMaterial
    private lateinit var swHideDebug: SwitchMaterial
    private lateinit var swPackages: SwitchMaterial
    private lateinit var swWebView: SwitchMaterial

    // Root Utils (Dalí: Changed to CompoundButton for SwitchMaterial compatibility)
    private lateinit var cbSnapchat: CompoundButton
    private lateinit var cbPlayStore: CompoundButton
    private lateinit var cbGms: CompoundButton

    private lateinit var btnForceStop: Button
    private lateinit var btnClearData: Button
    private lateinit var btnReboot: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)
        bindViews(view)
        setupState()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        // Bind Toggles (New from Main)
        swHideRoot = view.findViewById(R.id.sw_hide_root)
        swHideDebug = view.findViewById(R.id.sw_hide_debug)
        swPackages = view.findViewById(R.id.sw_packages)
        swWebView = view.findViewById(R.id.sw_webview)

        // Bind Root Utils (Dalí)
        cbSnapchat = view.findViewById(R.id.cb_snapchat)
        cbPlayStore = view.findViewById(R.id.cb_play_store)
        cbGms = view.findViewById(R.id.cb_gms)

        btnForceStop = view.findViewById(R.id.btn_force_stop)
        btnClearData = view.findViewById(R.id.btn_clear_data)
        btnReboot = view.findViewById(R.id.btn_reboot)
    }

    private fun setupState() {
        val ctx = requireContext()
        // PrefsManager uses keys that match MainHook logic
        swHideRoot.isChecked = PrefsManager.getBoolean(ctx, "hook_hide_root", true)
        swHideDebug.isChecked = PrefsManager.getBoolean(ctx, "hook_hide_debug", true)
        swPackages.isChecked = PrefsManager.getBoolean(ctx, "hook_packages", true)
        swWebView.isChecked = PrefsManager.getBoolean(ctx, "hook_webview", true)
    }

    private fun setupListeners() {
        val ctx = requireContext()

        // Toggles Listeners
        swHideRoot.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(ctx, "hook_hide_root", isChecked)
        }
        swHideDebug.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(ctx, "hook_hide_debug", isChecked)
        }
        swPackages.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(ctx, "hook_packages", isChecked)
        }
        swWebView.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(ctx, "hook_webview", isChecked)
        }

        // Root Utils Listeners
        btnForceStop.setOnClickListener {
            val apps = getSelectedApps()
            if (apps.isEmpty()) {
                Toast.makeText(context, "Select at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Execute
            var successCount = 0
            apps.forEach { pkg ->
                if (RootUtils.forceStop(pkg)) successCount++
            }
            if (successCount > 0) {
                Toast.makeText(context, "Force Stopped $successCount apps", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed. Is Root granted?", Toast.LENGTH_LONG).show()
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
                .setMessage("This will wipe all data for selected apps. This is irreversible.")
                .setPositiveButton("WIPE") { _, _ ->
                    var successCount = 0
                    apps.forEach { pkg ->
                        if (RootUtils.clearData(pkg)) successCount++
                    }
                     if (successCount > 0) {
                        Toast.makeText(context, "Data cleared for $successCount apps", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed. Is Root granted?", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnReboot.setOnClickListener {
             AlertDialog.Builder(context)
                .setTitle("System Reboot")
                .setMessage("Are you sure you want to restart the device?")
                .setPositiveButton("REBOOT") { _, _ -> RootUtils.rebootDevice() }
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
