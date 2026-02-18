package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.vortex.PrefsManager
import com.vortex.R

class LocationFragment : Fragment() {

    private lateinit var swMockLocation: SwitchMaterial
    private lateinit var etLatitude: TextInputEditText
    private lateinit var etLongitude: TextInputEditText
    private lateinit var etAltitude: TextInputEditText
    private lateinit var etAccuracy: TextInputEditText
    private lateinit var btnRandomLocation: Button
    private lateinit var btnSaveLocation: Button

    // Simple list of US cities (Lat, Lon)
    private val usCities = listOf(
        Pair(40.7128, -74.0060), // New York
        Pair(34.0522, -118.2437), // Los Angeles
        Pair(41.8781, -87.6298), // Chicago
        Pair(29.7604, -95.3698), // Houston
        Pair(33.4484, -112.0740), // Phoenix
        Pair(39.9526, -75.1652), // Philadelphia
        Pair(29.4241, -98.4936), // San Antonio
        Pair(32.7157, -117.1611), // San Diego
        Pair(32.7767, -96.7970), // Dallas
        Pair(37.3382, -121.8863)  // San Jose
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)

        bindViews(view)
        setupListeners()
        loadData()

        return view
    }

    private fun bindViews(view: View) {
        swMockLocation = view.findViewById(R.id.sw_mock_location)
        etLatitude = view.findViewById(R.id.et_latitude)
        etLongitude = view.findViewById(R.id.et_longitude)
        etAltitude = view.findViewById(R.id.et_altitude)
        etAccuracy = view.findViewById(R.id.et_accuracy)
        btnRandomLocation = view.findViewById(R.id.btn_random_location)
        btnSaveLocation = view.findViewById(R.id.btn_save_location)
    }

    private fun setupListeners() {
        btnRandomLocation.setOnClickListener { randomizeLocation() }
        btnSaveLocation.setOnClickListener { saveLocation() }
    }

    private fun loadData() {
        val context = requireContext()
        swMockLocation.isChecked = PrefsManager.getBoolean(context, "mock_location_enabled", false)
        etLatitude.setText(PrefsManager.getString(context, "mock_latitude", "40.7128"))
        etLongitude.setText(PrefsManager.getString(context, "mock_longitude", "-74.0060"))
        etAltitude.setText(PrefsManager.getString(context, "mock_altitude", "10.0"))
        etAccuracy.setText(PrefsManager.getString(context, "mock_accuracy", "5.0"))
    }

    private fun randomizeLocation() {
        val city = usCities.random()
        // Add small offset for "within city"
        val latOffset = (Math.random() - 0.5) * 0.1
        val lonOffset = (Math.random() - 0.5) * 0.1

        etLatitude.setText((city.first + latOffset).toString())
        etLongitude.setText((city.second + lonOffset).toString())
        etAltitude.setText(((Math.random() * 100) + 10).toString())
        etAccuracy.setText(((Math.random() * 20) + 5).toString())
    }

    private fun saveLocation() {
        val context = requireContext()
        PrefsManager.saveBoolean(context, "mock_location_enabled", swMockLocation.isChecked)
        PrefsManager.saveString(context, "mock_latitude", etLatitude.text.toString())
        PrefsManager.saveString(context, "mock_longitude", etLongitude.text.toString())
        PrefsManager.saveString(context, "mock_altitude", etAltitude.text.toString())
        PrefsManager.saveString(context, "mock_accuracy", etAccuracy.text.toString())

        Toast.makeText(context, "Location Saved Securely", Toast.LENGTH_SHORT).show()
    }
}
