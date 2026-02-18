package com.vortex.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.vortex.PrefsManager
import com.vortex.R
import kotlin.random.Random

class LocationFragment : Fragment() {

    private lateinit var etLat: TextInputEditText
    private lateinit var etLon: TextInputEditText
    private lateinit var etAlt: TextInputEditText
    private lateinit var etAcc: TextInputEditText

    // SwitchMaterial instead of CheckBox
    private lateinit var switchJitter: SwitchMaterial
    private lateinit var switchMoving: SwitchMaterial

    // FAB
    private lateinit var btnRandomize: FloatingActionButton

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
        etLat = view.findViewById(R.id.et_lat)
        etLon = view.findViewById(R.id.et_lon)
        etAlt = view.findViewById(R.id.et_alt)
        etAcc = view.findViewById(R.id.et_acc)

        switchJitter = view.findViewById(R.id.switch_jitter)
        switchMoving = view.findViewById(R.id.switch_moving)

        btnRandomize = view.findViewById(R.id.btn_random_location)
    }

    private fun loadData() {
        val context = requireContext()
        etLat.setText(PrefsManager.getString(context, "mock_latitude", "40.7128"))
        etLon.setText(PrefsManager.getString(context, "mock_longitude", "-74.0060"))
        etAlt.setText(PrefsManager.getString(context, "mock_altitude", "10.0"))
        etAcc.setText(PrefsManager.getString(context, "mock_accuracy", "5.0"))

        switchJitter.isChecked = PrefsManager.getBoolean(context, "location_jitter_enabled", true)
        switchMoving.isChecked = PrefsManager.getBoolean(context, "location_is_moving", false)
    }

    private fun setupListeners() {
        val context = requireContext()

        // Auto-save Text Watcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveData()
            }
        }

        etLat.addTextChangedListener(textWatcher)
        etLon.addTextChangedListener(textWatcher)
        etAlt.addTextChangedListener(textWatcher)
        etAcc.addTextChangedListener(textWatcher)

        switchJitter.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "location_jitter_enabled", isChecked)
        }

        switchMoving.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "location_is_moving", isChecked)
        }

        btnRandomize.setOnClickListener {
            // FIX #33: Use kotlin.random.Random
            // Move significantly to a new US location approx
            val lat = 30.0 + Random.nextDouble() * 15.0 // 30-45
            val lon = -120.0 + Random.nextDouble() * 40.0 // -120 to -80

            etLat.setText(String.format("%.6f", lat))
            etLon.setText(String.format("%.6f", lon))
            // Auto-save triggers via TextWatcher
        }
    }

    private fun saveData() {
        val context = context ?: return
        PrefsManager.saveString(context, "mock_latitude", etLat.text.toString())
        PrefsManager.saveString(context, "mock_longitude", etLon.text.toString())
        PrefsManager.saveString(context, "mock_altitude", etAlt.text.toString())
        PrefsManager.saveString(context, "mock_accuracy", etAcc.text.toString())
    }
}
