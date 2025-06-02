package com.warehouse.camera.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.warehouse.camera.R
import com.warehouse.camera.utils.FileSystemDiagnostic
import com.warehouse.camera.utils.FileUtils

/**
 * Активность для диагностики проблем с файловой системой
 * Добавлена в AndroidManifest.xml как внутренняя активность для отладки
 */
class DiagnosticActivity : AppCompatActivity() {
    
    private lateinit var diagnosticText: TextView
    private val diagnostic by lazy { FileSystemDiagnostic(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)
        
        // Инициализация views
        diagnosticText = findViewById(R.id.tv_diagnostic_output)
        
        // Настройка кнопок
        findViewById<Button>(R.id.btn_run_diagnostic).setOnClickListener { runFullDiagnostic() }
        findViewById<Button>(R.id.btn_test_save).setOnClickListener { testFileSave() }
        findViewById<Button>(R.id.btn_clear_log).setOnClickListener { clearLog() }
        
        // Запрашиваем разрешения
        requestPermissions()
        
        // Показываем начальную информацию
        appendToLog("🔧 Диагностическая утилита файловой системы")
        appendToLog("Для начала нажмите кнопку диагностики или теста сохранения.\n")
    }
    
    private fun clearLog() {
        runOnUiThread {
            diagnosticText.text = "🔧 Диагностическая утилита файловой системы\nДля начала нажмите кнопку диагностики или теста сохранения.\n"
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Проверяем какие разрешения нужны
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                appendToLog("⚠️ Рекомендуется предоставить разрешение MANAGE_EXTERNAL_STORAGE")
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
        }
    }
    
    private fun runFullDiagnostic() {
        appendToLog("=== ЗАПУСК ПОЛНОЙ ДИАГНОСТИКИ ===\n")
        
        try {
            val result = diagnostic.runFullDiagnostic()
            
            // Информация об устройстве
            appendToLog("📱 УСТРОЙСТВО:")
            appendToLog("   ${result.deviceInfo}\n")
            
            // Разрешения
            appendToLog("🔐 РАЗРЕШЕНИЯ:")
            appendToLog("   ${result.permissions}\n")
            
            // Доступные пути
            appendToLog("📁 ДОСТУПНЫЕ ПУТИ:")
            result.availablePaths.forEach { pathInfo ->
                val status = if (pathInfo.writable) "✅" else "❌"
                appendToLog("   $status ${pathInfo.type}")
                appendToLog("      ${pathInfo.path}")
                appendToLog("      Exists: ${pathInfo.exists}, Writable: ${pathInfo.writable}")
            }
            appendToLog("")
            
            // Тесты записи
            appendToLog("✏️ ТЕСТЫ ЗАПИСИ:")
            result.writeTests.forEach { testResult ->
                appendToLog("   ${testResult}")
            }
            appendToLog("")
            
            // Рекомендации
            appendToLog("💡 РЕКОМЕНДАЦИИ:")
            result.recommendations.forEach { recommendation ->
                appendToLog("   $recommendation")
            }
            appendToLog("")
            
            // Лучший путь
            val bestPath = diagnostic.getBestSavePath()
            appendToLog("🎯 РЕКОМЕНДУЕМЫЙ ПУТЬ:")
            appendToLog("   $bestPath")
            
        } catch (e: Exception) {
            appendToLog("❌ ОШИБКА ДИАГНОСТИКИ: ${e.message}")
            Log.e("DiagnosticActivity", "Diagnostic error", e)
        }
    }
    
