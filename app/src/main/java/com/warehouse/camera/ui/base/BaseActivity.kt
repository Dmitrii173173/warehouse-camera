package com.warehouse.camera.ui.base

import android.content.Context
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
    
    override fun onResume() {
        super.onResume()
        // Re-apply language when activity is resumed
        LanguageUtils.applyLanguage(this)
    }
}