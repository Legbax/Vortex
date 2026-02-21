package com.vortex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.vortex.fragments.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> { tab.setIcon(R.drawable.ic_nav_status) }
                1 -> { tab.setIcon(R.drawable.ic_nav_device) }
                2 -> { tab.setIcon(R.drawable.ic_nav_network) }
                3 -> { tab.setIcon(R.drawable.ic_nav_location) }
                4 -> { tab.setIcon(R.drawable.ic_nav_advanced) }
            }
        }.attach()

        // Seleccionar íconos cuando se hace click para cambiar el tint
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.icon?.setTint(getColor(R.color.vortex_accent))
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.icon?.setTint(getColor(R.color.vortex_text_secondary))
            }
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Initial Tint Update
        tabLayout.getTabAt(0)?.icon?.setTint(getColor(R.color.vortex_accent))
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> StatusFragment()
                1 -> IdentityFragment()
                2 -> NetworkFragment()
                3 -> LocationFragment()
                4 -> AdvancedFragment()
                else -> StatusFragment()
            }
        }
    }
}