    private fun testFileSave() {
        appendToLog("\n=== ТЕСТ СОХРАНЕНИЯ ФАЙЛА ===")
        
        try {
            // Инициализация FileUtils
            FileUtils.initialize(this)
            
            // Создание тестового контента
            val testContent = """
                ТЕСТ СОХРАНЕНИЯ ФАЙЛА
                =====================
                Время: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
                Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
                
                Этот файл создан для тестирования функции сохранения.
                Если вы видите этот текст - сохранение работает корректно!
            """.trimIndent()
            
            // Тестирование различных методов сохранения
            val testResults = mutableListOf<Pair<String, Boolean>>()
            
            // Тест 1: Базовая директория приложения
            try {
                val baseDir = FileUtils.getBaseDirectory(this)
                val testFile1 = java.io.File(baseDir, "test_file_1.txt")
                testFile1.writeText(testContent)
                val success1 = testFile1.exists() && testFile1.length() > 0
                testResults.add("Base Directory" to success1)
                if (success1) {
                    appendToLog("✅ Тест 1 (Base Directory): УСПЕХ")
                    appendToLog("   Путь: ${testFile1.absolutePath}")
                    testFile1.delete() // Удаляем тестовый файл
                } else {
                    appendToLog("❌ Тест 1 (Base Directory): НЕУДАЧА")
                }
            } catch (e: Exception) {
                appendToLog("❌ Тест 1 (Base Directory): ОШИБКА - ${e.message}")
                testResults.add("Base Directory" to false)
            }
            
            // Тест 2: Приватная папка приложения
            try {
                val privateDir = getExternalFilesDir(null) ?: filesDir
                val testFile2 = java.io.File(privateDir, "test_file_2.txt")
                testFile2.writeText(testContent)
                val success2 = testFile2.exists() && testFile2.length() > 0
                testResults.add("Private Directory" to success2)
                if (success2) {
                    appendToLog("✅ Тест 2 (Private Directory): УСПЕХ")
                    appendToLog("   Путь: ${testFile2.absolutePath}")
                    testFile2.delete()
                } else {
                    appendToLog("❌ Тест 2 (Private Directory): НЕУДАЧА")
                }
            } catch (e: Exception) {
                appendToLog("❌ Тест 2 (Private Directory): ОШИБКА - ${e.message}")
                testResults.add("Private Directory" to false)
            }
            
            // Тест 3: MediaStore (только Android 10+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    val resolver = contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "test_file_3.txt")
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents/warehouse_test")
                    }
                    
                    val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(testContent.toByteArray())
                        }
                        appendToLog("✅ Тест 3 (MediaStore): УСПЕХ")
                        appendToLog("   URI: $uri")
                        
                        // Удаляем тестовый файл
                        resolver.delete(uri, null, null)
                        testResults.add("MediaStore" to true)
                    } else {
                        appendToLog("❌ Тест 3 (MediaStore): НЕУДАЧА - не удалось создать URI")
                        testResults.add("MediaStore" to false)
                    }
                } catch (e: Exception) {
                    appendToLog("❌ Тест 3 (MediaStore): ОШИБКА - ${e.message}")
                    testResults.add("MediaStore" to false)
                }
            } else {
                appendToLog("⚠️ Тест 3 (MediaStore): ПРОПУЩЕН - Android версия < 10")
                testResults.add("MediaStore" to false)
            }
            
            // Итоговая статистика
            val successCount = testResults.count { it.second }
            val totalCount = testResults.size
            
            appendToLog("\n📊 РЕЗУЛЬТАТЫ ТЕСТОВ:")
            appendToLog("   Успешных: $successCount из $totalCount")
            
            if (successCount > 0) {
                appendToLog("✅ Как минимум один метод сохранения работает!")
                appendToLog("   Проблема может быть в конкретном коде приложения.")
            } else {
                appendToLog("❌ НИ ОДИН метод сохранения не работает!")
                appendToLog("   Проблема связана с разрешениями или системными ограничениями.")
            }
            
        } catch (e: Exception) {
            appendToLog("❌ КРИТИЧЕСКАЯ ОШИБКА ТЕСТА: ${e.message}")
            Log.e("DiagnosticActivity", "Test save error", e)
        }
    }
    
    private fun appendToLog(message: String) {
        runOnUiThread {
            val currentText = diagnosticText.text.toString()
            diagnosticText.text = currentText + message + "\n"
        }
        
        // Также выводим в Android Log
        Log.d("DiagnosticActivity", message)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1001) {
            val results = permissions.zip(grantResults.toTypedArray()).map { (permission, result) ->
                val granted = result == PackageManager.PERMISSION_GRANTED
                "$permission: ${if (granted) "✅ ПРЕДОСТАВЛЕНО" else "❌ ОТКЛОНЕНО"}"
            }
            
            appendToLog("\n📋 РЕЗУЛЬТАТЫ ЗАПРОСА РАЗРЕШЕНИЙ:")
            results.forEach { appendToLog("   $it") }
            appendToLog("")
        }
    }
}
