package com.vortex.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vortex.DeviceData
import com.vortex.R

class DeviceAdapter(
    private val profiles: Map<String, DeviceData.DeviceFingerprint>,
    private val onSelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    private var selectedKey = ""
    private val keys = profiles.keys.toList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView    = v.findViewById(R.id.tv_device_name)
        val tvDetail: TextView  = v.findViewById(R.id.tv_device_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))

    override fun getItemCount() = keys.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val key = keys[pos]
        val fp = profiles[key]!!
        h.tvName.text = key
        h.tvDetail.text = "${fp.manufacturer} · Android ${fp.release} · ${fp.hardware.uppercase()}"
        h.itemView.setBackgroundColor(
            if (key == selectedKey)
                ContextCompat.getColor(h.itemView.context, R.color.vortex_accent_20)
            else
                ContextCompat.getColor(h.itemView.context, R.color.vortex_card_background)
        )
        h.itemView.setOnClickListener {
            val prev = selectedKey; selectedKey = key
            notifyItemChanged(keys.indexOf(prev)); notifyItemChanged(pos)
            onSelected(key)
        }
    }

    fun setSelected(key: String) {
        val prev = selectedKey; selectedKey = key
        notifyItemChanged(keys.indexOf(prev)); notifyItemChanged(keys.indexOf(key))
    }
}
