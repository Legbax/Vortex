package com.vortex.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vortex.MainHook
import com.vortex.R

class DeviceAdapter(
    private val profiles: Map<String, MainHook.DeviceFingerprint>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private val keys = profiles.keys.toList()
    private var selectedKey: String? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_device_name)
        val tvDetails: TextView = view.findViewById(R.id.tv_device_details)
        val card: View = view.findViewById(R.id.card_device)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = keys[position]
        val profile = profiles[key] ?: return

        holder.tvName.text = key
        holder.tvDetails.text = "${profile.manufacturer} ${profile.model} (Android ${profile.release})"

        if (key == selectedKey) {
            holder.card.setBackgroundResource(R.drawable.bg_card_selected) // Assume this exists or use color
        } else {
            holder.card.setBackgroundResource(R.drawable.bg_card_normal) // Assume this exists
        }

        // Fallback if drawables don't exist, just alpha/color
        if (key == selectedKey) {
            holder.card.alpha = 1.0f
        } else {
            holder.card.alpha = 0.7f
        }

        holder.itemView.setOnClickListener {
            selectedKey = key
            notifyDataSetChanged()
            onSelect(key)
        }
    }

    override fun getItemCount() = keys.size

    fun setSelected(key: String) {
        selectedKey = key
        notifyDataSetChanged()
    }
}
