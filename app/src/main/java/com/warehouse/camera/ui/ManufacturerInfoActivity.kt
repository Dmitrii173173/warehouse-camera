package com.warehouse.camera.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.warehouse.camera.R
import com.warehouse.camera.model.ManufacturerInfo
import com.warehouse.camera.model.project.ProductReception
import com.warehouse.camera.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.*

class ManufacturerInfoActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var manufacturerCodeEditText: TextInputEditText
    private lateinit var datePickerButton: Button
    private lateinit var nextButton: Button
    
    private var selectedDate: String = ""
    private var reception: ProductReception? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manufacturer_info)
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        manufacturerCodeEditText = findViewById(R.id.editText_manufacturer_code)
        datePickerButton = findViewById(R.id.button_date_picker)
        nextButton = findViewById(R.id.button_next)
        
        // Setup toolbar with back button
        setupToolbar(toolbar)
        
        // Set default date to today
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        selectedDate = dateFormat.format(today.time)
        
        // Check if we have a reception from intent
        reception = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("reception", ProductReception::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("reception")
        }
        
        // Pre-fill fields if we have a reception
        if (reception != null) {
            manufacturerCodeEditText.setText(reception!!.manufacturerCode)
            selectedDate = reception!!.date
            datePickerButton.text = selectedDate
            manufacturerCodeEditText.isEnabled = false  // Prevent editing
            datePickerButton.isEnabled = false  // Prevent changing date
        } else {
            datePickerButton.text = selectedDate
        }
        
        // Date picker button click
        datePickerButton.setOnClickListener {
            showDatePicker()
        }
        
        // Next button click
        nextButton.setOnClickListener {
            if (validateInputs()) {
                val manufacturerCode = manufacturerCodeEditText.text.toString()
                
                val manufacturerInfo = ManufacturerInfo(
                    manufacturerCode = manufacturerCode,
                    date = selectedDate
                )
                
                // Navigate to next screen
                val intent = Intent(this, ArticleInfoActivity::class.java)
                intent.putExtra("manufacturerInfo", manufacturerInfo)
                startActivityWithAnimation(intent)
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Parse the current date to set the date picker
        try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = dateFormat.parse(selectedDate)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            // Use today's date as fallback
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                datePickerButton.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun validateInputs(): Boolean {
        val manufacturerCode = manufacturerCodeEditText.text.toString()
        
        // Check manufacturer code
        if (manufacturerCode.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_manufacturer_code, Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (manufacturerCode.length != 4 || !manufacturerCode.all { it.isDigit() }) {
            Toast.makeText(this, R.string.error_invalid_manufacturer_code, Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
}