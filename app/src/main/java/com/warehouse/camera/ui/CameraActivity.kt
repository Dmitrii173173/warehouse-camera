package com.warehouse.camera.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.TorchState
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.utils.FileUtils
import com.warehouse.camera.utils.ImageUtils
import com.warehouse.camera.utils.LanguageUtils
import com.warehouse.camera.utils.PermissionUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.*

class CameraActivity : BaseActivity() {
    private lateinit var viewFinder: PreviewView
    private var camera: Camera? = null
    private lateinit var captureButton: FloatingActionButton
    private lateinit var previewContainer: FrameLayout
    private lateinit var imagePreview: ImageView
    private lateinit var retakeButton: Button
    private lateinit var usePhotoButton: Button
    private lateinit var addMoreButton: Button
    private lateinit var photoCountText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var backPreviewButton: ImageButton
    private lateinit var normalPhotoSwitch: Switch
    private lateinit var flashButton: ImageButton
    private lateinit var processingIndicator: FrameLayout
    private lateinit var processingText: TextView
    
    // Radio buttons for the color circles
    private lateinit var radioGroupCircles: RadioGroup
    private lateinit var radioButtonGreen: RadioButton
    private lateinit var radioButtonYellow: RadioButton
    private lateinit var radioButtonRed: RadioButton
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    
    // Coroutine scope for async operations
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var outputFile: File? = null
    private var outputUri: Uri? = null
    private var selectedCircleColor: Int = Color.GREEN
    private var selectedCircleText: String = "1"
    private var normalPhotoMode: Boolean = false // Режим обычного фото без маркера
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var itemData: ItemData
    private var isBoxPhoto: Boolean = false
    private var defectCategory: Int = 1 // Default category
    private var currentPhotoIndex: Int = 0 // Track the current photo index
    
    // Variables to store temporary photo data during processing
    private var tempPhotoPath: String? = null
    private var isProcessingPhoto = false
    
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
        addMoreButton = findViewById(R.id.button_add_more_photos)
        photoCountText = findViewById(R.id.photo_count_text)
        backButton = findViewById(R.id.button_back)
        backPreviewButton = findViewById(R.id.button_back_preview)
        normalPhotoSwitch = findViewById(R.id.switch_normal_photo)
        flashButton = findViewById(R.id.button_flash)
        processingIndicator = findViewById(R.id.processing_indicator)
        processingText = findViewById(R.id.processing_text)
        
        // Update photo count display
        updatePhotoCountDisplay()
        
        // Set up the radio buttons for color selection
        radioGroupCircles = findViewById(R.id.radioGroup_circles)
        radioButtonGreen = findViewById(R.id.radioButton_green)
        radioButtonYellow = findViewById(R.id.radioButton_yellow)
        radioButtonRed = findViewById(R.id.radioButton_red)
        
        // Set initial values for circle color and text based on defect category
        when (defectCategory) {
            1 -> {
                selectedCircleColor = Color.parseColor("#34C759")  // Green
                selectedCircleText = "1"
                radioButtonGreen.isChecked = true
            }
            2 -> {
                selectedCircleColor = Color.parseColor("#FFCC00")  // Yellow
                selectedCircleText = "2"
                radioButtonYellow.isChecked = true
            }
            3 -> {
                selectedCircleColor = Color.parseColor("#FF3B30")  // Red
                selectedCircleText = "3"
                radioButtonRed.isChecked = true
            }
        }
        
        // Set up radio button listeners
        radioButtonGreen.setOnClickListener {
            selectedCircleColor = Color.parseColor("#34C759")
            selectedCircleText = "1"
            defectCategory = 1
        }
        
        radioButtonYellow.setOnClickListener {
            selectedCircleColor = Color.parseColor("#FFCC00")
            selectedCircleText = "2"
            defectCategory = 2
        }
        
