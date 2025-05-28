package com.warehouse.camera.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.DefectDetails
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.ui.gallery.GalleryBrowserActivity
import com.warehouse.camera.utils.FileUtils
import com.warehouse.camera.utils.LanguageUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class GalleryActivity : AppCompatActivity() {
    
    private lateinit var articleInfoTextView: TextView
    private lateinit var boxPhotoImageView: ImageView
    private lateinit var noBoxPhotoTextView: TextView
    private lateinit var productPhotoImageView: ImageView
    private lateinit var noProductPhotoTextView: TextView
    private lateinit var itemDetailsTextView: TextView
    private lateinit var deleteButton: Button
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var defectDetails: DefectDetails
    private lateinit var itemData: ItemData
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_gallery)
        
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
        
        defectDetails = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("defectDetails", DefectDetails::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("defectDetails")
        } ?: throw IllegalStateException("DefectDetails must be provided")
        
        itemData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("itemData", ItemData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("itemData")
        } ?: throw IllegalStateException("ItemData must be provided")
        
        // Initialize views
        articleInfoTextView = findViewById(R.id.textView_article_info)
        boxPhotoImageView = findViewById(R.id.imageView_box_photo)
        noBoxPhotoTextView = findViewById(R.id.textView_no_box_photo)
        productPhotoImageView = findViewById(R.id.imageView_product_photo)
        noProductPhotoTextView = findViewById(R.id.textView_no_product_photo)
        itemDetailsTextView = findViewById(R.id.textView_item_details)
        deleteButton = findViewById(R.id.button_delete_item)
        
        // Set article info
        articleInfoTextView.text = getString(
            R.string.gallery_article_info,
            manufacturerInfo.manufacturerCode,
            manufacturerInfo.date,
            itemData.fullArticleCode
        )
        
        // Load box photo if available (use the first one from the list if available)
        val boxPhotoPath = if (itemData.boxPhotoPaths.isNotEmpty()) {
            itemData.boxPhotoPaths[0]
        } else {
            itemData.boxPhotoPath
        }
        loadPhoto(boxPhotoPath, boxPhotoImageView, noBoxPhotoTextView)
        
        // Set click listener for box photo to open viewer or list if multiple photos
        boxPhotoImageView.setOnClickListener {
            val allBoxPhotos = getAllBoxPhotos(itemData)
            if (allBoxPhotos.size > 1) {
                // Open gallery browser with all box photos (including marked ones)
                val intent = Intent(this, GalleryBrowserActivity::class.java)
                intent.putStringArrayListExtra("photosList", ArrayList(allBoxPhotos))
                intent.putExtra("photoType", getString(R.string.gallery_box_photo))
                startActivity(intent)
            } else if (allBoxPhotos.isNotEmpty() && File(allBoxPhotos[0]).exists()) {
                // Open single photo viewer
                openImageViewer(allBoxPhotos[0])
            }
        }
        
        // Load product photo if available (use the first one from the list if available)
        val productPhotoPath = if (itemData.productPhotoPaths.isNotEmpty()) {
            itemData.productPhotoPaths[0]
        } else {
            itemData.productPhotoPath
        }
        loadPhoto(productPhotoPath, productPhotoImageView, noProductPhotoTextView)
        
        // Set click listener for product photo to open viewer or list if multiple photos
        productPhotoImageView.setOnClickListener {
            val allProductPhotos = getAllProductPhotos(itemData)
            if (allProductPhotos.size > 1) {
                // Open gallery browser with all product photos (including marked ones)
                val intent = Intent(this, GalleryBrowserActivity::class.java)
                intent.putStringArrayListExtra("photosList", ArrayList(allProductPhotos))
                intent.putExtra("photoType", getString(R.string.gallery_product_photo))
                startActivity(intent)
            } else if (allProductPhotos.isNotEmpty() && File(allProductPhotos[0]).exists()) {
                // Open single photo viewer
                openImageViewer(allProductPhotos[0])
            }
        }
        
        // Load text file details
        loadTextDetails()
        
        // Set click listener for delete button
        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }
    
    private fun openImageViewer(imagePath: String) {
        val intent = Intent(this, ImageViewerActivity::class.java)
        intent.putExtra("imagePath", imagePath)
        startActivity(intent)
    }
    
    private fun loadPhoto(photoPath: String?, imageView: ImageView, noPhotoTextView: TextView) {
        if (photoPath != null && File(photoPath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(photoPath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    noPhotoTextView.visibility = View.GONE
                    
                    Log.d(TAG, "Loaded photo from: $photoPath")
                } else {
                    imageView.visibility = View.GONE
                    noPhotoTextView.visibility = View.VISIBLE
                    
                    Log.e(TAG, "Failed to decode bitmap from: $photoPath")
                }
            } catch (e: Exception) {
                imageView.visibility = View.GONE
                noPhotoTextView.visibility = View.VISIBLE
                
                Log.e(TAG, "Error loading photo: ${e.message}", e)
            }
        } else {
            imageView.visibility = View.GONE
            noPhotoTextView.visibility = View.VISIBLE
            
            Log.d(TAG, "Photo not found: $photoPath")
        }
    }
    
    private fun loadTextDetails() {
        try {
            // Create expected path for text file
            val photoPath = itemData.boxPhotoPath ?: itemData.productPhotoPath
            if (photoPath == null) {
                itemDetailsTextView.text = getString(R.string.no_details_available)
                return
            }
            
            val photoFile = File(photoPath)
            val parentDir = photoFile.parentFile
            val textFile = File(parentDir, "${itemData.fullArticleCode}.txt")
            
            if (textFile.exists()) {
                val content = StringBuilder()
                BufferedReader(FileReader(textFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        content.append(line).append("\n")
                    }
                }
                
                itemDetailsTextView.text = content.toString()
                Log.d(TAG, "Loaded text details from: ${textFile.absolutePath}")
            } else {
                // If there's no text file yet, show defect details anyway
                val content = """
                    Артикул: ${itemData.fullArticleCode}
                    Причина: ${defectDetails.reason}
                    Шаблон: ${defectDetails.template}
                    Описание: ${defectDetails.description}
                """.trimIndent()
                
                itemDetailsTextView.text = content
                Log.d(TAG, "Using default defect details (no text file found)")
            }
        } catch (e: Exception) {
            itemDetailsTextView.text = getString(R.string.error_loading_details)
            Log.e(TAG, "Error loading text details: ${e.message}", e)
        }
    }
    
    /**
     * Показать диалог подтверждения удаления всей папки с данным элементом
     */
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.confirm_delete_folder, itemData.fullArticleCode))
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                dialog.dismiss()
                deleteItemFolder()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Удалить папку с элементом и всеми его файлами
     */
    private fun deleteItemFolder() {
        try {
            // Определяем путь к папке элемента на основе пути к фото
            val photoPath = itemData.boxPhotoPath ?: itemData.productPhotoPath
            
            if (photoPath == null) {
                Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show()
                return
            }
            
            // Получаем папку элемента (родительскую директорию фото)
            val photoFile = File(photoPath)
            val itemFolder = photoFile.parentFile
            
            if (itemFolder != null && itemFolder.exists()) {
                // Используем метод FileUtils для рекурсивного удаления папки
                val success = FileUtils.deleteFileOrDirectory(itemFolder)
                
                if (success) {
                    Toast.makeText(this, R.string.delete_success, Toast.LENGTH_SHORT).show()
                    // Закрываем активность после успешного удаления
                    finish()
                } else {
                    Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Item folder does not exist: ${itemFolder?.absolutePath}")
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.delete_error, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error deleting item folder", e)
        }
    }
    
    /**
     * Получить все фотографии коробки (включая маркированные)
     */
    private fun getAllBoxPhotos(itemData: ItemData): List<String> {
        val allPhotos = mutableListOf<String>()
        
        // Добавляем все обычные фотографии коробки
        allPhotos.addAll(itemData.boxPhotoPaths.filter { File(it).exists() })
        
        // Добавляем все маркированные фотографии коробки  
        allPhotos.addAll(itemData.boxPhotoMarkedPaths.filter { File(it).exists() })
        
        // Если списки пустые, проверяем старые одиночные поля для обратной совместимости
        if (allPhotos.isEmpty()) {
            itemData.boxPhotoPath?.let { if (File(it).exists()) allPhotos.add(it) }
            itemData.boxPhotoMarkedPath?.let { if (File(it).exists()) allPhotos.add(it) }
        }
        
        return allPhotos
    }
    
    /**
     * Получить все фотографии продукта (включая маркированные)
     */
    private fun getAllProductPhotos(itemData: ItemData): List<String> {
        val allPhotos = mutableListOf<String>()
        
        // Добавляем все обычные фотографии продукта
        allPhotos.addAll(itemData.productPhotoPaths.filter { File(it).exists() })
        
        // Добавляем все маркированные фотографии продукта
        allPhotos.addAll(itemData.productPhotoMarkedPaths.filter { File(it).exists() })
        
        // Если списки пустые, проверяем старые одиночные поля для обратной совместимости
        if (allPhotos.isEmpty()) {
            itemData.productPhotoPath?.let { if (File(it).exists()) allPhotos.add(it) }
            itemData.productPhotoMarkedPath?.let { if (File(it).exists()) allPhotos.add(it) }
        }
        
        return allPhotos
    }
    
    companion object {
        private const val TAG = "GalleryActivity"
    }
}