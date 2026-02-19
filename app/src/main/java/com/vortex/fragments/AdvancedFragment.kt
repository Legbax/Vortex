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
import com.vortex.utils.ShellUtils

class AdvancedFragment : Fragment() {

    private lateinit var switchSnap: SwitchMaterial
    private lateinit var switchGPlay: SwitchMaterial
    private lateinit var switchGServices: SwitchMaterial

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
        switchSnap = view.findViewById(R.id.switch_snap)
        switchGPlay = view.findViewById(R.id.switch_gplay)
        switchGServices = view.findViewById(R.id.switch_gservices)

        btnForceStop = view.findViewById(R.id.btn_force_stop)
        btnClearData = view.findViewById(R.id.btn_clear_data)
        btnReboot = view.findViewById(R.id.btn_reboot)
    }

    private fun setupListeners() {
        btnForceStop.setOnClickListener { executeAction(false) }
        btnClearData.setOnClickListener { executeAction(true) }
        btnReboot.setOnClickListener { showRebootConfirmation() }
    }

    private fun getSelectedPackages(): List<String> {
        val packages = mutableListOf<String>()
        if (switchSnap.isChecked) packages.add("com.snapchat.android")
        if (switchGPlay.isChecked) packages.add("com.android.vending")
        if (switchGServices.isChecked) packages.add("com.google.android.gms")
        return packages
    }

    private fun executeAction(clearData: Boolean) {
        val packages = getSelectedPackages()
        if (packages.isEmpty()) {
            Toast.makeText(requireContext(), "No apps selected", Toast.LENGTH_SHORT).show()
            return
        }

        var successCount = 0
        for (pkg in packages) {
            val cmd = if (clearData) "pm clear $pkg" else "am force-stop $pkg"
            if (ShellUtils.execRootCmd(cmd)) {
                successCount++
            }
        }

        val actionName = if (clearData) "Data Cleared" else "Force Stopped"
        if (successCount == packages.size) {
            Toast.makeText(requireContext(), "$actionName for all selected apps", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "$actionName failed for some apps (Root?)", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRebootConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reboot Device")
            .setMessage("Are you sure you want to reboot?")
            .setPositiveButton("Reboot") { _, _ ->
                if (!ShellUtils.execRootCmd("reboot")) {
                    Toast.makeText(requireContext(), "Reboot failed (Root?)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
