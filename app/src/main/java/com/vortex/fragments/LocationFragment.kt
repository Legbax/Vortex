package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.vortex.PrefsManager
import com.vortex.R
import kotlin.random.Random

class LocationFragment : Fragment() {

    private lateinit var etLat: TextInputEditText
    private lateinit var etLon: TextInputEditText
    private lateinit var etAlt: TextInputEditText
    private lateinit var etAcc: TextInputEditText
    private lateinit var cbEnabled: CheckBox
    private lateinit var cbJitter: CheckBox
    private lateinit var cbMoving: CheckBox
    private lateinit var btnSave: Button
    private lateinit var btnRandomize: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)
        bindViews(view)
        loadData()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        etLat = view.findViewById(R.id.et_latitude)
        etLon = view.findViewById(R.id.et_longitude)
        etAlt = view.findViewById(R.id.et_altitude)
        etAcc = view.findViewById(R.id.et_accuracy)
        cbEnabled = view.findViewById(R.id.cb_mock_location_enabled)
        cbJitter = view.findViewById(R.id.cb_jitter)
        cbMoving = view.findViewById(R.id.cb_moving)
        btnSave = view.findViewById(R.id.btn_save_location)
        btnRandomize = view.findViewById(R.id.btn_random_location)
    }

    private fun loadData() {
        val context = requireContext()
        etLat.setText(PrefsManager.getString(context, "mock_latitude", "40.7128"))
        etLon.setText(PrefsManager.getString(context, "mock_longitude", "-74.0060"))
        etAlt.setText(PrefsManager.getString(context, "mock_altitude", "10.0"))
        etAcc.setText(PrefsManager.getString(context, "mock_accuracy", "5.0"))
        cbEnabled.isChecked = PrefsManager.getBoolean(context, "mock_location_enabled", false)
        cbJitter.isChecked = PrefsManager.getBoolean(context, "location_jitter_enabled", true)
        cbMoving.isChecked = PrefsManager.getBoolean(context, "location_is_moving", false)
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            val context = requireContext()
            PrefsManager.saveString(context, "mock_latitude", etLat.text.toString())
            PrefsManager.saveString(context, "mock_longitude", etLon.text.toString())
            PrefsManager.saveString(context, "mock_altitude", etAlt.text.toString())
            PrefsManager.saveString(context, "mock_accuracy", etAcc.text.toString())
            PrefsManager.saveBoolean(context, "mock_location_enabled", cbEnabled.isChecked)
            PrefsManager.saveBoolean(context, "location_jitter_enabled", cbJitter.isChecked)
            PrefsManager.saveBoolean(context, "location_is_moving", cbMoving.isChecked)
        }

        btnRandomize.setOnClickListener {
            // FIX #33: Use kotlin.random.Random
            val offsetLat = (Random.nextDouble() - 0.5) * 0.001
            val offsetLon = (Random.nextDouble() - 0.5) * 0.001

            // Randomize around current or default NY
            var lat = etLat.text.toString().toDoubleOrNull() ?: 40.7128
            var lon = etLon.text.toString().toDoubleOrNull() ?: -74.0060

            // Move significantly to a new US location approx
            lat = 30.0 + Random.nextDouble() * 15.0 // 30-45
            lon = -120.0 + Random.nextDouble() * 40.0 // -120 to -80

            etLat.setText(String.format("%.6f", lat))
            etLon.setText(String.format("%.6f", lon))
        }
    }
}
