package com.warehouse.camera.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class FileSystemDiagnostic(private val context: Context) {
    
    private val TAG = "FileSystemDiagnostic"
    
    /**
     * Полная диагностика доступа к файловой системе
     */
    fun runFullDiagnostic(): DiagnosticResult {
        val result = DiagnosticResult()
        
        Log.d(TAG, "=== НАЧАЛО ДИАГНОСТИКИ ФАЙЛОВОЙ СИСТЕМЫ ===")
        
        // 1. Информация об устройстве
        result.deviceInfo = getDeviceInfo()
        Log.d(TAG, "Устройство: ${result.deviceInfo}")
        
        // 2. Проверка разрешений
        result.permissions = checkPermissions()
        Log.d(TAG, "Разрешения: ${result.permissions}")
        
        // 3. Проверка доступных путей
        result.availablePaths = checkAvailablePaths()
        Log.d(TAG, "Доступные пути: ${result.availablePaths}")
        
        // 4. Тест записи в разные локации
        result.writeTests = performWriteTests()
        Log.d(TAG, "Тесты записи: ${result.writeTests}")
        
        // 5. Рекомендации
        result.recommendations = generateRecommendations(result)
        
        Log.d(TAG, "=== КОНЕЦ ДИАГНОСТИКИ ===")
        result.recommendations.forEach { Log.d(TAG, it) }
        
        return result
    }
    
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            hasExternalStorage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED,
            isScopedStorageEnforced = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        )
    }
    
    private fun checkPermissions(): PermissionStatus {
        val hasCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - нужны специфичные разрешения для медиа
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 - проверяем старые разрешения, но могут не работать
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        val hasManageStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Не требуется для старых версий
        }
        
        return PermissionStatus(hasCamera, hasStorage, hasManageStorage)
    }
    
    private fun checkAvailablePaths(): List<PathInfo> {
        val paths = mutableListOf<PathInfo>()
        
        // 1. Приватная папка приложения (всегда доступна)
        val privateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "warehouse")
        paths.add(PathInfo(
            path = privateDir.absolutePath,
            type = "Private App Directory",
            exists = privateDir.exists() || privateDir.mkdirs(),
            writable = privateDir.canWrite(),
            recommended = true
        ))
        
        // 2. DCIM папка (может быть недоступна на новых Android)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dcimDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "warehouse")
            paths.add(PathInfo(
                path = dcimDir.absolutePath,
                type = "DCIM Public Directory",
                exists = dcimDir.exists() || tryCreateDirectory(dcimDir),
                writable = dcimDir.canWrite(),
                recommended = false
            ))
        }
        
        // 3. Pictures папка
        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "warehouse")
        paths.add(PathInfo(
            path = picturesDir.absolutePath,
            type = "Pictures Public Directory", 
            exists = picturesDir.exists() || tryCreateDirectory(picturesDir),
            writable = picturesDir.canWrite(),
            recommended = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ))
        
        return paths
    }
    
    private fun tryCreateDirectory(dir: File): Boolean {
        return try {
            dir.mkdirs()
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось создать директорию ${dir.absolutePath}: ${e.message}")
            false
        }
    }
    
    private fun performWriteTests(): List<WriteTestResult> {
        val results = mutableListOf<WriteTestResult>()
        val testContent = "Test file content - ${System.currentTimeMillis()}"
        
        checkAvailablePaths().forEach { pathInfo ->
            val testFile = File(pathInfo.path, "test_file.txt")
            val testResult = try {
                // Попытка создать директорию
                testFile.parentFile?.mkdirs()
                
                // Попытка записи файла
                FileOutputStream(testFile).use { fos ->
                    fos.write(testContent.toByteArray())
                }
                
                // Проверка, что файл создался и читается
                val readContent = testFile.readText()
                val success = readContent == testContent
                
                // Удаляем тестовый файл
                testFile.delete()
                
                WriteTestResult(pathInfo.path, success, null)
            } catch (e: Exception) {
                WriteTestResult(pathInfo.path, false, e.message)
            }
            
            results.add(testResult)
        }
        
        return results
    }
    
    private fun generateRecommendations(result: DiagnosticResult): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Проверка разрешений
        if (!result.permissions.hasStorage) {
            recommendations.add("❌ Отсутствует разрешение на доступ к хранилищу - запросить разрешения")
        }
        
        if (result.deviceInfo.apiLevel >= Build.VERSION_CODES.R && !result.permissions.hasManageStorage) {
            recommendations.add("⚠️ Для Android 11+ рекомендуется запросить MANAGE_EXTERNAL_STORAGE")
        }
        
        // Анализ тестов записи
        val successfulPaths = result.writeTests.filter { it.success }
        if (successfulPaths.isEmpty()) {
            recommendations.add("❌ КРИТИЧНО: Ни один путь не доступен для записи!")
            recommendations.add("🔧 Использовать MediaStore API для создания файлов")
        } else {
            val privatePath = successfulPaths.find { it.path.contains("Android/data") }
            if (privatePath != null) {
                recommendations.add("✅ Использовать приватную папку приложения: ${privatePath.path}")
            }
        }
        
        // Специфичные рекомендации по устройству
        when {
            result.deviceInfo.manufacturer.contains("Xiaomi", ignoreCase = true) -> {
                recommendations.add("📱 Xiaomi: Проверить настройки разрешений в MIUI Security")
            }
            result.deviceInfo.manufacturer.contains("Huawei", ignoreCase = true) -> {
                recommendations.add("📱 Huawei: Проверить настройки разрешений в Phone Manager")
            }
            result.deviceInfo.manufacturer.contains("Samsung", ignoreCase = true) -> {
                recommendations.add("📱 Samsung: Убедиться что приложение не в режиме энергосбережения")
            }
            result.deviceInfo.manufacturer.contains("Oppo", ignoreCase = true) ||
            result.deviceInfo.manufacturer.contains("OnePlus", ignoreCase = true) -> {
                recommendations.add("📱 Oppo/OnePlus: Проверить автозапуск и разрешения в настройках безопасности")
            }
        }
        
        // Рекомендации по версии Android
        if (result.deviceInfo.apiLevel >= Build.VERSION_CODES.Q) {
            recommendations.add("⚠️ Android 10+: Использовать Scoped Storage или MediaStore API")
        }
        
        return recommendations
    }
    
    /**
     * Получить лучший путь для сохранения файлов на текущем устройстве
     */
    fun getBestSavePath(): String {
        val diagnostic = runFullDiagnostic()
        val workingPaths = diagnostic.writeTests.filter { it.success }
        
        return if (workingPaths.isNotEmpty()) {
            // Предпочтение отдаем приватной папке приложения
            workingPaths.find { it.path.contains("Android/data") }?.path 
                ?: workingPaths.first().path
        } else {
            // Fallback на приватную папку приложения
            File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "warehouse").absolutePath
        }
    }
}

