package com.warehouse.camera.ui

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.warehouse.camera.R
import com.warehouse.camera.model.GalleryItem
import com.warehouse.camera.utils.FileUtils
import java.io.File

class ImageViewerActivity : AppCompatActivity() {
    
    private lateinit var photoView: PhotoView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabDelete: FloatingActionButton
    private var currentGalleryItem: GalleryItem? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        photoView = findViewById(R.id.photoView)
        progressBar = findViewById(R.id.progressBar)
        fabDelete = findViewById(R.id.fab_delete)
        
        // Get gallery item from intent
        val galleryItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("galleryItem", GalleryItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("galleryItem")
        }
        
        if (galleryItem != null && galleryItem.isImage) {
            currentGalleryItem = galleryItem
            loadImage(galleryItem.path)
            setupDeleteButton()
        } else {
            Toast.makeText(this, R.string.no_photo_available, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun loadImage(imagePath: String) {
        progressBar.visibility = View.VISIBLE
        
        try {
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                // Load image
                val bitmap = BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    photoView.setImageBitmap(bitmap)
                    Log.d(TAG, "Image loaded successfully: $imagePath")
                } else {
                    Toast.makeText(this, R.string.error_loading_details, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to decode bitmap: $imagePath")
                }
            } else {
                Toast.makeText(this, R.string.no_photo_available, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Image file doesn't exist: $imagePath")
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_loading_details, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error loading image: ${e.message}", e)
        } finally {
            progressBar.visibility = View.GONE
        }
    }
    
    private fun setupDeleteButton() {
        fabDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удалить фотографию")
            .setMessage("Вы уверены, что хотите удалить эту фотографию? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deletePhoto()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun deletePhoto() {
        val galleryItem = currentGalleryItem ?: return
        
        try {
            val photoFile = File(galleryItem.path)
            
            // Попытка удалить файл
            val deleted = FileUtils.deleteFileOrDirectory(photoFile, this)
            
            if (deleted) {
                Log.d(TAG, "Photo deleted successfully: ${galleryItem.path}")
                Toast.makeText(this, "Фотография удалена", Toast.LENGTH_SHORT).show()
                
                // Возврат с результатом, чтобы родительская Activity обновилась
                setResult(RESULT_OK)
                finish()
            } else {
                Log.e(TAG, "Failed to delete photo: ${galleryItem.path}")
                Toast.makeText(this, "Ошибка при удалении фотографии", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting photo", e)
            Toast.makeText(this, "Ошибка при удалении: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        private const val TAG = "ImageViewerActivity"
    }
}
