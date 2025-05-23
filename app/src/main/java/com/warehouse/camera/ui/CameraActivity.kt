package com.warehouse.camera.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
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
import com.warehouse.camera.utils.ImageUtils
import com.warehouse.camera.utils.LanguageUtils
import com.warehouse.camera.utils.PermissionUtils
import java.io.File
import java.io.IOException
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
    private lateinit var addMoreButton: Button
    private lateinit var photoCountText: TextView
    
    // Radio buttons are still in the layout but hidden
    private lateinit var radioGroupCircles: RadioGroup
    private lateinit var radioButtonGreen: RadioButton
    private lateinit var radioButtonYellow: RadioButton
    private lateinit var radioButtonRed: RadioButton
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var selectedCircleColor: Int = Color.GREEN
    private var selectedCircleText: String = "1"
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var itemData: ItemData
    private var isBoxPhoto: Boolean = false
    private var defectCategory: Int = 1 // Default category
    private var currentPhotoIndex: Int = 0 // Track the current photo index
    
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
        defectCategory = intent.getIntExtra("defectCategory", 1)
        
        // Initialize the counter for photos
        currentPhotoIndex = if (isBoxPhoto) itemData.getBoxPhotoCount() else itemData.getProductPhotoCount()
        
        // Initialize views
        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.button_capture)
        previewContainer = findViewById(R.id.preview_container)
        imagePreview = findViewById(R.id.image_preview)
        retakeButton = findViewById(R.id.button_retake)
        usePhotoButton = findViewById(R.id.button_use_photo)
        nextItemButton = findViewById(R.id.button_next_item)
        addMoreButton = findViewById(R.id.button_add_more_photos)
        photoCountText = findViewById(R.id.photo_count_text)
        
        // Update photo count display
        updatePhotoCountDisplay()
        
        // The radio buttons are now hidden but we still keep references to them
        radioGroupCircles = findViewById(R.id.radioGroup_circles)
        radioButtonGreen = findViewById(R.id.radioButton_green)
        radioButtonYellow = findViewById(R.id.radioButton_yellow)
        radioButtonRed = findViewById(R.id.radioButton_red)
        
        // Set initial values for circle color and text based on defect category
        when (defectCategory) {
            1 -> {
                selectedCircleColor = Color.parseColor("#4CAF50")  // Green
                selectedCircleText = "1"
                radioButtonGreen.isChecked = true
            }
            2 -> {
                selectedCircleColor = Color.parseColor("#FFC107")  // Yellow
                selectedCircleText = "2"
                radioButtonYellow.isChecked = true
            }
            3 -> {
                selectedCircleColor = Color.parseColor("#F44336")  // Red
                selectedCircleText = "3"
                radioButtonRed.isChecked = true
            }
        }
        
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
        addMoreButton.setOnClickListener { addMorePhotos() }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun updatePhotoCountDisplay() {
        val count = if (isBoxPhoto) itemData.getBoxPhotoCount() else itemData.getProductPhotoCount()
        photoCountText.text = getString(R.string.photo_count, count)
        
        // Show the add more button only if there are already photos
        addMoreButton.visibility = if (count > 0) View.VISIBLE else View.GONE
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
            // Update the defect category in the item data
            itemData.defectCategory = defectCategory
            
            // Determine if we need to create a new file with index
            val needsIndex = currentPhotoIndex > 0
            
            // Create output file
            outputFile = if (needsIndex) {
                FileUtils.createImageFile(
                    this,
                    manufacturerInfo,
                    articleInfo,
                    itemData,
                    isBoxPhoto,
                    currentPhotoIndex
                )
            } else {
                FileUtils.createImageFile(
                    this,
                    manufacturerInfo,
                    articleInfo,
                    itemData,
                    isBoxPhoto
                )
            }
            
            if (outputFile == null) {
                Toast.makeText(this, R.string.error_create_directory, Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Saving photo to: ${outputFile?.absolutePath}")
            
            // Create output options object
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile!!).build()
            
            // Disable capture button during photo capture
            captureButton.isEnabled = false
            
            // Show a toast indicating photo is being taken
            Toast.makeText(this, "Taking photo...", Toast.LENGTH_SHORT).show()
            
            // Set up image capture listener
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        // Re-enable capture button
                        captureButton.isEnabled = true
                        
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(this@CameraActivity, R.string.error_photo_processing, Toast.LENGTH_LONG).show()
                        
                        // Clean up any partially written file
                        outputFile?.delete()
                        outputFile = null
                        outputUri = null
                    }
                    
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            // Re-enable capture button
                            captureButton.isEnabled = true
                            
                            // Verify file exists and has non-zero size
                            if (outputFile == null || !outputFile!!.exists() || outputFile!!.length() == 0L) {
                                throw IOException("File was not created properly")
                            }
                            
                            Log.d(TAG, "Photo captured successfully: ${outputFile?.absolutePath}")
                            
                            // Read the bitmap from file to verify it's valid
                            val bitmap = BitmapFactory.decodeFile(outputFile!!.absolutePath)
                            if (bitmap == null) {
                                throw IOException("Failed to decode bitmap from file")
                            }
                            
                            // Save the bitmap back to the file to ensure it's properly written
                            FileUtils.saveBitmapToFile(bitmap, outputFile!!)
                            
                            // Create a marked version with the defect category indicator
                            val markedBitmap = ImageUtils.addCircleToImage(bitmap, selectedCircleColor, selectedCircleText)
                            
                            // Create file path for marked version
                            val originalPath = outputFile!!.absolutePath
                            val markedPath = originalPath.replace(".jpg", "_marked.jpg")
                            val markedFile = File(markedPath)
                            
                            // Save the marked version
                            val markedSaved = FileUtils.saveBitmapToFile(markedBitmap, markedFile)
                            
                            if (!markedSaved) {
                                Log.e(TAG, "Failed to save marked image")
                                Toast.makeText(this@CameraActivity, R.string.error_save_image, Toast.LENGTH_LONG).show()
                                return
                            }
                            
                            // Add the paths to the ItemData (in the lists and legacy fields)
                            if (isBoxPhoto) {
                                itemData.addBoxPhoto(originalPath, markedPath)
                            } else {
                                itemData.addProductPhoto(originalPath, markedPath)
                            }
                            
                            // Increment the photo index for next photo
                            currentPhotoIndex++
                            
                            // Update the photo count display
                            updatePhotoCountDisplay()
                            
                            Log.d(TAG, "Original photo saved at: $originalPath")
                            Log.d(TAG, "Marked photo saved at: $markedPath")
                            
                            // Use the marked file for preview
                            outputFile = markedFile
                            outputUri = Uri.fromFile(markedFile)
                            showImagePreview()
                            
                            Toast.makeText(this@CameraActivity, "Photo saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error after saving image", e)
                            Toast.makeText(this@CameraActivity, R.string.error_save_image, Toast.LENGTH_LONG).show()
                            
                            // Clean up any partially written files
                            outputFile?.delete()
                            outputFile = null
                            outputUri = null
                        }
                    }
                }
            )
        } catch (e: Exception) {
            // Re-enable capture button
            captureButton.isEnabled = true
            
            Log.e(TAG, "Error taking photo", e)
            Toast.makeText(this, R.string.error_photo_processing, Toast.LENGTH_LONG).show()
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
        
        // Decrement the photo index if we're deleting the most recent photo
        if (currentPhotoIndex > 0) {
            currentPhotoIndex--
            
            // Also remove the photo from ItemData
            if (isBoxPhoto && itemData.boxPhotoPaths.isNotEmpty()) {
                val lastIndex = itemData.boxPhotoPaths.size - 1
                itemData.boxPhotoPaths.removeAt(lastIndex)
                itemData.boxPhotoMarkedPaths.removeAt(lastIndex)
                
                // Update legacy fields
                if (lastIndex == 0) {
                    itemData.boxPhotoPath = null
                    itemData.boxPhotoMarkedPath = null
                } else if (lastIndex > 0) {
                    itemData.boxPhotoPath = itemData.boxPhotoPaths[0]
                    itemData.boxPhotoMarkedPath = itemData.boxPhotoMarkedPaths[0]
                }
            } else if (!isBoxPhoto && itemData.productPhotoPaths.isNotEmpty()) {
                val lastIndex = itemData.productPhotoPaths.size - 1
                itemData.productPhotoPaths.removeAt(lastIndex)
                itemData.productPhotoMarkedPaths.removeAt(lastIndex)
                
                // Update legacy fields
                if (lastIndex == 0) {
                    itemData.productPhotoPath = null
                    itemData.productPhotoMarkedPath = null
                } else if (lastIndex > 0) {
                    itemData.productPhotoPath = itemData.productPhotoPaths[0]
                    itemData.productPhotoMarkedPath = itemData.productPhotoMarkedPaths[0]
                }
            }
            
            // Update display
            updatePhotoCountDisplay()
        }
    }
    
    private fun usePhoto() {
        // Update item data
        if (outputFile == null || !outputFile!!.exists()) {
            // If we have at least one photo, allow to proceed
            if ((isBoxPhoto && itemData.getBoxPhotoCount() > 0) || 
                (!isBoxPhoto && itemData.getProductPhotoCount() > 0)) {
                // Continue with saving
            } else {
                Toast.makeText(this, R.string.error_save_image, Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Update category in the item data
        itemData.defectCategory = defectCategory
        
        // Set result and finish
        val resultIntent = Intent()
        resultIntent.putExtra("updatedItemData", itemData)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
    
    private fun addMorePhotos() {
        // Hide preview container and go back to camera
        previewContainer.visibility = View.GONE
        
        // Ready for the next photo
        outputFile = null
        outputUri = null
    }
    
    private fun saveAndMoveToNextItem() {
        // First save current photo if one was taken
        if ((outputFile == null || !outputFile!!.exists()) && 
            ((isBoxPhoto && itemData.getBoxPhotoCount() == 0) || 
             (!isBoxPhoto && itemData.getProductPhotoCount() == 0))) {
            Toast.makeText(this, R.string.error_save_image, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update category in the item data
        itemData.defectCategory = defectCategory
        
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