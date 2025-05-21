package com.warehouse.camera.ui

import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.warehouse.camera.R
import com.warehouse.camera.model.project.ProductReceptionRepository
import com.warehouse.camera.ui.adapter.FileStructureAdapter
import com.warehouse.camera.utils.FileUtils
import java.io.File

class FileStructureActivity : AppCompatActivity() {

    private lateinit var tvCurrentPath: TextView
    private lateinit var lstFiles: ListView
    private lateinit var adapter: FileStructureAdapter
    
    private var currentDirectory: File? = null
    private var rootDirectory: File? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_structure)
        
        // Инициализация View
        tvCurrentPath = findViewById(R.id.tv_current_path)
        lstFiles = findViewById(R.id.lst_files)
        
        // Получаем базовую директорию
        rootDirectory = FileUtils.getBaseDirectory(this)
        currentDirectory = rootDirectory
        
        // Настраиваем адаптер
        adapter = FileStructureAdapter(
            this, 
            mutableListOf(), 
            { // Обработчик удаления файла
                onFileDeleted()
            },
            { file -> // Обработчик нажатия на элемент
                if (file.isDirectory) {
                    // Если это директория - открываем её
                    currentDirectory = file
                    updateDirectoryView()
                } else {
                    // Если это файл - показываем его содержимое
                    showFileDetails(file)
                }
            }
        )
        lstFiles.adapter = adapter
        
        // Обновляем текущую директорию
        updateDirectoryView()
        
        // Отключаем стандартный обработчик нажатий - мы используем свой в адаптере
        lstFiles.setOnItemClickListener(null)
        
        // Настраиваем кнопку "Назад"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun updateDirectoryView() {
        // Обновляем отображение текущего пути
        tvCurrentPath.text = currentDirectory?.absolutePath ?: "/"
        
        // Check if we're in a manufacturer code folder
        if (isManufacturerFolder(currentDirectory)) {
            // Create/update the summary file
            currentDirectory?.let { directory ->
                FileUtils.createManufacturerSummaryFile(directory)
            }
        }
        
        // Обновляем список файлов
        val files = currentDirectory?.listFiles()?.sortedWith(
            compareBy({ !it.isDirectory }, { it.name.lowercase() })
        ) ?: emptyList()
        
        adapter.clear()
        adapter.addAll(files)
        adapter.notifyDataSetChanged()
    }
    
    /**
     * Checks if the current directory is a manufacturer folder
     * A manufacturer folder is a direct child of the warehouse folder and contains date folders
     */
    private fun isManufacturerFolder(directory: File?): Boolean {
        if (directory == null || !directory.isDirectory) return false
        
        // Check if parent is the warehouse folder (rootDirectory)
        if (directory.parent != rootDirectory?.absolutePath) return false
        
        // Check if the directory contains at least one date-formatted subfolder
        val dateFormatRegex = Regex("\\d{2}-\\d{2}-\\d{4}")
        val hasDateFolder = directory.listFiles()?.any { 
            it.isDirectory && dateFormatRegex.matches(it.name)
        } ?: false
        
        return hasDateFolder
    }
    
    private fun showFileDetails(file: File) {
        // Проверяем, является ли файл текстовым
        if (isTextFile(file)) {
            // Читаем содержимое текстового файла
            try {
                val content = file.readText()
                
                // Показываем диалог с содержимым файла
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(file.name)
                    .setMessage(content)
                    .setPositiveButton(R.string.close, null)
                    .show()
            } catch (e: Exception) {
                // В случае ошибки показываем сообщение об ошибке
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.error_loading_details)
                    .setMessage(e.message)
                    .setPositiveButton(R.string.close, null)
                    .show()
            }
        } else if (isImageFile(file)) {
            // Здесь можно добавить логику просмотра изображений
            // Например, открыть ImageViewerActivity
            android.widget.Toast.makeText(this, R.string.image_viewer_description, android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // Для других типов файлов показываем информацию о файле
            val fileInfo = """
                ${getString(R.string.file_info, formatFileSize(file.length()), formatDate(file.lastModified()))}
                ${getString(R.string.path)}: ${file.absolutePath}
            """.trimIndent()
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(file.name)
                .setMessage(fileInfo)
                .setPositiveButton(R.string.close, null)
                .show()
        }
    }
    
    /**
     * Проверяет, является ли файл текстовым
     */
    private fun isTextFile(file: File): Boolean {
        val name = file.name.lowercase(java.util.Locale.getDefault())
        return name.endsWith(".txt") || name.endsWith(".json") || 
               name.endsWith(".xml") || name.endsWith(".csv")
    }
    
    /**
     * Проверяет, является ли файл изображением
     */
    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(java.util.Locale.getDefault())
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
    }
    
    /**
     * Форматирует размер файла в читаемый вид
     */
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
     * Форматирует дату в читаемый вид
     */
    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(date)
    }
    
    /**
     * Called when a file is deleted
     * Updates the view and refreshes the manufacturer summary if needed
     * Also synchronizes the receptions list with the file system
     */
    private fun onFileDeleted() {
        // Find the manufacturer folder if we're inside a date or category folder
        val manufacturerFolder = findParentManufacturerFolder(currentDirectory)
        
        // Update the manufacturer summary file if found
        manufacturerFolder?.let {
            FileUtils.createManufacturerSummaryFile(it)
        }
        
        // Synchronize the receptions with the file system
        val receptionRepository = ProductReceptionRepository(this)
        receptionRepository.synchronizeWithFileSystem()
        
        // Refresh the view
        updateDirectoryView()
    }
    
    /**
     * Finds the parent manufacturer folder by traversing up the directory structure
     */
    private fun findParentManufacturerFolder(directory: File?): File? {
        var current = directory
        
        // Traverse up at most 3 levels (article -> category -> date -> manufacturer)
        for (i in 0 until 3) {
            if (current == null) return null
            
            // Check if this is a manufacturer folder
            if (isManufacturerFolder(current)) {
                return current
            }
            
            // Check if we've reached the root directory
            if (current.absolutePath == rootDirectory?.absolutePath) {
                return null
            }
            
            // Move up one level
            current = current.parentFile
        }
        
        return null
    }
    
    override fun onSupportNavigateUp(): Boolean {
        // Если мы находимся в корневой директории, то закрываем активность
        if (currentDirectory?.absolutePath == rootDirectory?.absolutePath) {
            finish()
            return true
        }
        
        // Иначе переходим в родительскую директорию
        currentDirectory = currentDirectory?.parentFile
        updateDirectoryView()
        return true
    }
    
    override fun onBackPressed() {
        // Аналогично кнопке "Назад" в ActionBar
        if (currentDirectory?.absolutePath == rootDirectory?.absolutePath) {
            super.onBackPressed()
        } else {
            currentDirectory = currentDirectory?.parentFile
            updateDirectoryView()
        }
    }
}