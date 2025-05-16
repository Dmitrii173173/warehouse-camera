package com.warehouse.camera.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.GalleryItem
import com.warehouse.camera.ui.GalleryAdapter
import java.io.File
import java.util.Date

class GalleryBrowserActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_browser)
        
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyTextView = findViewById(R.id.textView_empty)
        
        // Set up RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        // Load gallery items
        loadGalleryItems()
    }
    
    private fun loadGalleryItems() {
        progressBar.visibility = View.VISIBLE
        
        // Get application files directory
        val filesDir = getExternalFilesDir(null) ?: filesDir
        
        // Load in background
        Thread {
            val galleryItems = mutableListOf<GalleryItem>()
            
            // Recursively scan directories
            scanDirectory(filesDir, galleryItems)
            
            // Update UI on main thread
            runOnUiThread {
                progressBar.visibility = View.GONE
                
                if (galleryItems.isEmpty()) {
                    emptyTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = GalleryAdapter(this, galleryItems)
                }
            }
        }.start()
    }
    
    private fun scanDirectory(directory: File, galleryItems: MutableList<GalleryItem>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanDirectory(file, galleryItems)
            } else {
                val galleryItem = createGalleryItem(file)
                if (galleryItem != null) {
                    galleryItems.add(galleryItem)
                }
            }
        }
    }
    
    private fun createGalleryItem(file: File): GalleryItem? {
        return try {
            val name = file.name
            val path = file.absolutePath
            val date = Date(file.lastModified())
            
            // Parse article code from filename (assuming format like "123456AA-1.jpg")
            val articleCode = if (name.contains("-")) {
                name.substringBefore("-")
            } else {
                name.substringBeforeLast(".")
            }
            
            // Determine type based on filename
            val type = when {
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") -> {
                    if (name.contains("-1")) {
                        GalleryItem.ItemType.BOX_PHOTO
                    } else if (name.contains("-2")) {
                        GalleryItem.ItemType.PRODUCT_PHOTO
                    } else {
                        GalleryItem.ItemType.UNKNOWN
                    }
                }
                name.endsWith(".txt") -> GalleryItem.ItemType.TEXT_FILE
                else -> GalleryItem.ItemType.UNKNOWN
            }
            
            GalleryItem(file, name, date, type, articleCode, path)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating gallery item for ${file.absolutePath}: ${e.message}", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "GalleryBrowserActivity"
    }
}
