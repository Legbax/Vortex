package com.vortex.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vortex.DeviceData
import com.vortex.R

class CarrierAdapter(
    private val allCarriers: List<DeviceData.UsCarrier>,
    private val onCarrierSelected: (DeviceData.UsCarrier) -> Unit
) : RecyclerView.Adapter<CarrierAdapter.CarrierViewHolder>() {

    private var filteredCarriers = allCarriers.toList()
    private var selectedPosition = -1
    // Track selected MCC/MNC to maintain selection across filters
    private var selectedMccMnc: String? = null

    class CarrierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_carrier_name)
        val tvMccMnc: TextView = itemView.findViewById(R.id.tv_mcc_mnc)

        fun bind(carrier: DeviceData.UsCarrier, isSelected: Boolean) {
            tvName.text = carrier.name
            tvMccMnc.text = carrier.mccMnc
            itemView.isSelected = isSelected
            itemView.alpha = if (isSelected) 1.0f else 0.7f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carrier, parent, false)
        return CarrierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarrierViewHolder, position: Int) {
        val carrier = filteredCarriers[position]
        // Check if this carrier matches the selected one
        val isSelected = carrier.mccMnc == selectedMccMnc
        holder.bind(carrier, isSelected)

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            selectedMccMnc = carrier.mccMnc
            notifyDataSetChanged() // Refresh all to update selection state visual
            onCarrierSelected(carrier)
        }
    }

    override fun getItemCount(): Int = filteredCarriers.size

    fun setSelected(mccMnc: String) {
        selectedMccMnc = mccMnc
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredCarriers = if (query.isEmpty()) {
            allCarriers.toList()
        } else {
            allCarriers.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.mccMnc.contains(query)
            }
        }
        notifyDataSetChanged()
    }
}
