package com.aurasapphire.aeroplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class TrackAdapter(
    private val items: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<TrackAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.trackNumber)
        val title: TextView = view.findViewById(R.id.trackTitle)
        val meta: TextView = view.findViewById(R.id.trackMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val file = items[position]
        holder.number.text = String.format("%02d", position + 1)
        holder.title.text = file.nameWithoutExtension
        holder.meta.text = "Local file • ${file.extension.uppercase()}"
        holder.itemView.setOnClickListener { onClick(file) }
    }

    override fun getItemCount() = items.size
}
