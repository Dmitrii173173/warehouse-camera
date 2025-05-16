package com.warehouse.camera.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.DefectDetails
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.model.project.ProductReception
import com.warehouse.camera.utils.PermissionUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private const val BASE_DIRECTORY = "DCIM/warehouse"
    private const val TAG = "FileUtils"
    
    // Получить базовую директорию для хранения файлов приложения
    fun getBaseDirectory(context: Context): File {
        // Для Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Используем MediaStore API для работы с DCIM
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "warehouse")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/warehouse")
            }
            
            try {
                // Создаем базовую директорию через MediaStore
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    // Путь создан через MediaStore
                    Log.d(TAG, "Created directory via MediaStore: DCIM/warehouse")
                    return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "warehouse")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating directory via MediaStore", e)
                // Продолжаем с альтернативным методом
            }
        }
        
        // Для Android 9 и ниже (или если MediaStore не сработал)
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "warehouse")
        if (!directory.exists()) {
            // Явно запрашиваем разрешение перед созданием
            if (PermissionUtils.hasStoragePermission(context)) {
                val success = directory.mkdirs()
                Log.d(TAG, "Creating directory using mkdirs: ${directory.absolutePath}, success: $success")
            } else {
                Log.e(TAG, "Storage permission denied")
            }
        }
        return directory
    }
    
    // Получить директорию для проекта приёмки
    fun getProjectDirectory(context: Context, reception: ProductReception): File? {
        try {
            val baseDir = getBaseDirectory(context)
            
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                return null
            }
            
            // Create manufacturer directory
            val manufacturerDir = File(baseDir, reception.manufacturerCode)
            if (!manufacturerDir.exists() && !manufacturerDir.mkdirs()) {
                return null
            }
            
            // Create date directory
            val dateDir = File(manufacturerDir, reception.date)
            if (!dateDir.exists() && !dateDir.mkdirs()) {
                return null
            }
            
            return dateDir
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    // Create directory structure based on manufacturer info and item data
    fun createDirectoryStructure(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        itemData: ItemData,
        defectCategory: Int
    ): File? {
        try {
            val baseDir = getBaseDirectory(context)
            Log.d(TAG, "Using base directory: ${baseDir.absolutePath}")
            
            if (!baseDir.exists()) {
                val success = baseDir.mkdirs()
                Log.d(TAG, "Creating base directory, success: $success")
                if (!success && !baseDir.exists()) {
                    Log.e(TAG, "Failed to create base directory: ${baseDir.absolutePath}")
                    // In case of failure, attempt to create a fallback directory
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    if (fallbackDir != null) {
                        Log.d(TAG, "Using fallback directory: ${fallbackDir.absolutePath}")
                        return createSubDirectories(fallbackDir, manufacturerInfo, itemData.fullArticleCode, defectCategory)
                    }
                    return null
                }
            }
            
            return createSubDirectories(baseDir, manufacturerInfo, itemData.fullArticleCode, defectCategory)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createDirectoryStructure", e)
            return null
        }
    }
    
    // Helper method to create subdirectories
    private fun createSubDirectories(baseDir: File, manufacturerInfo: ManufacturerInfo, articleCode: String, defectCategory: Int): File? {
        try {
            // Create manufacturer directory
            val manufacturerDir = File(baseDir, manufacturerInfo.manufacturerCode)
            if (!ensureDirectoryExists(manufacturerDir)) {
                return null
            }
            
            // Create date directory
            val dateDir = File(manufacturerDir, manufacturerInfo.date)
            if (!ensureDirectoryExists(dateDir)) {
                return null
            }
            
            // Create defect category directory
            val categoryDir = File(dateDir, defectCategory.toString())
            if (!ensureDirectoryExists(categoryDir)) {
                return null
            }
            
            // Create article directory
            val articleDir = File(categoryDir, articleCode)
            if (!ensureDirectoryExists(articleDir)) {
                return null
            }
            
            return articleDir
        } catch (e: Exception) {
            Log.e(TAG, "Error creating subdirectories", e)
            return null
        }
    }
    
    // Helper method to ensure a directory exists
    private fun ensureDirectoryExists(directory: File): Boolean {
        if (!directory.exists()) {
            val success = directory.mkdirs()
            Log.d(TAG, "Creating directory: ${directory.absolutePath}, success: $success")
            if (!success && !directory.exists()) {
                Log.e(TAG, "Failed to create directory: ${directory.absolutePath}")
                return false
            }
        }
        return true
    }
    
    // Improved version of createImageFile
    fun createImageFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        itemData: ItemData,
        isBoxPhoto: Boolean
    ): File? {
        try {
            val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData, articleInfo.defectCategory)
            if (itemDir == null) {
                Log.e(TAG, "Failed to create directory structure")
                return null
            }
            
            val prefix = if (isBoxPhoto) "box" else "product"
            val imageFileName = "$prefix-${itemData.fullArticleCode}.jpg"
            
            // Make sure parent directory exists and is writable
            if (!itemDir.exists()) {
                val success = itemDir.mkdirs()
                if (!success) {
                    Log.e(TAG, "Failed to create directory: ${itemDir.absolutePath}")
                    // Fallback to app's private directory if public directory creation fails
                    val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val privateItemDir = File(privateDir, "${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${articleInfo.defectCategory}/${itemData.fullArticleCode}")
                    privateItemDir.mkdirs()
                    val privateFile = File(privateItemDir, imageFileName)
                    Log.d(TAG, "Using private directory instead: ${privateFile.absolutePath}")
                    return privateFile
                }
            }
            
            val imageFile = File(itemDir, imageFileName)
            
            // If file already exists, delete it
            if (imageFile.exists()) {
                imageFile.delete()
            }
            
            Log.d(TAG, "Image file will be saved to: ${imageFile.absolutePath}")
            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file", e)
            return null
        }
    }
    
    // Get URI for file using FileProvider
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }
    
    // Improved method to save text file with defect information
    fun saveTextFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        defectDetails: DefectDetails,
        itemData: ItemData
    ): Boolean {
        try {
            val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData, articleInfo.defectCategory)
            if (itemDir == null) {
                Log.e(TAG, "Failed to create directory structure for text file")
                // Fallback to app's private directory
                val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val privateItemDir = File(privateDir, "${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${articleInfo.defectCategory}/${itemData.fullArticleCode}")
                if (!privateItemDir.exists()) {
                    privateItemDir.mkdirs()
                }
                
                val textFileName = "${itemData.fullArticleCode}.txt"
                val textFile = File(privateItemDir, textFileName)
                
                return saveTextToFile(textFile, itemData, defectDetails)
            }
            
            // Make sure directory exists
            if (!itemDir.exists()) {
                val success = itemDir.mkdirs()
                if (!success) {
                    Log.e(TAG, "Failed to create directory for text file: ${itemDir.absolutePath}")
                    return false
                }
            }
            
            val textFileName = "${itemData.fullArticleCode}.txt"
            val textFile = File(itemDir, textFileName)
            
            return saveTextToFile(textFile, itemData, defectDetails)
            
        } catch (e: IOException) {
            Log.e(TAG, "Error saving text file", e)
            return false
        }
    }
    
    // Helper method to save text content to file with multilingual support
    private fun saveTextToFile(file: File, itemData: ItemData, defectDetails: DefectDetails): Boolean {
        try {
            FileOutputStream(file).use { outputStream ->
                // Russian content
                val russianContent = """
                    Артикул: ${itemData.fullArticleCode}
                    Причина: ${defectDetails.reason}
                    Шаблон: ${defectDetails.template}
                    Описание: ${defectDetails.description}
                """.trimIndent()
                
                // Chinese content
                val chineseContent = """
                    
                    物品编号: ${itemData.fullArticleCode}
                    原因: ${defectDetails.reason}
                    模板: ${defectDetails.template}
                    描述: ${defectDetails.description}
                """.trimIndent()
                
                // English content
                val englishContent = """
                    
                    Article: ${itemData.fullArticleCode}
                    Reason: ${defectDetails.reason}
                    Template: ${defectDetails.template}
                    Description: ${defectDetails.description}
                """.trimIndent()
                
                // Combine all languages
                val fullContent = "$russianContent\n\n$chineseContent\n\n$englishContent"
                
                outputStream.write(fullContent.toByteArray())
            }
            Log.d(TAG, "Text file saved successfully: ${file.absolutePath}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "Error writing text to file: ${file.absolutePath}", e)
            return false
        }
    }
    
    // Check if the file structure for an item exists
    fun checkItemFilesExist(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        itemData: ItemData
    ): Boolean {
        val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData, articleInfo.defectCategory) ?: return false
        val textFile = File(itemDir, "${itemData.fullArticleCode}.txt")
        return textFile.exists()
    }
    
    // Format date to required format
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }
    
    // Scan a directory for images and get a list of file paths
    fun scanDirectoryForImages(directory: File): List<File> {
        val result = mutableListOf<File>()
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    result.addAll(scanDirectoryForImages(file))
                } else if (isImageFile(file)) {
                    result.add(file)
                }
            }
        }
        return result
    }
    
    // Check if file is an image
    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
    }
}