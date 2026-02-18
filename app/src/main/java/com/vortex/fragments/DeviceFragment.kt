package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.adapters.DeviceAdapter
import com.vortex.R

class DeviceFragment : Fragment() {

    private lateinit var rvDevices: RecyclerView
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)
        rvDevices = view.findViewById(R.id.recycler_devices)
        setupRecyclerView()
        return view
    }

    private fun setupRecyclerView() {
        val currentProfile = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        // Get keys from MainHook.DEVICE_FINGERPRINTS
        val devices = MainHook.DEVICE_FINGERPRINTS.keys.toList()

        adapter = DeviceAdapter(devices) { selectedDevice ->
            PrefsManager.saveString(requireContext(), "profile", selectedDevice)
            Toast.makeText(requireContext(), "Selected: $selectedDevice", Toast.LENGTH_SHORT).show()
        }

        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = adapter

        val index = devices.indexOfFirst { it == currentProfile }
        if (index != -1) {
            adapter.selectedPosition = index
            adapter.notifyItemChanged(index)
            rvDevices.scrollToPosition(index)
        }
    }
}
