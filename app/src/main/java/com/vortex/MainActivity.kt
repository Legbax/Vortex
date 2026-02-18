package com.vortex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vortex.fragments.*

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

        bottomNav.selectedItemId = R.id.nav_status
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}
