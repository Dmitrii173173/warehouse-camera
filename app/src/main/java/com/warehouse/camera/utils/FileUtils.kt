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
    
    // Improved method to save text file with defect information
    fun saveTextFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        defectDetails: DefectDetails,
        itemData: ItemData
    ): Boolean {
        try {
            val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData)
            if (itemDir == null) {
                Log.e(TAG, "Failed to create directory structure for text file")
                
                // Fallback to app's private directory
                val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val privateItemDir = File(privateDir, "${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${itemData.defectCategory}/${itemData.fullArticleCode}")
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
            // Ensure the parent directory exists
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }
            
            // Определяем исходный язык текста
            val sourceLanguage = TranslationUtils.detectLanguage(defectDetails.description)
            
            // Переводим термины на все языки
            val translatedReasons = TranslationUtils.getAvailableLanguages().associateWith { lang ->
                TranslationUtils.translateReason(defectDetails.reason, lang)
            }
            
            val translatedTemplates = TranslationUtils.getAvailableLanguages().associateWith { lang ->
                TranslationUtils.translateTemplate(defectDetails.template, lang)
            }
            
            FileOutputStream(file).use { outputStream ->
                // Russian content
                val russianContent = """
                    Артикул: ${itemData.fullArticleCode}
                    Категория дефекта: ${itemData.defectCategory}
                    Причина: ${translatedReasons["ru"] ?: defectDetails.reason}
                    Шаблон: ${translatedTemplates["ru"] ?: defectDetails.template}
                    Описание: ${defectDetails.description}
                """.trimIndent()
                
                // Chinese content
                val chineseContent = """
                    
                    物品编号: ${itemData.fullArticleCode}
                    缺陷类别: ${itemData.defectCategory}
                    原因 (Reason): ${translatedReasons["zh"] ?: defectDetails.reason}
                    模板 (Template): ${translatedTemplates["zh"] ?: defectDetails.template}
                    描述: ${defectDetails.description}
                """.trimIndent()
                
                // English content
                val englishContent = """
                    
                    Article: ${itemData.fullArticleCode}
                    Defect Category: ${itemData.defectCategory}
                    Reason (原因): ${translatedReasons["en"] ?: defectDetails.reason}
                    Template (模板): ${translatedTemplates["en"] ?: defectDetails.template}
                    Description: ${defectDetails.description}
                """.trimIndent()
                
                // Combine all languages
                val fullContent = "$russianContent\n\n$chineseContent\n\n$englishContent"
                
                outputStream.write(fullContent.toByteArray())
                outputStream.flush()
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
}