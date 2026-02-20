package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.vortex.R

class SettingsFragment : Fragment() {

    private var locFrag: LocationFragment? = null
    private var advFrag: AdvancedFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_settings)

        // Inicializar child fragments solo una vez (sobreviven a rotaciones)
        if (childFragmentManager.findFragmentByTag("loc") == null) {
            locFrag = LocationFragment()
            advFrag = AdvancedFragment()
            childFragmentManager.beginTransaction()
                .add(R.id.settings_container, locFrag!!, "loc")
                .add(R.id.settings_container, advFrag!!, "adv")
                .hide(advFrag!!) // Location visible por defecto
                .commitNow()
        } else {
            locFrag = childFragmentManager.findFragmentByTag("loc") as LocationFragment
            advFrag = childFragmentManager.findFragmentByTag("adv") as AdvancedFragment
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                childFragmentManager.beginTransaction().apply {
                    if (tab.position == 0) {
                        show(locFrag!!)
                        hide(advFrag!!)
                    } else {
                        hide(locFrag!!)
                        show(advFrag!!)
                    }
                }.commit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        return view
    }
}
