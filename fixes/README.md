# Исправления для приложения Warehouse Camera

## Проблема 1: Удаление фотографий из галереи при удалении файлов в File structure

### Шаги для реализации:

1. Измените метод `deleteFileOrDirectory` в файле `FileUtils.kt`, добавив параметр контекста и логику удаления из MediaStore:

```kotlin
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
```

2. Сделайте метод `isImageFile` публичным в `FileUtils.kt`:

```kotlin
// Изменить с:
private fun isImageFile(file: File): Boolean {
    val name = file.name.lowercase(Locale.ROOT)
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
           name.endsWith(".png") || name.endsWith(".webp")
}

// На:
fun isImageFile(file: File): Boolean {
    val name = file.name.lowercase(Locale.ROOT)
    return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
           name.endsWith(".png") || name.endsWith(".webp")
}
```

3. Обновите вызов удаления в `FileStructureAdapter.kt`, чтобы передавать контекст:

```kotlin
// Изменить с:
if (FileUtils.deleteFileOrDirectory(file)) {

// На:
if (FileUtils.deleteFileOrDirectory(file, context)) {
```

## Проблема 2: Синхронизация списка приемок с File structure

### Шаги для реализации:

1. Измените метод `getAllReceptions` в `ProductReceptionRepository.kt`:

```kotlin
/**
 * Получить все проекты приёмки
 * Включает как сохраненные приемки, так и найденные в файловой системе
 */
fun getAllReceptions(): List<ProductReception> {
    // Получаем сохраненные приемки из SharedPreferences
    val savedReceptions = getSavedReceptions()
    
    // Сканируем файловую систему на наличие дополнительных приемок
    val fileReceptions = scanFileSystemForReceptions()
    
    // Объединяем два списка, удаляя дубликаты
    val allReceptions = savedReceptions.toMutableList()
    
    // Добавляем приемки из файловой системы, которых нет в SharedPreferences
    for (fileReception in fileReceptions) {
        if (!allReceptions.any { it.manufacturerCode == fileReception.manufacturerCode && it.date == fileReception.date }) {
            allReceptions.add(fileReception)
        }
    }
    
    // Обновляем SharedPreferences, чтобы они содержали все найденные приемки
    saveReceptions(allReceptions)
    
    return allReceptions
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
```

2. Сделайте метод `isDateFormat` публичным в `FileUtils.kt`:

```kotlin
// Изменить с:
private fun isDateFormat(name: String): Boolean {
    return try {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        dateFormat.isLenient = false
        dateFormat.parse(name)
        true
    } catch (e: Exception) {
        false
    }
}

// На:
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
```

3. Обновите `saveReceptions` в `ProductReceptionRepository.kt` для поддержки новой логики:

```kotlin
/**
 * Сохранить список приёмок
 */
private fun saveReceptions(receptions: List<ProductReception>): Boolean {
    val json = gson.toJson(receptions)
    return prefs.edit().putString(KEY_RECEPTIONS, json).commit()
}
```

## Дополнительные рекомендации:

1. Убедитесь, что в методе `onFileDeleted` в `FileStructureActivity.kt` вызов `updateDirectoryView()` происходит после всех операций удаления, чтобы обновить UI.

2. Убедитесь, что в `ReceptionSelectionActivity` вызывается `loadReceptions()` в `onResume()`, чтобы обновить список при возвращении пользователя к списку приемок.

3. Добавьте необходимые импорты в файлы, которые вы изменили:

```kotlin
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.provider.MediaStore
import java.util.UUID
```
