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
import com.vortex.R

class DeviceFragment : Fragment() {

    private lateinit var rvDevices: RecyclerView
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)
        rvDevices = view.findViewById(R.id.rv_devices)

        adapter = DeviceAdapter(MainHook.DEVICE_FINGERPRINTS) { key ->
            PrefsManager.saveString(requireContext(), "profile", key)
            Toast.makeText(requireContext(), "Profile saved: $key", Toast.LENGTH_SHORT).show()
        }
        rvDevices.layoutManager = LinearLayoutManager(context)
        rvDevices.adapter = adapter

        val saved = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        if (saved.isNotEmpty()) adapter.setSelected(saved)

        return view
    }
}
