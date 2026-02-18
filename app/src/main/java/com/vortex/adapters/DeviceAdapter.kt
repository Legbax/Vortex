package com.vortex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vortex.R

class DeviceAdapter(
    private val devices: List<String>,
    private val onDeviceSelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    var selectedPosition = -1

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_device_name)
        // Removed references to tv_device_details and card_device as they don't exist in item_device.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.tvName.text = device

        holder.itemView.isSelected = (position == selectedPosition)

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            onDeviceSelected(devices[pos])
            updateSelection(pos)
        }
    }

    fun updateSelection(newPosition: Int) {
        val old = selectedPosition
        if (old == newPosition) return
        selectedPosition = newPosition
        if (old != -1) notifyItemChanged(old)
        if (newPosition != -1) notifyItemChanged(newPosition)
    }

    override fun getItemCount(): Int = devices.size
}
