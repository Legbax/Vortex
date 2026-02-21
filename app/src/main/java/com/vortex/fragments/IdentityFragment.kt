package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.vortex.R

class IdentityFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_identity, container, false)

        tabLayout = view.findViewById(R.id.tab_layout_identity)
        viewPager = view.findViewById(R.id.view_pager_identity)

        // Disable swipe to avoid conflict with horizontal sliders or just personal preference for "solid" feel
        // User requested "Merge", typically tabs allow swipe, but inner elements might conflict.
        // I'll enable swipe as it's standard for Tabs.
        viewPager.isUserInputEnabled = true

        val adapter = IdentityPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Device"
                    tab.setIcon(R.drawable.ic_nav_device)
                }
                1 -> {
                    tab.text = "Identifiers"
                    tab.setIcon(R.drawable.ic_nav_ids)
                }
            }
        }.attach()

        return view
    }

    private inner class IdentityPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DeviceFragment()
                1 -> IDsFragment()
                else -> DeviceFragment()
            }
        }
    }
}
