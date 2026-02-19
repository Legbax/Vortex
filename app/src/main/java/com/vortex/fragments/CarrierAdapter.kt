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

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = carriers[pos]
        h.tvName.text = c.name
        h.tvMcc.text  = "MCC/MNC: ${c.mccMnc} · ${c.spn}"
        h.itemView.setBackgroundColor(
            if (pos == selectedIdx)
                ContextCompat.getColor(h.itemView.context, R.color.vortex_accent_20)
            else
                ContextCompat.getColor(h.itemView.context, R.color.vortex_card_background)
        )
        h.itemView.setOnClickListener {
            val prev = selectedIdx; selectedIdx = pos
            notifyItemChanged(prev); notifyItemChanged(pos)
            onSelected(c)
        }
    }

    fun setSelected(mccMnc: String) {
        val prev = selectedIdx
        selectedIdx = carriers.indexOfFirst { it.mccMnc == mccMnc }
        notifyItemChanged(prev); notifyItemChanged(selectedIdx)
    }
}
