package com.warehouse.camera.ui.base

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.warehouse.camera.R
import com.warehouse.camera.utils.LanguageUtils

/**
 * Base activity class that automatically applies the selected language and animations
 * All activities should extend this class instead of AppCompatActivity
 * Provides automatic back button support for all activities
 */
open class BaseActivity : AppCompatActivity() {
    
    // Back button reference
    protected var backButton: android.widget.ImageButton? = null
    
    override fun attachBaseContext(newBase: Context) {
        // Apply selected language before the activity's view is created
        val language = LanguageUtils.getSelectedLanguage(newBase)
        val context = LanguageUtils.createConfigurationContext(newBase, language)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lock screen orientation to portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        // Automatically setup back button if it exists in the layout
        setupBackButton()
    }
    
    override fun setContentView(view: android.view.View?) {
        super.setContentView(view)
        // Automatically setup back button if it exists in the layout
        setupBackButton()
    }
    
    /**
     * Automatically find and setup back button in the layout
     */
    protected open fun setupBackButton() {
        // Try to find common back button IDs
        val backButtonIds = arrayOf(
            R.id.button_back
        )
        
        for (id in backButtonIds) {
            try {
                val button = findViewById<android.widget.ImageButton>(id)
                if (button != null) {
                    backButton = button
                    setupBackButtonClickListener(button)
                    break
                }
            } catch (e: Exception) {
                // ID not found, continue
            }
        }
    }
    
    /**
     * Setup click listener for back button
     */
    protected open fun setupBackButtonClickListener(button: android.widget.ImageButton) {
        button.setOnClickListener {
            onBackButtonPressed()
        }
    }
    
    /**
     * Handle back button click - can be overridden by subclasses
     */
    protected open fun onBackButtonPressed() {
        onBackPressed()
    }
    
    override fun onResume() {
        super.onResume()
        // Re-apply language when activity is resumed
        LanguageUtils.applyLanguage(this)
        // Ensure orientation remains locked to portrait even after app resumes
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    
    /**
     * Set up toolbar with back button
     */
    protected fun setupToolbar(toolbar: Toolbar, showBackButton: Boolean = true, title: String? = null) {
        setSupportActionBar(toolbar)
        
        // Set title if provided
        if (title != null) {
            supportActionBar?.title = title
        }
        
        // Enable back button if requested
        if (showBackButton) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
    }
    
    /**
     * Apply slide animations to start activity
     */
    protected fun startActivityWithAnimation(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    
    /**
     * Handle back button navigation
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Handle back button click
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    /**
     * Apply slide animations when finishing activity
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
    
    /**
     * Apply slide animations when back pressed
     */
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}