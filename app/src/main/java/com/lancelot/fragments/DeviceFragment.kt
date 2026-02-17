package com.lancelot.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lancelot.MainHook
import com.lancelot.PrefsManager
import com.lancelot.R

class DeviceFragment : Fragment() {

    private lateinit var rvDevices: RecyclerView
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)
        rvDevices = view.findViewById(R.id.rv_devices)

        setupRecyclerView()
        loadSavedData()

        return view
    }

    private fun setupRecyclerView() {
        val profiles = MainHook.DEVICE_FINGERPRINTS

        adapter = DeviceAdapter(profiles) { selectedKey ->
            saveDevicePreference(selectedKey)
        }

        rvDevices.layoutManager = LinearLayoutManager(context)
        rvDevices.adapter = adapter
    }

    private fun saveDevicePreference(key: String) {
        val context = requireContext()
        PrefsManager.saveString(context, "profile", key)
        Toast.makeText(context, "Profile saved: $key", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val context = requireContext()
        val savedProfile = PrefsManager.getString(context, "profile", "Redmi 9")

        if (savedProfile.isNotEmpty()) {
            adapter.setSelected(savedProfile)
        }
    }
}
