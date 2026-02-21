package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.vortex.PrefsManager
import com.vortex.ProxyManager
import com.vortex.R
import com.vortex.utils.CryptoUtils

class ProxyFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_proxy, container, false)

        val swEnable = view.findViewById<SwitchMaterial>(R.id.sw_proxy_enable)
        val tvStatus = view.findViewById<TextView>(R.id.tv_proxy_status)
        val etHost = view.findViewById<TextInputEditText>(R.id.et_proxy_host)
        val etPort = view.findViewById<TextInputEditText>(R.id.et_proxy_port)
        val etUser = view.findViewById<TextInputEditText>(R.id.et_proxy_user)
        val etPass = view.findViewById<TextInputEditText>(R.id.et_proxy_pass)
        val swGlobal = view.findViewById<SwitchMaterial>(R.id.sw_proxy_global)
        val swWebView = view.findViewById<SwitchMaterial>(R.id.sw_proxy_webview)
        val btnApply = view.findViewById<MaterialButton>(R.id.btn_proxy_apply)
        val btnStop = view.findViewById<MaterialButton>(R.id.btn_proxy_stop)

        val ctx = requireContext()

        // Carga Inicial
        CryptoUtils.getProxyConfig(ctx)?.let {
            etHost.setText(it.host)
            etPort.setText(it.port.toString())
            etUser.setText(it.user)
            etPass.setText(it.pass)
            swGlobal.isChecked = it.isGlobal
            swWebView.isChecked = it.includeWebView
        }

        val isProxyActive = PrefsManager.getBoolean(ctx, "proxy_active", false)
        swEnable.isChecked = isProxyActive
        updateStatusUI(isProxyActive, tvStatus)

        // Lógica del Switch Automático
        swEnable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ProxyManager.startProxy(ctx)) {
                    PrefsManager.saveBoolean(ctx, "proxy_active", true)
                    updateStatusUI(true, tvStatus)
                    Toast.makeText(ctx, "Kernel Proxy Routing ACTIVE ✓", Toast.LENGTH_SHORT).show()
                } else {
                    swEnable.isChecked = false
                    updateStatusUI(false, tvStatus)
                    Toast.makeText(ctx, "Error starting redsocks daemon", Toast.LENGTH_LONG).show()
                }
            } else {
                ProxyManager.stopProxy(ctx)
                PrefsManager.saveBoolean(ctx, "proxy_active", false)
                updateStatusUI(false, tvStatus)
            }
        }

        // Guardar Configuración
        btnApply.setOnClickListener {
            val host = etHost.text.toString().trim()
            if (host.isEmpty()) return@setOnClickListener

            val config = CryptoUtils.ProxyConfig(
                host, etPort.text.toString().toIntOrNull() ?: 1080,
                etUser.text.toString().trim(), etPass.text.toString().trim(),
                swGlobal.isChecked, swWebView.isChecked
            )
            CryptoUtils.saveProxyConfig(ctx, config)
            Toast.makeText(ctx, "Config Saved. Toggle switch to apply.", Toast.LENGTH_SHORT).show()
        }

        // Freno de Emergencia
        btnStop.setOnClickListener {
            swEnable.isChecked = false // Lanza el listener de apagado
            Toast.makeText(ctx, "Emergency Stop / Iptables Flushed", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun updateStatusUI(isActive: Boolean, tvStatus: TextView) {
        val ctx = requireContext()
        if (isActive) {
            tvStatus.text = "✓ ACTIVE (Kernel NAT)"
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.vortex_accent))
        } else {
            tvStatus.text = "✗ STOPPED"
            tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.vortex_text_secondary))
        }
    }
}
