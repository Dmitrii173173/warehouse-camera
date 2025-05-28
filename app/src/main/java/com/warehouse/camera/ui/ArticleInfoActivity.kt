package com.warehouse.camera.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.ui.scanner.BarcodeScannerActivity

class ArticleInfoActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var articleCodeEditText: TextInputEditText
    private lateinit var quantityEditText: TextInputEditText
    private lateinit var defectCategoryRadioGroup: RadioGroup
    private lateinit var nextButton: Button
    private lateinit var nextTopButton: Button
    private lateinit var scanBarcodeButton: Button
    
    private lateinit var barcodeScannerLauncher: ActivityResultLauncher<Intent>
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_info)
        
        // Get manufacturer info from intent
        manufacturerInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("manufacturerInfo", ManufacturerInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("manufacturerInfo")
        } ?: throw IllegalStateException("ManufacturerInfo must be provided")
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        articleCodeEditText = findViewById(R.id.editText_article_code)
        quantityEditText = findViewById(R.id.editText_quantity)
        defectCategoryRadioGroup = findViewById(R.id.radioGroup_defect_category)
        nextButton = findViewById(R.id.button_next)
        nextTopButton = findViewById(R.id.button_next_top)
        scanBarcodeButton = findViewById(R.id.button_scan_barcode)
        
        // Setup toolbar with back button
        setupToolbar(toolbar)
        
        // Ensure first radio button is checked by default
        if (defectCategoryRadioGroup.checkedRadioButtonId == -1) {
            val firstRadioButton = findViewById<RadioButton>(R.id.radioButton_category_1)
            firstRadioButton.isChecked = true
        }
        
        // Регистрируем обработчик результата сканирования
        barcodeScannerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedBarcode = result.data?.getStringExtra("scanned_barcode")
                if (scannedBarcode != null) {
                    articleCodeEditText.setText(scannedBarcode)
                    Toast.makeText(this, R.string.barcode_scanned_successfully, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Scan barcode button click
        scanBarcodeButton.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            barcodeScannerLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        
        // Next button click (both top and bottom buttons)
        val nextClickListener = View.OnClickListener {
            if (validateInputs()) {
                val articleCode = articleCodeEditText.text.toString()
                val quantity = quantityEditText.text.toString().toInt()
                val defectCategory = getSelectedDefectCategory()
                
                val articleInfo = ArticleInfo(
                    articleCode = articleCode,
                    quantity = quantity,
                    defectCategory = defectCategory
                )
                
                // Navigate to next screen
                val intent = Intent(this, DefectDetailsActivity::class.java)
                intent.putExtra("manufacturerInfo", manufacturerInfo)
                intent.putExtra("articleInfo", articleInfo)
                startActivityWithAnimation(intent)
            }
        }
        
        nextButton.setOnClickListener(nextClickListener)
        nextTopButton.setOnClickListener(nextClickListener)
    }
    
    private fun validateInputs(): Boolean {
        val articleCode = articleCodeEditText.text.toString()
        val quantityText = quantityEditText.text.toString()
        
        // Check article code
        if (articleCode.isBlank()) {
            Toast.makeText(this, R.string.error_invalid_article, Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check quantity
        if (quantityText.isBlank()) {
            Toast.makeText(this, R.string.error_invalid_quantity, Toast.LENGTH_SHORT).show()
            return false
        }
        
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity < 1 || quantity > 10) {
            Toast.makeText(this, R.string.error_invalid_quantity, Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check if a defect category is selected
        if (defectCategoryRadioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun getSelectedDefectCategory(): Int {
        val selectedId = defectCategoryRadioGroup.checkedRadioButtonId
        val radioButton = findViewById<RadioButton>(selectedId)
        
        return when (radioButton.id) {
            R.id.radioButton_category_1 -> 1 // Незначительные дефекты
            R.id.radioButton_category_2 -> 2 // Потертости
            R.id.radioButton_category_3 -> 3 // Бракованный
            else -> 1 // Default to 1 if none is selected (should not happen due to validation)
        }
    }
}