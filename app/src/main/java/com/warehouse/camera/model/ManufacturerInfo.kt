package com.warehouse.camera.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ManufacturerInfo(
    val manufacturerCode: String,
    val date: String
) : Parcelable
