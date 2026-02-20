package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.vortex.DeviceData
import com.vortex.PrefsManager
import com.vortex.R
import com.google.android.material.textfield.TextInputLayout

class DeviceFragment : Fragment() {

    private lateinit var tilProfile: TextInputLayout
    private lateinit var actvProfile: AutoCompleteTextView
    private lateinit var btnSelect: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)

        // Remove search bar & list from previous step, replace with Dropdown + Button logic as requested
        // Need to update XML too. For now, let's bind assuming updated XML.

        tilProfile = view.findViewById(R.id.til_profile_selector)
        actvProfile = view.findViewById(R.id.actv_profile_selector)
        btnSelect = view.findViewById(R.id.btn_apply_profile)

        setupDropdown()

        return view
    }

    private fun setupDropdown() {
        val profiles = DeviceData.DEVICE_FINGERPRINTS.keys.toList().sorted()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, profiles)
        actvProfile.setAdapter(adapter)

        val current = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        actvProfile.setText(current, false)

        btnSelect.setOnClickListener {
            val selected = actvProfile.text.toString()
            if (selected in DeviceData.DEVICE_FINGERPRINTS) {
                PrefsManager.saveString(requireContext(), "profile", selected)
                Toast.makeText(context, "Profile Applied: $selected. Reboot required.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Invalid Profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
