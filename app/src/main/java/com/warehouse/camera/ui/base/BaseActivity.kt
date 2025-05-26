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
 */
open class BaseActivity : AppCompatActivity() {
    
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