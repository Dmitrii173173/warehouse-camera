package com.warehouse.camera.model.project

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.warehouse.camera.utils.FileUtils
import java.io.File
import java.util.UUID

/**
 * Репозиторий для управления проектами приёмки продукции
 */
class ProductReceptionRepository(private val context: Context) {
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Получить все проекты приёмки
     * Включает как сохраненные приемки, так и найденные в файловой системе
     */
    fun getAllReceptions(): List<ProductReception> {
        // Call the synchronizeWithFileSystem method to make sure we have up-to-date data
        synchronizeWithFileSystem()
        
        return getSavedReceptions()
    }
    
    /**
     * Синхронизирует сохраненные приёмки с файловой системой
     * Удаляет приёмки, которых нет в файловой системе, и добавляет новые
     */
    fun synchronizeWithFileSystem() {
        // Получаем сохраненные приемки из SharedPreferences
        val savedReceptions = getSavedReceptions()
        
        // Сканируем файловую систему на наличие приемок
        val fileReceptions = scanFileSystemForReceptions()
        
        // Создаем новый список, содержащий только те приёмки, которые существуют в файловой системе
        val validReceptions = savedReceptions.filter { savedReception ->
            fileReceptions.any { fileReception -> 
                fileReception.manufacturerCode == savedReception.manufacturerCode && 
                fileReception.date == savedReception.date
            }
        }
        
        // Добавляем приемки из файловой системы, которых нет в SharedPreferences
        val allReceptions = validReceptions.toMutableList()
        for (fileReception in fileReceptions) {
            if (!allReceptions.any { it.manufacturerCode == fileReception.manufacturerCode && it.date == fileReception.date }) {
                allReceptions.add(fileReception)
            }
        }
        
        // Обновляем SharedPreferences, чтобы они содержали все найденные приемки
        saveReceptions(allReceptions)
    }
    
    /**
     * Получить сохраненные проекты приёмки из SharedPreferences
     */
    private fun getSavedReceptions(): List<ProductReception> {
        val json = prefs.getString(KEY_RECEPTIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<ProductReception>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing receptions", e)
            emptyList()
        }
    }
    
    /**
     * Сканирует файловую систему для поиска существующих приемок
     */
    private fun scanFileSystemForReceptions(): List<ProductReception> {
        val result = mutableListOf<ProductReception>()
        try {
            // Получаем базовую директорию (warehouse)
            val baseDir = FileUtils.getBaseDirectory(context)
            if (!baseDir.exists() || !baseDir.isDirectory) {
                return emptyList()
            }
            
            // Перебираем все поддиректории базовой директории (коды производителей)
            baseDir.listFiles()?.filter { it.isDirectory }?.forEach { manufacturerDir ->
                val manufacturerCode = manufacturerDir.name
                
                // Перебираем все поддиректории кода производителя (даты)
                manufacturerDir.listFiles()?.filter { it.isDirectory && FileUtils.isDateFormat(it.name) }?.forEach { dateDir ->
                    val date = dateDir.name
                    
                    // Создаем объект приемки
                    val reception = ProductReception(
                        id = UUID.randomUUID().toString(),
                        manufacturerCode = manufacturerCode,
                        date = date,
                        createdAt = dateDir.lastModified()
                    )
                    
                    result.add(reception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning file system for receptions", e)
        }
        
        return result
    }
    
    /**
     * Добавить новый проект приёмки
     */
    fun addReception(reception: ProductReception): Boolean {
        val receptions = getAllReceptions().toMutableList()
        
        // Проверка на существование приёмки с такими же параметрами
        if (receptions.any { it.manufacturerCode == reception.manufacturerCode && it.date == reception.date }) {
            return false
        }
        
        // Создаем директорию для новой приёмки
        val projectDir = FileUtils.getProjectDirectory(context, reception)
        if (projectDir == null || !projectDir.exists()) {
            Log.e(TAG, "Failed to create directory for reception")
            // Даже если директория не создалась, продолжаем добавление приёмки в список
        }
        
        receptions.add(reception)
        return saveReceptions(receptions)
    }
    
    /**
     * Получить проект по ID
     */
    fun getReceptionById(id: String): ProductReception? {
        return getAllReceptions().find { it.id == id }
    }
    
    /**
     * Получить директорию для проекта
     */
    fun getReceptionDirectory(reception: ProductReception): File? {
        return FileUtils.getProjectDirectory(context, reception)
    }
    
    /**
     * Сохранить список приёмок
     */
    private fun saveReceptions(receptions: List<ProductReception>): Boolean {
        val json = gson.toJson(receptions)
        return prefs.edit().putString(KEY_RECEPTIONS, json).commit()
    }
    
    companion object {
        private const val TAG = "ReceptionRepository"
        private const val PREF_NAME = "receptions_preferences"
        private const val KEY_RECEPTIONS = "receptions"
    }
}