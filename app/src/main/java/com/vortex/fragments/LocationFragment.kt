package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.vortex.PrefsManager
import com.vortex.R

class LocationFragment : Fragment() {

    private lateinit var swMock:    SwitchMaterial
    private lateinit var etLat:     TextInputEditText
    private lateinit var etLon:     TextInputEditText
    private lateinit var etAlt:     TextInputEditText
    private lateinit var etAcc:     TextInputEditText
    private lateinit var btnRandom: Button
    private lateinit var btnSave:   Button
    private lateinit var webView:   WebView

    inner class WebAppInterface {
        @JavascriptInterface
        fun setCoordinates(lat: Double, lng: Double) {
            activity?.runOnUiThread {
                etLat.setText(lat.toString())
                etLon.setText(lng.toString())
            }
        }
    }

    private val usCities = listOf(
        Pair(40.7128, -74.0060),   // New York
        Pair(34.0522, -118.2437),  // Los Angeles
        Pair(41.8781, -87.6298),   // Chicago
        Pair(29.7604, -95.3698),   // Houston
        Pair(33.4484, -112.0740),  // Phoenix
        Pair(39.9526, -75.1652),   // Philadelphia
        Pair(32.7767, -96.7970),   // Dallas
        Pair(37.3382, -121.8863),  // San Jose
        Pair(47.6062, -122.3321),  // Seattle
        Pair(25.7617, -80.1918)    // Miami
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_location, container, false)
        swMock   = view.findViewById(R.id.sw_mock_location)
        etLat    = view.findViewById(R.id.et_latitude)
        etLon    = view.findViewById(R.id.et_longitude)
        etAlt    = view.findViewById(R.id.et_altitude)
        etAcc    = view.findViewById(R.id.et_accuracy)
        btnRandom = view.findViewById(R.id.btn_random_location)
        btnSave   = view.findViewById(R.id.btn_save_location)
        webView   = view.findViewById(R.id.webview_map)

        val ctx = requireContext()
        swMock.isChecked = PrefsManager.getBoolean(ctx, "mock_location_enabled", false)
        etLat.setText(PrefsManager.getString(ctx, "mock_latitude",  "40.7128"))
        etLon.setText(PrefsManager.getString(ctx, "mock_longitude", "-74.0060"))
        etAlt.setText(PrefsManager.getString(ctx, "mock_altitude",  "10.0"))
        etAcc.setText(PrefsManager.getString(ctx, "mock_accuracy",  "5.0"))

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val lat = etLat.text.toString().toDoubleOrNull() ?: 40.7128
                val lon = etLon.text.toString().toDoubleOrNull() ?: -74.0060
                webView.evaluateJavascript("setView($lat, $lon)", null)
            }
        }
        webView.loadUrl("file:///android_asset/map.html")

        btnRandom.setOnClickListener {
            val city = usCities.random()
            val lat = city.first  + (Math.random() - 0.5) * 0.05
            val lon = city.second + (Math.random() - 0.5) * 0.05

            etLat.setText(lat.toString())
            etLon.setText(lon.toString())
            etAlt.setText(((Math.random() * 80) + 5).toString())
            etAcc.setText(((Math.random() * 15) + 3).toString())

            webView.evaluateJavascript("setView($lat, $lon)", null)
        }

        btnSave.setOnClickListener {
            PrefsManager.saveBoolean(ctx, "mock_location_enabled", swMock.isChecked)
            PrefsManager.saveString(ctx, "mock_latitude",  etLat.text.toString())
            PrefsManager.saveString(ctx, "mock_longitude", etLon.text.toString())
            PrefsManager.saveString(ctx, "mock_altitude",  etAlt.text.toString())
            PrefsManager.saveString(ctx, "mock_accuracy",  etAcc.text.toString())
            Toast.makeText(ctx, "Location saved ✓", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
