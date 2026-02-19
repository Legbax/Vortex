package com.vortex.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.vortex.R
import com.vortex.utils.RootUtils

class AdvancedFragment : Fragment() {

    private val PKG_SNAPCHAT = "com.snapchat.android"
    private val PKG_PLAY_STORE = "com.android.vending"
    private val PKG_GMS = "com.google.android.gms"

    private lateinit var cbSnapchat: CheckBox
    private lateinit var cbPlayStore: CheckBox
    private lateinit var cbGms: CheckBox
    private lateinit var btnForceStop: Button
    private lateinit var btnClearData: Button
    private lateinit var btnReboot: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)
        bindViews(view)
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        cbSnapchat = view.findViewById(R.id.cb_snapchat)
        cbPlayStore = view.findViewById(R.id.cb_play_store)
        cbGms = view.findViewById(R.id.cb_gms)
        btnForceStop = view.findViewById(R.id.btn_force_stop)
        btnClearData = view.findViewById(R.id.btn_clear_data)
        btnReboot = view.findViewById(R.id.btn_reboot)
    }

    private fun setupListeners() {
        btnForceStop.setOnClickListener {
            val apps = getSelectedApps()
            if (apps.isEmpty()) {
                Toast.makeText(context, "Select at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
