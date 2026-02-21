package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vortex.DeviceData
import com.vortex.PrefsManager
import com.vortex.R
import com.vortex.adapters.DeviceAdapter
import com.google.android.material.textfield.TextInputLayout

class DeviceFragment : Fragment() {

    private lateinit var tilProfile: TextInputLayout
    private lateinit var actvProfile: AutoCompleteTextView
    private lateinit var btnSelect: Button

    // UI Elements for new features
    private lateinit var tvCurrentModel: TextView
    private lateinit var tvCurrentDetail: TextView
    private lateinit var rvRecents: RecyclerView
    private lateinit var recentAdapter: DeviceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)

        tilProfile = view.findViewById(R.id.til_profile_selector)
        actvProfile = view.findViewById(R.id.actv_profile_selector)
        btnSelect = view.findViewById(R.id.btn_apply_profile)

        tvCurrentModel = view.findViewById(R.id.tv_current_device_model)
        tvCurrentDetail = view.findViewById(R.id.tv_current_device_detail)
        rvRecents = view.findViewById(R.id.rv_recent_profiles)

        setupDropdown()
        updateCurrentDeviceInfo()
        setupRecentsList()

        return view
    }

    private fun updateCurrentDeviceInfo() {
        val currentProfileName = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        val fp = DeviceData.DEVICE_FINGERPRINTS[currentProfileName]

        tvCurrentModel.text = currentProfileName
        tvCurrentDetail.text = if (fp != null) {
            "${fp.manufacturer} · Android ${fp.release}"
        } else {
            "Unknown Device"
        }
    }

    private fun setupRecentsList() {
        val ctx = requireContext()
        var recentsStr = PrefsManager.getString(ctx, "recent_profiles", "")

        // Initial setup if empty: pick 3-5 random profiles
        if (recentsStr.isEmpty()) {
            val allProfiles = DeviceData.DEVICE_FINGERPRINTS.keys.toList()
            val count = (3..5).random()
            val initialRecents = allProfiles.shuffled().take(count)
            recentsStr = initialRecents.joinToString(",")
            PrefsManager.saveString(ctx, "recent_profiles", recentsStr)
        }

        val recentList = recentsStr.split(",").filter { it.isNotEmpty() }.toMutableList()

        // Filter out any invalid profiles just in case
        val validRecents = recentList.filter { it in DeviceData.DEVICE_FINGERPRINTS }

        // Map to full fingerprint objects for the adapter
        val profilesMap = validRecents.associateWith { DeviceData.DEVICE_FINGERPRINTS[it]!! }

        recentAdapter = DeviceAdapter(profilesMap) { selected ->
            actvProfile.setText(selected, false)
            applyProfile(selected)
        }

        rvRecents.layoutManager = LinearLayoutManager(ctx)
        rvRecents.adapter = recentAdapter
    }

    private fun addToRecents(profile: String) {
        val ctx = requireContext()
        val recentsStr = PrefsManager.getString(ctx, "recent_profiles", "")
        val list = recentsStr.split(",").filter { it.isNotEmpty() }.toMutableList()

        // Remove if exists to move to top, limit to 5
        list.remove(profile)
        list.add(0, profile)
        if (list.size > 5) list.removeAt(list.size - 1)

        PrefsManager.saveString(ctx, "recent_profiles", list.joinToString(","))
        setupRecentsList() // Refresh list
    }

    private fun setupDropdown() {
        val profiles = DeviceData.DEVICE_FINGERPRINTS.keys.toList().sorted()
        // Use custom layout for dropdown items (Fix visibility issue)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, profiles)
        actvProfile.setAdapter(adapter)

        val current = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        actvProfile.setText(current, false)

        btnSelect.setOnClickListener {
            val selected = actvProfile.text.toString()
            if (selected in DeviceData.DEVICE_FINGERPRINTS) {
                applyProfile(selected)
            } else {
                Toast.makeText(context, "Invalid Profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyProfile(profile: String) {
        PrefsManager.saveString(requireContext(), "profile", profile)
        addToRecents(profile)
        updateCurrentDeviceInfo()
        Toast.makeText(context, "Profile Applied: $profile. Reboot required.", Toast.LENGTH_LONG).show()
    }
}
