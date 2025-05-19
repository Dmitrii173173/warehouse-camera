package com.warehouse.camera.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    const val REQUEST_CAMERA_PERMISSION = 100
    const val REQUEST_STORAGE_PERMISSION = 101
    
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && 
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestCameraPermission(context: Context) {
        // Only proceed if context is an Activity
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            // Log if context is not an Activity
            android.util.Log.e("PermissionUtils", "Cannot request camera permission - context is not an Activity")
        }
    }
    
    fun requestStoragePermission(context: Context) {
        // Only proceed if context is an Activity as ActivityCompat.requestPermissions requires an Activity
        if (context is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_STORAGE_PERMISSION
                )
            } else {
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
            }
        } else {
            // Log if context is not an Activity - cannot request permissions from non-Activity context
            android.util.Log.e("PermissionUtils", "Cannot request permissions - context is not an Activity")
        }
    }
    
    fun requestAllPermissions(context: Context) {
        // Only proceed if context is an Activity
        if (context is Activity) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            
            ActivityCompat.requestPermissions(
                context,
                permissions,
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            // Log if context is not an Activity
            android.util.Log.e("PermissionUtils", "Cannot request permissions - context is not an Activity")
        }
    }
}
