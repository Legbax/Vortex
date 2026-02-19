package com.vortex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vortex.MainHook
import com.vortex.R

class DeviceAdapter(
    private val devices: List<String>,
    private val onDeviceSelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    var selectedPosition = -1

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvModel: TextView = itemView.findViewById(R.id.tv_model)
        val tvDeviceCode: TextView = itemView.findViewById(R.id.tv_device_code)
        val tvInitials: TextView = itemView.findViewById(R.id.tv_initials)
        val ivSelected: ImageView = itemView.findViewById(R.id.iv_selected)

        // Expanded details
        val layoutExpanded: LinearLayout = itemView.findViewById(R.id.layout_expanded)
        val tvFingerprint: TextView = itemView.findViewById(R.id.tv_fingerprint)
        val tvBoard: TextView = itemView.findViewById(R.id.tv_board)
        val btnRandomize: Button = itemView.findViewById(R.id.btn_randomize_build)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val deviceName = devices[position]
        val fingerprint = MainHook.DEVICE_FINGERPRINTS[deviceName]

        holder.tvModel.text = fingerprint?.model ?: deviceName
        holder.tvDeviceCode.text = fingerprint?.device ?: "unknown"

        val manu = fingerprint?.manufacturer ?: "XX"
        holder.tvInitials.text = manu.take(2).uppercase()

        val isSelected = (position == selectedPosition)
        holder.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Expansion logic: Expand if selected
        holder.layoutExpanded.visibility = if (isSelected) View.VISIBLE else View.GONE

        if (fingerprint != null) {
            holder.tvFingerprint.text = fingerprint.fingerprint
            holder.tvBoard.text = fingerprint.board
        }

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            // Toggle selection
            val oldPos = selectedPosition
            if (oldPos != pos) {
                selectedPosition = pos
                notifyItemChanged(oldPos)
                notifyItemChanged(pos)
                onDeviceSelected(deviceName)
            }
        }

        holder.btnRandomize.setOnClickListener {
            // Mock randomize action
            // In real app this would trigger randomization logic
        }
    }

    override fun getItemCount(): Int = devices.size
}
