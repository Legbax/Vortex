package com.vortex.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vortex.MainHook
import com.vortex.R

class CarrierAdapter(
    private val carriers: List<MainHook.Companion.UsCarrier>,
    private val onCarrierSelected: (MainHook.Companion.UsCarrier) -> Unit
) : RecyclerView.Adapter<CarrierAdapter.CarrierViewHolder>() {

    private var selectedPosition = -1

    fun setSelected(mccMnc: String) {
        val index = carriers.indexOfFirst { it.mccMnc == mccMnc }
        if (index != -1) {
            val previous = selectedPosition
            selectedPosition = index
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrierViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrier, parent, false)
        return CarrierViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarrierViewHolder, position: Int) {
        val carrier = carriers[position]
        holder.bind(carrier, position == selectedPosition)
    }

    override fun getItemCount() = carriers.size

    inner class CarrierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardRoot: MaterialCardView = itemView.findViewById(R.id.card_root)
        private val tvName: TextView = itemView.findViewById(R.id.tv_carrier_name)
        private val tvMccMnc: TextView = itemView.findViewById(R.id.tv_mcc_mnc)
        private val ivCheck: ImageView = itemView.findViewById(R.id.iv_check)
        private val ivDot: ImageView = itemView.findViewById(R.id.iv_dot)

        fun bind(carrier: MainHook.Companion.UsCarrier, isSelected: Boolean) {
            tvName.text = carrier.name
            tvMccMnc.text = "MCC/MNC ${carrier.mccMnc}"

            if (isSelected) {
                cardRoot.strokeColor = ContextCompat.getColor(itemView.context, R.color.vortex_accent)
                ivCheck.visibility = View.VISIBLE
            } else {
                cardRoot.strokeColor = ContextCompat.getColor(itemView.context, R.color.vortex_card_background)
                ivCheck.visibility = View.GONE
            }

            val dotColor = when(carrier.name) {
                "T-Mobile" -> "#E91E63" // Pink
                "AT&T" -> "#03A9F4" // Blue
                "Verizon" -> "#F44336" // Red
                "Sprint" -> "#FFEB3B" // Yellow
                "Cricket" -> "#4CAF50" // Green
                else -> "#2196F3" // Default Blue
            }
            ivDot.background.setTint(Color.parseColor(dotColor))

            itemView.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
                onCarrierSelected(carrier)
            }
        }
    }
}