        radioButtonRed.setOnClickListener {
            selectedCircleColor = Color.parseColor("#FF3B30")
            selectedCircleText = "3"
            defectCategory = 3
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
        addMoreButton.setOnClickListener { addMorePhotos() }
        backButton.setOnClickListener { onBackPressed() }
        backPreviewButton.setOnClickListener { retakePhoto() }
        normalPhotoSwitch.setOnCheckedChangeListener { _, isChecked ->
            normalPhotoMode = isChecked
        }
        
        // Set up flash button
        flashButton.setOnClickListener {
            toggleFlash()
        }
        
        // Set up touch listener for focus
        viewFinder.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                focusOnPoint(event.x, event.y)
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
        
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
            
            // Image capture with optimized settings for speed
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Changed for speed
                .setJpegQuality(85) // Slightly reduce quality for faster processing
                .build()
            
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        // Prevent multiple simultaneous photo captures
        if (isProcessingPhoto) {
            Toast.makeText(this, R.string.processing_previous_photo, Toast.LENGTH_SHORT).show()
            return
        }
        
        val imageCapture = imageCapture ?: return
        
        try {
            // Update the defect category in the item data
            itemData.defectCategory = defectCategory
            
            // Determine if we need to create a new file with index
            val needsIndex = currentPhotoIndex > 0
            
            // Create temporary output file (quick operation)
            val tempFile = if (needsIndex) {
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
            
            if (tempFile == null) {
                Toast.makeText(this, R.string.error_create_directory, Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d(TAG, "Taking photo to: ${tempFile.absolutePath}")
            
            // Create output options object
            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
            
            // Mark as processing and disable capture button
            isProcessingPhoto = true
            captureButton.isEnabled = false
            
            // Show immediate feedback
            showCaptureAnimation()
            
            // Show processing indicator
            processingIndicator.visibility = View.VISIBLE
            processingText.text = getString(R.string.photo_captured_processing)
            
            // Set up image capture listener
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        runOnUiThread {
                            // Hide processing indicator
                            processingIndicator.visibility = View.GONE
                            
                            // Re-enable capture button
                            isProcessingPhoto = false
                            captureButton.isEnabled = true
                            
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                            Toast.makeText(this@CameraActivity, R.string.error_photo_processing, Toast.LENGTH_LONG).show()
                            
                            tempFile.delete()
                        }
                    }
                    
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // Photo is captured! Now process it asynchronously
                        processPhotoAsync(tempFile)
                    }

                }
            )
        } catch (e: Exception) {
            // Hide processing indicator
            processingIndicator.visibility = View.GONE
            
            isProcessingPhoto = false
            captureButton.isEnabled = true
            Log.e(TAG, "Error taking photo", e)
            Toast.makeText(this, R.string.error_photo_processing, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Show immediate visual feedback that photo was captured
     */
    private fun showCaptureAnimation() {
        // Create a quick flash effect to show photo was taken
        val flashOverlay = View(this).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0f
        }
        
        val container = findViewById<FrameLayout>(R.id.camera_container)
        container.addView(flashOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // Animate the flash
        flashOverlay.animate()
            .alpha(0.7f)
            .setDuration(100)
            .withEndAction {
                flashOverlay.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction {
                        container.removeView(flashOverlay)
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Process the captured photo asynchronously in background thread
     */
    private fun processPhotoAsync(tempFile: File) {
        processingScope.launch {
            try {
                // Update processing text on main thread
                withContext(Dispatchers.Main) {
                    processingText.text = getString(R.string.processing_previous_photo)
                }
                
                // Verify file exists and has non-zero size
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw IOException("File was not created properly")
                }
                
                Log.d(TAG, "Processing photo: ${tempFile.absolutePath}")
                
                // Read and fix orientation (heavy operation in background)
                val bitmap = ImageUtils.fixPhotoOrientation(tempFile.absolutePath)
                if (bitmap == null) {
                    throw IOException("Failed to decode bitmap from file")
                }
                
                // Create marked version (heavy operation in background)
                val markedBitmap: Bitmap = if (normalPhotoMode) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    ImageUtils.addCircleToImage(bitmap, selectedCircleColor, selectedCircleText)
                }
                
                // Create file path for marked version
                val originalPath = tempFile.absolutePath
                val markedPath = originalPath.replace(".jpg", "_marked.jpg")
                val markedFile = File(markedPath)
                
                // Save the marked version (heavy operation in background)
                val markedSaved = FileUtils.saveBitmapToFile(markedBitmap, markedFile)
                
                if (!markedSaved) {
                    throw IOException("Failed to save marked image")
                }
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Hide processing indicator
                    processingIndicator.visibility = View.GONE
                    
                    // Add the paths to the ItemData
                    if (isBoxPhoto) {
                        itemData.addBoxPhoto(originalPath, markedPath)
                    } else {
                        itemData.addProductPhoto(originalPath, markedPath)
                    }
                    
                    // Increment the photo index for next photo
                    currentPhotoIndex++
                    
                    // Update the photo count display
                    updatePhotoCountDisplay()
                    
                    Log.d(TAG, "Photo processed successfully")
                    Log.d(TAG, "Original: $originalPath")
                    Log.d(TAG, "Marked: $markedPath")
                    
                    // Store references for preview
                    outputFile = markedFile
                    outputUri = Uri.fromFile(markedFile)
                    
                    // Show preview immediately
                    showImagePreview()
                    
                    // Re-enable capture button
                    isProcessingPhoto = false
                    captureButton.isEnabled = true
                    
                    Toast.makeText(this@CameraActivity, R.string.photo_ready, Toast.LENGTH_SHORT).show()
                }
                
                // Clean up bitmaps
                bitmap.recycle()
                markedBitmap.recycle()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing photo", e)
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Hide processing indicator
                    processingIndicator.visibility = View.GONE
                    
                    Toast.makeText(this@CameraActivity, R.string.error_photo_processing, Toast.LENGTH_LONG).show()
                    
                    // Clean up
                    tempFile.delete()
                    isProcessingPhoto = false
                    captureButton.isEnabled = true
                }
            }
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
    
    /**
     * Focus camera on a specific point on the preview
     */
    private fun focusOnPoint(x: Float, y: Float) {
        val camera = camera ?: return
        
        // Convert the tap coordinates to a MeteringPoint
        val meteringPoint = viewFinder.meteringPointFactory.createPoint(x, y)
        
        // Create a MeteringAction with auto-focus, auto-exposure, and auto-white-balance
        val action = FocusMeteringAction.Builder(meteringPoint)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        // Start the focus animation
        showFocusPoint(x, y)
        
        // Execute the focus action
        camera.cameraControl.startFocusAndMetering(action)
            .addListener({
                // Focus completed
                hideFocusPoint()
            }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * Show focus animation at the tapped point
     */
    private fun showFocusPoint(x: Float, y: Float) {
        // Create and add a focus indicator to the UI
        val focusIndicator = View(this).apply {
            id = View.generateViewId()
            background = ContextCompat.getDrawable(this@CameraActivity, R.drawable.focus_circle)
            elevation = 10f
            alpha = 0.7f
        }
        
        // Add the indicator to the layout
        val container = findViewById<FrameLayout>(R.id.camera_container)
        container.addView(focusIndicator)
        
        // Position the indicator
        focusIndicator.post {
            val size = resources.getDimensionPixelSize(R.dimen.focus_circle_size)
            focusIndicator.x = x - (size / 2)
            focusIndicator.y = y - (size / 2)
            focusIndicator.layoutParams = FrameLayout.LayoutParams(size, size)
            
            // Animate the indicator
            focusIndicator.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(100)
                .withEndAction {
                    focusIndicator.animate()
                        .scaleX(1.0f).scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
        
        // Remove the indicator after a delay
        focusIndicator.postDelayed({
            container.removeView(focusIndicator)
        }, 1000)
    }
    
    /**
     * Hide focus animation
     */
    private fun hideFocusPoint() {
        // This will be called when focus is complete
        // The focus point view will be removed by the delayed post in showFocusPoint
    }
    
    /**
     * Toggle flash on/off
     */
    private fun toggleFlash() {
        val cam = camera ?: return
        
        try {
            // Get current torch state
            val torchState = cam.cameraInfo.torchState.value
            val isCurrentlyOn = torchState == TorchState.ON
            
            // Toggle to opposite state
            val newTorchState = !isCurrentlyOn
            
            // Enable or disable torch
            cam.cameraControl.enableTorch(newTorchState)
            
            // Update flash button icon
            val flashIcon = if (newTorchState) {
                R.drawable.ic_flash_on
            } else {
                R.drawable.ic_flash_off
            }
            flashButton.setImageResource(flashIcon)
            
            // Show feedback to user
            val flashMessage = if (newTorchState) {
                getString(R.string.flash_on)
            } else {
                getString(R.string.flash_off)
            }
            Toast.makeText(this, flashMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flash", e)
            Toast.makeText(this, R.string.error_flash_not_available, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        processingScope.cancel() // Cancel all background operations
    }
    
    companion object {
        private const val TAG = "CameraActivity"
    }
}