package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemData(
    val articleCode: String,
    val index: Int,
    var boxPhotoPath: String? = null,
    var productPhotoPath: String? = null,
    var isCompleted: Boolean = false
) : Parcelable {
    val fullArticleCode: String
        get() = "$articleCode-$index"
}
