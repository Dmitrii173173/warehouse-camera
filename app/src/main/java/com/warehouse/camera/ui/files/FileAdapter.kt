package com.warehouse.camera.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val onItemClick: (File) -> Unit,
    private val onDeleteClick: ((File) -> Unit)? = null
) : ListAdapter<File, FileAdapter.FileViewHolder>(FileDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view, onItemClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class FileViewHolder(
        itemView: View,
        private val onItemClick: (File) -> Unit,
        private val onDeleteClick: ((File) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val iconView: ImageView = itemView.findViewById(R.id.icon_file)
        private val nameView: TextView = itemView.findViewById(R.id.text_file_name)
        private val infoView: TextView = itemView.findViewById(R.id.text_file_info)
        private val deleteButton: Button = itemView.findViewById(R.id.button_delete)
        
        fun bind(file: File) {
            // Set icon based on file type
            if (file.isDirectory) {
                iconView.setImageResource(R.drawable.ic_folder)
            } else {
                when {
                    file.extension.equals("jpg", ignoreCase = true) || 
                    file.extension.equals("jpeg", ignoreCase = true) ||
                    file.extension.equals("png", ignoreCase = true) -> {
                        iconView.setImageResource(R.drawable.ic_image)
                    }
                    file.extension.equals("txt", ignoreCase = true) -> {
                        iconView.setImageResource(R.drawable.ic_text_file)
                    }
                    else -> {
                        iconView.setImageResource(R.drawable.ic_file)
                    }
                }
            }
            
            // Set file name
            nameView.text = file.name
            
            // Set file info (size or item count for directories)
            if (file.isDirectory) {
                val childCount = file.listFiles()?.size ?: 0
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val lastModified = dateFormat.format(Date(file.lastModified()))
                infoView.text = itemView.context.getString(R.string.file_item_count, childCount) + " | " + lastModified
            } else {
                // Format file size
                val size = file.length()
                val readableSize = when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> "${size / 1024} KB"
                    else -> "${size / (1024 * 1024)} MB"
                }
                
                // Format last modified date
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val lastModified = dateFormat.format(Date(file.lastModified()))
                
                infoView.text = "$readableSize | $lastModified"
            }
            
            // Force the delete button to be visible
            deleteButton.visibility = View.VISIBLE
            
            // Set the button's click listener
            deleteButton.setOnClickListener {
                onDeleteClick?.invoke(file)
            }
            
            // Set click listener for the entire item
            itemView.setOnClickListener {
                onItemClick(file)
            }
        }
    }
    
    class FileDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }
        
        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath &&
                   oldItem.lastModified() == newItem.lastModified() &&
                   oldItem.length() == newItem.length()
        }
    }
}