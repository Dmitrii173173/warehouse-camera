package com.warehouse.camera.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.warehouse.camera.MainActivity
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.DefectDetails
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.utils.FileUtils

class ItemListActivity : BaseActivity(), ItemAdapter.ItemActionsListener {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var completeButton: Button
    
    private lateinit var category1Button: RadioButton
    private lateinit var category2Button: RadioButton
    private lateinit var category3Button: RadioButton
    
    private lateinit var category1Indicator: ImageView
    private lateinit var category2Indicator: ImageView
    private lateinit var category3Indicator: ImageView
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    private lateinit var defectDetails: DefectDetails
    private lateinit var items: ArrayList<ItemData>
    
    private lateinit var adapter: ItemAdapter
    
    private var currentItemPosition = -1
    private var isBoxPhoto = false
    private var selectedCategory = 1 // Default to category 1 (green)
    
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
                    item.boxPhotoMarkedPath = updatedItem.boxPhotoMarkedPath
                    item.boxPhotoPaths = updatedItem.boxPhotoPaths
                    item.boxPhotoMarkedPaths = updatedItem.boxPhotoMarkedPaths
                } else {
                    item.productPhotoPath = updatedItem.productPhotoPath
                    item.productPhotoMarkedPath = updatedItem.productPhotoMarkedPath
                    item.productPhotoPaths = updatedItem.productPhotoPaths
                    item.productPhotoMarkedPaths = updatedItem.productPhotoMarkedPaths
                }
                // Set the selected category
                item.defectCategory = selectedCategory
                
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
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView_items)
        completeButton = findViewById(R.id.button_complete)
        
        // Setup toolbar with back button
        setupToolbar(toolbar)
        
        // Initialize category buttons
        category1Button = findViewById(R.id.category_1_button)
        category2Button = findViewById(R.id.category_2_button)
        category3Button = findViewById(R.id.category_3_button)
        
        // Initialize category indicators
        category1Indicator = findViewById(R.id.category_1_selected_indicator)
        category2Indicator = findViewById(R.id.category_2_selected_indicator)
        category3Indicator = findViewById(R.id.category_3_selected_indicator)
        
        // Set initial category selection
        selectCategory(1)
        
        // Setup category button click listeners
        category1Button.setOnClickListener { selectCategory(1) }
        category2Button.setOnClickListener { selectCategory(2) }
        category3Button.setOnClickListener { selectCategory(3) }
        
        // Setup RecyclerView
        adapter = ItemAdapter(this, items, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Complete button click
        completeButton.setOnClickListener {
            // Check if all items are completed
            if (items.all { it.isCompleted }) {
                // Navigate to ArticleInfoActivity
                val intent = Intent(this, ArticleInfoActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Pass the manufacturerInfo so the flow can continue
                intent.putExtra("manufacturerInfo", manufacturerInfo)
                startActivityWithAnimation(intent)
                finish()
            } else {
                Toast.makeText(this, R.string.complete_all_items, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun selectCategory(category: Int) {
        selectedCategory = category
        
        // Reset all indicators
        category1Indicator.visibility = View.INVISIBLE
        category2Indicator.visibility = View.INVISIBLE
        category3Indicator.visibility = View.INVISIBLE
        
        // Reset all button animations
        resetButtonAnimations()
        
        // Apply selection based on category
        when (category) {
            1 -> {
                category1Button.isChecked = true
                category1Indicator.visibility = View.VISIBLE
                animateButtonSelection(category1Button)
            }
            2 -> {
                category2Button.isChecked = true
                category2Indicator.visibility = View.VISIBLE
                animateButtonSelection(category2Button)
            }
            3 -> {
                category3Button.isChecked = true
                category3Indicator.visibility = View.VISIBLE
                animateButtonSelection(category3Button)
            }
        }
        
        // If there's a current item, update its category
        if (currentItemPosition >= 0 && currentItemPosition < items.size) {
            items[currentItemPosition].defectCategory = category
            adapter.notifyItemChanged(currentItemPosition)
        }
    }
    
    private fun resetButtonAnimations() {
        category1Button.scaleX = 1.0f
        category1Button.scaleY = 1.0f
        category2Button.scaleX = 1.0f
        category2Button.scaleY = 1.0f
        category3Button.scaleX = 1.0f
        category3Button.scaleY = 1.0f
    }
    
    private fun animateButtonSelection(button: View) {
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1.0f, 1.1f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1.0f, 1.1f, 1.0f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }
    
    override fun onTakeBoxPhoto(position: Int) {
        currentItemPosition = position
        isBoxPhoto = true
        
        val item = items[position]
        
        // Update the selected category based on the item's category
        if (item.defectCategory > 0) {
            selectCategory(item.defectCategory)
        }
        
        // Launch camera activity
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("manufacturerInfo", manufacturerInfo)
        intent.putExtra("articleInfo", articleInfo)
        intent.putExtra("itemData", item)
        intent.putExtra("isBoxPhoto", true)
        intent.putExtra("defectCategory", selectedCategory)
        takePictureResult.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    override fun onTakeProductPhoto(position: Int) {
        currentItemPosition = position
        isBoxPhoto = false
        
        val item = items[position]
        
        // Update the selected category based on the item's category
        if (item.defectCategory > 0) {
            selectCategory(item.defectCategory)
        }
        
        // Launch camera activity
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra("manufacturerInfo", manufacturerInfo)
        intent.putExtra("articleInfo", articleInfo)
        intent.putExtra("itemData", item)
        intent.putExtra("isBoxPhoto", false)
        intent.putExtra("defectCategory", selectedCategory)
        takePictureResult.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    override fun onSaveItem(position: Int) {
        val item = items[position]
        
        // Make sure the item has the selected category
        item.defectCategory = selectedCategory
        
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
            startActivityWithAnimation(intent)
        } else {
            Toast.makeText(this, R.string.no_photo_available, Toast.LENGTH_SHORT).show()
        }
    }
}