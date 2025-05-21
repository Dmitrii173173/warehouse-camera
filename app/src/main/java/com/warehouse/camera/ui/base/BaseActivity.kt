package com.warehouse.camera.ui.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.warehouse.camera.utils.LanguageUtils

/**
 * Base activity class that automatically applies the selected language
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
}