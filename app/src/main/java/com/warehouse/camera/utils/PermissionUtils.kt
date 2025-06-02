package com.warehouse.camera.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    const val REQUEST_CAMERA_PERMISSION = 100
    const val REQUEST_STORAGE_PERMISSION = 101
    const val REQUEST_MANAGE_STORAGE_PERMISSION = 102
    
    private const val TAG = "PermissionUtils"
    
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ - нужно READ_MEDIA_IMAGES
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12 - проверяем старые разрешения, но они могут не работать в полной мере
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 9 и ниже - проверяем WRITE_EXTERNAL_STORAGE
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Проверяет, есть ли разрешение на управление всеми файлами (Android 11+)
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Не требуется для старых версий Android
        }
    }
    
    /**
     * Проверяет все необходимые разрешения для работы с файлами
     */
    fun hasAllNecessaryPermissions(context: Context): Boolean {
        val hasCamera = hasCameraPermission(context)
        val hasStorage = hasStoragePermission(context)
        val hasManageStorage = hasManageStoragePermission()
        
        Log.d(TAG, "Permissions check: Camera=$hasCamera, Storage=$hasStorage, ManageStorage=$hasManageStorage")
        
        return hasCamera && hasStorage && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || hasManageStorage)
    }
    
    fun requestCameraPermission(context: Context) {
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            Log.e(TAG, "Cannot request camera permission - context is not an Activity")
        }
    }
    
    fun requestStoragePermission(context: Context) {
        if (context is Activity) {
            val permissions = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }
            
            ActivityCompat.requestPermissions(
                context,
                permissions,
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            Log.e(TAG, "Cannot request storage permission - context is not an Activity")
        }
    }
    
    /**
     * Запрашивает разрешение на управление всеми файлами (Android 11+)
     */
    fun requestManageStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context is Activity) {
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog(context)
            }
        }
    }
    
    private fun showManageStoragePermissionDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Доступ к файлам")
            .setMessage("Для надежного сохранения файлов на вашем устройстве приложению требуется разрешение на управление файлами.\n\nЭто поможет избежать ошибок сохранения на некоторых устройствах.")
            .setPositiveButton("Предоставить") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening manage storage settings", e)
                    // Fallback на общие настройки
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    activity.startActivity(intent)
                }
            }
            .setNegativeButton("Пропустить") { dialog, _ ->
                dialog.dismiss()
                Log.i(TAG, "User declined MANAGE_EXTERNAL_STORAGE permission")
            }
            .show()
    }
    
    fun requestAllPermissions(context: Context) {
        if (context is Activity) {
            val permissions = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                else -> {
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }
            
            ActivityCompat.requestPermissions(
                context,
                permissions,
                REQUEST_CAMERA_PERMISSION
            )
            
            // Для Android 11+ также предлагаем MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Задержка, чтобы сначала обработались основные разрешения
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    requestManageStoragePermission(context)
                }, 1000)
            }
        } else {
            Log.e(TAG, "Cannot request permissions - context is not an Activity")
        }
    }
    
    /**
     * Проверяет, нужно ли показывать пояснение для разрешения
     */
    fun shouldShowPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * Показывает диалог с пояснением, зачем нужны разрешения
     */
    fun showPermissionRationaleDialog(activity: Activity, onContinue: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Необходимые разрешения")
            .setMessage("Приложение использует:\n\n" +
                    "📷 Камеру - для фотографирования товаров\n" +
                    "📁 Файлы - для сохранения фотографий и отчетов\n\n" +
                    "Без этих разрешений приложение не сможет работать корректно.")
            .setPositiveButton("Предоставить") { _, _ -> onContinue() }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    /**
     * Получить список отсутствующих разрешений
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!hasCameraPermission(context)) {
            missingPermissions.add("Камера")
        }
        
        if (!hasStoragePermission(context)) {
            missingPermissions.add("Доступ к файлам")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasManageStoragePermission()) {
            missingPermissions.add("Управление файлами")
        }
        
        return missingPermissions
    }
    
    /**
     * Получить статус разрешений в виде строки для диагностики
     */
    fun getPermissionsStatus(context: Context): String {
        return buildString {
            appendLine("=== СТАТУС РАЗРЕШЕНИЙ ===")
            appendLine("Android версия: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Камера: ${if (hasCameraPermission(context)) "✅ Предоставлено" else "❌ Отсутствует"}")
            appendLine("Файлы: ${if (hasStoragePermission(context)) "✅ Предоставлено" else "❌ Отсутствует"}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appendLine("Управление файлами: ${if (hasManageStoragePermission()) "✅ Предоставлено" else "❌ Отсутствует"}")
            }
            
            val missing = getMissingPermissions(context)
            if (missing.isNotEmpty()) {
                appendLine("\n⚠️ Отсутствующие разрешения: ${missing.joinToString(", ")}")
            } else {
                appendLine("\n✅ Все необходимые разрешения предоставлены")
            }
        }
    }
}
