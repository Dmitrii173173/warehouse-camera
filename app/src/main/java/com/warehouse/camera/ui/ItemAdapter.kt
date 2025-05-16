package com.warehouse.camera.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.ItemData

class ItemAdapter(
    private val context: Context,
    private val items: List<ItemData>,
    private val itemActionsListener: ItemActionsListener
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    
    interface ItemActionsListener {
        fun onTakeBoxPhoto(position: Int)
        fun onTakeProductPhoto(position: Int)
        fun onSaveItem(position: Int)
        fun onViewInGallery(position: Int)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ItemViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val articleCodeTextView: TextView = itemView.findViewById(R.id.textView_article_code)
        private val statusTextView: TextView = itemView.findViewById(R.id.textView_status)
        private val boxPhotoButton: Button = itemView.findViewById(R.id.button_photo_box)
        private val productPhotoButton: Button = itemView.findViewById(R.id.button_photo_product)
        private val boxPhotoStatusTextView: TextView = itemView.findViewById(R.id.textView_box_photo_status)
        private val productPhotoStatusTextView: TextView = itemView.findViewById(R.id.textView_product_photo_status)
        private val saveButton: Button = itemView.findViewById(R.id.button_save)
        private val viewGalleryButton: Button = itemView.findViewById(R.id.button_view_gallery)
        
        fun bind(item: ItemData, position: Int) {
            articleCodeTextView.text = item.fullArticleCode
            
            // Status
            if (item.isCompleted) {
                statusTextView.text = context.getString(R.string.status_completed)
                statusTextView.setBackgroundResource(R.drawable.bg_status)
            } else {
                statusTextView.text = context.getString(R.string.status_pending)
                statusTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.status_pending))
            }
            
            // Box photo status
            if (item.boxPhotoPath != null) {
                boxPhotoStatusTextView.text = context.getString(R.string.box_photo_taken)
                boxPhotoStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
            } else {
                boxPhotoStatusTextView.text = context.getString(R.string.box_photo_not_taken)
                boxPhotoStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.gray))
            }
            
            // Product photo status
            if (item.productPhotoPath != null) {
                productPhotoStatusTextView.text = context.getString(R.string.product_photo_taken)
                productPhotoStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.green))
            } else {
                productPhotoStatusTextView.text = context.getString(R.string.product_photo_not_taken)
                productPhotoStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.gray))
            }
            
            // Disable buttons if completed
            boxPhotoButton.isEnabled = !item.isCompleted
            productPhotoButton.isEnabled = !item.isCompleted
            saveButton.isEnabled = !item.isCompleted
            viewGalleryButton.isEnabled = item.boxPhotoPath != null || item.productPhotoPath != null
            
            // Button click listeners
            boxPhotoButton.setOnClickListener {
                itemActionsListener.onTakeBoxPhoto(position)
            }
            
            productPhotoButton.setOnClickListener {
                itemActionsListener.onTakeProductPhoto(position)
            }
            
            saveButton.setOnClickListener {
                itemActionsListener.onSaveItem(position)
            }
            
            viewGalleryButton.setOnClickListener {
                itemActionsListener.onViewInGallery(position)
            }
        }
    }
}
