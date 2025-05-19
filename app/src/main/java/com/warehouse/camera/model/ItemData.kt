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
    var defectCategory: Int = 1 // Default to category 1 (green)
) : Parcelable {
    val fullArticleCode: String
        get() = "$articleCode-$index"
}
