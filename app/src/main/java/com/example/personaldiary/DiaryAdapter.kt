package com.example.personaldiary

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DiaryAdapter(
    private var entries: List<DiaryEntry> = emptyList(),
    private val onItemClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<DiaryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.tvTitle.text = entry.title
        holder.tvContent.text = entry.content
        holder.tvDate.text = entry.createdAt

        if (!entry.imagePath.isNullOrBlank()) {
            try {
                val file = File(entry.imagePath)
                if (file.exists() && file.length() > 0) {
                    val bitmap = BitmapFactory.decodeFile(entry.imagePath)
                    if (bitmap != null) {
                        holder.ivImage.setImageBitmap(bitmap)
                        holder.ivImage.visibility = View.VISIBLE
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        holder.ivImage.visibility = View.GONE
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<DiaryEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}