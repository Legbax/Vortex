package com.vortex.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
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
    
    // Bandera para evitar bucles infinitos al actualizar texto vs mapa
    private var isUpdatingFromMap = false

    inner class WebAppInterface {
        @JavascriptInterface
        fun setCoordinates(lat: Double, lng: Double) {
            activity?.runOnUiThread {
                isUpdatingFromMap = true
                etLat.setText(lat.toString())
                etLon.setText(lng.toString())
                isUpdatingFromMap = false
            }
        }
    }

    private val usCities = listOf(
        Pair(40.7128, -74.0060),     // 1. New York, NY
        Pair(34.0522, -118.2437),    // 2. Los Angeles, CA
        Pair(41.8781, -87.6298),     // 3. Chicago, IL
        Pair(29.7604, -95.3698),     // 4. Houston, TX
        Pair(33.4484, -112.0740),    // 5. Phoenix, AZ
        Pair(29.4241, -98.4936),     // 6. San Antonio, TX
        Pair(32.7157, -117.1611),    // 7. San Diego, CA
        Pair(32.7767, -96.7970),     // 8. Dallas, TX
        Pair(37.3382, -121.8863),    // 9. San Jose, CA
        Pair(30.2672, -97.7431),     // 10. Austin, TX
        Pair(39.9526, -75.1652),     // 11. Philadelphia, PA
        Pair(30.3322, -81.6557),     // 12. Jacksonville, FL
        Pair(32.7555, -97.3308),     // 13. Fort Worth, TX
        Pair(39.9612, -82.9988),     // 14. Columbus, OH
        Pair(39.7684, -86.1581),     // 15. Indianapolis, IN
        Pair(35.2271, -80.8431),     // 16. Charlotte, NC
        Pair(37.7749, -122.4194),    // 17. San Francisco, CA
        Pair(47.6062, -122.3321),    // 18. Seattle, WA
        Pair(39.7392, -104.9903),    // 19. Denver, CO
        Pair(35.4676, -97.5164),     // 20. Oklahoma City, OK
        Pair(36.1627, -86.7816),     // 21. Nashville, TN
        Pair(31.7619, -106.4850),    // 22. El Paso, TX
        Pair(38.9072, -77.0369),     // 23. Washington, DC
        Pair(42.3601, -71.0589),     // 24. Boston, MA
        Pair(36.1699, -115.1398),    // 25. Las Vegas, NV
        Pair(45.5231, -122.6765),    // 26. Portland, OR
        Pair(42.3314, -83.0458),     // 27. Detroit, MI
        Pair(38.2527, -85.7585),     // 28. Louisville, KY
        Pair(35.1495, -90.0490),     // 29. Memphis, TN
        Pair(39.2904, -76.6122),     // 30. Baltimore, MD
        Pair(43.0389, -87.9065),     // 31. Milwaukee, WI
        Pair(35.0844, -106.6504),    // 32. Albuquerque, NM
        Pair(32.2226, -110.9747),    // 33. Tucson, AZ
        Pair(36.7378, -119.7871),    // 34. Fresno, CA
        Pair(38.5816, -121.4944),    // 35. Sacramento, CA
        Pair(33.4152, -111.8315),    // 36. Mesa, AZ
        Pair(39.0997, -94.5786),     // 37. Kansas City, MO
        Pair(33.7490, -84.3880),     // 38. Atlanta, GA
        Pair(33.7701, -118.1937),    // 39. Long Beach, CA
        Pair(41.2565, -95.9345),     // 40. Omaha, NE
        Pair(35.7796, -78.6382),     // 41. Raleigh, NC
        Pair(38.8339, -104.8214),    // 42. Colorado Springs, CO
        Pair(25.7617, -80.1918),     // 43. Miami, FL
        Pair(36.8529, -75.9779),     // 44. Virginia Beach, VA
        Pair(37.8044, -122.2711),    // 45. Oakland, CA
        Pair(44.9778, -93.2650),     // 46. Minneapolis, MN
        Pair(36.1540, -95.9928),     // 47. Tulsa, OK
        Pair(32.7357, -97.1081),     // 48. Arlington, TX
        Pair(29.9511, -90.0715),     // 49. New Orleans, LA
        Pair(37.6872, -97.3301)      // 50. Wichita, KS
    )

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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

        // Configuración del WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        
        // --- LA MAGIA ESTÁ AQUÍ ---
        // Prevenir que el NestedScrollView robe los eventos táctiles del mapa
        webView.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                updateMapFromInputs()
            }
        }
        webView.loadUrl("file:///android_asset/map.html")

        // Listeners para actualizar el mapa si el usuario edita los textos manualmente
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isUpdatingFromMap) {
                    updateMapFromInputs()
                }
            }
        }
        etLat.addTextChangedListener(textWatcher)
        etLon.addTextChangedListener(textWatcher)

        // Botón Random City
        btnRandom.setOnClickListener {
            val city = usCities.random()
            val lat = city.first  + (Math.random() - 0.5) * 0.05
            val lon = city.second + (Math.random() - 0.5) * 0.05

            // El TextWatcher se encargará de llamar a updateMapFromInputs()
            etLat.setText(lat.toString())
            etLon.setText(lon.toString())
            etAlt.setText(String.format("%.1f", (Math.random() * 80) + 5).replace(",", "."))
            etAcc.setText(String.format("%.1f", (Math.random() * 15) + 3).replace(",", "."))
        }

        // Botón Guardar
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

    private fun updateMapFromInputs() {
        val lat = etLat.text.toString().toDoubleOrNull() ?: 40.7128
        val lon = etLon.text.toString().toDoubleOrNull() ?: -74.0060
        webView.evaluateJavascript("setView($lat, $lon)", null)
    }
}
