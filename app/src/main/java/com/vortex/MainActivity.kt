package com.vortex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vortex.fragments.*

class MainActivity : AppCompatActivity() {

    // FIX #19: currentFragment rastreado para usar show/hide en lugar de replace,
    // lo que preserva el estado de la View sin recrear el Fragment.
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.setOnItemSelectedListener { item ->
            navigateTo(item.itemId)
            true
        }

        // FIX #20: Solo establecer tab inicial si es un arranque limpio (no rotación).
        // Sin esta guarda, la tab vuelve a nav_status cada vez que se rota la pantalla.
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_status
        } else {
            // Restaurar referencia al fragment actualmente visible tras recreación
            val savedTag = savedInstanceState.getString("current_fragment_tag")
            if (savedTag != null) {
                currentFragment = supportFragmentManager.findFragmentByTag(savedTag)
            }
        }
    }

    // FIX #20: Guardar el tag del fragment actual para restaurarlo tras rotación
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFragment?.let { outState.putString("current_fragment_tag", it.tag) }
    }

    // FIX #19: show/hide preserva las Views; findFragmentByTag evita recrear
    // instancias en cada cambio de tab.
    private fun navigateTo(itemId: Int) {
        val tag = "frag_$itemId"
        val transaction = supportFragmentManager.beginTransaction()

        // Ocultar el fragment actual
        currentFragment?.let { transaction.hide(it) }

        // Buscar o crear el fragment destino
        var target = supportFragmentManager.findFragmentByTag(tag)
        if (target == null) {
            target = createFragment(itemId)
            transaction.add(R.id.fragment_container, target, tag)
        } else {
            transaction.show(target)
        }

        currentFragment = target
        transaction.commitAllowingStateLoss()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_status   -> StatusFragment()
        R.id.nav_device   -> DeviceFragment()
        R.id.nav_ids      -> IDsFragment()
        R.id.nav_network  -> NetworkFragment()
        R.id.nav_location -> LocationFragment()
        R.id.nav_advanced -> AdvancedFragment()
        else              -> StatusFragment()
    }
}
