package com.warehouse.camera.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.warehouse.camera.R
import com.warehouse.camera.model.ArticleInfo
import com.warehouse.camera.model.DefectDetails
import com.warehouse.camera.model.ItemData
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.ui.base.BaseActivity

class DefectDetailsActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var reasonSpinner: Spinner
    private lateinit var templateSpinner: Spinner
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var nextButton: Button
    
    private lateinit var manufacturerInfo: ManufacturerInfo
    private lateinit var articleInfo: ArticleInfo
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_defect_details)
        
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
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        reasonSpinner = findViewById(R.id.spinner_reason)
        templateSpinner = findViewById(R.id.spinner_template)
        descriptionEditText = findViewById(R.id.editText_description)
        nextButton = findViewById(R.id.button_next)
        
        // Setup toolbar with back button
        setupToolbar(toolbar)
        
        // Setup spinners
        setupReasonSpinner()
        setupTemplateSpinner()
        
        // Next button click
        nextButton.setOnClickListener {
            if (validateInputs()) {
                val reason = reasonSpinner.selectedItem.toString()
                val template = templateSpinner.selectedItem.toString()
                val description = descriptionEditText.text.toString()
                
                val defectDetails = DefectDetails(
                    reason = reason,
                    template = template,
                    description = description
                )
                
                // Create list of items based on quantity
                val items = createItemList(articleInfo)
                
                // Navigate to item list screen
                val intent = Intent(this, ItemListActivity::class.java)
                intent.putExtra("manufacturerInfo", manufacturerInfo)
                intent.putExtra("articleInfo", articleInfo)
                intent.putExtra("defectDetails", defectDetails)
                intent.putParcelableArrayListExtra("items", ArrayList(items))
                startActivityWithAnimation(intent)
            }
        }
    }
    
    private fun setupReasonSpinner() {
        val reasons = arrayOf(
            getString(R.string.reason_damage),
            getString(R.string.reason_factory_dialect),
            getString(R.string.reason_invalid_attachment),
            getString(R.string.reason_others),
            getString(R.string.reason_returned_to_supplier),
            getString(R.string.reason_short_delivery),
            getString(R.string.reason_undercarriage)
        )
        
        // Create a custom adapter for better styling
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            reasons
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setPadding(24, 16, 24, 16)
                return view
            }
        }
        
        reasonSpinner.adapter = adapter
        
        // Add a selection listener
        reasonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Apply style to selected item
                val selectedItem = parent?.getChildAt(0) as? TextView
                selectedItem?.setTextColor(resources.getColor(R.color.colorTextPrimary, theme))
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing to do here
            }
        }
    }
    
    private fun setupTemplateSpinner() {
        val templates = arrayOf(
            getString(R.string.template_broken),
            getString(R.string.template_damage_during_transportation),
            getString(R.string.template_damage_during_unloading),
            getString(R.string.template_deformation),
            getString(R.string.template_dent),
            getString(R.string.template_mechanical_damage),
            getString(R.string.template_others),
            getString(R.string.template_packing_error),
            getString(R.string.template_returned_as_resorted),
            getString(R.string.template_returned_due_to_marriage),
            getString(R.string.template_scratch),
            getString(R.string.template_scuffs),
            getString(R.string.template_chip)
        )
        
        // Create a custom adapter for better styling
        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item,
            templates
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                view.setPadding(24, 16, 24, 16)
                return view
            }
        }
        
        templateSpinner.adapter = adapter
        
        // Add a selection listener
        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Apply style to selected item
                val selectedItem = parent?.getChildAt(0) as? TextView
                selectedItem?.setTextColor(resources.getColor(R.color.colorTextPrimary, theme))
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nothing to do here
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val description = descriptionEditText.text.toString()
        
        // Проверка выбранных элементов в spinner
        if (reasonSpinner.selectedItemPosition == 0 && reasonSpinner.selectedItem.toString().isEmpty()) {
            Toast.makeText(this, R.string.error_select_reason, Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (templateSpinner.selectedItemPosition == 0 && templateSpinner.selectedItem.toString().isEmpty()) {
            Toast.makeText(this, R.string.error_select_template, Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Description is not required but can't be just whitespace if entered
        if (description.isNotBlank() && description.trim().isEmpty()) {
            Toast.makeText(this, R.string.error_valid_description, Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun createItemList(articleInfo: ArticleInfo): List<ItemData> {
        val items = mutableListOf<ItemData>()
        
        for (i in 1..articleInfo.quantity) {
            items.add(
                ItemData(
                    articleCode = articleInfo.articleCode,
                    index = i
                )
            )
        }
        
        return items
    }
}