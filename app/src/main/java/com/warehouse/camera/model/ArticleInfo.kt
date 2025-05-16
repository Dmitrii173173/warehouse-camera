package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArticleInfo(
    val articleCode: String,
    val quantity: Int,
    val defectCategory: Int = 1
) : Parcelable
