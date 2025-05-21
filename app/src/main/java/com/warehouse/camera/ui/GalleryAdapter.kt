package com.warehouse.camera.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.GalleryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GalleryAdapter(
    private val context: Context,
    private val items: List<GalleryItem>
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return GalleryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.imageView_thumbnail)
        private val filenameTextView: TextView = itemView.findViewById(R.id.textView_filename)
        private val dateTextView: TextView = itemView.findViewById(R.id.textView_date)
        private val typeTextView: TextView = itemView.findViewById(R.id.textView_type)
        
        fun bind(item: GalleryItem) {
            filenameTextView.text = item.name
            dateTextView.text = dateFormat.format(item.date)
            
            // Set type
            when (item.type) {
                GalleryItem.ItemType.BOX_PHOTO -> {
                    typeTextView.text = context.getString(R.string.gallery_box_photo)
                    loadThumbnail(item)
                }
                GalleryItem.ItemType.PRODUCT_PHOTO -> {
                    typeTextView.text = context.getString(R.string.gallery_product_photo)
                    loadThumbnail(item)
                }
                GalleryItem.ItemType.TEXT_FILE -> {
                    typeTextView.text = context.getString(R.string.gallery_details)
                    thumbnailImageView.setImageResource(R.drawable.ic_document)
                }
                else -> {
                    typeTextView.text = item.name
                    thumbnailImageView.setImageResource(R.drawable.ic_document)
                }
            }
            
            // Set click listener
            itemView.setOnClickListener {
                val intent = Intent(context, ImageViewerActivity::class.java)
                intent.putExtra("galleryItem", item)
                context.startActivity(intent)
            }
        }
        
        private fun loadThumbnail(item: GalleryItem) {
            try {
                // Load thumbnail efficiently
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(item.path, options)
                
                // Define thumbnail dimensions
                val thumbnailWidth = 300
                val thumbnailHeight = 300
                
                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, thumbnailWidth, thumbnailHeight)
                
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeFile(item.path, options)
                thumbnailImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                thumbnailImageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
        
        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
        inSampleSize *= 2
        }
        }
        
        return inSampleSize
        }
    
    /**
     * Check if file is an image
     */
    fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
    }
    }
}
