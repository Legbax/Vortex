package com.vortex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vortex.DeviceData
import com.vortex.R

class DeviceAdapter(
    private val devices: Map<String, DeviceData.DeviceFingerprint>,
    private val onDeviceSelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val deviceList = devices.toList()
    var selectedPosition = -1

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_device_name)
        val tvDetails: TextView = itemView.findViewById(R.id.tv_device_details)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val (name, fp) = deviceList[position]
        holder.tvName.text = name
        holder.tvDetails.text = "${fp.manufacturer} · Android ${fp.release}"

        holder.itemView.isSelected = (position == selectedPosition)
        // Simple visual indication for selection if background supports it (state_selected)
        holder.itemView.alpha = if (position == selectedPosition) 1.0f else 0.7f

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            onDeviceSelected(name)
            updateSelection(pos)
        }
    }

    private fun updateSelection(newPosition: Int) {
        val old = selectedPosition
        if (old == newPosition) return
        selectedPosition = newPosition
        if (old != -1) notifyItemChanged(old)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    override fun getItemCount(): Int = deviceList.size
}
