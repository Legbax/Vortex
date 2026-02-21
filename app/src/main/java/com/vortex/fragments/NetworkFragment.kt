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

class NetworkFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        val viewPager = view.findViewById<ViewPager2>(R.id.vp_network_sub)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_network_sub)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) CarrierFragment() else ProxyFragment()
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Carrier / IDs" else "SOCKS5 Proxy"
        }.attach()

        return view
    }
}
