package com.warehouse.camera.ui.reception

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.project.ProductReception
import java.text.SimpleDateFormat
import java.util.*

class ReceptionAdapter(
    private val context: android.content.Context,
    private var receptions: List<ProductReception>,
    private val listener: ReceptionClickListener
) : RecyclerView.Adapter<ReceptionAdapter.ReceptionViewHolder>() {
    
    interface ReceptionClickListener {
        fun onReceptionClicked(reception: ProductReception)
    }
    
    fun updateData(newReceptions: List<ProductReception>) {
        this.receptions = newReceptions.sortedByDescending { it.createdAt }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reception, parent, false)
        return ReceptionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ReceptionViewHolder, position: Int) {
        holder.bind(receptions[position])
    }
    
    override fun getItemCount(): Int = receptions.size
    
    inner class ReceptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textView_reception_name)
        private val dateTextView: TextView = itemView.findViewById(R.id.textView_reception_date)
        
        fun bind(reception: ProductReception) {
            nameTextView.text = reception.displayName
            
            // Format created date
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val createdDate = dateFormat.format(Date(reception.createdAt))
            dateTextView.text = context.getString(R.string.reception_date_format, createdDate)
            
            itemView.setOnClickListener {
                listener.onReceptionClicked(reception)
            }
        }
    }
}