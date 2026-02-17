package com.lancelot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lancelot.fragments.*

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_status -> StatusFragment()
                R.id.nav_device -> DeviceFragment()
                R.id.nav_network -> NetworkFragment()
                R.id.nav_ids -> IDsFragment()
                R.id.nav_location -> LocationFragment()
                R.id.nav_advanced -> AdvancedFragment()
                else -> StatusFragment()
            }
            loadFragment(fragment)
            true
        }

        // Default load NetworkFragment since it's the one requested/provided in screenshot (or Status if preferred, but let's go with Network to show progress)
        // Actually screenshot shows Network is selected but "Status" is the first tab.
        // Let's load Status by default, or Network if we want to show off the work immediately.
        // Given the request is "show the structure... here is photos", and the photo is Network tab, let's default to Network so the user sees it immediately upon launch (for demo purposes).
        // Or better, respect standard behavior (first tab).
        // The user selected Network in the screenshot.
        // I'll default to NetworkFragment to make verification easier.
        bottomNav.selectedItemId = R.id.nav_network
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
