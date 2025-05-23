package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemData(
    val articleCode: String,
    val index: Int,
    var boxPhotoPath: String? = null,
    var productPhotoPath: String? = null,
    var boxPhotoMarkedPath: String? = null,
    var productPhotoMarkedPath: String? = null,
    var isCompleted: Boolean = false,
    var defectCategory: Int = 1, // Default to category 1 (green)
    var boxPhotoPaths: ArrayList<String> = ArrayList(),
    var boxPhotoMarkedPaths: ArrayList<String> = ArrayList(),
    var productPhotoPaths: ArrayList<String> = ArrayList(),
    var productPhotoMarkedPaths: ArrayList<String> = ArrayList()
) : Parcelable {
    val fullArticleCode: String
        get() = "$articleCode-$index"
    
    // Helper methods for backward compatibility
    fun addBoxPhoto(path: String, markedPath: String) {
        // Add to single path for backward compatibility
        if (boxPhotoPath == null) {
            boxPhotoPath = path
            boxPhotoMarkedPath = markedPath
        }
        
        // Add to the lists
        boxPhotoPaths.add(path)
        boxPhotoMarkedPaths.add(markedPath)
    }
    
    fun addProductPhoto(path: String, markedPath: String) {
        // Add to single path for backward compatibility
        if (productPhotoPath == null) {
            productPhotoPath = path
            productPhotoMarkedPath = markedPath
        }
        
        // Add to the lists
        productPhotoPaths.add(path)
        productPhotoMarkedPaths.add(markedPath)
    }
    
    // Get photo count
    fun getBoxPhotoCount(): Int = boxPhotoPaths.size
    fun getProductPhotoCount(): Int = productPhotoPaths.size
}
