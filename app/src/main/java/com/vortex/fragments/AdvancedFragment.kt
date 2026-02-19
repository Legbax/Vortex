package com.vortex.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.R
import com.vortex.utils.RootUtils

class AdvancedFragment : Fragment() {

    private val PKG_SNAPCHAT = "com.snapchat.android"
    private val PKG_PLAY_STORE = "com.android.vending"
    private val PKG_GMS = "com.google.android.gms"

    private lateinit var cbSnapchat: SwitchMaterial
    private lateinit var cbPlayStore: SwitchMaterial
    private lateinit var cbGms: SwitchMaterial
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
            // Run in background thread ideally, but for now keeping simple as per request
            Thread {
                apps.forEach { pkg ->
                    if (RootUtils.forceStop(pkg)) successCount++
                }
                activity?.runOnUiThread {
                    if (successCount > 0) {
                        Toast.makeText(context, "Force Stopped $successCount apps", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed. Is Root granted?", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        btnClearData.setOnClickListener {
            val apps = getSelectedApps()
            if (apps.isEmpty()) {
                Toast.makeText(context, "Select at least one app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(context)
                .setTitle("Clear Data?")
                .setMessage("This will permanently delete all file settings, accounts, and databases for the selected apps.\n\nAre you sure?")
                .setPositiveButton("WIPE DATA") { _, _ ->
                    Thread {
                        var successCount = 0
                        apps.forEach { pkg ->
                            if (RootUtils.clearData(pkg)) successCount++
                        }
                        activity?.runOnUiThread {
                            if (successCount > 0) {
                                Toast.makeText(context, "Data cleared for $successCount apps", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed. Is Root granted?", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnReboot.setOnClickListener {
             AlertDialog.Builder(context)
                .setTitle("System Reboot")
                .setMessage("Are you sure you want to restart the device immediately?")
                .setPositiveButton("REBOOT") { _, _ ->
                    Thread {
                        RootUtils.rebootDevice()
                    }.start()
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
