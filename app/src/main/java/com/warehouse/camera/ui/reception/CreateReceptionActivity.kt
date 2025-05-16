package com.warehouse.camera.ui.reception

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.warehouse.camera.R
import com.warehouse.camera.model.project.ProductReception
import com.warehouse.camera.model.project.ProductReceptionRepository
import com.warehouse.camera.ui.base.BaseActivity
import com.warehouse.camera.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*

class CreateReceptionActivity : BaseActivity() {
    
    private lateinit var manufacturerCodeEditText: TextInputEditText
    private lateinit var dateButton: Button
    private lateinit var createButton: Button
    
    private lateinit var receptionRepository: ProductReceptionRepository
    private var selectedDate: Date? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_reception)
        
        manufacturerCodeEditText = findViewById(R.id.editText_manufacturer_code)
        dateButton = findViewById(R.id.button_date)
        createButton = findViewById(R.id.button_create)
        
        receptionRepository = ProductReceptionRepository(this)
        
        // Set current date as default
        selectedDate = Calendar.getInstance().time
        updateDateButton()
        
        // Setup date button
        dateButton.setOnClickListener {
            showDatePicker()
        }
        
        // Setup create button
        createButton.setOnClickListener {
            if (validateInputs()) {
                createReception()
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDate != null) {
            calendar.time = selectedDate!!
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = calendar.time
                updateDateButton()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun updateDateButton() {
        if (selectedDate != null) {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            dateButton.text = dateFormat.format(selectedDate!!)
        }
    }
    
    private fun validateInputs(): Boolean {
        val manufacturerCode = manufacturerCodeEditText.text.toString().trim()
        
        if (manufacturerCode.length != 4 || !manufacturerCode.all { it.isDigit() }) {
            Toast.makeText(this, R.string.error_invalid_manufacturer_code, Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (selectedDate == null) {
            Toast.makeText(this, R.string.error_invalid_date, Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun createReception() {
        val manufacturerCode = manufacturerCodeEditText.text.toString().trim()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val dateString = dateFormat.format(selectedDate!!)
        
        val reception = ProductReception(
            manufacturerCode = manufacturerCode,
            date = dateString
        )
        
        if (receptionRepository.addReception(reception)) {
            Toast.makeText(this, R.string.reception_created, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.reception_already_exists, Toast.LENGTH_SHORT).show()
        }
    }
}