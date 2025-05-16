package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DefectDetails(
    val reason: String,
    val template: String,
    val description: String
) : Parcelable
