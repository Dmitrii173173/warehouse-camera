package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Date

@Parcelize
data class GalleryItem(
    val file: File,
    val name: String,
    val date: Date,
    val type: ItemType,
    val articleCode: String,
    val path: String
) : Parcelable {
    
    enum class ItemType {
        BOX_PHOTO, PRODUCT_PHOTO, TEXT_FILE, UNKNOWN
    }
    
    val isImage: Boolean
        get() = type == ItemType.BOX_PHOTO || type == ItemType.PRODUCT_PHOTO
    
    val isText: Boolean
        get() = type == ItemType.TEXT_FILE
}