// Дата-классы для результатов диагностики
data class DiagnosticResult(
    var deviceInfo: DeviceInfo = DeviceInfo(),
    var permissions: PermissionStatus = PermissionStatus(),
    var availablePaths: List<PathInfo> = emptyList(),
    var writeTests: List<WriteTestResult> = emptyList(),
    var recommendations: List<String> = emptyList()
)

data class DeviceInfo(
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val manufacturer: String = "",
    val model: String = "",
    val hasExternalStorage: Boolean = false,
    val isScopedStorageEnforced: Boolean = false
) {
    override fun toString(): String {
        return "$manufacturer $model, Android $androidVersion (API $apiLevel), " +
                "External: $hasExternalStorage, Scoped: $isScopedStorageEnforced"
    }
}

data class PermissionStatus(
    val hasCamera: Boolean = false,
    val hasStorage: Boolean = false,
    val hasManageStorage: Boolean = false
) {
    override fun toString(): String {
        return "Camera: $hasCamera, Storage: $hasStorage, ManageStorage: $hasManageStorage"
    }
}

data class PathInfo(
    val path: String,
    val type: String,
    val exists: Boolean,
    val writable: Boolean,
    val recommended: Boolean = false
) {
    override fun toString(): String {
        return "$type: $path (exists: $exists, writable: $writable, recommended: $recommended)"
    }
}

data class WriteTestResult(
    val path: String,
    val success: Boolean,
    val error: String?
) {
    override fun toString(): String {
        return if (success) {
            "✅ $path - SUCCESS"
        } else {
            "❌ $path - FAILED: $error"
        }
    }
}
