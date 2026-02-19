package com.vortex.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vortex.PrefsManager
import com.vortex.R

class StatusFragment : Fragment() {

    private lateinit var progressEvasion: CircularProgressIndicator
    private lateinit var textScore: TextView
    private lateinit var switchMaster: SwitchMaterial
    private lateinit var textProfile: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)
        bindViews(view)
        loadData()
        setupListeners()
        return view
    }

    private fun bindViews(view: View) {
        progressEvasion = view.findViewById(R.id.progress_evasion)
        textScore = view.findViewById(R.id.text_score)
        switchMaster = view.findViewById(R.id.switch_master)
        textProfile = view.findViewById(R.id.text_current_profile)
    }

    private fun loadData() {
        val context = requireContext()
        val profile = PrefsManager.getString(context, "profile", "Redmi 9")
        val masterEnabled = PrefsManager.getBoolean(context, "master_switch", true)

        textProfile.text = profile
        switchMaster.isChecked = masterEnabled

        // Mock Score Logic
        val score = if (masterEnabled) 85 else 0
        updateScore(score)
    }

    private fun setupListeners() {
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.saveBoolean(requireContext(), "master_switch", isChecked)
            updateScore(if (isChecked) 85 else 0)
        }
    }

    private fun updateScore(score: Int) {
        progressEvasion.setProgressCompat(score, true)
        textScore.text = score.toString()
    }
}
