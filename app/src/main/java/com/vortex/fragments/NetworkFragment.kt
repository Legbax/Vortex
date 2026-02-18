package com.vortex.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.vortex.MainHook
import com.vortex.PrefsManager
import com.vortex.SpoofingUtils
import com.vortex.R
import com.vortex.utils.ValidationUtils
import java.util.UUID

class NetworkFragment : Fragment() {

    private lateinit var etOperator: TextInputEditText
    private lateinit var etMcc: TextInputEditText
    private lateinit var etMnc: TextInputEditText
    private lateinit var etSimSerial: TextInputEditText
    private lateinit var etImsi: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etWifi: TextInputEditText
    private lateinit var etBt: TextInputEditText

    private lateinit var tilSimSerial: TextInputLayout
    private lateinit var tilImsi: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilWifi: TextInputLayout
    private lateinit var tilBt: TextInputLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        bindViews(view)
        loadData()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        etOperator = view.findViewById(R.id.et_operator_name)
        etMcc = view.findViewById(R.id.et_mcc)
        etMnc = view.findViewById(R.id.et_mnc)
        etSimSerial = view.findViewById(R.id.et_sim_serial)
        etImsi = view.findViewById(R.id.et_imsi)
        etPhone = view.findViewById(R.id.et_phone_number)
        etWifi = view.findViewById(R.id.et_wifi_mac)
        etBt = view.findViewById(R.id.et_bluetooth_mac)

        tilSimSerial = view.findViewById(R.id.til_sim_serial)
        tilImsi = view.findViewById(R.id.til_imsi)
        tilPhone = view.findViewById(R.id.til_phone_number)
        tilWifi = view.findViewById(R.id.til_wifi_mac)
        tilBt = view.findViewById(R.id.til_bluetooth_mac)
    }

    private fun loadData() {
        val context = requireContext()
        etOperator.setText(PrefsManager.getString(context, "operator_name", "T-Mobile"))
        etMcc.setText(PrefsManager.getString(context, "mcc", "310"))
        etMnc.setText(PrefsManager.getString(context, "mnc", "260"))
        etSimSerial.setText(PrefsManager.getString(context, "sim_serial", ""))
        etImsi.setText(PrefsManager.getString(context, "subscriber_id", ""))
        etPhone.setText(PrefsManager.getString(context, "phone_number", ""))
        etWifi.setText(PrefsManager.getString(context, "wifi_mac", "02:00:00:00:00:00"))
        etBt.setText(PrefsManager.getString(context, "bt_mac", "02:00:00:00:00:00"))
    }

    private fun setupListeners() {
        val context = requireContext()

        // Auto-save logic could be added here similar to LocationFragment
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveData()
            }
        }

        etOperator.addTextChangedListener(textWatcher)
        etMcc.addTextChangedListener(textWatcher)
        etMnc.addTextChangedListener(textWatcher)
        etSimSerial.addTextChangedListener(textWatcher)
        etImsi.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)
        etWifi.addTextChangedListener(textWatcher)
        etBt.addTextChangedListener(textWatcher)

        // Randomize Listeners
        tilSimSerial.setEndIconOnClickListener {
            // Generate valid ICCID (Luhn)
            // Implementation detail: simplified random here
            val random = (1000000000000000000L + (Math.random() * 8999999999999999999L).toLong()).toString()
            etSimSerial.setText(random)
        }

        tilImsi.setEndIconOnClickListener {
            // Generate IMSI based on MCC/MNC
            val mcc = etMcc.text.toString().ifEmpty { "310" }
            val mnc = etMnc.text.toString().ifEmpty { "260" }
            val random = (1000000000L + (Math.random() * 8999999999L).toLong()).toString()
            etImsi.setText("$mcc$mnc$random")
        }

        tilPhone.setEndIconOnClickListener {
            // Generate US phone
            val area = (200..999).random()
            val prefix = (200..999).random()
            val line = (1000..9999).random()
            etPhone.setText("+1 $area-$prefix-$line")
        }

        tilWifi.setEndIconOnClickListener {
            etWifi.setText(SpoofingUtils.generateRandomMac())
        }

        tilBt.setEndIconOnClickListener {
            etBt.setText(SpoofingUtils.generateRandomMac())
        }
    }

    private fun saveData() {
        val context = context ?: return
        PrefsManager.saveString(context, "operator_name", etOperator.text.toString())
        PrefsManager.saveString(context, "mcc", etMcc.text.toString())
        PrefsManager.saveString(context, "mnc", etMnc.text.toString())
        PrefsManager.saveString(context, "sim_serial", etSimSerial.text.toString())
        PrefsManager.saveString(context, "subscriber_id", etImsi.text.toString())
        PrefsManager.saveString(context, "phone_number", etPhone.text.toString())
        PrefsManager.saveString(context, "wifi_mac", etWifi.text.toString())
        PrefsManager.saveString(context, "bt_mac", etBt.text.toString())
    }
}
