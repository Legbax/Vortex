package com.lancelot.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lancelot.MainHook
import com.lancelot.R

class CarrierAdapter(
    private val carriers: List<MainHook.Companion.UsCarrier>,
    private val onCarrierSelected: (MainHook.Companion.UsCarrier) -> Unit
) : RecyclerView.Adapter<CarrierAdapter.CarrierViewHolder>() {

    class CarrierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_carrier_name)
        val tvMccMnc: TextView = itemView.findViewById(R.id.tv_mcc_mnc)

        fun bind(carrier: MainHook.Companion.UsCarrier) {
            tvName.text = carrier.name
            tvMccMnc.text = carrier.mccMnc
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carrier, parent, false)
        return CarrierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarrierViewHolder, position: Int) {
        val carrier = carriers[position]
        holder.bind(carrier)

        holder.itemView.setOnClickListener {
            // FIX #21: Reemplazado adapterPosition por bindingAdapterPosition + guarda
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            onCarrierSelected(carriers[pos])
        }
    }

    override fun getItemCount(): Int = carriers.size
}
