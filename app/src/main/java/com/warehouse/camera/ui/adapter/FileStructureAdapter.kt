package com.warehouse.camera.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.warehouse.camera.R
import com.warehouse.camera.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileStructureAdapter(
    context: Context,
    private val files: MutableList<File>,
    private val onFileDeleted: () -> Unit,
    private val onItemClick: (File) -> Unit
) : ArrayAdapter<File>(context, 0, files) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = getItem(position) ?: return convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_file_structure, parent, false
        )
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_file_structure, parent, false
        )
        
        val ivIcon = view.findViewById<ImageView>(R.id.iv_file_icon)
        val tvFileName = view.findViewById<TextView>(R.id.tv_file_name)
        val tvFileInfo = view.findViewById<TextView>(R.id.tv_file_info)
        val btnDelete = view.findViewById<TextView>(R.id.btn_delete)
        val layoutContent = view.findViewById<LinearLayout>(R.id.layout_item_content)
        
        // Устанавливаем иконку в зависимости от типа файла
        if (file.isDirectory) {
            ivIcon.setImageResource(R.drawable.ic_folder)
        } else {
            // Определяем тип файла по расширению
            when {
                isImageFile(file) -> ivIcon.setImageResource(R.drawable.ic_image)
                isTextFile(file) -> ivIcon.setImageResource(R.drawable.ic_text_file)
                else -> ivIcon.setImageResource(R.drawable.ic_file)
            }
        }
        
        // Устанавливаем имя файла
        tvFileName.text = file.name
        
        // Устанавливаем информацию о файле
        val lastModified = Date(file.lastModified())
        val formattedDate = dateFormat.format(lastModified)
        
        if (file.isDirectory) {
            val childCount = file.listFiles()?.size ?: 0
            tvFileInfo.text = context.getString(R.string.file_item_count, childCount) + " | " + formattedDate
        } else {
            val fileSize = formatFileSize(file.length())
            tvFileInfo.text = context.getString(R.string.file_info, fileSize, formattedDate)
        }
        
        // Показываем кнопку удаления для всех файлов и папок
        btnDelete.visibility = View.VISIBLE
        
        // Настраиваем обработчик нажатия на кнопку удаления
        btnDelete.setOnClickListener {
            showDeleteConfirmation(file, position)
        }
        
        // Настраиваем обработчик нажатия на основную часть элемента
        layoutContent.setOnClickListener {
            onItemClick(file)
        }
        
        return view
    }
    
    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
    }
    
    private fun isTextFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".txt") || name.endsWith(".json") || 
               name.endsWith(".xml") || name.endsWith(".csv")
    }
    
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            "%.1f %s", 
            size / Math.pow(1024.0, digitGroups.toDouble()), 
            units[digitGroups]
        )
    }
    
    /**
     * Shows a confirmation dialog before deleting a file or directory
     */
    private fun showDeleteConfirmation(file: File, position: Int) {
        val messageResId = if (file.isDirectory) {
            R.string.confirm_delete_folder
        } else {
            R.string.confirm_delete_file
        }
        
        AlertDialog.Builder(context)
            .setTitle(R.string.delete)
            .setMessage(context.getString(messageResId, file.name))
            .setPositiveButton(R.string.confirm) { _, _ ->
                if (FileUtils.deleteFileOrDirectory(file, context)) {
                    // Remove item from adapter
                    remove(getItem(position))
                    notifyDataSetChanged()
                    
                    // Notify the activity that a file was deleted
                    onFileDeleted()
                } else {
                    // Show error message
                    AlertDialog.Builder(context)
                        .setTitle(R.string.delete_error)
                        .setMessage(R.string.delete_error)
                        .setPositiveButton(R.string.confirm, null)
                        .show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}