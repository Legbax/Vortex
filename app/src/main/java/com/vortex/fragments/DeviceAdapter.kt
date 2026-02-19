package com.vortex.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vortex.MainHook
import com.vortex.R

class DeviceAdapter(
    private val profiles: Map<String, MainHook.DeviceFingerprint>,
    private val onSelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    private var selectedKey = ""
    private val keys = profiles.keys.toList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvInitial: TextView = v.findViewById(R.id.tv_brand_initial)
        val tvName: TextView    = v.findViewById(R.id.tv_device_name)
        val tvDetail: TextView  = v.findViewById(R.id.tv_device_details)
        val ivSelected: ImageView = v.findViewById(R.id.iv_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))

    override fun getItemCount() = keys.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val key = keys[position]
        val fp = profiles[key]!!

        h.tvInitial.text = fp.manufacturer.firstOrNull()?.toString()?.uppercase() ?: "?"
        h.tvName.text = fp.model // Use model as main name, it's more descriptive usually
        h.tvDetail.text = "${fp.manufacturer} · Android ${fp.release}"

        val isSelected = (key == selectedKey)
        h.ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Highlight background slightly if selected
        h.itemView.setBackgroundColor(
             if (isSelected) ContextCompat.getColor(h.itemView.context, R.color.vortex_accent_20)
             else ContextCompat.getColor(h.itemView.context, R.color.vortex_card_background)
        )

        h.itemView.setOnClickListener {
            val currentPos = h.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                val clickedKey = keys[currentPos]
                val prevKey = selectedKey
                selectedKey = clickedKey

                // Notify changes to update UI (checkbox and background)
                notifyItemChanged(keys.indexOf(prevKey))
                notifyItemChanged(currentPos)

                onSelected(clickedKey)
            }
        }
    }

    fun setSelected(key: String) {
        val prev = selectedKey
        selectedKey = key
        notifyItemChanged(keys.indexOf(prev))
        notifyItemChanged(keys.indexOf(key))
    }
}
