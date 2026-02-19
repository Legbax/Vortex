package com.vortex.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vortex.MainHook
import com.vortex.R

class CarrierAdapter(
    private val carriers: List<MainHook.Companion.UsCarrier>,
    private val onSelected: (MainHook.Companion.UsCarrier) -> Unit
) : RecyclerView.Adapter<CarrierAdapter.VH>() {

    private var selectedIdx = -1

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView   = v.findViewById(R.id.tv_carrier_name)
        val tvMcc: TextView    = v.findViewById(R.id.tv_mcc_mnc)
    }

    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_carrier, p, false))

    override fun getItemCount() = carriers.size

    override fun onBindViewHolder(h: VH, position: Int) {
        // Use bindingAdapterPosition when clicking, but for initial binding use position
        val c = carriers[position]
        h.tvName.text = c.name
        h.tvMcc.text  = "${c.mccMnc} · ${c.spn}" // Cleaned up format for new horizontal layout

        val isSelected = (position == selectedIdx)
        h.itemView.setBackgroundColor(
            if (isSelected)
                ContextCompat.getColor(h.itemView.context, R.color.vortex_accent_20)
            else
                ContextCompat.getColor(h.itemView.context, R.color.vortex_card_background)
        )

        h.itemView.setOnClickListener {
            val currentPos = h.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                val prev = selectedIdx
                selectedIdx = currentPos
                notifyItemChanged(prev)
                notifyItemChanged(selectedIdx)
                onSelected(carriers[currentPos])
            }
        }
    }

    fun setSelected(mccMnc: String) {
        val prev = selectedIdx
        selectedIdx = carriers.indexOfFirst { it.mccMnc == mccMnc }
        notifyItemChanged(prev)
        notifyItemChanged(selectedIdx)
    }
}
