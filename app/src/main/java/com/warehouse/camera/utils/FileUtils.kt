package com.warehouse.camera.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private const val BASE_DIRECTORY = "warehouse"
    private const val TAG = "FileUtils"
    
    /**
     * Deletes a file or directory recursively and removes entries from MediaStore
     * @param file The file or directory to delete
     * @param context Optional context to update MediaStore for images
     * @return true if deletion was successful, false otherwise
     */
    fun deleteFileOrDirectory(file: File, context: Context? = null): Boolean {
        if (!file.exists()) {
            return false
        }
        
        // If it's a directory, delete all contents first
        if (file.isDirectory) {
            val children = file.listFiles() ?: return false
            
            // Delete all children recursively
            for (child in children) {
                deleteFileOrDirectory(child, context)
            }
        } else {
            // If it's an image file and context is provided, remove from MediaStore
            if (context != null && isImageFile(file)) {
                removeImageFromGallery(context, file)
            }
        }
        
        // Delete the file or empty directory
        return file.delete()
    }
    
    /**
     * Remove an image file from the MediaStore gallery
     * @param context The context to use for ContentResolver
     * @param file The image file to remove
     */
    private fun removeImageFromGallery(context: Context, file: File) {
        try {
            // Get the content resolver
            val contentResolver = context.contentResolver
            
            // Create a selection for the file path
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, we need to use relative path
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" // Only works for photos in standard directories
            } else {
                // For older versions, use the absolute path
                "${MediaStore.MediaColumns.DATA} = ?"
            }
            
            // Selection argument depends on Android version
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, extract the relative path
                val path = file.absolutePath
                val dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
                val relativePath = if (path.startsWith(dcimPath)) {
                    val pathAfterDcim = path.substring(dcimPath.length + 1)
                    "DCIM/${pathAfterDcim.substringBeforeLast('/')}"
                } else {
                    // If not in DCIM, use a wildcard
                    "%${file.parent?.substringAfterLast('/') ?: ""}%"
                }
                arrayOf(relativePath)
            } else {
                // For older versions, use the absolute path
                arrayOf(file.absolutePath)
            }
            
            // Try to delete from MediaStore
            val deletedRows = contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs
            )
            
            Log.d(TAG, "Removed $deletedRows entries from MediaStore gallery for: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing image from gallery", e)
        }
    }
    
    // Получить базовую директорию для хранения файлов приложения
    fun getBaseDirectory(context: Context): File {
        // Always use the public DCIM directory
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), BASE_DIRECTORY)
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            if (PermissionUtils.hasStoragePermission(context)) {
                val success = directory.mkdirs()
                Log.d(TAG, "Creating directory: ${directory.absolutePath}, success: $success")
                
                // Ensure directory exists after creation attempt
                if (!success && !directory.exists()) {
                    Log.e(TAG, "Failed to create directory despite having permissions")
                }
            } else {
                Log.e(TAG, "Storage permission denied, cannot create directory")
                // Request permissions if they're not granted
                PermissionUtils.requestStoragePermission(context)
            }
        }
        
        // Log directory path
        Log.d(TAG, "Base directory path: ${directory.absolutePath}")
        return directory
    }
    
    // Получить директорию для проекта приёмки
    fun getProjectDirectory(context: Context, reception: ProductReception): File? {
        try {
            val baseDir = getBaseDirectory(context)
            
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Log.e(TAG, "Failed to create base directory for project")
                return null
            }
            
            // Create manufacturer directory
            val manufacturerDir = File(baseDir, reception.manufacturerCode)
            if (!manufacturerDir.exists() && !manufacturerDir.mkdirs()) {
                Log.e(TAG, "Failed to create manufacturer directory")
                return null
            }
            
            // Create date directory
            val dateDir = File(manufacturerDir, reception.date)
            if (!dateDir.exists() && !dateDir.mkdirs()) {
                Log.e(TAG, "Failed to create date directory")
                return null
            }
            
            return dateDir
        } catch (e: Exception) {
            Log.e(TAG, "Error creating project directory", e)
            return null
        }
    }
    
    // Create directory structure based on manufacturer info and item data
    fun createDirectoryStructure(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        itemData: ItemData
    ): File? {
        try {
            val baseDir = getBaseDirectory(context)
            Log.d(TAG, "Using base directory: ${baseDir.absolutePath}")
            
            if (!baseDir.exists()) {
                val success = baseDir.mkdirs()
                Log.d(TAG, "Creating base directory, success: $success")
                
                if (!success && !baseDir.exists()) {
                    Log.e(TAG, "Failed to create base directory: ${baseDir.absolutePath}")
                    
                    // In case of failure, attempt to create a fallback directory within app's private storage
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    if (fallbackDir != null) {
                        Log.d(TAG, "Using fallback directory: ${fallbackDir.absolutePath}")
                        return createSubDirectories(fallbackDir, manufacturerInfo, itemData)
                    }
                    return null
                }
            }
            
            return createSubDirectories(baseDir, manufacturerInfo, itemData)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createDirectoryStructure", e)
            return null
        }
    }
    
    // Helper method to create subdirectories
    private fun createSubDirectories(baseDir: File, manufacturerInfo: ManufacturerInfo, itemData: ItemData): File? {
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
            
            // Create category directory
            val categoryDir = File(dateDir, itemData.defectCategory.toString())
            if (!ensureDirectoryExists(categoryDir)) {
                return null
            }
            
            // Create article directory
            val articleDir = File(categoryDir, itemData.fullArticleCode)
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
            
            // Double-check if directory exists after creation attempt
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
        return createImageFile(context, manufacturerInfo, articleInfo, itemData, isBoxPhoto, null)
    }
    
    // Overloaded version to support multiple photos
    fun createImageFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        itemData: ItemData,
        isBoxPhoto: Boolean,
        photoIndex: Int?
    ): File? {
        try {
            val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData)
            if (itemDir == null) {
                Log.e(TAG, "Failed to create directory structure")
                return null
            }
            
            val prefix = if (isBoxPhoto) "damage" else "barcode"
            
            // Create a unique filename based on whether this is a multiple photo or not
            val imageFileName = if (photoIndex != null) {
                "$prefix-${itemData.fullArticleCode}-$photoIndex.jpg"
            } else {
                "$prefix-${itemData.fullArticleCode}.jpg"
            }
            
            // Make sure parent directory exists and is writable
            if (!itemDir.exists()) {
                val success = itemDir.mkdirs()
                if (!success) {
                    Log.e(TAG, "Failed to create directory: ${itemDir.absolutePath}")
                    
                    // Fallback to app's private directory if public directory creation fails
                    val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val privateItemDir = File(privateDir, "${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${itemData.defectCategory}/${itemData.fullArticleCode}")
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
            
            // Create an empty file to ensure it exists
            val created = imageFile.createNewFile()
            if (!created) {
                Log.e(TAG, "Failed to create new image file: ${imageFile.absolutePath}")
                
                // Try to make parent directories again
                itemDir.mkdirs()
                
                // Try creating the file again
                if (!imageFile.createNewFile()) {
                    Log.e(TAG, "Second attempt to create image file failed")
                    return null
                }
            }
            
            Log.d(TAG, "Image file will be saved to: ${imageFile.absolutePath}")
            return imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating image file", e)
            return null
        }
    }
    
    // Save bitmap to file
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            Log.d(TAG, "Successfully saved bitmap to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            false
        }
    }
    
    // Save bitmap to file
    fun saveBitmapToFile(bitmap: Bitmap, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            
            // Ensure the parent directory exists
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            Log.d(TAG, "Successfully saved bitmap to $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to file", e)
            false
        }
    }
    
    // Save a copy of an image file to a new location
    fun copyImageFile(sourceFile: File, destFile: File): Boolean {
        return try {
            // Ensure the parent directory exists
            val parentDir = destFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            if (!destFile.exists()) {
                destFile.createNewFile()
            }
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                    }
                }
            }
            Log.d(TAG, "Successfully copied image to ${destFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying image file", e)
            false
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
    
    // Improved method to save text file with defect information - PROPER VERSION
    fun saveTextFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        defectDetails: DefectDetails,
        itemData: ItemData
    ): Boolean {
        return try {
            Log.d(TAG, "[SAVE] Starting to save text file for article: ${itemData.fullArticleCode}")
            Log.d(TAG, "[SAVE] Manufacturer: ${manufacturerInfo.manufacturerCode}, Date: ${manufacturerInfo.date}")
            Log.d(TAG, "[SAVE] Defect Category: ${itemData.defectCategory}")
            
            // Check storage permissions first
            if (!PermissionUtils.hasStoragePermission(context)) {
                Log.e(TAG, "[SAVE] No storage permission - requesting permissions")
                PermissionUtils.requestStoragePermission(context)
                return false
            }
            
            // Create the same directory structure as photos
            val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData)
            if (itemDir == null) {
                Log.e(TAG, "[SAVE] Failed to create directory structure")
                
                // Try fallback to app's private directory
                Log.d(TAG, "[SAVE] Trying fallback to app private directory")
                val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                if (privateDir != null) {
                    val fallbackDir = File(privateDir, "warehouse/${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${itemData.defectCategory}")
                    if (fallbackDir.mkdirs() || fallbackDir.exists()) {
                        val textFileName = "${itemData.fullArticleCode}.txt"
                        val textFile = File(fallbackDir, textFileName)
                        Log.d(TAG, "[SAVE] Using fallback directory: ${textFile.absolutePath}")
                        return saveTextContentToFile(textFile, itemData, defectDetails)
                    }
                }
                return false
            }
            
            // Create text file in the same directory as photos
            val textFileName = "${itemData.fullArticleCode}.txt"
            val textFile = File(itemDir, textFileName)
            
            Log.d(TAG, "[SAVE] Target file: ${textFile.absolutePath}")
            
            // If file exists, delete it first
            if (textFile.exists()) {
                textFile.delete()
                Log.d(TAG, "[SAVE] Deleted existing file")
            }
            
            val result = saveTextContentToFile(textFile, itemData, defectDetails)
            
            Log.d(TAG, "[SAVE] Final result: $result")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "[SAVE] Exception in saveTextFile", e)
            false
        }
    }
    
    // Ultra simple and reliable method to save text content to file
    private fun saveTextContentToFile(file: File, itemData: ItemData, defectDetails: DefectDetails): Boolean {
        return try {
            Log.d(TAG, "[CONTENT] Preparing content for file: ${file.absolutePath}")
            
            // Validate input data
            if (itemData.fullArticleCode.isBlank()) {
                Log.e(TAG, "[CONTENT] Cannot save - article code is blank")
                return false
            }
            
            // Create simple, clean text content
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val content = buildString {
                appendLine("WAREHOUSE DOCUMENTATION")
                appendLine("========================")
                appendLine("")
                appendLine("Article Code: ${itemData.fullArticleCode}")
                appendLine("Defect Category: ${itemData.defectCategory}")
                appendLine("Reason: ${defectDetails.reason}")
                appendLine("Template: ${defectDetails.template}")
                appendLine("Description: ${defectDetails.description}")
                appendLine("Created: $timestamp")
                appendLine("")
                appendLine("РУССКИЙ")
                appendLine("========")
                appendLine("Артикул: ${itemData.fullArticleCode}")
                appendLine("Категория: ${itemData.defectCategory}")
                appendLine("Причина: ${defectDetails.reason}")
                appendLine("Шаблон: ${defectDetails.template}")
                appendLine("Описание: ${defectDetails.description}")
                appendLine("Создано: $timestamp")
                appendLine("")
                appendLine("中文")
                appendLine("====")
                appendLine("物品编号: ${itemData.fullArticleCode}")
                appendLine("缺陷类别: ${itemData.defectCategory}")
                appendLine("原因: ${defectDetails.reason}")
                appendLine("模板: ${defectDetails.template}")
                appendLine("描述: ${defectDetails.description}")
                appendLine("创建: $timestamp")
            }
            
            Log.d(TAG, "[CONTENT] Content prepared, length: ${content.length} characters")
            
            // Try multiple approaches to write the file
            var success = false
            
            // Method 1: Use writeText (most reliable)
            try {
                file.writeText(content, Charsets.UTF_8)
                success = true
                Log.d(TAG, "[CONTENT] Method 1 (writeText) successful")
            } catch (e: Exception) {
                Log.w(TAG, "[CONTENT] Method 1 (writeText) failed", e)
            }
            
            // Method 2: Use FileOutputStream if Method 1 failed
            if (!success) {
                try {
                    FileOutputStream(file).use { fos ->
                        fos.write(content.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    success = true
                    Log.d(TAG, "[CONTENT] Method 2 (FileOutputStream) successful")
                } catch (e: Exception) {
                    Log.w(TAG, "[CONTENT] Method 2 (FileOutputStream) failed", e)
                }
            }
            
            // Method 3: Use FileWriter if both above failed  
            if (!success) {
                try {
                    java.io.FileWriter(file, Charsets.UTF_8).use { writer ->
                        writer.write(content)
                        writer.flush()
                    }
                    success = true
                    Log.d(TAG, "[CONTENT] Method 3 (FileWriter) successful")
                } catch (e: Exception) {
                    Log.w(TAG, "[CONTENT] Method 3 (FileWriter) failed", e)
                }
            }
            
            // Verify the file was created successfully
            if (success && file.exists() && file.length() > 0) {
                Log.d(TAG, "[CONTENT] File verification successful: ${file.absolutePath}, size: ${file.length()} bytes")
                
                // Try to read back first few characters to ensure file is readable
                try {
                    val firstChars = file.readText(Charsets.UTF_8).take(50)
                    Log.d(TAG, "[CONTENT] Read verification: '$firstChars'")
                } catch (e: Exception) {
                    Log.w(TAG, "[CONTENT] Read verification failed, but file exists", e)
                }
                
                return true
            } else {
                Log.e(TAG, "[CONTENT] File verification failed - success: $success, exists: ${file.exists()}, size: ${if(file.exists()) file.length() else "N/A"}")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "[CONTENT] Critical error in saveTextContentToFile", e)
            false
        }
    }
    
    // Check if the file structure for an item exists
    fun checkItemFilesExist(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        itemData: ItemData
    ): Boolean {
        val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData) ?: return false
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
    fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
    }
    
    /**
     * Creates or updates a summary file for a manufacturer folder
     * The file will list all articles found in date subfolders
     * 
     * @param manufacturerFolder The manufacturer folder
     * @return True if successful, false otherwise
     */
    fun createManufacturerSummaryFile(manufacturerFolder: File): Boolean {
        if (!manufacturerFolder.exists() || !manufacturerFolder.isDirectory) {
            Log.e(TAG, "Invalid manufacturer folder: ${manufacturerFolder.absolutePath}")
            return false
        }
        
        try {
            // Get all date folders
            val dateFolders = manufacturerFolder.listFiles { file -> 
                file.isDirectory && isDateFormat(file.name)
            } ?: return false
            
            // Collect all unique article codes
            val articleCodes = mutableSetOf<String>()
            
            for (dateFolder in dateFolders) {
                // Get category folders
                val categoryFolders = dateFolder.listFiles { file -> file.isDirectory } ?: continue
                
                for (categoryFolder in categoryFolders) {
                    // Get article folders
                    val articleFolders = categoryFolder.listFiles { file -> file.isDirectory } ?: continue
                    
                    // Add each article code to the set
                    for (articleFolder in articleFolders) {
                        articleCodes.add(articleFolder.name)
                    }
                }
            }
            
            // Create or update the summary file
            val summaryFileName = "${manufacturerFolder.name}main.txt"
            val summaryFile = File(manufacturerFolder, summaryFileName)
            
            FileOutputStream(summaryFile).use { output ->
                // Write header with total count
                val header = "общее количество ${articleCodes.size}\n\n"
                output.write(header.toByteArray())
                
                // Write each article code on a new line
                for (articleCode in articleCodes.sorted()) {
                    output.write("${articleCode}\n".toByteArray())
                }
            }
            
            Log.d(TAG, "Created manufacturer summary file: ${summaryFile.absolutePath}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating manufacturer summary file", e)
            return false
        }
    }
    
    /**
     * Checks if a string is in date format dd-MM-yyyy
     */
    fun isDateFormat(name: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            dateFormat.isLenient = false
            dateFormat.parse(name)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the expected storage location information for the user
     */
    fun getStorageLocationInfo(context: Context): String {
        val baseDir = getBaseDirectory(context)
        return if (baseDir.exists()) {
            "Файлы сохраняются в:\n${baseDir.absolutePath}\n\nДля просмотра используйте:\n• Встроенный файловый менеджер приложения\n• Любой файловый менеджер Android\n• Подключите устройство к компьютеру"
        } else {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            "Основная папка недоступна.\nФайлы сохраняются в приватную папку:\n${fallbackDir?.absolutePath}/warehouse\n\nДля просмотра используйте встроенный файловый менеджер приложения."
        }
    }
    
    /**
     * Check if text file exists for the given item
     */
    fun getTextFileForItem(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        itemData: ItemData
    ): File? {
        val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData) ?: return null
        val textFile = File(itemDir, "${itemData.fullArticleCode}.txt")
        return if (textFile.exists()) textFile else null
    }
}