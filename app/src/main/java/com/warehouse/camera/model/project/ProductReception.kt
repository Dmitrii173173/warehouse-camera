package com.warehouse.camera.model.project

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
// import java.io.Serializable
import java.util.*

/**
 * Представляет собой проект приёмки продукции
 */
@Parcelize
data class ProductReception(
    val id: String = UUID.randomUUID().toString(),
    val manufacturerCode: String,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    // Получение отображаемого имени для приёмки
    val displayName: String get() = "$manufacturerCode - $date"
    
    // Получение пути к директории для хранения приёмки
    val directoryPath: String get() = "$manufacturerCode/$date"
}