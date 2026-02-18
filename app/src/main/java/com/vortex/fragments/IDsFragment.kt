package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.utils.ValidationUtils
import com.vortex.R

class IDsFragment : Fragment() {

    private lateinit var tilImei: TextInputLayout
    private lateinit var etImei: TextInputEditText
    private lateinit var tilAndroidId: TextInputLayout
    private lateinit var etAndroidId: TextInputEditText
    private lateinit var tilGaid: TextInputLayout
    private lateinit var etGaid: TextInputEditText
    private lateinit var tilGsfId: TextInputLayout
    private lateinit var etGsfId: TextInputEditText
    private lateinit var tilSerial: TextInputLayout
    private lateinit var etSerial: TextInputEditText

    private lateinit var btnRandomize: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ids, container, false)
        bindViews(view)
        setupListeners()
        loadData()
        return view
    }

    private fun bindViews(view: View) {
        tilImei = view.findViewById(R.id.til_imei)
        etImei = view.findViewById(R.id.et_imei)
        tilAndroidId = view.findViewById(R.id.til_android_id)
        etAndroidId = view.findViewById(R.id.et_android_id)
        tilGaid = view.findViewById(R.id.til_gaid)
        etGaid = view.findViewById(R.id.et_gaid)
        tilGsfId = view.findViewById(R.id.til_gsf)
        etGsfId = view.findViewById(R.id.et_gsf)
        tilSerial = view.findViewById(R.id.til_serial)
        etSerial = view.findViewById(R.id.et_serial)

        btnRandomize = view.findViewById(R.id.btn_randomize_all_ids)
    }

    private fun loadData() {
        val context = requireContext()
        etImei.setText(PrefsManager.getString(context, "imei", ""))
        etAndroidId.setText(PrefsManager.getString(context, "android_id", ""))
        etGaid.setText(PrefsManager.getString(context, "gaid", ""))
        etGsfId.setText(PrefsManager.getString(context, "gsf_id", ""))
        etSerial.setText(PrefsManager.getString(context, "serial", ""))
    }

    private fun setupListeners() {
        btnRandomize.setOnClickListener { randomizeAll() }

        // Individual Randomization
        tilImei.setEndIconOnClickListener {
            val profile = PrefsManager.getString(requireContext(), "profile", "Redmi 9")
            val imei = SpoofingUtils.generateValidImei(profile)
            etImei.setText(imei)
            PrefsManager.saveString(requireContext(), "imei", imei)
        }

        tilAndroidId.setEndIconOnClickListener {
            val id = SpoofingUtils.generateRandomId(16)
            etAndroidId.setText(id)
            PrefsManager.saveString(requireContext(), "android_id", id)
        }

        tilGaid.setEndIconOnClickListener {
            val gaid = SpoofingUtils.generateRandomGaid()
            etGaid.setText(gaid)
            PrefsManager.saveString(requireContext(), "gaid", gaid)
        }

        tilGsfId.setEndIconOnClickListener {
            val gsf = SpoofingUtils.generateRandomId(16)
            etGsfId.setText(gsf)
            PrefsManager.saveString(requireContext(), "gsf_id", gsf)
        }

        tilSerial.setEndIconOnClickListener {
            val serial = SpoofingUtils.generateRandomSerial()
            etSerial.setText(serial)
            PrefsManager.saveString(requireContext(), "serial", serial)
        }
    }

    private fun randomizeAll() {
        val context = requireContext()
        val profile = PrefsManager.getString(context, "profile", "Redmi 9")

        val imei = SpoofingUtils.generateValidImei(profile)
        val androidId = SpoofingUtils.generateRandomId(16)
        val gaid = SpoofingUtils.generateRandomGaid()
        val gsf = SpoofingUtils.generateRandomId(16)
        val serial = SpoofingUtils.generateRandomSerial()

        etImei.setText(imei)
        etAndroidId.setText(androidId)
        etGaid.setText(gaid)
        etGsfId.setText(gsf)
        etSerial.setText(serial)

        PrefsManager.saveString(context, "imei", imei)
        PrefsManager.saveString(context, "android_id", androidId)
        PrefsManager.saveString(context, "gaid", gaid)
        PrefsManager.saveString(context, "gsf_id", gsf)
        PrefsManager.saveString(context, "serial", serial)

        Toast.makeText(context, "All IDs Regenerated & Saved", Toast.LENGTH_SHORT).show()
    }
}
