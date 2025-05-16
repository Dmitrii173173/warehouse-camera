package com.warehouse.camera.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.utils.FileUtils
import com.warehouse.camera.utils.LanguageUtils
import com.warehouse.camera.utils.PermissionUtils
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var previewContainer: FrameLayout
    private lateinit var imagePreview: ImageView
    private lateinit var retakeButton: Button
    private lateinit var usePhotoButton: Button
    private lateinit var nextItemButton: Button
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var itemData: ItemData
    private var isBoxPhoto: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_camera)
        
        // Get data from intent
        manufacturerInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("manufacturerInfo", ManufacturerInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("manufacturerInfo")
        } ?: throw IllegalStateException("ManufacturerInfo must be provided")
        
        articleInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("articleInfo", ArticleInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("articleInfo")
        } ?: throw IllegalStateException("ArticleInfo must be provided")
        
        itemData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("itemData", ItemData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("itemData")
        } ?: throw IllegalStateException("ItemData must be provided")
        isBoxPhoto = intent.getBooleanExtra("isBoxPhoto", false)
        
        // Initialize views
        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.button_capture)
        previewContainer = findViewById(R.id.preview_container)
        imagePreview = findViewById(R.id.image_preview)
        retakeButton = findViewById(R.id.button_retake)
        usePhotoButton = findViewById(R.id.button_use_photo)
        nextItemButton = findViewById(R.id.button_next_item)
        
        // Check permissions
        if (!PermissionUtils.hasCameraPermission(this) || !PermissionUtils.hasStoragePermission(this)) {
            PermissionUtils.requestAllPermissions(this)
        } else {
            startCamera()
        }
        
        // Set click listeners
        captureButton.setOnClickListener { takePhoto() }
        retakeButton.setOnClickListener { retakePhoto() }
        usePhotoButton.setOnClickListener { usePhoto() }
        nextItemButton.setOnClickListener { saveAndMoveToNextItem() }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        
        try {
            // Create output file
            outputFile = FileUtils.createImageFile(
                this,
                manufacturerInfo,
                articleInfo,
                itemData,
                isBoxPhoto
            )
            
            if (outputFile == null) {
                Toast.makeText(this, R.string.error_create_directory, Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Saving photo to: ${outputFile?.absolutePath}")
            
            // Create output options object
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile!!).build()
            
            // Show a toast indicating photo is being taken
            Toast.makeText(this, "Taking photo...", Toast.LENGTH_SHORT).show()
            
            // Set up image capture listener
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(this@CameraActivity, "Error: ${exc.message}", Toast.LENGTH_LONG).show()
                    }
                    
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            Log.d(TAG, "Photo saved successfully: ${outputFile?.absolutePath}")
                            Log.d(TAG, "File exists: ${outputFile?.exists()}")
                            Log.d(TAG, "File size: ${outputFile?.length()} bytes")
                            
                            outputUri = Uri.fromFile(outputFile)
                            showImagePreview()
                            
                            Toast.makeText(this@CameraActivity, "Photo saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error after saving image", e)
                            Toast.makeText(this@CameraActivity, "Error after saving: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showImagePreview() {
        // Show preview container
        previewContainer.visibility = View.VISIBLE
        
        // Set image in preview
        imagePreview.setImageURI(outputUri)
    }
    
    private fun retakePhoto() {
        // Hide preview container
        previewContainer.visibility = View.GONE
        
        // Delete the captured photo
        outputFile?.delete()
        outputFile = null
        outputUri = null
    }
    
    private fun usePhoto() {
        // Update item data
        if (outputFile == null || !outputFile!!.exists()) {
            Toast.makeText(this, R.string.error_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isBoxPhoto) {
            itemData.boxPhotoPath = outputFile?.absolutePath
            Log.d(TAG, "Box photo path set to: ${itemData.boxPhotoPath}")
        } else {
            itemData.productPhotoPath = outputFile?.absolutePath
            Log.d(TAG, "Product photo path set to: ${itemData.productPhotoPath}")
        }
        
        // Set result and finish
        val resultIntent = Intent()
        resultIntent.putExtra("updatedItemData", itemData)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    private fun saveAndMoveToNextItem() {
        // First save current photo
        if (outputFile == null || !outputFile!!.exists()) {
            Toast.makeText(this, R.string.error_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update current item data
        if (isBoxPhoto) {
            itemData.boxPhotoPath = outputFile?.absolutePath
        } else {
            itemData.productPhotoPath = outputFile?.absolutePath
        }
        
        // Get next item in same reception
        val resultIntent = Intent()
        resultIntent.putExtra("updatedItemData", itemData)
        resultIntent.putExtra("moveToNextItem", true)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PermissionUtils.REQUEST_CAMERA_PERMISSION) {
            if (PermissionUtils.hasCameraPermission(this) && PermissionUtils.hasStoragePermission(this)) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    companion object {
        private const val TAG = "CameraActivity"
    }
}