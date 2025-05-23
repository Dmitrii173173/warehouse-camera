package com.warehouse.camera.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.R
import com.warehouse.camera.model.GalleryItem
import com.warehouse.camera.ui.ImageViewerActivity
import java.io.File
import java.util.Date

class GalleryBrowserActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryBrowserAdapter
    private var photosList: ArrayList<String> = ArrayList()
    private var photoType: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery_browser)
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Get photos list from intent
        photosList = intent.getStringArrayListExtra("photosList") ?: ArrayList()
        photoType = intent.getStringExtra("photoType") ?: getString(R.string.gallery_image)
        
        // Set title
        supportActionBar?.title = "$photoType (${photosList.size})"
        
        recyclerView = findViewById(R.id.recyclerView_photos)
        setupRecyclerView()
    }
    
    private fun setupRecyclerView() {
        val galleryItems = photosList.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) {
                val type = if (path.contains("damage")) {
                    GalleryItem.ItemType.BOX_PHOTO
                } else {
                    GalleryItem.ItemType.PRODUCT_PHOTO
                }
                
                // Extract article code from the file name
                val articleCode = try {
                    val fileName = file.name
                    if (fileName.contains("-")) {
                        val parts = fileName.split("-")
                        if (parts.size >= 2) {
                            // Remove _marked suffix and .jpg extension if present
                            parts[1].replace("_marked", "").replace(".jpg", "")
                        } else {
                            "Unknown"
                        }
                    } else {
                        "Unknown"
                    }
                } catch (e: Exception) {
                    "Unknown"
                }
                
                GalleryItem(
                    file = file,
                    name = file.name,
                    path = path,
                    date = Date(file.lastModified()),
                    type = type,
                    articleCode = articleCode
                )
            } else null
        }
        
        adapter = GalleryBrowserAdapter(this, galleryItems) { item ->
            // Open image viewer when clicked
            val intent = Intent(this, ImageViewerActivity::class.java)
            intent.putExtra("galleryItem", item)
            startActivity(intent)
        }
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}