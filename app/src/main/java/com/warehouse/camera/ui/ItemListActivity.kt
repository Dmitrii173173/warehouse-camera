package com.warehouse.camera.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.MainActivity
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.DefectDetails
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.utils.FileUtils
import com.warehouse.camera.utils.LanguageUtils

class ItemListActivity : AppCompatActivity(), ItemAdapter.ItemActionsListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var completeButton: Button
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var defectDetails: DefectDetails
    private lateinit var items: ArrayList<ItemData>
    
    private lateinit var adapter: ItemAdapter
    
    private var currentItemPosition = -1
    private var isBoxPhoto = false
    
    private val takePictureResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Get updated item data
            val updatedItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("updatedItemData", ItemData::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("updatedItemData")
            }
            
            if (updatedItem != null && currentItemPosition >= 0 && currentItemPosition < items.size) {
                // Update the item in the list
                val item = items[currentItemPosition]
                if (isBoxPhoto) {
                    item.boxPhotoPath = updatedItem.boxPhotoPath
                } else {
                    item.productPhotoPath = updatedItem.productPhotoPath
                }
                // Update the current item in the adapter
                adapter.notifyItemChanged(currentItemPosition)
                
                // Check if we need to move to the next item
                val moveToNext = result.data?.getBooleanExtra("moveToNextItem", false) ?: false
                if (moveToNext) {
                    // Automatically save the current item
                    onSaveItem(currentItemPosition)
                    
                    // Move to the next item if available
                    if (currentItemPosition + 1 < items.size) {
                        val nextPosition = currentItemPosition + 1
                        // Delay a bit to ensure the save completes
                        recyclerView.postDelayed({
                            if (updatedItem.boxPhotoPath == null) {
                                onTakeBoxPhoto(nextPosition)
                            } else {
                                onTakeProductPhoto(nextPosition)
                            }
                        }, 500) // 500ms delay
                    } else {
                        Toast.makeText(this, R.string.no_more_items, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved language
        LanguageUtils.applyLanguage(this)
        
        setContentView(R.layout.activity_item_list)
        
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
        
        items = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("items", ItemData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("items")
        } ?: throw IllegalStateException("Items must be provided")
        
        // Initialize views
        recyclerView = findViewById(R.id.recyclerView_items)
        completeButton = findViewById(R.id.button_complete)
        
        // Setup RecyclerView
        adapter = ItemAdapter(this, items, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Complete button click
        completeButton.setOnClickListener {
            // Check if all items are completed
            if (items.all { it.isCompleted }) {
                // Return to ManufacturerInfoActivity
                val intent = Intent(this, ManufacturerInfoActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please complete documenting all items", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onTakeBoxPhoto(position: Int) {
        currentItemPosition = position
        isBoxPhoto = true
        
        val item = items[position]
        
        // Launch camera activity
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("manufacturerInfo", manufacturerInfo)
        intent.putExtra("articleInfo", articleInfo)
        intent.putExtra("itemData", item)
        intent.putExtra("isBoxPhoto", true)
        takePictureResult.launch(intent)
    }
    
    override fun onTakeProductPhoto(position: Int) {
        currentItemPosition = position
        isBoxPhoto = false
        
        val item = items[position]
        
        // Launch camera activity
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("manufacturerInfo", manufacturerInfo)
        intent.putExtra("articleInfo", articleInfo)
        intent.putExtra("itemData", item)
        intent.putExtra("isBoxPhoto", false)
        takePictureResult.launch(intent)
    }
    
    override fun onSaveItem(position: Int) {
        val item = items[position]
        
        // Save text file
        val success = FileUtils.saveTextFile(
            this,
            manufacturerInfo,
            articleInfo,
            defectDetails,
            item
        )
        
        if (success) {
            // Mark item as completed
            item.isCompleted = true
            adapter.notifyItemChanged(position)
            
            Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.error_save_text, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onViewInGallery(position: Int) {
        val item = items[position]
        
        // Only open gallery if at least one photo exists
        if (item.boxPhotoPath != null || item.productPhotoPath != null) {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("manufacturerInfo", manufacturerInfo)
            intent.putExtra("articleInfo", articleInfo)
            intent.putExtra("defectDetails", defectDetails)
            intent.putExtra("itemData", item)
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.no_photo_available, Toast.LENGTH_SHORT).show()
        }
    }
}
