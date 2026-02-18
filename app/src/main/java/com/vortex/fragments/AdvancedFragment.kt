package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.PrefsManager
import com.vortex.R

class AdvancedFragment : Fragment() {

    private lateinit var switchRoot: SwitchMaterial
    private lateinit var switchDebug: SwitchMaterial
    private lateinit var switchFiles: SwitchMaterial
    private lateinit var switchProcess: SwitchMaterial
    private lateinit var switchPackages: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)
        bindViews(view)
        loadData()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        switchRoot = view.findViewById(R.id.switch_hide_root)
        switchDebug = view.findViewById(R.id.switch_hide_debug)
        switchFiles = view.findViewById(R.id.switch_hide_files)
        switchProcess = view.findViewById(R.id.switch_process)
        switchPackages = view.findViewById(R.id.switch_packages)
    }

    private fun loadData() {
        val context = requireContext()
        // Default to TRUE for evasion features
        switchRoot.isChecked = PrefsManager.getBoolean(context, "hook_hide_root", true)
        switchDebug.isChecked = PrefsManager.getBoolean(context, "hook_hide_debug", true)
        switchFiles.isChecked = PrefsManager.getBoolean(context, "hook_hide_files", true)
        switchProcess.isChecked = PrefsManager.getBoolean(context, "hook_process", true)
        switchPackages.isChecked = PrefsManager.getBoolean(context, "hook_packages", true)
    }

    private fun setupListeners() {
        val context = requireContext()

        switchRoot.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "hook_hide_root", isChecked)
        }

        switchDebug.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "hook_hide_debug", isChecked)
        }

        switchFiles.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "hook_hide_files", isChecked)
        }

        switchProcess.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "hook_process", isChecked)
        }

        switchPackages.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(context, "hook_packages", isChecked)
        }
    }
}
