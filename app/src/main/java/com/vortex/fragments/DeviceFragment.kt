package com.vortex.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.adapters.DeviceAdapter
import com.vortex.R
import kotlin.random.Random

class DeviceFragment : Fragment() {

    private lateinit var rvDevices: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvCurrentDevice: TextView
    private lateinit var tvFullFingerprint: TextView
    private lateinit var tvBrand: TextView
    private lateinit var tvOS: TextView
    private lateinit var btnRandomize: Button
    private lateinit var emptyState: View

    private lateinit var adapter: DeviceAdapter
    private var allDevices: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device, container, false)
        bindViews(view)
        loadData()
        setupRecyclerView()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        rvDevices = view.findViewById(R.id.rv_devices)
        etSearch = view.findViewById(R.id.et_search)
        tvCurrentDevice = view.findViewById(R.id.tv_current_device_name)
        tvFullFingerprint = view.findViewById(R.id.tv_full_fingerprint)
        tvBrand = view.findViewById(R.id.tv_current_brand)
        tvOS = view.findViewById(R.id.tv_current_os)
        btnRandomize = view.findViewById(R.id.btn_randomize_device)
        emptyState = view.findViewById(R.id.layout_empty_state)
    }

    private fun loadData() {
        allDevices = MainHook.DEVICE_FINGERPRINTS.keys.toList()
        updateCurrentProfileUI()
    }

    private fun updateCurrentProfileUI() {
        val context = context ?: return
        val currentProfile = PrefsManager.getString(context, "profile", "Redmi 9")
        tvCurrentDevice.text = currentProfile

        val deviceFp = MainHook.DEVICE_FINGERPRINTS[currentProfile]
        val fp = deviceFp?.fingerprint ?: "Unknown"
        tvFullFingerprint.text = fp

        // Parse manufacturer from fingerprint (Brand/Product/Device...)
        val parts = fp.split("/")
        if (parts.isNotEmpty()) {
            tvBrand.text = parts[0].replaceFirstChar { it.uppercase() }
        }

        // Parse Android version (usually :release-keys or similar if present, hard to parse accurately from just build string without structured data)
        // Assuming Android 11 for all as per project constraint
        tvOS.text = "Android 11"
    }

    private fun setupRecyclerView() {
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        updateList(allDevices)
    }

    private fun updateList(devices: List<String>) {
        if (devices.isEmpty()) {
            rvDevices.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvDevices.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }

        adapter = DeviceAdapter(devices) { selectedDevice ->
            PrefsManager.saveString(requireContext(), "profile", selectedDevice)
            updateCurrentProfileUI()
            Toast.makeText(requireContext(), "Selected: $selectedDevice", Toast.LENGTH_SHORT).show()
        }

        rvDevices.adapter = adapter

        // Highlight current
        val currentProfile = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
        val index = devices.indexOfFirst { it == currentProfile }
        if (index != -1) {
            adapter.updateSelection(index)
            rvDevices.scrollToPosition(index)
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = allDevices.filter { it.lowercase().contains(query) }
                updateList(filtered)
            }
        })

        btnRandomize.setOnClickListener {
            if (allDevices.isNotEmpty()) {
                val randomDevice = allDevices[Random.nextInt(allDevices.size)]
                PrefsManager.saveString(requireContext(), "profile", randomDevice)
                updateCurrentProfileUI()

                // Clear search and reset list to show selection
                etSearch.setText("")
                updateList(allDevices)

                Toast.makeText(requireContext(), "Randomized: $randomDevice", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
