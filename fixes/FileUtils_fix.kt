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
                    "%${file.parent?.substringAfterLast('/') ?: ''}%"
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
    
    // Check if file is an image
    fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp")
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
