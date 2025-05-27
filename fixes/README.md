# Исправления для приложения Warehouse Camera

В этом файле описаны реализованные исправления и добавленные функции в версии 1.1.0. Для всех исправлений приведен код реализации.

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

## Проблема 3: Фикс ориентации фотографий

### Шаги для реализации:

1. Добавьте метод `fixPhotoOrientation` в класс `ImageUtils`:

```kotlin
/**
 * Исправляет ориентацию изображения на основе данных EXIF
 * @param photoPath Путь к файлу изображения
 * @return Исправленный Bitmap или null в случае ошибки
 */
fun fixPhotoOrientation(photoPath: String): Bitmap? {
    try {
        // Получаем исходный Bitmap
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }
        var bitmap = BitmapFactory.decodeFile(photoPath, options) ?: return null
        
        // Получаем ориентацию из EXIF
        val exif = ExifInterface(photoPath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        
        Log.d("ImageUtils", "Original EXIF orientation: $orientation")
        
        // Создаем матрицу для поворота
        val matrix = Matrix()
        
        // Применяем поворот в зависимости от ориентации
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.preScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            else -> return bitmap // Поворот не требуется
        }
        
        // Применяем трансформацию и получаем новый Bitmap
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        
        // Очищаем исходный Bitmap, если он отличается от повернутого
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
            bitmap = rotatedBitmap
        }
        
        // Сохраняем повернутый Bitmap обратно в файл
        FileOutputStream(photoPath).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        // Очищаем информацию об ориентации в EXIF
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        exif.saveAttributes()
        
        Log.d("ImageUtils", "Photo orientation fixed: $photoPath")
        return bitmap
        
    } catch (e: IOException) {
        Log.e("ImageUtils", "Error fixing photo orientation", e)
        return null
    }
}
```

2. Добавьте необходимые импорты в `ImageUtils.kt`:

```kotlin
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.IOException
```

3. Обновите метод фотосъемки в `CameraActivity.kt` для использования нового метода исправления ориентации:

```kotlin
// Замените этот блок кода:
// Читаем битмап из файла для проверки
val bitmap = BitmapFactory.decodeFile(outputFile!!.absolutePath)
if (bitmap == null) {
    throw IOException("Failed to decode bitmap from file")
}

// Сохраняем битмап обратно в файл для гарантии правильной записи
FileUtils.saveBitmapToFile(bitmap, outputFile!!)

// На этот новый код:
// Исправляем ориентацию и читаем исправленный битмап
val bitmap = ImageUtils.fixPhotoOrientation(outputFile!!.absolutePath)
if (bitmap == null) {
    throw IOException("Failed to decode bitmap from file")
}

// Битмап уже сохранен методом fixPhotoOrientation
// FileUtils.saveBitmapToFile(bitmap, outputFile!!)
```

## Проблема 4: Добавление возможности просмотра фотографий из файлового менеджера

### Шаги для реализации:

1. Обновите метод `showFileDetails` в `FileStructureActivity.kt` для открытия фотографий в ImageViewerActivity:

```kotlin
else if (isImageFile(file)) {
    // Открываем просмотрщик изображений
    val intent = Intent(this, ImageViewerActivity::class.java)
    val itemType = when {
        file.name.startsWith("damage-") -> GalleryItem.ItemType.BOX_PHOTO
        file.name.startsWith("barcode-") -> GalleryItem.ItemType.PRODUCT_PHOTO
        else -> GalleryItem.ItemType.UNKNOWN
    }
    
    // Извлекаем код артикула из имени файла или используем пустую строку
    val articleCode = try {
        val fileName = file.name
        val prefixEnd = fileName.indexOf('-') + 1
        val suffixStart = fileName.lastIndexOf('.')
        if (prefixEnd > 0 && suffixStart > prefixEnd) {
            fileName.substring(prefixEnd, suffixStart).replace("_marked", "")
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
    
    val galleryItem = GalleryItem(
        file = file,
        name = file.name,
        date = Date(file.lastModified()),
        type = itemType,
        articleCode = articleCode,
        path = file.absolutePath
    )
    
    intent.putExtra("galleryItem", galleryItem)
    startActivity(intent)
}
```

2. Добавьте необходимые импорты в `FileStructureActivity.kt`:

```kotlin
import android.content.Intent
import com.warehouse.camera.model.GalleryItem
import java.util.Date
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
