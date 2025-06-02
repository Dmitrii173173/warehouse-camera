package com.warehouse.camera.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private const val BASE_DIRECTORY = "warehouse"
    private const val TAG = "FileUtils"
    
    // Кэш для лучшего пути сохранения файлов
    private var cachedBestPath: String? = null
    private var diagnostic: FileSystemDiagnostic? = null
    
    /**
     * Инициализация FileUtils с диагностикой файловой системы
     */
    fun initialize(context: Context) {
        diagnostic = FileSystemDiagnostic(context)
        val diagResult = diagnostic!!.runFullDiagnostic()
        cachedBestPath = diagnostic!!.getBestSavePath()
        
        Log.d(TAG, "FileUtils initialized with best path: $cachedBestPath")
        diagResult.recommendations.forEach { recommendation ->
            Log.i(TAG, "Recommendation: $recommendation")
        }
    }
    
    /**
     * Получить базовую директорию для хранения файлов приложения с учетом совместимости
     */
    fun getBaseDirectory(context: Context): File {
        // Если диагностика не была запущена, запустить её
        if (diagnostic == null) {
            initialize(context)
        }
        
        val bestPath = cachedBestPath ?: run {
            // Fallback на приватную папку приложения
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + "/warehouse"
        }
        
        val directory = File(bestPath)
        
        // Попытка создать директорию
        if (!directory.exists()) {
            val success = directory.mkdirs()
            Log.d(TAG, "Creating base directory: ${directory.absolutePath}, success: $success")
            
            if (!success && !directory.exists()) {
                Log.e(TAG, "Failed to create preferred directory, using fallback")
                // Fallback на гарантированно доступную приватную папку
                val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), BASE_DIRECTORY)
                fallbackDir.mkdirs()
                return fallbackDir
            }
        }
        
        Log.d(TAG, "Using base directory: ${directory.absolutePath}")
        return directory
    }
    
    /**
     * Улучшенный метод сохранения текстового файла с множественными fallback опциями
     */
    fun saveTextFile(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        articleInfo: ArticleInfo,
        defectDetails: DefectDetails,
        itemData: ItemData
    ): Boolean {
        return try {
            Log.i(TAG, "=== SAVING TEXT FILE ===")
            Log.d(TAG, "Article: ${itemData.fullArticleCode}")
            Log.d(TAG, "Manufacturer: ${manufacturerInfo.manufacturerCode}")
            Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            
            // Проверить разрешения
            if (!PermissionUtils.hasStoragePermission(context)) {
                Log.e(TAG, "❌ No storage permission")
                return false
            }
            
            // Подготовить контент файла
            val content = prepareTextContent(itemData, defectDetails)
            val fileName = "${itemData.fullArticleCode}.txt"
            
            // Попробовать несколько методов сохранения по приоритету
            val saveMethods = listOf(
                { saveToStructuredDirectory(context, manufacturerInfo, itemData, fileName, content) },
                { saveToPrivateDirectory(context, manufacturerInfo, itemData, fileName, content) },
                { saveToMediaStore(context, fileName, content) },
                { saveToDownloads(context, fileName, content) }
            )
            
            for ((index, saveMethod) in saveMethods.withIndex()) {
                try {
                    val result = saveMethod()
                    if (result) {
                        Log.i(TAG, "✅ Successfully saved using method ${index + 1}")
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Save method ${index + 1} failed: ${e.message}")
                }
            }
            
            Log.e(TAG, "❌ All save methods failed")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in saveTextFile", e)
            false
        }
    }
    
    /**
     * Метод 1: Сохранение в структурированную директорию (как раньше)
     */
    private fun saveToStructuredDirectory(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        itemData: ItemData,
        fileName: String,
        content: String
    ): Boolean {
        val itemDir = createDirectoryStructure(context, manufacturerInfo, itemData) ?: return false
        val textFile = File(itemDir, fileName)
        
        return saveContentToFile(textFile, content, "Structured Directory")
    }
    
    /**
     * Метод 2: Сохранение в приватную папку приложения (гарантированно работает)
     */
    private fun saveToPrivateDirectory(
        context: Context,
        manufacturerInfo: ManufacturerInfo,
        itemData: ItemData,
        fileName: String,
        content: String
    ): Boolean {
        val privateDir = context.getExternalFilesDir(null) ?: context.filesDir
        val warehouseDir = File(privateDir, "warehouse/${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${itemData.defectCategory}")
        
        if (!warehouseDir.exists() && !warehouseDir.mkdirs()) {
            return false
        }
        
        val textFile = File(warehouseDir, fileName)
        return saveContentToFile(textFile, content, "Private Directory")
    }
    
    /**
     * Метод 3: Сохранение через MediaStore (для Android 10+)
     */
    private fun saveToMediaStore(context: Context, fileName: String, content: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/warehouse")
            }
            
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                }
                Log.d(TAG, "✅ Saved via MediaStore: $uri")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore save failed", e)
            false
        }
    }
    
    /**
     * Метод 4: Сохранение в папку Downloads (последний резерв)
     */
    private fun saveToDownloads(context: Context, fileName: String, content: String): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val warehouseDir = File(downloadsDir, "warehouse")
            
            if (!warehouseDir.exists() && !warehouseDir.mkdirs()) {
                return false
            }
            
            val textFile = File(warehouseDir, fileName)
            saveContentToFile(textFile, content, "Downloads Directory")
        } catch (e: Exception) {
            Log.w(TAG, "Downloads save failed", e)
            false
        }
    }
    
    /**
     * Универсальный метод сохранения контента в файл
     */
    private fun saveContentToFile(file: File, content: String, method: String): Boolean {
        return try {
            // Убедиться что папка существует
            file.parentFile?.mkdirs()
            
            // Попробовать различные способы записи
            var success = false
            
            // Способ 1: writeText
            try {
                file.writeText(content, Charsets.UTF_8)
                success = true
            } catch (e: Exception) {
                Log.w(TAG, "$method - writeText failed", e)
            }
            
            // Способ 2: FileOutputStream
            if (!success) {
                try {
                    FileOutputStream(file).use { fos ->
                        fos.write(content.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    success = true
                } catch (e: Exception) {
                    Log.w(TAG, "$method - FileOutputStream failed", e)
                }
            }
            
            // Проверка успеха
            if (success && file.exists() && file.length() > 0) {
                Log.d(TAG, "✅ $method successful: ${file.absolutePath} (${file.length()} bytes)")
                return true
            } else {
                Log.w(TAG, "❌ $method verification failed")
                return false
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "$method critical error", e)
            false
        }
    }
    
    /**
     * Подготовка контента текстового файла
     */
    private fun prepareTextContent(itemData: ItemData, defectDetails: DefectDetails): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        return buildString {
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
            appendLine("创建时间: $timestamp")
        }
    }
    
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
            val contentResolver = context.contentResolver
            
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            } else {
                "${MediaStore.MediaColumns.DATA} = ?"
            }
            
            val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val path = file.absolutePath
                val dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
                val relativePath = if (path.startsWith(dcimPath)) {
                    val pathAfterDcim = path.substring(dcimPath.length + 1)
                    "DCIM/${pathAfterDcim.substringBeforeLast('/')}"
                } else {
                    "%${file.parent?.substringAfterLast('/') ?: ""}%"
                }
                arrayOf(relativePath)
            } else {
                arrayOf(file.absolutePath)
            }
            
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
                    
                    // Fallback to private directory
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
            val imageFileName = if (photoIndex != null) {
                "$prefix-${itemData.fullArticleCode}-$photoIndex.jpg"
            } else {
                "$prefix-${itemData.fullArticleCode}.jpg"
            }
            
            if (!itemDir.exists()) {
                val success = itemDir.mkdirs()
                if (!success) {
                    Log.e(TAG, "Failed to create directory: ${itemDir.absolutePath}")
                    
                    // Fallback to app's private directory
                    val privateDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val privateItemDir = File(privateDir, "${manufacturerInfo.manufacturerCode}/${manufacturerInfo.date}/${itemData.defectCategory}/${itemData.fullArticleCode}")
                    privateItemDir.mkdirs()
                    val privateFile = File(privateItemDir, imageFileName)
                    Log.d(TAG, "Using private directory instead: ${privateFile.absolutePath}")
                    return privateFile
                }
            }
            
            val imageFile = File(itemDir, imageFileName)
            
            if (imageFile.exists()) {
                imageFile.delete()
            }
            
            val created = imageFile.createNewFile()
            if (!created) {
                Log.e(TAG, "Failed to create new image file: ${imageFile.absolutePath}")
                
                itemDir.mkdirs()
                
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
     */
    fun createManufacturerSummaryFile(manufacturerFolder: File): Boolean {
        if (!manufacturerFolder.exists() || !manufacturerFolder.isDirectory) {
            Log.e(TAG, "Invalid manufacturer folder: ${manufacturerFolder.absolutePath}")
            return false
        }
        
        try {
            val dateFolders = manufacturerFolder.listFiles { file -> 
                file.isDirectory && isDateFormat(file.name)
            } ?: return false
            
            val articleCodes = mutableSetOf<String>()
            
            for (dateFolder in dateFolders) {
                val categoryFolders = dateFolder.listFiles { file -> file.isDirectory } ?: continue
                
                for (categoryFolder in categoryFolders) {
                    val articleFolders = categoryFolder.listFiles { file -> file.isDirectory } ?: continue
                    
                    for (articleFolder in articleFolders) {
                        articleCodes.add(articleFolder.name)
                    }
                }
            }
            
            val summaryFileName = "${manufacturerFolder.name}main.txt"
            val summaryFile = File(manufacturerFolder, summaryFileName)
            
            FileOutputStream(summaryFile).use { output ->
                val header = "общее количество ${articleCodes.size}\n\n"
                output.write(header.toByteArray())
                
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
    
    /**
     * Запустить диагностику файловой системы и вывести результаты в лог
     */
    fun runDiagnostics(context: Context) {
        val diagnostic = FileSystemDiagnostic(context)
        val result = diagnostic.runFullDiagnostic()
        
        Log.i(TAG, "=== ДИАГНОСТИКА ФАЙЛОВОЙ СИСТЕМЫ ===")
        Log.i(TAG, "Устройство: ${result.deviceInfo}")
        Log.i(TAG, "Разрешения: ${result.permissions}")
        
        result.availablePaths.forEach { pathInfo ->
            Log.i(TAG, "Путь: ${pathInfo}")
        }
        
        result.writeTests.forEach { testResult ->
            Log.i(TAG, "Тест записи: ${testResult}")
        }
        
        result.recommendations.forEach { recommendation ->
            Log.i(TAG, "Рекомендация: $recommendation")
        }
    }
}
