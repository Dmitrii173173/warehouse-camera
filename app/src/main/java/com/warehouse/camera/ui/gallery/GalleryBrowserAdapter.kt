package com.warehouse.camera.ui.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.GalleryItem

class GalleryBrowserAdapter(
    private val context: Context,
    private val items: List<GalleryItem>,
    private val onItemClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<GalleryBrowserAdapter.PhotoViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_browser, parent, false)
        return PhotoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.imageView_photo)
        private val fileNameTextView: TextView = itemView.findViewById(R.id.textView_file_name)
        
        fun bind(item: GalleryItem) {
            fileNameTextView.text = item.name
            
            try {
                // Load thumbnail efficiently
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(item.path, options)
                
                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, 500, 500)
                
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeFile(item.path, options)
                photoImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                photoImageView.setImageResource(R.drawable.ic_launcher_foreground)
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onItemClick(item)
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
    }
}